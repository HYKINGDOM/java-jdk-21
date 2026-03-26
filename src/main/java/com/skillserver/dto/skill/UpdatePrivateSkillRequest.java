package com.skillserver.dto.skill;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record UpdatePrivateSkillRequest(
    @NotBlank(message = "lockToken 不能为空")
    String lockToken,
    @NotBlank(message = "baseRevision 不能为空")
    String baseRevision,
    String summary,
    @Valid
    List<SkillFilePayload> upserts,
    List<String> deletePaths
) {
}
