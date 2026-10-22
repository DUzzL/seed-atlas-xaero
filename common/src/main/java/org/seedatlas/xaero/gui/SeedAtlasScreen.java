package org.seedatlas.xaero.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import xaero.lib.client.gui.IScreenBase;
import xaero.lib.client.gui.widget.dropdown.DropDownWidget;

/**
 * Base of the Seed Atlas menus. Like Xaero's own screens, they ask World Map to skip the world
 * behind them (its "skip world render" option); otherwise every frame redrew the whole world,
 * shaders included, only to blur it, which ran the GPU at full load while a menu was open.
 */
abstract class SeedAtlasScreen extends Screen implements IScreenBase {
	/** What vanilla allows its menus outside a world; at an unlimited limit these ran at ~500 FPS. */
	private static final int MENU_FRAMERATE_LIMIT = 60;
	protected final Screen parent;

	protected SeedAtlasScreen(final Component title, final Screen parent) {
		super(title);
		this.parent = parent;
	}

	@Override
	public void added() {
		super.added();
		this.minecraft.getFramerateLimitTracker().setFramerateLimit(
			Math.min(this.minecraft.options.framerateLimit().get(), MENU_FRAMERATE_LIMIT));
	}

	@Override
	public void removed() {
		this.minecraft.getFramerateLimitTracker().setFramerateLimit(this.minecraft.options.framerateLimit().get());
		super.removed();
	}

	@Override
	public boolean shouldSkipWorldRender() {
		return true;
	}

	@Override
	public Screen getEscape() {
		return this.parent;
	}

	@Override
	public void onDropdownOpen(final DropDownWidget menu) {
	}

	@Override
	public void onDropdownClosed(final DropDownWidget menu) {
	}

	/** Opaque and unblurred: the skipped world leaves nothing to blur, and a shown one stays covered. */
	@Override
	public void extractBackground(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
		graphics.fill(0, 0, this.width, this.height, 0xFF000000);
		extractMenuBackgroundTexture(graphics, MENU_BACKGROUND, 0, 0, 0.0F, 0.0F, this.width, this.height);
		this.minecraft.gui.hud.extractDeferredSubtitles();
	}
}
