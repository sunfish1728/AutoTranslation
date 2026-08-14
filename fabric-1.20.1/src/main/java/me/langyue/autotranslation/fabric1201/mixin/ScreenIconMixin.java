package me.langyue.autotranslation.fabric1201.mixin;

import me.langyue.autotranslation.client1201.ClientIconButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Each newly initialized client screen owns exactly one icon; no static widget survives a screen change. */
@Mixin(Screen.class)
abstract class ScreenIconMixin {
    @Shadow protected abstract <T extends GuiEventListener & Renderable & NarratableEntry> T addRenderableWidget(T widget);

    @Inject(method = "init(Lnet/minecraft/client/Minecraft;II)V", at = @At("TAIL"))
    private void autotranslation$installIcon(Minecraft client, int width, int height, CallbackInfo callback) {
        addRenderableWidget(new ClientIconButton((Screen) (Object) this));
    }
}
