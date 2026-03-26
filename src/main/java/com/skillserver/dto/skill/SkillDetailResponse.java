package com.skillserver.dto.skill;

import java.time.LocalDateTime;
import java.util.List;

public record SkillDetailResponse(
    String skillUid,
    String slug,
    String title,
    String summary,
    List<String> tags,
    String bodyMarkdown,
    String bodyHtml,
    String sourceType,
    String status,
    String currentRevision,
    String relativeDir,
    String originLocator,
    int favoriteCount,
    int downloadCount,
    LocalDateTime updatedAt,
    boolean favorite,
    boolean readable,
    boolean editable,
    boolean deletable
) {
}
