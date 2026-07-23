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
	public static final int CURRENT_VERSION = 5;
	public static final int DEFAULT_OPACITY = 255;
	public static final int DEFAULT_BIOME_RESOLUTION = 1;
	public static final int DEFAULT_WORKER_THREADS = 0;
	public static final int DEFAULT_PREFETCH_RADIUS = 1;
	public static final int[] BIOME_RESOLUTION_STEPS = {1, 2, 4, 8, 16};
	public static final int[] WORKER_THREAD_STEPS = {0, 1, 2, 4, 8};
	public static final int[] PREFETCH_RADIUS_STEPS = {0, 1, 2, 3};
	public static final int DEFAULT_OVERWORLD_Y = 255;
	public static final int DEFAULT_NETHER_Y = 64;
	public static final int DEFAULT_END_Y = 128;
	public static final int MIN_SAMPLE_Y = -2048;
	public static final int MAX_SAMPLE_Y = 2048;

	private boolean layerEnabled = true;
	private int opacity = DEFAULT_OPACITY;
	private boolean structuresEnabled;
	private int biomeResolution = DEFAULT_BIOME_RESOLUTION;
	private int workerThreads = DEFAULT_WORKER_THREADS;
	private int prefetchRadius = DEFAULT_PREFETCH_RADIUS;
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
