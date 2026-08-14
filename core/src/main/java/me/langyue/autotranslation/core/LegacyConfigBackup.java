package me.langyue.autotranslation.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Creates the required one-time backup before Cloth may rewrite a legacy JSON5 config. */
public final class LegacyConfigBackup {
    private LegacyConfigBackup() { }

    public static void createOnce(Path gameDirectory) throws IOException {
        Path config = gameDirectory.toAbsolutePath().resolve("config").resolve("autotranslation.json5");
        Path backup = config.resolveSibling("autotranslation.json5.pre-1.3.0.bak");
        if (Files.isRegularFile(config) && !Files.exists(backup)) {
            Files.copy(config, backup, StandardCopyOption.COPY_ATTRIBUTES);
        }
    }
}
