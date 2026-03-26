package com.skillserver.dto.skill;

import jakarta.validation.constraints.NotBlank;

public record SkillFilePayload(
    @NotBlank(message = "文件路径不能为空")
    String path,
    String content
) {
}
