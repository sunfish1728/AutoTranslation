package me.langyue.autotranslation.core;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Creates a resource pack with the JDK ZIP implementation and atomically publishes it. */
public final class ResourcePackWriter {
    private ResourcePackWriter() { }

    public static void write(Path target, Map<String, Path> entries) throws IOException {
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(entries, "entries");
        Path absoluteTarget = target.toAbsolutePath();
        Files.createDirectories(absoluteTarget.getParent());
        Path temporary = absoluteTarget.resolveSibling(absoluteTarget.getFileName() + ".tmp");
        try {
            try (OutputStream file = Files.newOutputStream(temporary);
                 ZipOutputStream zip = new ZipOutputStream(new BufferedOutputStream(file))) {
                for (Map.Entry<String, Path> entry : new TreeMap<>(entries).entrySet()) {
                    String name = validateEntryName(entry.getKey());
                    Path source = Objects.requireNonNull(entry.getValue(), "entry source");
                    ZipEntry zipEntry = new ZipEntry(name);
                    zipEntry.setTime(0L);
                    zip.putNextEntry(zipEntry);
                    try (InputStream input = new BufferedInputStream(Files.newInputStream(source))) {
                        input.transferTo(zip);
                    }
                    zip.closeEntry();
                }
            }
            try {
                Files.move(temporary, absoluteTarget, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (java.nio.file.AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, absoluteTarget, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static String validateEntryName(String rawName) {
        String name = Objects.requireNonNull(rawName, "entry name").replace('\\', '/');
        if (name.isBlank() || name.startsWith("/") || name.contains("../") || name.equals("..") || name.contains(":")) {
            throw new IllegalArgumentException("Unsafe ZIP entry name");
        }
        return name;
    }
}
