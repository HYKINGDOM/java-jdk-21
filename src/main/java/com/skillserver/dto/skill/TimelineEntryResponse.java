package com.skillserver.dto.skill;

import java.time.LocalDateTime;

public record TimelineEntryResponse(
    String revision,
    String parentRevision,
    String sourceType,
    String committedBy,
    LocalDateTime committedAt,
    boolean hasSkillMdChange,
    boolean hasOtherFilesChange,
    String changeScope,
    int changedFilesCount,
    int addedFilesCount,
    int modifiedFilesCount,
    int deletedFilesCount,
    boolean affectsSearch,
    String summary
) {
}
