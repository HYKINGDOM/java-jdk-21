package com.skillserver.service;

import com.skillserver.common.exception.ConflictException;
import com.skillserver.common.exception.NotFoundException;
import com.skillserver.domain.entity.RepoSourceEntity;
import com.skillserver.domain.entity.SkillEntity;
import com.skillserver.domain.entity.SyncJobEntity;
import com.skillserver.domain.enums.ActionType;
import com.skillserver.domain.enums.JobStatus;
import com.skillserver.domain.enums.NotificationType;
import com.skillserver.domain.enums.RepoStatus;
import com.skillserver.domain.enums.ResourceType;
import com.skillserver.domain.enums.SkillStatus;
import com.skillserver.domain.enums.SyncStatus;
import com.skillserver.dto.skill.RepoImportRequest;
import com.skillserver.dto.skill.RepoResponse;
import com.skillserver.repository.RepoSourceRepository;
import com.skillserver.repository.SkillRepository;
import com.skillserver.repository.SyncJobRepository;
import com.skillserver.security.AppUserPrincipal;
import java.net.URI;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GitRepoService {

    private final RepoSourceRepository repoSourceRepository;
    private final SkillRepository skillRepository;
    private final SyncJobRepository syncJobRepository;
    private final WorkspaceService workspaceService;
    private final SkillService skillService;
    private final SearchIndexService searchIndexService;
    private final AccessControlService accessControlService;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;

    @Transactional
    public RepoResponse importRepo(RepoImportRequest request, AppUserPrincipal actor) {
        long start = System.currentTimeMillis();
        String normalizedUrl = normalizeRepoUrl(request.repoUrl());
        repoSourceRepository.findByNormalizedRepoUrlAndBranch(normalizedUrl, request.branch())
            .ifPresent(existing -> {
                throw new ConflictException("仓库已导入: " + existing.getRepoUrl() + " [" + existing.getBranch() + "]");
            });

        RepoSourceEntity repo = repoSourceRepository.save(RepoSourceEntity.builder()
            .name(request.name())
            .repoUrl(request.repoUrl())
            .normalizedRepoUrl(normalizedUrl)
            .branch(request.branch())
            .defaultBranch(request.branch())
            .authType("none")
            .syncEnabled(Boolean.TRUE.equals(request.syncEnabled()))
            .syncCron(request.syncCron())
            .syncStatus(SyncStatus.SYNCING)
            .status(RepoStatus.ACTIVE)
            .createdBy(actor.getUsername())
            .build());
        accessControlService.assignOwner(actor.getUserId(), ResourceType.REPO, String.valueOf(repo.getId()), actor.getUsername());
        try {
            syncInternal(repo, actor, true, "manual");
            auditLogService.success(actor, "repo_import", ActionType.WRITE, "repo", String.valueOf(repo.getId()), repo.getName(),
                java.util.Map.of("url", request.repoUrl(), "branch", request.branch()), System.currentTimeMillis() - start);
            return toResponse(repo);
        } catch (RuntimeException ex) {
            auditLogService.failure(actor, "repo_import", ActionType.WRITE, "repo", String.valueOf(repo.getId()), repo.getName(),
                java.util.Map.of("url", request.repoUrl(), "branch", request.branch()), ex, System.currentTimeMillis() - start);
            throw ex;
        }
    }

    @Transactional
    public RepoResponse syncRepo(Long repoId, AppUserPrincipal actor) {
        long start = System.currentTimeMillis();
        RepoSourceEntity repo = repoSourceRepository.findById(repoId)
            .orElseThrow(() -> new NotFoundException("仓库不存在: " + repoId));
        accessControlService.assertCanManageRepo(actor, String.valueOf(repoId));
        try {
            syncInternal(repo, actor, false, "manual");
            auditLogService.success(actor, "repo_sync", ActionType.WRITE, "repo", String.valueOf(repoId), repo.getName(),
                null, System.currentTimeMillis() - start);
            return toResponse(repo);
        } catch (RuntimeException ex) {
            auditLogService.failure(actor, "repo_sync", ActionType.WRITE, "repo", String.valueOf(repoId), repo.getName(),
                null, ex, System.currentTimeMillis() - start);
            throw ex;
        }
    }

    @Transactional
    public void deleteRepo(Long repoId, AppUserPrincipal actor) {
        long start = System.currentTimeMillis();
        RepoSourceEntity repo = repoSourceRepository.findById(repoId)
            .orElseThrow(() -> new NotFoundException("仓库不存在: " + repoId));
        accessControlService.assertCanManageRepo(actor, String.valueOf(repoId));
        repo.setStatus(RepoStatus.DELETED);
        repo.setSyncStatus(SyncStatus.SYNC_FAILED);
        skillRepository.findAllByRepoSource_Id(repoId).forEach(skill -> {
            skill.setStatus(SkillStatus.DELETED_PENDING_PURGE);
            skill.setDeletedAt(LocalDateTime.now());
            searchIndexService.delete(skill.getSkillUid());
        });
        workspaceService.deleteDirectory(workspaceService.repoMirrorPath(repoId));
        auditLogService.success(actor, "repo_delete", ActionType.DELETE, "repo", String.valueOf(repoId), repo.getName(),
            null, System.currentTimeMillis() - start);
    }

    private void syncInternal(RepoSourceEntity repo, AppUserPrincipal actor, boolean cloneIfMissing, String triggerType) {
        SyncJobEntity job = syncJobRepository.save(SyncJobEntity.builder()
            .repoSourceId(repo.getId())
            .triggerType(triggerType)
            .status(JobStatus.RUNNING)
            .skillsChangedCount(0)
            .retryCount(0)
            .startedAt(LocalDateTime.now())
            .createdAt(LocalDateTime.now())
            .build());
        repo.setLastSyncJobId(job.getId());
        repo.setSyncStatus(SyncStatus.SYNCING);
        job.setRevisionBefore(repo.getLastRevision());

        try {
            String headCommit = prepareRepository(repo, cloneIfMissing);
            int changedSkills = skillService.syncGitRepositorySkills(repo, headCommit, actor);
            repo.setLastSyncedAt(LocalDateTime.now());
            repo.setLastSyncedCommit(headCommit);
            repo.setLastRevision(headCommit.length() > 12 ? headCommit.substring(0, 12) : headCommit);
            repo.setSyncStatus(SyncStatus.IDLE);
            job.setStatus(JobStatus.SUCCEEDED);
            job.setSkillsChangedCount(changedSkills);
            job.setRevisionAfter(repo.getLastRevision());
            job.setFinishedAt(LocalDateTime.now());
            notificationService.notifyUser(actor.getUserId(), NotificationType.REPO_SYNCED, null, repo.getLastRevision(),
                "仓库同步完成", "仓库 " + repo.getName() + " 已同步，变更 Skill 数量: " + changedSkills);
        } catch (RuntimeException ex) {
            repo.setSyncStatus(SyncStatus.SYNC_FAILED);
            job.setStatus(JobStatus.FAILED);
            job.setErrorMessage(ex.getMessage());
            job.setFinishedAt(LocalDateTime.now());
            notificationService.notifyUser(actor.getUserId(), NotificationType.REPO_FAILED, null, null,
                "仓库同步失败", "仓库 " + repo.getName() + " 同步失败: " + ex.getMessage());
            throw ex;
        }
    }

    private String prepareRepository(RepoSourceEntity repo, boolean cloneIfMissing) {
        Path mirrorPath = workspaceService.repoMirrorPath(repo.getId());
        try {
            Path localRepoPath = resolveLocalRepoPath(repo.getRepoUrl());
            if (localRepoPath != null) {
                String headCommit;
                try (Git git = Git.open(localRepoPath.toFile())) {
                    Ref head = git.getRepository().findRef(Constants.HEAD);
                    headCommit = head.getObjectId().getName();
                }
                workspaceService.deleteDirectory(mirrorPath);
                workspaceService.copyDirectoryExcludingGit(localRepoPath, mirrorPath);
                return headCommit;
            }

            if (cloneIfMissing || Files.notExists(mirrorPath)) {
                workspaceService.deleteDirectory(mirrorPath);
                Files.createDirectories(mirrorPath.getParent());
                try (Git git = Git.cloneRepository()
                    .setURI(repo.getRepoUrl())
                    .setDirectory(mirrorPath.toFile())
                    .setBranch(branchRef(repo.getBranch()))
                    .call()) {
                    Ref head = git.getRepository().findRef(Constants.HEAD);
                    return head.getObjectId().getName();
                }
            }

            try (Git git = Git.open(mirrorPath.toFile())) {
                checkoutBranch(git, repo.getBranch());
                git.pull().setRemoteBranchName(branchName(repo.getBranch())).call();
                Ref head = git.getRepository().findRef(Constants.HEAD);
                return head.getObjectId().getName();
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Git 操作失败: " + repo.getRepoUrl() + " -> " + ex.getMessage(), ex);
        }
    }

    private Path resolveLocalRepoPath(String repoUrl) {
        try {
            if (repoUrl.startsWith("file:/")) {
                Path path = Path.of(URI.create(repoUrl));
                return Files.exists(path.resolve(".git")) ? path : null;
            }
            Path path = Path.of(repoUrl);
            return Files.exists(path.resolve(".git")) ? path : null;
        } catch (Exception ex) {
            return null;
        }
    }

    private void checkoutBranch(Git git, String branch) throws Exception {
        String branchName = branchName(branch);
        if (git.getRepository().findRef(branchName) == null) {
            git.checkout()
                .setCreateBranch(true)
                .setName(branchName)
                .setStartPoint("origin/" + branchName)
                .call();
        } else {
            git.checkout().setName(branchName).call();
        }
    }

    private String branchRef(String branch) {
        return branch.startsWith("refs/") ? branch : "refs/heads/" + branch;
    }

    private String branchName(String branch) {
        return branch.startsWith("refs/heads/") ? branch.substring("refs/heads/".length()) : branch;
    }

    private RepoResponse toResponse(RepoSourceEntity repo) {
        return new RepoResponse(
            repo.getId(),
            repo.getName(),
            repo.getRepoUrl(),
            repo.getBranch(),
            repo.isSyncEnabled(),
            repo.getSyncStatus().name(),
            repo.getStatus().name(),
            repo.getLastSyncedCommit(),
            repo.getLastSyncedAt(),
            repo.getLastRevision()
        );
    }

    private String normalizeRepoUrl(String rawUrl) {
        String normalized = rawUrl.trim().replace('\\', '/');
        if (normalized.endsWith(".git")) {
            normalized = normalized.substring(0, normalized.length() - 4);
        }
        return normalized.toLowerCase();
    }
}
