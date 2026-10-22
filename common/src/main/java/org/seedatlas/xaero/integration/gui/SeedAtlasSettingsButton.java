package org.seedatlas.xaero.integration.gui;

import java.util.function.Supplier;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import xaero.lib.client.gui.widget.Tooltip;
import xaero.map.gui.TooltipButton;

/**
 * Seed Atlas settings on the world map: a regular framed button with Minecraft's filled map, next
 * to the SA layer button, so it stands apart from Xaero's monochrome toolbar icons.
 */
public final class SeedAtlasSettingsButton extends TooltipButton {
    private static final Identifier ICON = Identifier.withDefaultNamespace("textures/item/filled_map.png");

    public SeedAtlasSettingsButton(int x, int y, Button.OnPress onPress, Supplier<Tooltip> tooltip) {
        super(x, y, 20, 20, Component.translatable("screen.seedatlas_xaero.settings"), onPress, tooltip);
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        // Frame only; the label stays for narration and the icon replaces its text.
        this.extractDefaultSprite(graphics);
        graphics.blit(RenderPipelines.GUI_TEXTURED, ICON, this.getX() + 2, this.getY() + 2, 0.0F, 0.0F, 16, 16, 16, 16);
    }
}
