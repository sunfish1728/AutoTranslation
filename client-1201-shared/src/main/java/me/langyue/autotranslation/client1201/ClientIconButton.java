package me.langyue.autotranslation.client1201;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** A screen-owned, texture-backed toggle. It stores no Screen outside its own lifetime. */
public final class ClientIconButton extends AbstractButton {
    private static final ResourceLocation TEXTURE = new ResourceLocation("autotranslation", "textures/gui/icon.png");
    private final Screen screen;

    public ClientIconButton(Screen screen) {
        super(0, 0, 12, 12, Component.translatable("checkbox.autotranslation.tooltip"));
        this.screen = screen;
    }

    @Override public void onPress() { ClientTranslationRuntime.toggleScreen(ScreenTranslationState.screenId(screen)); }

    @Override protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float tickDelta) {
        ClientConfig config = ClientTranslationRuntime.screenConfig();
        if (config == null) { visible = false; return; }
        String id = ScreenTranslationState.screenId(screen);
        boolean enabled = ClientTranslationRuntime.screenEnabled(id);
        visible = ClientTranslationRuntime.screenAllowed(id) && (config.icon.alwaysDisplay || enabled);
        if (!visible) return;
        setX(x(config)); setY(y(config));
        RenderSystem.enableDepthTest();
        RenderSystem.enableBlend();
        graphics.blit(TEXTURE, getX(), getY(), isHoveredOrFocused() ? 12 : 0, enabled ? 12 : 0, width, height, 64, 64);
        if (isHovered) graphics.renderTooltip(Minecraft.getInstance().font, getMessage(), mouseX, mouseY);
    }

    private int x(ClientConfig config) {
        return switch (config.icon.displayArea) {
            case TOP_LEFT, MIDDLE_LEFT, BOTTOM_LEFT -> Math.abs(config.icon.offsetX) + 10;
            case TOP_CENTER, MIDDLE_CENTER, BOTTOM_CENTER -> (screen.width - width) / 2 + config.icon.offsetX;
            case TOP_RIGHT, MIDDLE_RIGHT, BOTTOM_RIGHT -> screen.width - width - Math.abs(config.icon.offsetX) - 10;
        };
    }

    private int y(ClientConfig config) {
        return switch (config.icon.displayArea) {
            case TOP_LEFT, TOP_CENTER, TOP_RIGHT -> Math.abs(config.icon.offsetY) + 10;
            case MIDDLE_LEFT, MIDDLE_CENTER, MIDDLE_RIGHT -> (screen.height - height) / 2 + config.icon.offsetY;
            case BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT -> screen.height - height - Math.abs(config.icon.offsetY) - 10;
        };
    }

    @Override protected void updateWidgetNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, getMessage());
    }
}
