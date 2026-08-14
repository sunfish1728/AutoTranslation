package me.langyue.autotranslation.fabric1201;

import me.langyue.autotranslation.core.Platform;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

import java.nio.file.Path;

final class FabricPlatform implements Platform {
    @Override public Path gameDirectory() { return FabricLoader.getInstance().getGameDir(); }
    @Override public void runOnClient(Runnable task) { Minecraft.getInstance().execute(task); }
    static void registerShutdown(AutoCloseable closeable) {
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> close(closeable));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> { });
    }
    private static void close(AutoCloseable closeable) { try { closeable.close(); } catch (Exception ignored) { } }
}
