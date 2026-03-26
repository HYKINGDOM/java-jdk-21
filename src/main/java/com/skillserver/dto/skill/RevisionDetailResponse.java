package com.skillserver.dto.skill;

import java.time.LocalDateTime;
import java.util.List;

public record RevisionDetailResponse(
    String skillUid,
    String revision,
    String parentRevision,
    String committedBy,
    LocalDateTime committedAt,
    String summary,
    List<RevisionFileResponse> files
) {
}
