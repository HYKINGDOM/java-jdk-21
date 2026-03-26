package com.skillserver.dto.skill;

import java.time.LocalDateTime;

public record RepoResponse(
    Long id,
    String name,
    String repoUrl,
    String branch,
    boolean syncEnabled,
    String syncStatus,
    String status,
    String lastSyncedCommit,
    LocalDateTime lastSyncedAt,
    String lastRevision
) {
}
