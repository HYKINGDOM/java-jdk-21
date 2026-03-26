package com.skillserver.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillserver.common.exception.BadRequestException;
import com.skillserver.common.exception.ConflictException;
import com.skillserver.common.exception.NotFoundException;
import com.skillserver.common.util.IdGenerator;
import com.skillserver.common.util.SlugUtils;
import com.skillserver.domain.entity.IndexJobEntity;
import com.skillserver.domain.entity.RepoSourceEntity;
import com.skillserver.domain.entity.SkillCurrentDocEntity;
import com.skillserver.domain.entity.SkillDownloadEntity;
import com.skillserver.domain.entity.SkillEntity;
import com.skillserver.domain.entity.SkillFileCurrentEntity;
import com.skillserver.domain.entity.SkillRevisionEntity;
import com.skillserver.domain.entity.SkillRevisionFileEntity;
import com.skillserver.domain.enums.ActionType;
import com.skillserver.domain.enums.ChangeType;
import com.skillserver.domain.enums.IndexStatus;
import com.skillserver.domain.enums.JobStatus;
import com.skillserver.domain.enums.NotificationType;
import com.skillserver.domain.enums.OriginMode;
import com.skillserver.domain.enums.ResourceType;
import com.skillserver.domain.enums.SkillStatus;
import com.skillserver.domain.enums.SourceType;
import com.skillserver.domain.enums.SyncStatus;
import com.skillserver.dto.common.PageResponse;
import com.skillserver.dto.skill.CreatePrivateSkillRequest;
import com.skillserver.dto.skill.FileNodeResponse;
import com.skillserver.dto.skill.RevisionDetailResponse;
import com.skillserver.dto.skill.RevisionFileResponse;
import com.skillserver.dto.skill.SkillFilePayload;
import com.skillserver.dto.skill.SkillSummaryResponse;
import com.skillserver.dto.skill.SkillDetailResponse;
import com.skillserver.dto.skill.TimelineEntryResponse;
import com.skillserver.dto.skill.UpdatePrivateSkillRequest;
import com.skillserver.repository.IndexJobRepository;
import com.skillserver.repository.SkillCurrentDocRepository;
import com.skillserver.repository.SkillDownloadRepository;
import com.skillserver.repository.SkillFavoriteRepository;
import com.skillserver.repository.SkillFileCurrentRepository;
import com.skillserver.repository.SkillRepository;
import com.skillserver.repository.SkillRevisionFileRepository;
import com.skillserver.repository.SkillRevisionRepository;
import com.skillserver.security.AppUserPrincipal;
import com.skillserver.service.model.ChangeSummary;
import com.skillserver.service.model.ChangedFile;
import com.skillserver.service.model.DownloadPayload;
import com.skillserver.service.model.ParsedSkillDocument;
import com.skillserver.service.model.SearchHit;
import com.skillserver.service.model.SnapshotEntry;
import com.skillserver.service.model.SnapshotResult;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class SkillService {

    private final SkillRepository skillRepository;
    private final SkillCurrentDocRepository currentDocRepository;
    private final SkillRevisionRepository revisionRepository;
    private final SkillRevisionFileRepository revisionFileRepository;
    private final SkillFileCurrentRepository fileCurrentRepository;
    private final SkillFavoriteRepository favoriteRepository;
    private final SkillDownloadRepository downloadRepository;
    private final IndexJobRepository indexJobRepository;
    private final WorkspaceService workspaceService;
    private final SearchIndexService searchIndexService;
    private final AccessControlService accessControlService;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;
    private final EditSessionService editSessionService;
    private final SkillMarkdownParser markdownParser;
    private final ObjectMapper objectMapper;

    @Transactional
    public SkillDetailResponse createPrivateSkill(CreatePrivateSkillRequest request, AppUserPrincipal actor) {
        long start = System.currentTimeMillis();
        Path tempDir = workspaceService.createTempDirectory("private-create-");
        try {
            materializeSkillDirectory(tempDir, request.name(), request.description(), request.tags(), request.skillMarkdown(), request.files());
            SkillDetailResponse response = persistNewPrivateSkill(tempDir, request.name(), request.slug(), OriginMode.WEB_CREATE, actor);
            auditLogService.success(actor, "skill_create", ActionType.WRITE, "skill", response.skillUid(), response.title(),
                Map.of("mode", OriginMode.WEB_CREATE.name()), System.currentTimeMillis() - start);
            return response;
        } catch (RuntimeException ex) {
            auditLogService.failure(actor, "skill_create", ActionType.WRITE, "skill", null, request.name(),
                Map.of("mode", OriginMode.WEB_CREATE.name()), ex, System.currentTimeMillis() - start);
            workspaceService.deleteDirectory(tempDir);
            throw ex;
        }
    }

    @Transactional
    public SkillDetailResponse uploadSkillMarkdown(MultipartFile file, AppUserPrincipal actor) {
        long start = System.currentTimeMillis();
        Path tempDir = workspaceService.createTempDirectory("skill-md-");
        try {
            String markdown = new String(file.getBytes(), StandardCharsets.UTF_8);
            materializeSkillDirectory(tempDir, file.getOriginalFilename(), null, List.of(), markdown, List.of());
            SkillDetailResponse response = persistNewPrivateSkill(tempDir, file.getOriginalFilename(), null, OriginMode.UPLOAD_SKILL_MD, actor);
            auditLogService.success(actor, "skill_upload_md", ActionType.WRITE, "skill", response.skillUid(), response.title(),
                Map.of("filename", file.getOriginalFilename()), System.currentTimeMillis() - start);
            return response;
        } catch (IOException ex) {
            throw new IllegalStateException("读取上传文件失败", ex);
        } catch (RuntimeException ex) {
            auditLogService.failure(actor, "skill_upload_md", ActionType.WRITE, "skill", null, file.getOriginalFilename(),
                null, ex, System.currentTimeMillis() - start);
            workspaceService.deleteDirectory(tempDir);
            throw ex;
        }
    }

    @Transactional
    public SkillDetailResponse uploadFolder(MultipartFile file, AppUserPrincipal actor) {
        long start = System.currentTimeMillis();
        Path extractDir = workspaceService.extractArchiveToTemp(file);
        try {
            Path skillRoot = workspaceService.detectSkillRoot(extractDir);
            SkillDetailResponse response = persistNewPrivateSkill(skillRoot, skillRoot.getFileName().toString(), null,
                OriginMode.UPLOAD_FOLDER, actor);
            auditLogService.success(actor, "skill_upload_folder", ActionType.WRITE, "skill", response.skillUid(), response.title(),
                Map.of("filename", file.getOriginalFilename()), System.currentTimeMillis() - start);
            return response;
        } catch (RuntimeException ex) {
            auditLogService.failure(actor, "skill_upload_folder", ActionType.WRITE, "skill", null, file.getOriginalFilename(),
                null, ex, System.currentTimeMillis() - start);
            throw ex;
        } finally {
            workspaceService.deleteDirectory(extractDir);
        }
    }

    @Transactional
    public SkillDetailResponse updatePrivateSkill(String skillUid, UpdatePrivateSkillRequest request, AppUserPrincipal actor) {
        long start = System.currentTimeMillis();
        SkillEntity skill = requireSkill(skillUid);
        accessControlService.assertCanEditSkill(actor, skill);
        if (skill.getSourceType() != SourceType.PRIVATE) {
            throw new BadRequestException("仅私有 Skill 支持在线编辑");
        }
        var editSession = editSessionService.validate(skillUid, request.lockToken(), actor);
        if (!Objects.equals(skill.getCurrentRevision(), request.baseRevision())) {
            throw new ConflictException("Skill 已被更新，请刷新后重试");
        }

        Path currentRoot = workspaceService.privateSkillPath(skillUid);
        Path tempDir = workspaceService.createTempDirectory("private-edit-");
        try {
            workspaceService.copyDirectory(currentRoot, tempDir);
            SnapshotResult before = snapshotFromCurrentState(skill, currentRoot);
            workspaceService.applyTextChanges(tempDir, request.upserts(), request.deletePaths());
            SnapshotResult after = workspaceService.snapshotSkillDirectory(tempDir);
            ChangeSummary changeSummary = workspaceService.compareSnapshots(before, after);
            if (changeSummary.changedFiles().isEmpty()) {
                throw new ConflictException("没有检测到目录变更");
            }

            String parentRevision = skill.getCurrentRevision();
            String newRevision = IdGenerator.revision();
            workspaceService.replaceDirectory(tempDir, currentRoot);
            applySnapshotToSkill(skill, after, newRevision, actor.getUsername(), changeSummary.hasSkillMdChange());
            persistCurrentFiles(skill, newRevision, after.entries());
            if (changeSummary.hasSkillMdChange()) {
                SkillCurrentDocEntity currentDoc = upsertCurrentDoc(skillUid, newRevision, after.parsedSkillDocument());
                runIndexJob("update", skill, currentDoc);
            }
            persistRevision(skill, newRevision, parentRevision, after, changeSummary,
                Optional.ofNullable(request.summary()).filter(value -> !value.isBlank()).orElse("在线编辑更新"), actor.getUsername());
            editSession.setBaseRevision(newRevision);
            editSession.setBaseTreeFingerprint(after.treeFingerprint());

            notificationService.notifyUser(actor.getUserId(), NotificationType.SKILL_UPDATED, skillUid, newRevision,
                "Skill 已更新", "Skill " + skill.getSlug() + " 已发布新版本 " + newRevision);
            auditLogService.success(actor, "skill_update", ActionType.WRITE, "skill", skillUid, skill.getSlug(),
                Map.of("revision", newRevision, "changedFiles", changeSummary.changedFiles().size()),
                System.currentTimeMillis() - start);
            return toDetailResponse(skill, actor);
        } catch (RuntimeException ex) {
            auditLogService.failure(actor, "skill_update", ActionType.WRITE, "skill", skillUid, skill.getSlug(),
                Map.of("baseRevision", request.baseRevision()), ex, System.currentTimeMillis() - start);
            workspaceService.deleteDirectory(tempDir);
            throw ex;
        }
    }

    @Transactional
    public void deletePrivateSkill(String skillUid, AppUserPrincipal actor) {
        long start = System.currentTimeMillis();
        SkillEntity skill = requireSkill(skillUid);
        accessControlService.assertCanDeleteSkill(actor, skill);
        if (skill.getSourceType() != SourceType.PRIVATE) {
            throw new BadRequestException("仅私有 Skill 支持直接删除");
        }
        skill.setStatus(SkillStatus.DELETED_PENDING_PURGE);
        skill.setDeletedAt(LocalDateTime.now());
        skill.setUpdatedBy(actor.getUsername());
        searchIndexService.delete(skillUid);
        notificationService.notifyUser(actor.getUserId(), NotificationType.SKILL_DELETED, skillUid, skill.getCurrentRevision(),
            "Skill 已删除", "Skill " + skill.getSlug() + " 已进入待清理状态");
        auditLogService.success(actor, "skill_delete", ActionType.DELETE, "skill", skillUid, skill.getSlug(), null,
            System.currentTimeMillis() - start);
    }

    @Transactional(readOnly = true)
    public PageResponse<SkillSummaryResponse> listSkills(String query,
                                                         String sourceType,
                                                         List<String> requiredTags,
                                                         String sortBy,
                                                         int page,
                                                         int size,
                                                         AppUserPrincipal actor) {
        List<SkillEntity> skills;
        Map<String, Float> scoreMap = new HashMap<>();
        if (query != null && !query.isBlank()) {
            List<SearchHit> hits = searchIndexService.search(query, 200);
            scoreMap = hits.stream().collect(Collectors.toMap(SearchHit::skillUid, SearchHit::score));
            skills = hits.isEmpty() ? List.of() : skillRepository.findAllBySkillUidIn(hits.stream().map(SearchHit::skillUid).toList());
        } else {
            skills = skillRepository.findAllByStatus(SkillStatus.ACTIVE);
        }

        Map<String, SkillCurrentDocEntity> docMap = currentDocRepository.findAllBySkillUidIn(
            skills.stream().map(SkillEntity::getSkillUid).toList()).stream()
            .collect(Collectors.toMap(SkillCurrentDocEntity::getSkillUid, doc -> doc));
        Set<String> favoriteSkillUids = favoriteRepository.findAllByUserId(actor.getUserId()).stream()
            .map(favorite -> favorite.getSkillUid())
            .collect(Collectors.toCollection(LinkedHashSet::new));

        List<SkillEntity> filtered = skills.stream()
            .filter(skill -> skill.getStatus() == SkillStatus.ACTIVE)
            .filter(skill -> accessControlService.canReadSkill(actor, skill))
            .filter(skill -> sourceType == null || sourceType.isBlank() || skill.getSourceType().name().equalsIgnoreCase(sourceType))
            .filter(skill -> tagMatch(requiredTags, docMap.get(skill.getSkillUid())))
            .sorted(skillComparator(query, sortBy, scoreMap))
            .toList();

        int from = Math.min(page * size, filtered.size());
        int to = Math.min(from + size, filtered.size());
        List<SkillSummaryResponse> items = filtered.subList(from, to).stream()
            .map(skill -> toSummaryResponse(skill, docMap.get(skill.getSkillUid()), favoriteSkillUids.contains(skill.getSkillUid())))
            .toList();
        return new PageResponse<>(items, filtered.size(), page, size);
    }

    @Transactional(readOnly = true)
    public SkillDetailResponse getSkillDetail(String skillUid, AppUserPrincipal actor) {
        SkillEntity skill = requireSkill(skillUid);
        accessControlService.assertCanReadSkill(actor, skill);
        return toDetailResponse(skill, actor);
    }

    @Transactional(readOnly = true)
    public List<FileNodeResponse> getTree(String skillUid, AppUserPrincipal actor) {
        SkillEntity skill = requireSkill(skillUid);
        accessControlService.assertCanReadSkill(actor, skill);
        return fileCurrentRepository.findAllBySkillUidOrderBySortOrderAsc(skillUid).stream()
            .map(entry -> new FileNodeResponse(
                entry.getRelativePath(),
                entry.getFileName(),
                entry.isDir(),
                entry.getFileExt(),
                entry.getSizeBytes(),
                entry.getMimeType(),
                entry.getPreviewMode().name(),
                entry.getSortOrder()
            ))
            .toList();
    }

    @Transactional(readOnly = true)
    public List<TimelineEntryResponse> getTimeline(String skillUid, AppUserPrincipal actor) {
        SkillEntity skill = requireSkill(skillUid);
        accessControlService.assertCanReadSkill(actor, skill);
        return revisionRepository.findAllBySkillUidOrderByCommittedAtDesc(skillUid).stream()
            .map(revision -> new TimelineEntryResponse(
                revision.getRevision(),
                revision.getParentRevision(),
                revision.getSourceType().name(),
                revision.getCommittedBy(),
                revision.getCommittedAt(),
                revision.isHasSkillMdChange(),
                revision.isHasOtherFilesChange(),
                revision.getChangeScope(),
                revision.getChangedFilesCount(),
                revision.getAddedFilesCount(),
                revision.getModifiedFilesCount(),
                revision.getDeletedFilesCount(),
                revision.isAffectsSearch(),
                revision.getSummary()
            ))
            .toList();
    }

    @Transactional(readOnly = true)
    public RevisionDetailResponse getRevisionDetail(String skillUid, String revision, AppUserPrincipal actor) {
        SkillEntity skill = requireSkill(skillUid);
        accessControlService.assertCanReadSkill(actor, skill);
        SkillRevisionEntity revisionEntity = revisionRepository.findBySkillUidAndRevision(skillUid, revision)
            .orElseThrow(() -> new NotFoundException("版本不存在: " + revision));
        List<RevisionFileResponse> files = revisionFileRepository.findAllBySkillUidAndRevisionOrderByRelativePathAsc(skillUid, revision).stream()
            .map(file -> new RevisionFileResponse(
                file.getRelativePath(),
                file.getChangeType().name(),
                file.isSkillMd(),
                file.getPreviewMode() == null ? null : file.getPreviewMode().name(),
                file.getSizeBefore(),
                file.getSizeAfter()
            ))
            .toList();
        return new RevisionDetailResponse(skillUid, revision, revisionEntity.getParentRevision(), revisionEntity.getCommittedBy(),
            revisionEntity.getCommittedAt(), revisionEntity.getSummary(), files);
    }

    @Transactional
    public DownloadPayload download(String skillUid, AppUserPrincipal actor) {
        SkillEntity skill = requireSkill(skillUid);
        accessControlService.assertCanReadSkill(actor, skill);
        Path root = resolveSkillRoot(skill);
        byte[] bytes = workspaceService.zipDirectory(root);
        String fileName = skill.getSlug() + "-" + skill.getCurrentRevision() + ".zip";
        downloadRepository.save(SkillDownloadEntity.builder()
            .userId(actor.getUserId())
            .actorName(actor.getUsername())
            .skillUid(skillUid)
            .revision(skill.getCurrentRevision())
            .packageName(fileName)
            .sourceType(skill.getSourceType())
            .createdAt(LocalDateTime.now())
            .build());
        skill.setDownloadCount((int) downloadRepository.countBySkillUid(skillUid));
        return new DownloadPayload(fileName, bytes);
    }

    @Transactional
    public int syncGitRepositorySkills(RepoSourceEntity repo, String commitHash, AppUserPrincipal actor) {
        Path repoRoot = workspaceService.repoMirrorPath(repo.getId());
        List<Path> skillRoots;
        try {
            skillRoots = Files.walk(repoRoot)
                .filter(path -> path.getFileName() != null && "SKILL.md".equals(path.getFileName().toString()))
                .map(Path::getParent)
                .distinct()
                .sorted()
                .toList();
        } catch (IOException ex) {
            throw new IllegalStateException("扫描仓库失败", ex);
        }

        Map<String, SkillEntity> existingByRelative = skillRepository.findAllByRepoSource_Id(repo.getId()).stream()
            .collect(Collectors.toMap(skill -> Optional.ofNullable(skill.getRelativeDir()).orElse(""), skill -> skill, (left, right) -> left));
        Set<String> seenRelativeDirs = new LinkedHashSet<>();
        int changedCount = 0;

        for (Path skillRoot : skillRoots) {
            String relativeDir = repoRoot.relativize(skillRoot).toString().replace('\\', '/');
            seenRelativeDirs.add(relativeDir);
            SnapshotResult after = workspaceService.snapshotSkillDirectory(skillRoot);
            SkillEntity existing = existingByRelative.get(relativeDir);
            String gitSkillUid = gitSkillUid(repo, relativeDir);
            String revision = commitHash.length() > 12 ? commitHash.substring(0, 12) : commitHash;

            if (existing == null) {
                SkillEntity created = SkillEntity.builder()
                    .skillUid(gitSkillUid)
                    .slug(SlugUtils.slugify(after.parsedSkillDocument().title(), gitSkillUid))
                    .sourceType(SourceType.GIT)
                    .repoSource(repo)
                    .relativeDir(relativeDir)
                    .entryFile("SKILL.md")
                    .currentRevision(revision)
                    .originType(SourceType.GIT)
                    .originLocator("git:" + repo.getNormalizedRepoUrl() + "#" + repo.getBranch() + ":" + relativeDir)
                    .originMode(OriginMode.GIT_IMPORT)
                    .currentTreeFingerprint(after.treeFingerprint())
                    .currentSkillMdFingerprint(after.skillMdFingerprint())
                    .currentManifestVersion(1)
                    .syncStatus(SyncStatus.SYNCED)
                    .indexStatus(IndexStatus.PENDING)
                    .searchUpdatedAt(LocalDateTime.now())
                    .favoriteCount(0)
                    .downloadCount(0)
                    .status(SkillStatus.ACTIVE)
                    .createdBy(actor.getUsername())
                    .updatedBy(actor.getUsername())
                    .build();
                skillRepository.save(created);
                persistCurrentFiles(created, revision, after.entries());
                SkillCurrentDocEntity currentDoc = upsertCurrentDoc(created.getSkillUid(), revision, after.parsedSkillDocument());
                runIndexJob("create", created, currentDoc);
                persistRevision(created, revision, null, after, initialChangeSummary(after), "Git 导入初始化", actor.getUsername());
                changedCount++;
                continue;
            }

            SnapshotResult before = snapshotFromDatabase(existing);
            ChangeSummary changeSummary = workspaceService.compareSnapshots(before, after);
            if (!changeSummary.changedFiles().isEmpty() || !Objects.equals(existing.getCurrentRevision(), revision)) {
                String parentRevision = existing.getCurrentRevision();
                applySnapshotToSkill(existing, after, revision, actor.getUsername(), changeSummary.hasSkillMdChange());
                existing.setRepoSource(repo);
                existing.setRelativeDir(relativeDir);
                persistCurrentFiles(existing, revision, after.entries());
                if (changeSummary.hasSkillMdChange()) {
                    SkillCurrentDocEntity currentDoc = upsertCurrentDoc(existing.getSkillUid(), revision, after.parsedSkillDocument());
                    runIndexJob("update", existing, currentDoc);
                }
                persistRevision(existing, revision, parentRevision, after, changeSummary, "Git 同步更新", actor.getUsername());
                changedCount++;
            }
        }

        for (SkillEntity missing : existingByRelative.values()) {
            String relativeDir = Optional.ofNullable(missing.getRelativeDir()).orElse("");
            if (!seenRelativeDirs.contains(relativeDir) && missing.getStatus() == SkillStatus.ACTIVE) {
                missing.setStatus(SkillStatus.DELETED_PENDING_PURGE);
                missing.setDeletedAt(LocalDateTime.now());
                searchIndexService.delete(missing.getSkillUid());
                changedCount++;
            }
        }
        return changedCount;
    }

    @Transactional
    public int purgeSoftDeletedSkills() {
        List<SkillEntity> deletedSkills = skillRepository.findAll().stream()
            .filter(skill -> skill.getStatus() == SkillStatus.DELETED_PENDING_PURGE)
            .toList();
        deletedSkills.forEach(skill -> {
            if (skill.getSourceType() == SourceType.PRIVATE) {
                workspaceService.deleteDirectory(workspaceService.privateSkillPath(skill.getSkillUid()));
            }
            searchIndexService.delete(skill.getSkillUid());
            skill.setStatus(SkillStatus.PURGED);
            currentDocRepository.findBySkillUid(skill.getSkillUid()).ifPresent(currentDocRepository::delete);
            fileCurrentRepository.deleteAllBySkillUid(skill.getSkillUid());
        });
        return deletedSkills.size();
    }

    private SkillDetailResponse persistNewPrivateSkill(Path preparedRoot,
                                                       String requestedName,
                                                       String slugHint,
                                                       OriginMode originMode,
                                                       AppUserPrincipal actor) {
        SnapshotResult snapshot = workspaceService.snapshotSkillDirectory(preparedRoot);
        String skillUid = IdGenerator.skillUid();
        String revision = IdGenerator.revision();
        Path finalPath = workspaceService.privateSkillPath(skillUid);
        workspaceService.replaceDirectory(preparedRoot, finalPath);

        SkillEntity skill = SkillEntity.builder()
            .skillUid(skillUid)
            .slug(SlugUtils.slugify(Optional.ofNullable(slugHint).filter(value -> !value.isBlank()).orElse(snapshot.parsedSkillDocument().title()), skillUid))
            .sourceType(SourceType.PRIVATE)
            .relativeDir("")
            .entryFile("SKILL.md")
            .currentRevision(revision)
            .originType(SourceType.PRIVATE)
            .originLocator("private:" + skillUid)
            .originMode(originMode)
            .currentTreeFingerprint(snapshot.treeFingerprint())
            .currentSkillMdFingerprint(snapshot.skillMdFingerprint())
            .currentManifestVersion(1)
            .syncStatus(SyncStatus.SYNCED)
            .indexStatus(IndexStatus.PENDING)
            .searchUpdatedAt(LocalDateTime.now())
            .favoriteCount(0)
            .downloadCount(0)
            .status(SkillStatus.ACTIVE)
            .createdBy(actor.getUsername())
            .updatedBy(actor.getUsername())
            .build();
        skillRepository.save(skill);
        accessControlService.assignOwner(actor.getUserId(), ResourceType.SKILL, skillUid, actor.getUsername());
        persistCurrentFiles(skill, revision, snapshot.entries());
        SkillCurrentDocEntity currentDoc = upsertCurrentDoc(skillUid, revision, snapshot.parsedSkillDocument());
        runIndexJob("create", skill, currentDoc);
        persistRevision(skill, revision, null, snapshot, initialChangeSummary(snapshot),
            Optional.ofNullable(requestedName).filter(value -> !value.isBlank()).orElse("创建 Skill"), actor.getUsername());
        notificationService.notifyUser(actor.getUserId(), NotificationType.SKILL_CREATED, skillUid, revision,
            "Skill 已创建", "Skill " + skill.getSlug() + " 已创建完成");
        return toDetailResponse(skill, actor);
    }

    private void materializeSkillDirectory(Path tempDir,
                                           String name,
                                           String description,
                                           List<String> tags,
                                           String skillMarkdown,
                                           List<SkillFilePayload> files) {
        workspaceService.writeSkillFiles(tempDir, Optional.ofNullable(files).orElse(List.of()).stream()
            .filter(file -> !"SKILL.md".equalsIgnoreCase(file.path()))
            .toList());
        String markdown = skillMarkdown;
        if ((markdown == null || markdown.isBlank()) && files != null) {
            markdown = files.stream()
                .filter(file -> "SKILL.md".equalsIgnoreCase(file.path()))
                .map(SkillFilePayload::content)
                .findFirst()
                .orElse(null);
        }
        if (markdown == null || markdown.isBlank()) {
            markdown = buildDefaultMarkdown(name, description, tags);
        }
        try {
            Files.writeString(tempDir.resolve("SKILL.md"), markdown, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalStateException("写入 SKILL.md 失败", ex);
        }
    }

    private String buildDefaultMarkdown(String name, String description, List<String> tags) {
        String tagLine = tags == null || tags.isEmpty() ? "" : "\n标签: " + String.join(", ", tags) + "\n";
        return "# " + Optional.ofNullable(name).filter(value -> !value.isBlank()).orElse("新技能") +
            "\n\n## 用途\n\n" +
            Optional.ofNullable(description).filter(value -> !value.isBlank()).orElse("请补充用途说明。") +
            tagLine +
            "\n## 使用方式\n\n请补充使用说明。\n";
    }

    private void applySnapshotToSkill(SkillEntity skill,
                                      SnapshotResult snapshot,
                                      String revision,
                                      String actorName,
                                      boolean rebuildSearch) {
        skill.setCurrentTreeFingerprint(snapshot.treeFingerprint());
        skill.setCurrentSkillMdFingerprint(snapshot.skillMdFingerprint());
        skill.setCurrentManifestVersion(skill.getCurrentManifestVersion() == null ? 1 : skill.getCurrentManifestVersion() + 1);
        skill.setSyncStatus(SyncStatus.SYNCED);
        skill.setUpdatedBy(actorName);
        skill.setCurrentRevision(revision);
        skill.setDeletedAt(null);
        skill.setStatus(SkillStatus.ACTIVE);
        skill.setUpdatedAt(LocalDateTime.now());
        if (rebuildSearch) {
            skill.setSearchUpdatedAt(LocalDateTime.now());
        }
    }

    private SkillCurrentDocEntity upsertCurrentDoc(String skillUid, String revision, ParsedSkillDocument parsedDocument) {
        SkillCurrentDocEntity entity = currentDocRepository.findBySkillUid(skillUid)
            .orElseGet(SkillCurrentDocEntity::new);
        entity.setSkillUid(skillUid);
        entity.setTitle(parsedDocument.title());
        entity.setSummary(parsedDocument.summary());
        entity.setTagsJson(toJson(parsedDocument.tags()));
        entity.setBodyMarkdown(parsedDocument.bodyMarkdown());
        entity.setBodyPlaintext(parsedDocument.bodyPlaintext());
        entity.setContentHash(parsedDocument.contentHash());
        entity.setRevision(revision);
        entity.setUpdatedAt(LocalDateTime.now());
        return currentDocRepository.save(entity);
    }

    private void persistCurrentFiles(SkillEntity skill, String revision, Collection<SnapshotEntry> entries) {
        fileCurrentRepository.deleteAllBySkillUid(skill.getSkillUid());
        fileCurrentRepository.flush();
        fileCurrentRepository.saveAll(entries.stream()
            .map(entry -> SkillFileCurrentEntity.builder()
                .skillUid(skill.getSkillUid())
                .revision(revision)
                .relativePath(entry.relativePath())
                .fileName(entry.fileName())
                .dir(entry.directory())
                .fileExt(entry.fileExt())
                .sizeBytes(entry.sizeBytes())
                .mimeType(entry.mimeType())
                .previewMode(entry.previewMode())
                .sha256(entry.sha256())
                .sortOrder(entry.sortOrder())
                .build())
            .toList());
    }

    private void persistRevision(SkillEntity skill,
                                 String revision,
                                 String parentRevision,
                                 SnapshotResult snapshot,
                                 ChangeSummary changeSummary,
                                 String summary,
                                 String actorName) {
        revisionRepository.save(SkillRevisionEntity.builder()
            .skillUid(skill.getSkillUid())
            .revision(revision)
            .parentRevision(parentRevision)
            .sourceType(skill.getSourceType())
            .committedBy(actorName)
            .committedAt(LocalDateTime.now())
            .hasSkillMdChange(changeSummary.hasSkillMdChange())
            .hasOtherFilesChange(changeSummary.hasOtherFilesChange())
            .changeScope(changeSummary.changeScope())
            .changedFilesCount(changeSummary.changedFiles().size())
            .addedFilesCount(changeSummary.addedFilesCount())
            .modifiedFilesCount(changeSummary.modifiedFilesCount())
            .deletedFilesCount(changeSummary.deletedFilesCount())
            .affectsSearch(changeSummary.hasSkillMdChange())
            .summary(summary)
            .treeFingerprint(snapshot.treeFingerprint())
            .skillMdFingerprint(snapshot.skillMdFingerprint())
            .originSnapshotJson(toJson(Map.of("originMode", skill.getOriginMode().name(), "originLocator", skill.getOriginLocator())))
            .manifestJson(snapshot.manifestJson())
            .changeSummaryJson(toJson(changeSummary.changedFiles()))
            .build());
        if (!changeSummary.changedFiles().isEmpty()) {
            revisionFileRepository.saveAll(changeSummary.changedFiles().stream()
                .map(file -> SkillRevisionFileEntity.builder()
                    .skillUid(skill.getSkillUid())
                    .revision(revision)
                    .relativePath(file.relativePath())
                    .changeType(file.changeType())
                    .skillMd(file.skillMd())
                    .fileExt(file.fileExt())
                    .sizeBefore(file.sizeBefore())
                    .sizeAfter(file.sizeAfter())
                    .oldFileSha256(file.hashBefore())
                    .newFileSha256(file.hashAfter())
                    .fileSize(Optional.ofNullable(file.sizeAfter()).orElse(file.sizeBefore()))
                    .hashBefore(file.hashBefore())
                    .hashAfter(file.hashAfter())
                    .previewMode(file.previewMode())
                    .build())
                .toList());
        }
    }

    private void runIndexJob(String jobType, SkillEntity skill, SkillCurrentDocEntity currentDoc) {
        IndexJobEntity job = indexJobRepository.save(IndexJobEntity.builder()
            .jobType(jobType)
            .skillUid(skill.getSkillUid())
            .revision(currentDoc.getRevision())
            .status(JobStatus.RUNNING)
            .retryCount(0)
            .maxRetries(3)
            .startedAt(LocalDateTime.now())
            .createdAt(LocalDateTime.now())
            .build());
        try {
            searchIndexService.upsert(currentDoc, skill);
            skill.setIndexStatus(IndexStatus.INDEXED);
            skill.setLastIndexAt(LocalDateTime.now());
            skill.setIndexedAt(LocalDateTime.now());
            job.setStatus(JobStatus.SUCCEEDED);
            job.setFinishedAt(LocalDateTime.now());
        } catch (RuntimeException ex) {
            job.setStatus(JobStatus.FAILED);
            job.setErrorMessage(ex.getMessage());
            job.setFinishedAt(LocalDateTime.now());
            skill.setIndexStatus(IndexStatus.INDEX_FAILED);
            throw ex;
        }
    }

    private ChangeSummary initialChangeSummary(SnapshotResult snapshot) {
        List<ChangedFile> changedFiles = snapshot.entries().stream()
            .filter(entry -> !entry.directory())
            .map(entry -> new ChangedFile(entry.relativePath(), ChangeType.ADD,
                "SKILL.md".equals(entry.relativePath()), entry.fileExt(), null, entry.sizeBytes(), null, entry.sha256(), entry.previewMode()))
            .toList();
        int added = changedFiles.size();
        boolean hasSkillMd = changedFiles.stream().anyMatch(ChangedFile::skillMd);
        boolean hasOther = changedFiles.stream().anyMatch(file -> !file.skillMd());
        String changeScope = hasSkillMd && hasOther ? "mixed" : (hasSkillMd ? "skill_md_only" : "non_index_files_only");
        return new ChangeSummary(changedFiles, hasSkillMd, hasOther, changeScope, added, 0, 0);
    }

    private SkillEntity requireSkill(String skillUid) {
        return skillRepository.findBySkillUid(skillUid)
            .orElseThrow(() -> new NotFoundException("Skill 不存在: " + skillUid));
    }

    private SkillDetailResponse toDetailResponse(SkillEntity skill, AppUserPrincipal actor) {
        SkillCurrentDocEntity doc = currentDocRepository.findBySkillUid(skill.getSkillUid())
            .orElseThrow(() -> new NotFoundException("Skill 文档不存在: " + skill.getSkillUid()));
        boolean favorite = favoriteRepository.findByUserIdAndSkillUid(actor.getUserId(), skill.getSkillUid()).isPresent();
        return new SkillDetailResponse(
            skill.getSkillUid(),
            skill.getSlug(),
            doc.getTitle(),
            doc.getSummary(),
            parseTags(doc.getTagsJson()),
            doc.getBodyMarkdown(),
            markdownParser.renderHtml(doc.getBodyMarkdown()),
            skill.getSourceType().name(),
            skill.getStatus().name(),
            skill.getCurrentRevision(),
            skill.getRelativeDir(),
            skill.getOriginLocator(),
            skill.getFavoriteCount(),
            skill.getDownloadCount(),
            skill.getUpdatedAt(),
            favorite,
            accessControlService.canReadSkill(actor, skill),
            accessControlService.canEditSkill(actor, skill),
            accessControlService.canDeleteSkill(actor, skill)
        );
    }

    private SkillSummaryResponse toSummaryResponse(SkillEntity skill, SkillCurrentDocEntity doc, boolean favorite) {
        return new SkillSummaryResponse(
            skill.getSkillUid(),
            skill.getSlug(),
            doc == null ? skill.getSlug() : doc.getTitle(),
            doc == null ? "" : doc.getSummary(),
            doc == null ? List.of() : parseTags(doc.getTagsJson()),
            skill.getSourceType().name(),
            skill.getStatus().name(),
            skill.getIndexStatus().name(),
            skill.getSyncStatus().name(),
            skill.getCurrentRevision(),
            skill.getFavoriteCount(),
            skill.getDownloadCount(),
            skill.getUpdatedAt(),
            skill.getSearchUpdatedAt(),
            favorite
        );
    }

    private Comparator<SkillEntity> skillComparator(String query, String sortBy, Map<String, Float> scoreMap) {
        if (query != null && !query.isBlank() && (sortBy == null || sortBy.isBlank() || "relevance".equalsIgnoreCase(sortBy))) {
            return Comparator.<SkillEntity, Float>comparing(skill -> scoreMap.getOrDefault(skill.getSkillUid(), 0F))
                .reversed()
                .thenComparing(SkillEntity::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder()));
        }
        if ("favorites".equalsIgnoreCase(sortBy)) {
            return Comparator.comparing(SkillEntity::getFavoriteCount).reversed()
                .thenComparing(SkillEntity::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder()));
        }
        if ("downloads".equalsIgnoreCase(sortBy)) {
            return Comparator.comparing(SkillEntity::getDownloadCount).reversed()
                .thenComparing(SkillEntity::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder()));
        }
        return Comparator.comparing(SkillEntity::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder()));
    }

    private boolean tagMatch(List<String> requiredTags, SkillCurrentDocEntity doc) {
        if (requiredTags == null || requiredTags.isEmpty()) {
            return true;
        }
        List<String> tags = doc == null ? List.of() : parseTags(doc.getTagsJson());
        return requiredTags.stream().allMatch(required -> tags.stream().anyMatch(tag -> tag.equalsIgnoreCase(required)));
    }

    private List<String> parseTags(String tagsJson) {
        if (tagsJson == null || tagsJson.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(tagsJson, new TypeReference<>() {
            });
        } catch (JsonProcessingException ex) {
            return List.of();
        }
    }

    private SnapshotResult snapshotFromDatabase(SkillEntity skill) {
        List<SnapshotEntry> entries = fileCurrentRepository.findAllBySkillUidOrderBySortOrderAsc(skill.getSkillUid()).stream()
            .map(entry -> new SnapshotEntry(
                entry.getRelativePath(),
                entry.getFileName(),
                entry.isDir(),
                entry.getFileExt(),
                Optional.ofNullable(entry.getSizeBytes()).orElse(0L),
                entry.getMimeType(),
                entry.getPreviewMode(),
                entry.getSortOrder(),
                Optional.ofNullable(entry.getSha256()).orElse("")
            ))
            .toList();
        SkillCurrentDocEntity currentDoc = currentDocRepository.findBySkillUid(skill.getSkillUid())
            .orElseThrow(() -> new NotFoundException("Skill 文档不存在: " + skill.getSkillUid()));
        ParsedSkillDocument parsedDocument = new ParsedSkillDocument(
            currentDoc.getTitle(),
            currentDoc.getSummary(),
            parseTags(currentDoc.getTagsJson()),
            currentDoc.getBodyMarkdown(),
            currentDoc.getBodyPlaintext(),
            currentDoc.getContentHash()
        );
        return new SnapshotResult(entries, skill.getCurrentTreeFingerprint(), skill.getCurrentSkillMdFingerprint(), "[]", parsedDocument);
    }

    private SnapshotResult snapshotFromCurrentState(SkillEntity skill, Path currentRoot) {
        if (skill.getSourceType() == SourceType.PRIVATE) {
            return workspaceService.snapshotSkillDirectory(currentRoot);
        }
        return snapshotFromDatabase(skill);
    }

    private Path resolveSkillRoot(SkillEntity skill) {
        if (skill.getSourceType() == SourceType.PRIVATE) {
            return workspaceService.privateSkillPath(skill.getSkillUid());
        }
        Path repoRoot = workspaceService.repoMirrorPath(skill.getRepoSource().getId());
        return repoRoot.resolve(Optional.ofNullable(skill.getRelativeDir()).orElse(""));
    }

    private String gitSkillUid(RepoSourceEntity repo, String relativeDir) {
        return "git_" + com.skillserver.common.util.HashUtils.sha256(repo.getNormalizedRepoUrl() + ":" + repo.getBranch() + ":" + relativeDir)
            .substring(0, 16);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("JSON 序列化失败", ex);
        }
    }
}
