package me.langyue.autotranslation.neoforge;

import me.langyue.autotranslation.client1201.ClientConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/** NeoForge 26.1 screen-owned, display-only translation toggle. */
public final class NeoIconButton extends AbstractButton {
    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath("autotranslation", "textures/gui/icon.png");
    private final Screen screen;

    public NeoIconButton(Screen screen) {
        super(0, 0, 12, 12, Component.translatable("checkbox.autotranslation.tooltip"));
        this.screen = screen;
    }

    @Override public void onPress(InputWithModifiers input) { DisplayTranslationRuntime.toggleCurrentScreen(); }

    @Override protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        ClientConfig config = DisplayTranslationRuntime.config();
        visible = config != null && DisplayTranslationRuntime.iconVisible(screen);
        if (!visible) return;
        setX(x(config));
        setY(y(config));
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, getX(), getY(),
                isHoveredOrFocused() ? 12 : 0,
                DisplayTranslationRuntime.screenEnabled(screen) ? 12 : 0,
                width, height, 64, 64);
        if (isHovered()) graphics.setTooltipForNextFrame(getMessage(), mouseX, mouseY);
    }

    private int x(ClientConfig config) {
        return switch (config.icon.displayArea) {
            case TOP_LEFT, MIDDLE_LEFT, BOTTOM_LEFT -> Math.abs(config.icon.offsetX) + 10;
            case TOP_CENTER, MIDDLE_CENTER, BOTTOM_CENTER -> (screen.width - width) / 2 + config.icon.offsetX;
            default -> screen.width - width - Math.abs(config.icon.offsetX) - 10;
        };
    }

    private int y(ClientConfig config) {
        return switch (config.icon.displayArea) {
            case TOP_LEFT, TOP_CENTER, TOP_RIGHT -> Math.abs(config.icon.offsetY) + 10;
            case MIDDLE_LEFT, MIDDLE_CENTER, MIDDLE_RIGHT -> (screen.height - height) / 2 + config.icon.offsetY;
            default -> screen.height - height - Math.abs(config.icon.offsetY) - 10;
        };
    }

    @Override protected void updateWidgetNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, getMessage());
    }
}
