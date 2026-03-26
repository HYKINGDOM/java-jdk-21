package com.skillserver.service;

import com.skillserver.common.util.HashUtils;
import com.skillserver.service.model.ParsedSkillDocument;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.stereotype.Component;

@Component
public class SkillMarkdownParser {

    private static final Pattern H1_PATTERN = Pattern.compile("(?m)^#\\s+(.+)$");
    private static final Pattern FRONTMATTER_PATTERN = Pattern.compile("^---\\s*\\R(.*?)\\R---\\s*\\R?", Pattern.DOTALL);
    private static final Pattern LABEL_PATTERN = Pattern.compile("(?m)^标签\\s*[:：]\\s*(.+)$");

    private final Parser parser = Parser.builder().build();
    private final HtmlRenderer renderer = HtmlRenderer.builder().build();

    public ParsedSkillDocument parse(String markdown, String fallbackTitle) {
        String title = extractTitle(markdown, fallbackTitle);
        List<String> tags = extractTags(markdown);
        String summary = extractSummary(markdown);
        String plaintext = stripMarkdown(markdown);
        return new ParsedSkillDocument(title, summary, tags, markdown, plaintext, HashUtils.sha256(markdown));
    }

    public String renderHtml(String markdown) {
        return renderer.render(parser.parse(markdown));
    }

    private String extractTitle(String markdown, String fallbackTitle) {
        Matcher matcher = H1_PATTERN.matcher(markdown);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return fallbackTitle;
    }

    private List<String> extractTags(String markdown) {
        Set<String> tags = new LinkedHashSet<>();
        Matcher frontmatter = FRONTMATTER_PATTERN.matcher(markdown);
        if (frontmatter.find()) {
            String block = frontmatter.group(1);
            Matcher tagsLine = Pattern.compile("(?m)^tags\\s*:\\s*(.*)$").matcher(block);
            if (tagsLine.find()) {
                String sameLine = tagsLine.group(1).trim();
                if (!sameLine.isBlank()) {
                    tags.addAll(splitTags(sameLine));
                }
                Matcher listMatcher = Pattern.compile("(?m)^\\s*-\\s*(.+)$").matcher(block);
                while (listMatcher.find()) {
                    String value = listMatcher.group(1).trim();
                    if (!value.isBlank()) {
                        tags.add(value);
                    }
                }
            }
        }
        Matcher labelMatcher = LABEL_PATTERN.matcher(markdown);
        if (labelMatcher.find()) {
            tags.addAll(splitTags(labelMatcher.group(1)));
        }
        return List.copyOf(tags);
    }

    private List<String> splitTags(String raw) {
        return Arrays.stream(raw.split("[,，]"))
            .map(String::trim)
            .filter(value -> !value.isBlank())
            .toList();
    }

    private String extractSummary(String markdown) {
        List<String> paragraphs = new ArrayList<>();
        boolean inCodeBlock = false;
        for (String line : markdown.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("```")) {
                inCodeBlock = !inCodeBlock;
                continue;
            }
            if (inCodeBlock || trimmed.isBlank() || trimmed.startsWith("#") || trimmed.startsWith("- ") || trimmed.startsWith("* ")) {
                continue;
            }
            paragraphs.add(trimmed);
        }
        return paragraphs.stream().findFirst().orElse("暂无摘要");
    }

    private String stripMarkdown(String markdown) {
        return markdown
            .replaceAll("(?s)```.*?```", " ")
            .replaceAll("(?m)^#+\\s*", "")
            .replaceAll("(?m)^>\\s*", "")
            .replaceAll("(?m)^[-*+]\\s*", "")
            .replaceAll("`", "")
            .replaceAll("\\[(.*?)]\\((.*?)\\)", "$1")
            .replaceAll("[*_~]", "")
            .replaceAll("\\s{2,}", " ")
            .trim()
            .toLowerCase(Locale.ROOT);
    }
}
