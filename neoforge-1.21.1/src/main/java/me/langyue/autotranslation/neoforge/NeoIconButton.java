package me.langyue.autotranslation.neoforge;

import com.mojang.blaze3d.systems.RenderSystem;
import me.langyue.autotranslation.client1201.ClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** NeoForge 1.21 screen-owned texture toggle. */
public final class NeoIconButton extends AbstractButton {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("autotranslation", "textures/gui/icon.png");
    private final Screen screen;
    public NeoIconButton(Screen screen) { super(0, 0, 12, 12, Component.translatable("checkbox.autotranslation.tooltip")); this.screen = screen; }
    @Override public void onPress() { DisplayTranslationRuntime.toggleCurrentScreen(); }
    @Override protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        ClientConfig c = DisplayTranslationRuntime.config(); visible = c != null && DisplayTranslationRuntime.iconVisible(screen); if (!visible) return;
        setX(x(c)); setY(y(c)); RenderSystem.enableDepthTest(); RenderSystem.enableBlend();
        graphics.blit(TEXTURE, getX(), getY(), isHoveredOrFocused() ? 12 : 0, DisplayTranslationRuntime.screenEnabled(screen) ? 12 : 0, width, height, 64, 64);
        if (isHovered) graphics.renderTooltip(Minecraft.getInstance().font, getMessage(), mouseX, mouseY);
    }
    private int x(ClientConfig c) { return switch(c.icon.displayArea) { case TOP_LEFT,MIDDLE_LEFT,BOTTOM_LEFT -> Math.abs(c.icon.offsetX)+10; case TOP_CENTER,MIDDLE_CENTER,BOTTOM_CENTER -> (screen.width-width)/2+c.icon.offsetX; default -> screen.width-width-Math.abs(c.icon.offsetX)-10; }; }
    private int y(ClientConfig c) { return switch(c.icon.displayArea) { case TOP_LEFT,TOP_CENTER,TOP_RIGHT -> Math.abs(c.icon.offsetY)+10; case MIDDLE_LEFT,MIDDLE_CENTER,MIDDLE_RIGHT -> (screen.height-height)/2+c.icon.offsetY; default -> screen.height-height-Math.abs(c.icon.offsetY)-10; }; }
    @Override protected void updateWidgetNarration(NarrationElementOutput out) { out.add(NarratedElementType.TITLE, getMessage()); }
}
