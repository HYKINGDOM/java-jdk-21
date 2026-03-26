package com.skillserver.common.util;

import com.skillserver.domain.enums.PreviewMode;
import java.nio.file.Path;
import java.util.Set;

public final class PreviewModeResolver {

    private static final Set<String> TEXT_EXTENSIONS = Set.of(
        "md", "txt", "json", "yaml", "yml", "xml", "java", "kt", "groovy", "properties",
        "js", "ts", "tsx", "jsx", "py", "sh", "sql"
    );
    private static final Set<String> IMAGE_EXTENSIONS = Set.of("png", "jpg", "jpeg", "gif", "webp", "svg");

    private PreviewModeResolver() {
    }

    public static PreviewMode resolve(Path path, boolean directory) {
        if (directory) {
            return PreviewMode.TREE;
        }
        String extension = extension(path.getFileName().toString());
        if (TEXT_EXTENSIONS.contains(extension)) {
            return PreviewMode.TEXT;
        }
        if (IMAGE_EXTENSIONS.contains(extension)) {
            return PreviewMode.IMAGE;
        }
        if ("pdf".equals(extension)) {
            return PreviewMode.PDF;
        }
        if ("html".equals(extension) || "htm".equals(extension)) {
            return PreviewMode.HTML_SANDBOX;
        }
        return PreviewMode.DOWNLOAD;
    }

    public static String extension(String name) {
        int index = name.lastIndexOf('.');
        if (index < 0 || index == name.length() - 1) {
            return "";
        }
        return name.substring(index + 1).toLowerCase();
    }
}
