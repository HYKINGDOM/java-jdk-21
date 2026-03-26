package com.skillserver.service.model;

import java.util.List;

public record ChangeSummary(
    List<ChangedFile> changedFiles,
    boolean hasSkillMdChange,
    boolean hasOtherFilesChange,
    String changeScope,
    int addedFilesCount,
    int modifiedFilesCount,
    int deletedFilesCount
) {
}
