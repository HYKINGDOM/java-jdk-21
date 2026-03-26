package com.skillserver.dto.skill;

import java.time.LocalDateTime;
import java.util.List;

public record SkillSummaryResponse(
    String skillUid,
    String slug,
    String title,
    String summary,
    List<String> tags,
    String sourceType,
    String status,
    String indexStatus,
    String syncStatus,
    String currentRevision,
    int favoriteCount,
    int downloadCount,
    LocalDateTime updatedAt,
    LocalDateTime searchUpdatedAt,
    boolean favorite
) {
}
