package com.skillserver.dto.skill;

import jakarta.validation.constraints.NotBlank;

public record RepoImportRequest(
    @NotBlank(message = "仓库名称不能为空")
    String name,
    @NotBlank(message = "仓库地址不能为空")
    String repoUrl,
    @NotBlank(message = "分支不能为空")
    String branch,
    Boolean syncEnabled,
    String syncCron
) {
}
