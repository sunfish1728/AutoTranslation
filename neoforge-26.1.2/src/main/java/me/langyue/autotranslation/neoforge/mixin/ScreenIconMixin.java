package me.langyue.autotranslation.neoforge.mixin;

import me.langyue.autotranslation.neoforge.NeoIconButton;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
abstract class ScreenIconMixin {
    @Shadow protected abstract <T extends GuiEventListener & Renderable & NarratableEntry> T addRenderableWidget(T widget);

    @Inject(method = "init(II)V", at = @At("TAIL"))
    private void autotranslation$icon(int width, int height, CallbackInfo callback) {
        addRenderableWidget(new NeoIconButton((Screen) (Object) this));
    }
}
