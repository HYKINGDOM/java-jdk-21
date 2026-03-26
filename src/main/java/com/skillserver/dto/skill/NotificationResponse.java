package com.skillserver.dto.skill;

import java.time.LocalDateTime;

public record NotificationResponse(
    Long id,
    String type,
    String skillUid,
    String revision,
    String title,
    String content,
    boolean read,
    LocalDateTime createdAt
) {
}
