package me.langyue.autotranslation.core;

import java.nio.file.Path;

/** Native loader bridge contract. No loader or Minecraft class is allowed here. */
public interface Platform {
    Path gameDirectory();
    void runOnClient(Runnable task);
}
