package org.seedatlas.xaero.config;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/** Mutable, synchronized representation of the client configuration. */
public final class SeedAtlasConfig {
	public static final int CURRENT_VERSION = 6;
	public static final int DEFAULT_OPACITY = 255;
	public static final int DEFAULT_BIOME_RESOLUTION = 1;
	public static final int DEFAULT_WORKER_THREADS = 0;
	public static final int DEFAULT_PREFETCH_RADIUS = 1;
	public static final int[] BIOME_RESOLUTION_STEPS = {1, 2, 4, 8, 16};
	public static final int[] WORKER_THREAD_STEPS = {0, 1, 2, 4, 8};
	public static final int[] PREFETCH_RADIUS_STEPS = {0, 1, 2, 3};
	/** Marker size in percent of the default pixel band. */
	public static final int DEFAULT_MARKER_SIZE = 150;
	public static final int[] MARKER_SIZE_STEPS = {50, 75, 100, 150, 200, 250};
	public static final int DEFAULT_OVERWORLD_Y = 255;
	public static final int DEFAULT_NETHER_Y = 64;
	public static final int DEFAULT_END_Y = 128;
	public static final int MIN_SAMPLE_Y = -2048;
	public static final int MAX_SAMPLE_Y = 2048;

	private boolean layerEnabled = true;
	private int opacity = DEFAULT_OPACITY;
	private boolean structuresEnabled;
	private boolean hideCompletedStructures;
	private final Set<StructureKey> completedStructures = new LinkedHashSet<>();
	private int biomeResolution = DEFAULT_BIOME_RESOLUTION;
	private int workerThreads = DEFAULT_WORKER_THREADS;
	private int prefetchRadius = DEFAULT_PREFETCH_RADIUS;
	private int markerSize = DEFAULT_MARKER_SIZE;
	private boolean biomeHighlightEnabled;
	private final Set<Integer> highlightedBiomes = new LinkedHashSet<>();
	private boolean largeBiomes;
	private int overworldY = DEFAULT_OVERWORLD_Y;
	private int netherY = DEFAULT_NETHER_Y;
	private int endY = DEFAULT_END_Y;
	private final Map<String, Boolean> markers = new LinkedHashMap<>();
	private final Map<String, SeedProfile> seedProfiles = new LinkedHashMap<>();

	public SeedAtlasConfig() {
		for (MarkerType type : MarkerType.all()) {
			this.markers.put(type.id(), type.enabledByDefault());
		}
	}

	public synchronized boolean layerEnabled() {
		return this.layerEnabled;
	}

	public synchronized int opacity() {
		return this.opacity;
	}

	public synchronized float opacityFraction() {
		return this.opacity / 255.0F;
	}

	public synchronized boolean structuresEnabled() {
		return this.structuresEnabled;
	}

	public synchronized int markerSize() {
		return this.markerSize;
	}

	/** Marker size as a factor of the default pixel band, read every frame by the renderer. */
	public synchronized float markerSizeFactor() {
		return this.markerSize / 100.0F;
	}

	public static int nextMarkerSize(final int current) {
		for (int index = 0; index < MARKER_SIZE_STEPS.length; index++) {
			if (MARKER_SIZE_STEPS[index] == current) {
				return MARKER_SIZE_STEPS[(index + 1) % MARKER_SIZE_STEPS.length];
			}
		}
		return DEFAULT_MARKER_SIZE;
	}

	public synchronized boolean biomeHighlightEnabled() {
		return this.biomeHighlightEnabled;
	}

	/** Snapshot of the highlighted engine biome ids; the set is not shared. */
	public synchronized Set<Integer> highlightedBiomes() {
		return Collections.unmodifiableSet(new LinkedHashSet<>(this.highlightedBiomes));
	}

	public synchronized int highlightedBiomeCount() {
		return this.highlightedBiomes.size();
	}

	/** Allocation-free check used by the selector UI on every frame. */
	public synchronized boolean isBiomeHighlighted(final int biomeId) {
		return this.highlightedBiomes.contains(biomeId);
	}

	/** Exact, immutable cache signature; independent of the order biomes were selected. */
	public synchronized Set<Integer> highlightSignature() {
		return this.biomeHighlightEnabled ? Set.copyOf(this.highlightedBiomes) : Set.of();
	}

	public synchronized boolean hideCompletedStructures() { return hideCompletedStructures; }

	synchronized boolean setHideCompletedStructures(boolean hide) {
		if (hideCompletedStructures == hide) return false;
		hideCompletedStructures = hide;
		return true;
	}

	public synchronized boolean isCompleted(StructureKey key) {
		return key != null && completedStructures.contains(key);
	}

	synchronized boolean setCompleted(StructureKey key, boolean completed) {
		java.util.Objects.requireNonNull(key);
		return completed ? completedStructures.add(key) : completedStructures.remove(key);
	}

	synchronized Set<StructureKey> completedSnapshot() { return new LinkedHashSet<>(completedStructures); }

	/** Y is deliberately excluded: approximate structure heights can change with sampling. */
	public record StructureKey(String world, long seed, boolean largeBiomes,
	                           String dimension, String type, int x, int z) {
		public StructureKey {
			if (world == null || world.isBlank() || dimension == null || dimension.isBlank()
				|| type == null || type.isBlank()) throw new IllegalArgumentException("Invalid structure identity");
		}
	}

	public synchronized int biomeResolution() {
		return this.biomeResolution;
	}

	/** Zero means an automatically selected worker count. */
	public synchronized int workerThreads() {
		return this.workerThreads;
	}

	/** Number of low-priority tile rings prefetched around the current view/player. */
	public synchronized int prefetchRadius() {
		return this.prefetchRadius;
	}

	public synchronized boolean largeBiomes() {
		return this.largeBiomes;
	}

	public synchronized int overworldY() {
		return this.overworldY;
	}

	public synchronized int netherY() {
		return this.netherY;
	}

	public synchronized int endY() {
		return this.endY;
	}

	public int yFor(final ResourceKey<Level> dimension) {
		return dimension == null ? this.overworldY() : this.yFor(dimension.identifier().toString());
	}

	/** Returns the configured sample height, falling back to Overworld for custom dimensions. */
	public synchronized int yFor(final String dimensionId) {
		if ("minecraft:the_nether".equals(dimensionId) || "the_nether".equals(dimensionId) || "nether".equals(dimensionId)) {
			return this.netherY;
		}
		if ("minecraft:the_end".equals(dimensionId) || "the_end".equals(dimensionId) || "end".equals(dimensionId)) {
			return this.endY;
		}
		return this.overworldY;
	}

	public synchronized boolean markerEnabled(final String markerId) {
		return MarkerType.byId(markerId)
			.map(type -> this.markers.getOrDefault(type.id(), type.enabledByDefault()))
			.orElse(false);
	}

	public synchronized Set<String> enabledMarkerIds() {
		Set<String> result = new LinkedHashSet<>();
		for (MarkerType type : MarkerType.all()) {
			if (this.markers.getOrDefault(type.id(), type.enabledByDefault())) {
				result.add(type.id());
			}
		}
		return Collections.unmodifiableSet(result);
	}

	synchronized boolean setLayerEnabled(final boolean enabled) {
		if (this.layerEnabled == enabled) {
			return false;
		}
		this.layerEnabled = enabled;
		return true;
	}

	synchronized boolean setOpacity(final int opacity) {
		int clamped = Math.max(0, Math.min(255, opacity));
		if (this.opacity == clamped) {
			return false;
		}
		this.opacity = clamped;
		return true;
	}

	synchronized boolean setStructuresEnabled(final boolean enabled) {
		if (this.structuresEnabled == enabled) {
			return false;
		}
		this.structuresEnabled = enabled;
		return true;
	}

	synchronized boolean setMarkerSize(final int markerSize) {
		int normalized = allowedValue(markerSize, MARKER_SIZE_STEPS, DEFAULT_MARKER_SIZE);
		if (this.markerSize == normalized) {
			return false;
		}
		this.markerSize = normalized;
		return true;
	}

	synchronized boolean setBiomeHighlightEnabled(final boolean enabled) {
		if (this.biomeHighlightEnabled == enabled) {
			return false;
		}
		this.biomeHighlightEnabled = enabled;
		return true;
	}

	// Sulfur Caves (187) is the last biome in 26.2; discard newer selections on import.
	synchronized boolean addHighlightedBiome(final int biomeId) {
		if (biomeId < 0 || biomeId > 187) {
			return false;
		}
		return this.highlightedBiomes.add(biomeId);
	}

	synchronized boolean toggleHighlightedBiome(final int biomeId) {
		if (biomeId < 0 || biomeId > 187) {
			return false;
		}
		if (!this.highlightedBiomes.remove(biomeId)) {
			this.highlightedBiomes.add(biomeId);
		}
		return true;
	}

	synchronized boolean clearHighlightedBiomes() {
		if (this.highlightedBiomes.isEmpty()) {
			return false;
		}
		this.highlightedBiomes.clear();
		return true;
	}

	synchronized boolean setBiomeResolution(final int resolution) {
		int normalized = allowedValue(resolution, BIOME_RESOLUTION_STEPS, DEFAULT_BIOME_RESOLUTION);
		if (this.biomeResolution == normalized) {
			return false;
		}
		this.biomeResolution = normalized;
		return true;
	}

	synchronized boolean setWorkerThreads(final int workerThreads) {
		int normalized = allowedValue(workerThreads, WORKER_THREAD_STEPS, DEFAULT_WORKER_THREADS);
		if (this.workerThreads == normalized) {
			return false;
		}
		this.workerThreads = normalized;
		return true;
	}

	synchronized boolean setPrefetchRadius(final int prefetchRadius) {
		int normalized = allowedValue(prefetchRadius, PREFETCH_RADIUS_STEPS, DEFAULT_PREFETCH_RADIUS);
		if (this.prefetchRadius == normalized) {
			return false;
		}
		this.prefetchRadius = normalized;
		return true;
	}

	synchronized boolean setLargeBiomes(final boolean largeBiomes) {
		if (this.largeBiomes == largeBiomes) {
			return false;
		}
		this.largeBiomes = largeBiomes;
		return true;
	}

	synchronized boolean setOverworldY(final int y) {
		int clamped = clampY(y);
		if (this.overworldY == clamped) {
			return false;
		}
		this.overworldY = clamped;
		return true;
	}

	synchronized boolean setNetherY(final int y) {
		int clamped = clampY(y);
		if (this.netherY == clamped) {
			return false;
		}
		this.netherY = clamped;
		return true;
	}

	synchronized boolean setEndY(final int y) {
		int clamped = clampY(y);
		if (this.endY == clamped) {
			return false;
		}
		this.endY = clamped;
		return true;
	}

	synchronized boolean setMarkerEnabled(final String markerId, final boolean enabled) {
		MarkerType type = MarkerType.byId(markerId).orElse(null);
		if (type == null || this.markers.getOrDefault(type.id(), type.enabledByDefault()) == enabled) {
			return false;
		}
		this.markers.put(type.id(), enabled);
		return true;
	}

	synchronized SeedProfile seedProfile(final String contextKey) {
		return this.seedProfiles.get(contextKey);
	}

	synchronized boolean putSeedProfile(final String contextKey, final SeedProfile profile) {
		return !profile.equals(this.seedProfiles.put(contextKey, profile));
	}

	synchronized boolean removeSeedProfile(final String contextKey) {
		return this.seedProfiles.remove(contextKey) != null;
	}

	synchronized Map<String, Boolean> markerSnapshot() {
		return new LinkedHashMap<>(this.markers);
	}

	synchronized Map<String, SeedProfile> profileSnapshot() {
		return new LinkedHashMap<>(this.seedProfiles);
	}

	static int clampY(final int y) {
		return Math.max(MIN_SAMPLE_Y, Math.min(MAX_SAMPLE_Y, y));
	}

	private static int allowedValue(final int value, final int[] allowed, final int fallback) {
		for (int candidate : allowed) {
			if (value == candidate) {
				return value;
			}
		}
		return fallback;
	}

	/** Seed text is kept so numeric and textual inputs can both be shown in status UI. */
	public record SeedProfile(String input, long seed, String contextLabel) {
		public SeedProfile {
			input = input == null ? Long.toString(seed) : input;
			contextLabel = contextLabel == null ? "" : contextLabel;
		}
	}
}
