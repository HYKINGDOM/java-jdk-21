package com.skillserver.dto.skill;

import java.time.LocalDateTime;

public record EditSessionResponse(
    String skillUid,
    String lockToken,
    String lockedBy,
    String baseRevision,
    String status,
    LocalDateTime expireAt,
    LocalDateTime heartbeatAt
) {
}
