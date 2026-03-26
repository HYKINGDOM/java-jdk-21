package com.skillserver.service.model;

import com.skillserver.domain.enums.ChangeType;
import com.skillserver.domain.enums.PreviewMode;

public record ChangedFile(
    String relativePath,
    ChangeType changeType,
    boolean skillMd,
    String fileExt,
    Long sizeBefore,
    Long sizeAfter,
    String hashBefore,
    String hashAfter,
    PreviewMode previewMode
) {
}
