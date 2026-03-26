package com.skillserver.service.model;

import com.skillserver.domain.enums.PreviewMode;

public record SnapshotEntry(
    String relativePath,
    String fileName,
    boolean directory,
    String fileExt,
    long sizeBytes,
    String mimeType,
    PreviewMode previewMode,
    int sortOrder,
    String sha256
) {
}
