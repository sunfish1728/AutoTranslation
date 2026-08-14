package me.langyue.autotranslation.core;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Writes cache/config data through a sibling temp file and keeps one backup. */
public final class AtomicFileStore {
    private AtomicFileStore() { }
    public static void writeUtf8(Path target, String content) throws IOException {
        Files.createDirectories(target.toAbsolutePath().getParent());
        Path temporary = target.resolveSibling(target.getFileName() + ".tmp");
        Path backup = target.resolveSibling(target.getFileName() + ".bak");
        if (Files.exists(target)) Files.copy(target, backup, StandardCopyOption.REPLACE_EXISTING);
        Files.writeString(temporary, content, StandardCharsets.UTF_8);
        try { Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
        catch (java.nio.file.AtomicMoveNotSupportedException ignored) { Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING); }
    }
}
