package com.skillserver.dto.skill;

public record FileNodeResponse(
    String relativePath,
    String fileName,
    boolean dir,
    String fileExt,
    Long sizeBytes,
    String mimeType,
    String previewMode,
    int sortOrder
) {
}
