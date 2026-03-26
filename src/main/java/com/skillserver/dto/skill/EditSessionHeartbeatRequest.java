package com.skillserver.dto.skill;

import jakarta.validation.constraints.NotBlank;

public record EditSessionHeartbeatRequest(
    @NotBlank(message = "lockToken 不能为空")
    String lockToken
) {
}
