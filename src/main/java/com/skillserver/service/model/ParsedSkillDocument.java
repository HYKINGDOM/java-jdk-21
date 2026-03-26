package com.skillserver.service.model;

import java.util.List;

public record ParsedSkillDocument(
    String title,
    String summary,
    List<String> tags,
    String bodyMarkdown,
    String bodyPlaintext,
    String contentHash
) {
}
