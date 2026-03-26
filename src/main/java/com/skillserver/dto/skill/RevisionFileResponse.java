package com.skillserver.dto.skill;

public record RevisionFileResponse(
    String relativePath,
    String changeType,
    boolean skillMd,
    String previewMode,
    Long sizeBefore,
    Long sizeAfter
) {
}
