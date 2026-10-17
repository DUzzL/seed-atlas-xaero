package org.seedatlas.xaero.gui;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.seedatlas.xaero.config.MarkerType;
import org.seedatlas.xaero.integration.MapNavigator;
import org.seedatlas.xaero.integration.SeedAtlasXaeroIntegration;
import org.seedatlas.xaero.integration.marker.NearestFinder;
import org.seedatlas.xaero.nativeapi.BiomeSample;

/** Multi-select biome/structure picker (same lists as the settings pages); opens the map at the nearest. */
public final class NearestScreen extends Screen {
	private static final int FOOTER_HEIGHT = 38;
	// Remembered for the session only.
	private static MapNavigator.WaypointMode waypointMode = MapNavigator.WaypointMode.TEMPORARY;

	private final Screen parent;
	private final boolean biomeMode;
	private final Set<Integer> selectedBiomes = new HashSet<>();
	private final Set<MarkerType> selectedTypes = new HashSet<>();
	private List<BiomeSample> biomes = List.of();
	private ResourceKey<Level> level;
	private int px;
	private int pz;
	private boolean busy;
	private boolean noSeed;
	private Button confirm;
	private Component status = Component.empty();

	public NearestScreen(final Screen parent, final boolean biomeMode) {
		super(Component.translatable(biomeMode ? "screen.seedatlas_xaero.go_biome" : "screen.seedatlas_xaero.go_structure"));
		this.parent = parent;
		this.biomeMode = biomeMode;
	}

	@Override
	protected void init() {
		SeedAtlasXaeroIntegration.setHeavyWorkPaused(true);
		var player = this.minecraft.player;
		if (player == null) return;
		this.level = player.level().dimension();
		this.px = player.getBlockX();
		this.pz = player.getBlockZ();

		int margin = Math.min(16, Math.max(6, this.width / 18));
		int listWidth = Math.min(420, Math.max(180, this.width - margin * 2));
		listWidth = Math.min(listWidth, this.width - 8);
		int left = (this.width - listWidth) / 2;
		int top = this.biomeMode ? 92 : 66;
		int bottom = Math.min(Math.max(top + 48, this.height - FOOTER_HEIGHT), this.height - 26);
		int listHeight = Math.max(48, bottom - top);

		this.addRenderableWidget(Button.builder(waypointMessage(), button -> {
			var modes = MapNavigator.WaypointMode.values();
			waypointMode = modes[(waypointMode.ordinal() + 1) % modes.length];
			button.setMessage(waypointMessage());
		}).bounds(left, 38, listWidth, 20).build());

		List<BiomeSample> available = SeedAtlasXaeroIntegration.availableBiomes();
		this.noSeed = available.isEmpty();
		if (!this.noSeed && this.biomeMode) {
			this.biomes = available.stream()
				.sorted(Comparator.comparing(b -> SeedAtlasXaeroIntegration.biomeDisplayName(b.name()).getString()))
				.toList();
			BiomeToggleList list = new BiomeToggleList(this.minecraft, left, top, listWidth, listHeight, this.biomes, this::refresh);
			list.useSelection(this.selectedBiomes);
			EditBox search = new EditBox(this.font, left, 64, listWidth, 20, Component.translatable("options.seedatlas_xaero.biome_search"));
			search.setHint(Component.translatable("options.seedatlas_xaero.biome_search"));
			search.setResponder(list::setFilter);
			this.addRenderableWidget(search);
			this.addRenderableWidget(list);
		} else if (!this.noSeed) {
			List<MarkerType> types = MarkerType.all().stream()
				.filter(t -> NearestFinder.appliesTo(t, this.level)).toList();
			MarkerToggleList list = new MarkerToggleList(this.minecraft, left, top, listWidth, listHeight, types);
			list.useSelection(this.selectedTypes, this::refresh);
			this.addRenderableWidget(list);
		}

		this.confirm = this.addRenderableWidget(Button.builder(Component.empty(), button -> start())
			.bounds((this.width - 204) / 2, Math.max(0, this.height - 28), 100, 20).build());
		this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> this.onClose())
			.bounds((this.width - 204) / 2 + 104, Math.max(0, this.height - 28), 100, 20).build());
		refresh();
	}

	private int selectedCount() {
		return this.biomeMode ? this.selectedBiomes.size() : this.selectedTypes.size();
	}

	private void refresh() {
		this.confirm.setMessage(Component.translatable("options.seedatlas_xaero.nearest_confirm", selectedCount()));
		this.confirm.active = selectedCount() > 0 && !this.busy;
	}

	private static Component waypointMessage() {
		return Component.translatable("options.seedatlas_xaero.nearest_waypoint",
			Component.translatable("options.seedatlas_xaero.nearest_waypoint." + waypointMode.name().toLowerCase(Locale.ROOT)));
	}

	private record Hit(String name, NearestFinder.Result result) { }

	private void start() {
		if (this.busy || selectedCount() == 0) return;
		this.busy = true;
		this.status = Component.translatable("message.seedatlas_xaero.nearest_searching");
		refresh();
		Set<Integer> wantedBiomes = Set.copyOf(this.selectedBiomes);
		List<MarkerType> wantedTypes = List.copyOf(this.selectedTypes);
		List<BiomeSample> known = this.biomes;
		Minecraft mc = this.minecraft;
		Thread thread = new Thread(() -> {
			Hit hit = null;
			try {
				if (this.biomeMode) {
					var r = NearestFinder.biome(wantedBiomes, this.level, this.px, this.pz);
					if (r != null) {
						String name = known.stream().filter(b -> b.id() == r.id()).findFirst()
							.map(b -> SeedAtlasXaeroIntegration.biomeDisplayName(b.name()).getString()).orElse("Biome");
						hit = new Hit(name, r);
					}
				} else {
					// Structure scans are independent, so run them concurrently.
					hit = wantedTypes.parallelStream().map(t -> {
						try {
							var r = NearestFinder.structure(t, this.level, this.px, this.pz);
							return r == null ? null : new Hit(t.displayName().getString(), r);
						} catch (RuntimeException | LinkageError ignored) {
							return null; // One failing type must not hide the others.
						}
					}).filter(Objects::nonNull)
						.min(Comparator.comparingInt(h -> h.result().distance())).orElse(null);
				}
			} catch (RuntimeException | LinkageError ignored) {
				// Reported as "not found" below.
			}
			Hit found = hit;
			mc.execute(() -> finish(found));
		}, "seedatlas-nearest");
		thread.setDaemon(true);
		thread.start();
	}

	private void finish(final Hit hit) {
		this.busy = false;
		if (this.minecraft.gui.screen() != this) return;
		refresh();
		if (hit == null) {
			this.status = Component.translatable("message.seedatlas_xaero.nearest_none");
		} else if (!MapNavigator.open(hit.result().x(), hit.result().z(), hit.name(), waypointMode)) {
			this.status = Component.translatable("message.seedatlas_xaero.nearest_map");
		}
	}

	@Override
	public void extractRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float partialTick) {
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);
		graphics.centeredText(this.font, this.title, this.width / 2, 8, 0xFFFFFFFF);
		graphics.centeredText(this.font, this.status, this.width / 2, 24, 0xFFB8B8B8);
		if (this.noSeed) {
			graphics.centeredText(this.font, Component.translatable("screen.seedatlas_xaero.no_seed"), this.width / 2, 100, 0xFFB8B8B8);
		}
	}

	@Override
	public void onClose() {
		this.minecraft.gui.setScreen(this.parent);
	}
}
