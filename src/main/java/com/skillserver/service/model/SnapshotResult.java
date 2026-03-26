package com.skillserver.service.model;

import java.util.List;

public record SnapshotResult(
    List<SnapshotEntry> entries,
    String treeFingerprint,
    String skillMdFingerprint,
    String manifestJson,
    ParsedSkillDocument parsedSkillDocument
) {
}
