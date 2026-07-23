package org.seedatlas.xaero.gui;

import java.util.OptionalLong;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.seedatlas.xaero.config.SeedAtlasClientState;
import org.seedatlas.xaero.config.SeedAtlasConfig;
import org.seedatlas.xaero.integration.SeedAtlasXaeroIntegration;
import xaero.map.gui.GuiMap;

/** Compact main settings screen, visually kept close to Xaero's option menus. */
public final class SeedAtlasSettingsScreen extends Screen {
	private static final Component TITLE = Component.translatable("screen.seedatlas_xaero.settings");
	private final Screen parent;
	private OpacitySlider opacity;
	private EditBox overworldY;
	private EditBox netherY;
	private EditBox endY;

	public SeedAtlasSettingsScreen(final Screen parent) {
		super(TITLE);
		this.parent = parent;
	}

	@Override
	protected void init() {
		SeedAtlasXaeroIntegration.setHeavyWorkPaused(true);
		SeedAtlasConfig config = SeedAtlasClientState.config();
		int contentWidth = Math.min(340, this.width - 20);
		int left = (this.width - contentWidth) / 2;
		int gap = 6;
		int halfWidth = (contentWidth - gap) / 2;

		Button layer = Button.builder(layerMessage(config.layerEnabled()), button -> {
			SeedAtlasClientState.toggleLayer();
			button.setMessage(layerMessage(SeedAtlasClientState.config().layerEnabled()));
		}).bounds(left, 34, halfWidth, 20).build();
		this.addRenderableWidget(layer);

		Button worldType = Button.builder(worldTypeMessage(config.largeBiomes()), button -> {
			boolean large = !SeedAtlasClientState.config().largeBiomes();
			SeedAtlasClientState.setLargeBiomes(large);
			button.setMessage(worldTypeMessage(large));
		}).bounds(left + halfWidth + gap, 34, halfWidth, 20).build();
		this.addRenderableWidget(worldType);

		this.opacity = new OpacitySlider(left, 59, contentWidth, config.opacity());
		this.addRenderableWidget(this.opacity);

		int fieldGap = 6;
		int fieldWidth = (contentWidth - fieldGap * 2) / 3;
		this.overworldY = heightField(left, 98, fieldWidth, "options.seedatlas_xaero.height.overworld", config.overworldY());
		this.netherY = heightField(left + fieldWidth + fieldGap, 98, fieldWidth, "options.seedatlas_xaero.height.nether", config.netherY());
		this.endY = heightField(left + (fieldWidth + fieldGap) * 2, 98, fieldWidth, "options.seedatlas_xaero.height.end", config.endY());
		this.addRenderableWidget(this.overworldY);
		this.addRenderableWidget(this.netherY);
		this.addRenderableWidget(this.endY);

		Button structures = Button.builder(structuresMessage(config.structuresEnabled()), button -> {
			boolean enabled = !SeedAtlasClientState.config().structuresEnabled();
			SeedAtlasClientState.setStructuresEnabled(enabled);
			button.setMessage(structuresMessage(enabled));
		}).bounds(left, 125, halfWidth, 20).build();
		this.addRenderableWidget(structures);

		this.addRenderableWidget(Button.builder(Component.translatable("options.seedatlas_xaero.markers"), button -> {
			this.commitValues();
			this.minecraft.gui.setScreen(new MarkerSettingsScreen(this));
		}).bounds(left + halfWidth + gap, 125, halfWidth, 20).build());

		this.addRenderableWidget(Button.builder(Component.translatable("options.seedatlas_xaero.performance"), button -> {
			this.commitValues();
			this.minecraft.gui.setScreen(new PerformanceSettingsScreen(this));
		}).bounds(left, 150, contentWidth, 20).build());

		this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> this.onClose())
			.bounds((this.width - 200) / 2, this.height - 28, 200, 20)
			.build());
	}

	private EditBox heightField(final int x, final int y, final int width, final String narrationKey, final int value) {
		EditBox field = new EditBox(this.font, x, y, width, 20, Component.translatable(narrationKey));
		field.setMaxLength(6);
		field.setValue(Integer.toString(value));
		field.setHint(Component.literal(Integer.toString(value)));
		return field;
	}

	@Override
	public void extractRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float partialTick) {
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);
		graphics.centeredText(this.font, this.title, this.width / 2, 14, -1);

		int contentWidth = Math.min(340, this.width - 20);
		int left = (this.width - contentWidth) / 2;
		int fieldGap = 6;
		int fieldWidth = (contentWidth - fieldGap * 2) / 3;
		graphics.centeredText(this.font, Component.translatable("options.seedatlas_xaero.height.overworld"), left + fieldWidth / 2, 87, 0xFFA0A0A0);
		graphics.centeredText(
			this.font,
			Component.translatable("options.seedatlas_xaero.height.nether"),
			left + fieldWidth + fieldGap + fieldWidth / 2,
			87,
			0xFFA0A0A0
		);
		graphics.centeredText(
			this.font,
			Component.translatable("options.seedatlas_xaero.height.end"),
			left + (fieldWidth + fieldGap) * 2 + fieldWidth / 2,
			87,
			0xFFA0A0A0
		);

		OptionalLong seed = SeedAtlasClientState.activeSeed();
		Component status = seed.isPresent()
			? Component.translatable("screen.seedatlas_xaero.active_seed", seed.getAsLong())
			: Component.translatable("screen.seedatlas_xaero.no_seed");
		graphics.centeredText(this.font, status, this.width / 2, 176, 0xFFA0A0A0);
	}

	@Override
	public void onClose() {
		this.commitValues();
		SeedAtlasClientState.save();
		// Only resume when returning to the world map, not when a nested
		// settings screen is about to open on top of this one.
		if (this.parent instanceof GuiMap) {
			SeedAtlasXaeroIntegration.setHeavyWorkPaused(false);
		}
		this.minecraft.gui.setScreen(this.parent);
	}

	private void commitValues() {
		if (this.opacity != null) {
			this.opacity.commit();
		}
		if (this.overworldY != null) {
			SeedAtlasClientState.setOverworldY(parseHeight(this.overworldY.getValue(), SeedAtlasClientState.config().overworldY()));
		}
		if (this.netherY != null) {
			SeedAtlasClientState.setNetherY(parseHeight(this.netherY.getValue(), SeedAtlasClientState.config().netherY()));
		}
		if (this.endY != null) {
			SeedAtlasClientState.setEndY(parseHeight(this.endY.getValue(), SeedAtlasClientState.config().endY()));
		}
	}

	private static int parseHeight(final String input, final int fallback) {
		try {
			return Integer.parseInt(input.trim());
		} catch (NumberFormatException exception) {
			return fallback;
		}
	}

	private static Component layerMessage(final boolean enabled) {
		return Component.translatable("options.seedatlas_xaero.layer", toggleValue(enabled));
	}

	private static Component worldTypeMessage(final boolean largeBiomes) {
		return Component.translatable(
			"options.seedatlas_xaero.world_type",
			Component.translatable(largeBiomes ? "options.seedatlas_xaero.world_type.large" : "options.seedatlas_xaero.world_type.normal")
		);
	}

	private static Component structuresMessage(final boolean enabled) {
		return Component.translatable("options.seedatlas_xaero.structures", toggleValue(enabled));
	}

	static Component toggleValue(final boolean enabled) {
		return Component.translatable(enabled ? "options.seedatlas_xaero.on" : "options.seedatlas_xaero.off");
	}

	private static final class OpacitySlider extends AbstractSliderButton {
		private int pendingOpacity;

		private OpacitySlider(final int x, final int y, final int width, final int opacity) {
			super(x, y, width, 20, Component.empty(), opacity / 255.0D);
			this.pendingOpacity = opacity;
			this.setTooltip(Tooltip.create(Component.translatable("tooltip.seedatlas_xaero.opacity")));
			this.updateMessage();
		}

		@Override
		protected void updateMessage() {
			this.setMessage(Component.translatable("options.seedatlas_xaero.opacity", Math.round(this.value * 100.0D)));
		}

		@Override
		protected void applyValue() {
			this.pendingOpacity = (int)Math.round(this.value * 255.0D);
		}

		private void commit() {
			SeedAtlasClientState.setOpacity(this.pendingOpacity);
		}
	}
}
