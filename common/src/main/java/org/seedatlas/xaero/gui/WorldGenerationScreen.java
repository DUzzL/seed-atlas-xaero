package org.seedatlas.xaero.gui;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.seedatlas.xaero.command.SeedAtlasCommands;
import org.seedatlas.xaero.config.SeedAtlasClientState;
import org.seedatlas.xaero.nativeapi.GameVersion;
import org.seedatlas.xaero.nativeapi.SeedAtlasNative;

/**
 * World type and the Minecraft version the world or server generates with, e.g. 26.2 for a 26.2
 * server joined from a 26.3 client. The version belongs to the current world or server only.
 */
public final class WorldGenerationScreen extends SeedAtlasScreen {
	private static final Component TITLE = Component.translatable("screen.seedatlas_xaero.world_generation");
	private static final int COLUMNS = 4;
	private static final int ROW_HEIGHT = 22;
	private static final int GRID_TOP = 76;

	private final List<VersionButton> versionButtons = new ArrayList<>();

	public WorldGenerationScreen(final Screen parent) {
		super(TITLE, parent);
	}

	@Override
	protected void init() {
		int contentWidth = Math.min(340, this.width - 20);
		int left = (this.width - contentWidth) / 2;

		this.addRenderableWidget(Button.builder(worldTypeMessage(SeedAtlasClientState.config().largeBiomes()), button -> {
			boolean large = !SeedAtlasClientState.config().largeBiomes();
			SeedAtlasClientState.setLargeBiomes(large);
			button.setMessage(worldTypeMessage(large));
		}).bounds(left, 34, contentWidth, 20).build());

		boolean hasContext = !SeedAtlasClientState.currentContextKey().isEmpty();
		Tooltip versionTooltip = Tooltip.create(Component.translatable("tooltip.seedatlas_xaero.game_version"));
		this.versionButtons.clear();
		List<GameVersion> choices = new ArrayList<>();
		choices.add(null);
		choices.addAll(GameVersion.ALL);
		int gap = 4;
		int cellWidth = (contentWidth - gap * (COLUMNS - 1)) / COLUMNS;
		for (int index = 0; index < choices.size(); index++) {
			GameVersion version = choices.get(index);
			int x = left + (index % COLUMNS) * (cellWidth + gap);
			int y = GRID_TOP + (index / COLUMNS) * ROW_HEIGHT;
			VersionButton entry = new VersionButton(version, Button.builder(Component.empty(), button -> {
				SeedAtlasClientState.setGameVersion(version);
				this.refreshVersionButtons();
			}).bounds(x, y, cellWidth, 20).tooltip(versionTooltip).build());
			entry.button.active = hasContext && (version == null || SeedAtlasNative.supports(version));
			this.versionButtons.add(entry);
			this.addRenderableWidget(entry.button);
		}
		this.refreshVersionButtons();

		this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> this.onClose())
			.bounds((this.width - 200) / 2, this.height - 28, 200, 20)
			.build());
	}

	private void refreshVersionButtons() {
		boolean automatic = SeedAtlasClientState.gameVersionAutomatic();
		GameVersion active = SeedAtlasClientState.activeGameVersion();
		for (VersionButton entry : this.versionButtons) {
			boolean selected = entry.version == null ? automatic : !automatic && entry.version.equals(active);
			Component label = entry.version == null
				? Component.translatable("options.seedatlas_xaero.game_version.auto", GameVersion.NEWEST.name())
				: Component.literal(entry.version.name());
			entry.button.setMessage(selected
				? Component.literal("> ").append(label).append(" <").withStyle(ChatFormatting.GREEN)
				: label);
		}
	}

	@Override
	public void extractRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float partialTick) {
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);
		graphics.centeredText(this.font, this.title, this.width / 2, 14, -1);
		Component heading = SeedAtlasClientState.currentContextKey().isEmpty()
			? Component.translatable("screen.seedatlas_xaero.game_version.no_context")
			: Component.translatable("screen.seedatlas_xaero.game_version", SeedAtlasCommands.gameVersionName());
		graphics.centeredText(this.font, heading, this.width / 2, 62, 0xFFA0A0A0);
		int rows = (GameVersion.ALL.size() + COLUMNS) / COLUMNS;
		int noteY = GRID_TOP + rows * ROW_HEIGHT + 4;
		if (noteY + 10 < this.height - 32) {
			Component note = SeedAtlasNative.supports(GameVersion.ALL.getLast())
				? Component.translatable("screen.seedatlas_xaero.game_version.note")
				: Component.translatable("screen.seedatlas_xaero.game_version.unsupported", GameVersion.NEWEST.name());
			graphics.centeredText(this.font, note, this.width / 2, noteY, 0xFFA0A0A0);
		}
	}

	@Override
	public void onClose() {
		SeedAtlasClientState.save();
		// Parent is the main Seed Atlas settings screen; keep heavy work paused.
		this.minecraft.gui.setScreen(this.parent);
	}

	static Component worldTypeMessage(final boolean largeBiomes) {
		return Component.translatable(
			"options.seedatlas_xaero.world_type",
			Component.translatable(largeBiomes ? "options.seedatlas_xaero.world_type.large" : "options.seedatlas_xaero.world_type.normal")
		);
	}

	private record VersionButton(GameVersion version, Button button) { }
}
