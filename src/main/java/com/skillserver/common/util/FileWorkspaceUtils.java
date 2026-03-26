package com.skillserver.common.util;

import com.skillserver.common.exception.BadRequestException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Comparator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public final class FileWorkspaceUtils {

    private FileWorkspaceUtils() {
    }

    public static void ensureDirectory(Path directory) {
        try {
            Files.createDirectories(directory);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to create directory: " + directory, ex);
        }
    }

    public static void deleteRecursively(Path path) {
        if (path == null || Files.notExists(path)) {
            return;
        }
        try {
            Files.walk(path)
                .sorted(Comparator.reverseOrder())
                .forEach(current -> {
                    try {
                        Files.deleteIfExists(current);
                    } catch (IOException ex) {
                        throw new IllegalStateException("Failed to delete path: " + current, ex);
                    }
                });
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to delete path: " + path, ex);
        }
    }

    public static void copyDirectory(Path source, Path target) {
        ensureDirectory(target);
        try {
            Files.walkFileTree(source, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    Path relative = source.relativize(dir);
                    Files.createDirectories(target.resolve(relative));
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Path relative = source.relativize(file);
                    Files.copy(file, target.resolve(relative), StandardCopyOption.REPLACE_EXISTING);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to copy directory: " + source, ex);
        }
    }

    public static void copyDirectoryExcludingGit(Path source, Path target) {
        ensureDirectory(target);
        try {
            Files.walkFileTree(source, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    Path relative = source.relativize(dir);
                    String relativePath = relative.toString().replace('\\', '/');
                    if (!relativePath.isEmpty() && relativePath.startsWith(".git")) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    Files.createDirectories(target.resolve(relative));
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Path relative = source.relativize(file);
                    String relativePath = relative.toString().replace('\\', '/');
                    if (relativePath.startsWith(".git")) {
                        return FileVisitResult.CONTINUE;
                    }
                    Files.copy(file, target.resolve(relative), StandardCopyOption.REPLACE_EXISTING);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to copy directory: " + source, ex);
        }
    }

    public static byte[] zipDirectory(Path root) {
        try (java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream();
             ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream)) {
            Files.walk(root)
                .filter(path -> !path.equals(root))
                .sorted()
                .forEach(path -> {
                    String relative = root.relativize(path).toString().replace('\\', '/');
                    try {
                        ZipEntry entry = new ZipEntry(Files.isDirectory(path) ? relative + "/" : relative);
                        zipOutputStream.putNextEntry(entry);
                        if (Files.isRegularFile(path)) {
                            Files.copy(path, zipOutputStream);
                        }
                        zipOutputStream.closeEntry();
                    } catch (IOException ex) {
                        throw new IllegalStateException("Failed to zip path: " + path, ex);
                    }
                });
            zipOutputStream.finish();
            return outputStream.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to zip directory: " + root, ex);
        }
    }

    public static void unzipSecurely(InputStream inputStream, Path targetDirectory) {
        ensureDirectory(targetDirectory);
        try (ZipInputStream zipInputStream = new ZipInputStream(inputStream)) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                Path resolved = targetDirectory.resolve(entry.getName()).normalize();
                if (!resolved.startsWith(targetDirectory.normalize())) {
                    throw new BadRequestException("ZIP contains illegal path: " + entry.getName());
                }
                if (entry.isDirectory()) {
                    Files.createDirectories(resolved);
                } else {
                    Files.createDirectories(resolved.getParent());
                    try (OutputStream outputStream = Files.newOutputStream(resolved)) {
                        zipInputStream.transferTo(outputStream);
                    }
                }
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to unzip archive", ex);
        }
    }

    public static String normalizeRelativePath(String rawPath) {
        if (rawPath == null || rawPath.isBlank()) {
            throw new BadRequestException("Path cannot be blank");
        }
        String normalized = rawPath.replace('\\', '/').replaceAll("^/+", "").trim();
        if (normalized.contains("..")) {
            throw new BadRequestException("Path cannot contain '..': " + rawPath);
        }
        return normalized;
    }
}
