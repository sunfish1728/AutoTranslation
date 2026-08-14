package me.langyue.autotranslation.forge1201;

import me.langyue.autotranslation.core.Platform;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Path;

final class ForgePlatform implements Platform {
    @Override public Path gameDirectory() { return FMLPaths.GAMEDIR.get(); }
    @Override public void runOnClient(Runnable task) { Minecraft.getInstance().execute(task); }
}
