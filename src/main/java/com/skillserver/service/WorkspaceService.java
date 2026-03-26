package com.skillserver.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillserver.common.exception.BadRequestException;
import com.skillserver.common.util.FileWorkspaceUtils;
import com.skillserver.common.util.HashUtils;
import com.skillserver.common.util.PreviewModeResolver;
import com.skillserver.config.SkillServerProperties;
import com.skillserver.domain.enums.ChangeType;
import com.skillserver.domain.enums.PreviewMode;
import com.skillserver.dto.skill.SkillFilePayload;
import com.skillserver.service.model.ChangeSummary;
import com.skillserver.service.model.ChangedFile;
import com.skillserver.service.model.ParsedSkillDocument;
import com.skillserver.service.model.SnapshotEntry;
import com.skillserver.service.model.SnapshotResult;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class WorkspaceService {

    private final SkillServerProperties properties;
    private final SkillMarkdownParser markdownParser;
    private final ObjectMapper objectMapper;

    @PostConstruct
    public void init() {
        FileWorkspaceUtils.ensureDirectory(properties.getWorkspace().getRoot());
        FileWorkspaceUtils.ensureDirectory(properties.getWorkspace().getPrivateSkillsDir());
        FileWorkspaceUtils.ensureDirectory(properties.getWorkspace().getMirrorsDir());
        FileWorkspaceUtils.ensureDirectory(properties.getWorkspace().getTempDir());
        FileWorkspaceUtils.ensureDirectory(properties.getWorkspace().getPackageDir());
        FileWorkspaceUtils.ensureDirectory(properties.getWorkspace().getSearchIndexDir());
    }

    public Path privateSkillPath(String skillUid) {
        return properties.getWorkspace().getPrivateSkillsDir().resolve(skillUid);
    }

    public Path repoMirrorPath(Long repoId) {
        return properties.getWorkspace().getMirrorsDir().resolve("repo-" + repoId);
    }

    public Path createTempDirectory(String prefix) {
        try {
            return Files.createTempDirectory(properties.getWorkspace().getTempDir(), prefix);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to create temp directory", ex);
        }
    }

    public Path extractArchiveToTemp(MultipartFile file) {
        Path tempDir = createTempDirectory("upload-");
        try {
            FileWorkspaceUtils.unzipSecurely(file.getInputStream(), tempDir);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read upload", ex);
        }
        return tempDir;
    }

    public Path detectSkillRoot(Path extractedRoot) {
        try {
            List<Path> matches = Files.walk(extractedRoot)
                .filter(path -> path.getFileName() != null && "SKILL.md".equals(path.getFileName().toString()))
                .map(Path::getParent)
                .distinct()
                .toList();
            if (matches.isEmpty()) {
                throw new BadRequestException("上传内容中未找到 SKILL.md");
            }
            if (matches.size() > 1) {
                throw new BadRequestException("压缩包中检测到多个 Skill 目录，请一次上传一个 Skill");
            }
            return matches.getFirst();
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to inspect uploaded archive", ex);
        }
    }

    public void writeSkillFiles(Path root, List<SkillFilePayload> files) {
        if (files == null) {
            return;
        }
        for (SkillFilePayload file : files) {
            String relative = FileWorkspaceUtils.normalizeRelativePath(file.path());
            Path resolved = root.resolve(relative).normalize();
            if (!resolved.startsWith(root.normalize())) {
                throw new BadRequestException("非法文件路径: " + file.path());
            }
            try {
                Files.createDirectories(resolved.getParent());
                Files.writeString(resolved, Optional.ofNullable(file.content()).orElse(""), StandardCharsets.UTF_8);
            } catch (IOException ex) {
                throw new IllegalStateException("Failed to write file: " + file.path(), ex);
            }
        }
    }

    public void applyTextChanges(Path root, List<SkillFilePayload> upserts, List<String> deletePaths) {
        writeSkillFiles(root, upserts);
        if (deletePaths == null) {
            return;
        }
        for (String rawDeletePath : deletePaths) {
            String relative = FileWorkspaceUtils.normalizeRelativePath(rawDeletePath);
            Path resolved = root.resolve(relative).normalize();
            if (!resolved.startsWith(root.normalize())) {
                throw new BadRequestException("非法删除路径: " + rawDeletePath);
            }
            FileWorkspaceUtils.deleteRecursively(resolved);
        }
    }

    public void replaceDirectory(Path source, Path target) {
        FileWorkspaceUtils.deleteRecursively(target);
        FileWorkspaceUtils.ensureDirectory(target.getParent());
        try {
            Files.move(source, target);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to replace directory: " + target, ex);
        }
    }

    public SnapshotResult snapshotSkillDirectory(Path root) {
        if (Files.notExists(root) || !Files.isDirectory(root)) {
            throw new BadRequestException("Skill 目录不存在: " + root);
        }
        Path skillMd = root.resolve("SKILL.md");
        if (Files.notExists(skillMd)) {
            throw new BadRequestException("Skill 目录缺少 SKILL.md: " + root);
        }

        List<SnapshotEntry> entries = new ArrayList<>();
        try {
            List<Path> paths = Files.walk(root)
                .filter(path -> !path.equals(root))
                .filter(path -> !root.relativize(path).toString().replace('\\', '/').startsWith(".git"))
                .sorted(Comparator.comparing(path -> root.relativize(path).toString().replace('\\', '/')))
                .toList();
            int sortOrder = 0;
            for (Path path : paths) {
                boolean directory = Files.isDirectory(path);
                String relative = root.relativize(path).toString().replace('\\', '/');
                String fileName = path.getFileName().toString();
                String fileExt = directory ? "" : PreviewModeResolver.extension(fileName);
                long size = directory ? 0L : Files.size(path);
                String mimeType = directory ? "inode/directory" :
                    Optional.ofNullable(Files.probeContentType(path)).orElse("application/octet-stream");
                PreviewMode previewMode = PreviewModeResolver.resolve(path, directory);
                String sha256 = directory ? "" : HashUtils.sha256(path);
                entries.add(new SnapshotEntry(relative, fileName, directory, fileExt, size, mimeType, previewMode, sortOrder++, sha256));
            }
            String markdown = Files.readString(skillMd, StandardCharsets.UTF_8);
            ParsedSkillDocument parsedSkillDocument = markdownParser.parse(markdown,
                Optional.ofNullable(root.getFileName()).map(Path::toString).orElse("skill"));
            List<Map<String, Object>> manifest = entries.stream()
                .map(entry -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("path", entry.relativePath());
                    item.put("directory", entry.directory());
                    item.put("sha256", entry.sha256());
                    item.put("size", entry.sizeBytes());
                    return item;
                })
                .toList();
            String manifestJson = toJson(manifest);
            return new SnapshotResult(entries, HashUtils.sha256(manifestJson), HashUtils.sha256(markdown), manifestJson, parsedSkillDocument);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to scan skill directory: " + root, ex);
        }
    }

    public ChangeSummary compareSnapshots(SnapshotResult before, SnapshotResult after) {
        Map<String, SnapshotEntry> beforeFiles = before.entries().stream()
            .filter(entry -> !entry.directory())
            .collect(Collectors.toMap(SnapshotEntry::relativePath, entry -> entry, (left, right) -> left, LinkedHashMap::new));
        Map<String, SnapshotEntry> afterFiles = after.entries().stream()
            .filter(entry -> !entry.directory())
            .collect(Collectors.toMap(SnapshotEntry::relativePath, entry -> entry, (left, right) -> left, LinkedHashMap::new));

        List<String> paths = new ArrayList<>();
        paths.addAll(beforeFiles.keySet());
        afterFiles.keySet().stream().filter(path -> !paths.contains(path)).forEach(paths::add);
        paths.sort(String::compareTo);

        List<ChangedFile> changedFiles = new ArrayList<>();
        int added = 0;
        int modified = 0;
        int deleted = 0;
        boolean skillMdChanged = false;
        boolean otherChanged = false;

        for (String path : paths) {
            SnapshotEntry previous = beforeFiles.get(path);
            SnapshotEntry current = afterFiles.get(path);
            ChangedFile changedFile = null;
            if (previous == null && current != null) {
                added++;
                changedFile = new ChangedFile(path, ChangeType.ADD, "SKILL.md".equals(path), current.fileExt(), null,
                    current.sizeBytes(), null, current.sha256(), current.previewMode());
            } else if (previous != null && current == null) {
                deleted++;
                changedFile = new ChangedFile(path, ChangeType.DELETE, "SKILL.md".equals(path), previous.fileExt(),
                    previous.sizeBytes(), null, previous.sha256(), null, previous.previewMode());
            } else if (previous != null && current != null && !previous.sha256().equals(current.sha256())) {
                modified++;
                changedFile = new ChangedFile(path, ChangeType.MODIFY, "SKILL.md".equals(path), current.fileExt(),
                    previous.sizeBytes(), current.sizeBytes(), previous.sha256(), current.sha256(), current.previewMode());
            }
            if (changedFile != null) {
                changedFiles.add(changedFile);
                if (changedFile.skillMd()) {
                    skillMdChanged = true;
                } else {
                    otherChanged = true;
                }
            }
        }

        String changeScope = skillMdChanged && otherChanged ? "mixed" :
            (skillMdChanged ? "skill_md_only" : "non_index_files_only");
        return new ChangeSummary(changedFiles, skillMdChanged, otherChanged, changeScope, added, modified, deleted);
    }

    public byte[] zipDirectory(Path root) {
        return FileWorkspaceUtils.zipDirectory(root);
    }

    public void copyDirectory(Path source, Path target) {
        FileWorkspaceUtils.copyDirectory(source, target);
    }

    public void copyDirectoryExcludingGit(Path source, Path target) {
        FileWorkspaceUtils.copyDirectoryExcludingGit(source, target);
    }

    public void deleteDirectory(Path path) {
        FileWorkspaceUtils.deleteRecursively(path);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize JSON", ex);
        }
    }
}
