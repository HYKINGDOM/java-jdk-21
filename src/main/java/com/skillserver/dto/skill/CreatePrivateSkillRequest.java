package com.skillserver.dto.skill;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record CreatePrivateSkillRequest(
    @NotBlank(message = "技能名称不能为空")
    String name,
    String slug,
    String description,
    List<String> tags,
    String skillMarkdown,
    @Valid
    List<SkillFilePayload> files
) {
}
