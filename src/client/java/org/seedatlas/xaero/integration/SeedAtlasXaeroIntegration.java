package org.seedatlas.xaero.integration;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.OptionalLong;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.seedatlas.xaero.config.SeedAtlasClientState;
import org.seedatlas.xaero.integration.biome.BiomeHighlighter;
import org.seedatlas.xaero.integration.biome.SeedAtlasBiomeOverlayRenderer;
import org.seedatlas.xaero.nativeapi.BiomeSample;
import org.seedatlas.xaero.nativeapi.Dimension;
import org.seedatlas.xaero.nativeapi.SeedAtlasNative;
import org.seedatlas.xaero.nativeapi.WorldType;
import xaero.map.WorldMapSession;

/** Runtime boundary between Xaero, the adaptive overlay and the native engine. */
public final class SeedAtlasXaeroIntegration {
    private static final Object NATIVE_LOCK = new Object();

    private static volatile boolean fullMapActive;
    /** True while Seed Atlas settings (or nested screens) cover the world map. */
    private static volatile boolean heavyWorkPaused;
    private static final StateRevisionTracker REVISIONS = new StateRevisionTracker();
    private static volatile double observedMapScale = 1.0;
    private static volatile List<BiomeSample> availableBiomes;
    private static boolean initialized;
    private static SeedAtlasNative session;
    private static long sessionSeed;
    private static boolean sessionLargeBiomes;

    private SeedAtlasXaeroIntegration() {
    }

    /** Keeps configured seeds separate for Xaero multiworlds on one server. */
    public static synchronized void initialize() {
        if (initialized) {
            return;
        }
        SeedAtlasClientState.setContextProvider(minecraft -> {
            WorldMapSession mapSession = WorldMapSession.getCurrentSession();
            if (mapSession == null || mapSession.getMapProcessor() == null) {
                return null;
            }
            String worldId = mapSession.getMapProcessor().getCurrentWorldId();
            String multiworldId = mapSession.getMapProcessor().getCurrentMWId();
            if (worldId == null || worldId.isBlank()) {
                return null;
            }
            String label = multiworldId == null || multiworldId.isBlank()
                ? worldId : worldId + " / " + multiworldId;
            return new SeedAtlasClientState.WorldContext(
                "xaero:" + contextDigest(worldId, multiworldId), label);
        });
        initialized = true;
    }

    public static boolean isLayerActive() {
        return fullMapActive && !heavyWorkPaused && isLayerConfigured();
    }

    public static boolean isLayerConfigured() {
        return SeedAtlasClientState.config().layerEnabled()
            && SeedAtlasClientState.activeSeed().isPresent();
    }

    public static boolean isFullMapActive() {
        return fullMapActive;
    }

    public static boolean isHeavyWorkPaused() {
        return heavyWorkPaused;
    }

    public static void setFullMapActive(boolean active) {
        fullMapActive = active;
        if (!active) {
            heavyWorkPaused = false;
        }
        synchronizeRevision();
        SeedAtlasBiomeOverlayRenderer.INSTANCE.setMapActive(active && !heavyWorkPaused);
        if (!active) {
            // Leaving the world map must idle native workers and GPU uploads.
            SeedAtlasBiomeOverlayRenderer.INSTANCE.idleCompletely();
        }
    }

    /**
     * Suspends biome/structure generation and GPU tile uploads while Seed Atlas
     * settings cover the map. Prevents coil-whine from background work under menus.
     */
    public static void setHeavyWorkPaused(boolean paused) {
        if (heavyWorkPaused == paused) {
            return;
        }
        heavyWorkPaused = paused;
        if (paused) {
            SeedAtlasBiomeOverlayRenderer.INSTANCE.setMapActive(false);
            SeedAtlasBiomeOverlayRenderer.INSTANCE.idleCompletely();
        } else if (fullMapActive) {
            synchronizeRevision();
            SeedAtlasBiomeOverlayRenderer.INSTANCE.setMapActive(true);
        }
    }

    /** Applies configuration changes without touching Xaero's leaf-region cache. */
    public static void synchronizeRevision() {
        REVISIONS.synchronize(SeedAtlasClientState.revision(), heavyWorkPaused,
            revision -> SeedAtlasBiomeOverlayRenderer.INSTANCE.onStateRevision(revision));
    }

    /**
     * Records and proactively schedules the current viewport. Unlike the old
     * highlighter path, this never waits for Xaero to request individual chunks.
     */
    public static void observeMapView(
        ResourceKey<Level> level,
        double cameraX,
        double cameraZ,
        double scale,
        int viewportWidth,
        int viewportHeight
    ) {
        if (level == null || !Double.isFinite(scale) || scale <= 0.0
            || !fullMapActive || heavyWorkPaused) {
            return;
        }
        observedMapScale = scale;
        SeedAtlasBiomeOverlayRenderer.INSTANCE.observeView(
            level, cameraX, cameraZ, scale, viewportWidth, viewportHeight);
    }

    /**
     * Map-open-only maintenance. Never generates tiles while the world map is
     * closed or Seed Atlas settings are open — that was the coil-whine source.
     */
    public static void tickBackground(Minecraft minecraft) {
        synchronizeRevision();
        if (!fullMapActive || heavyWorkPaused) {
            return;
        }
        SeedAtlasBiomeOverlayRenderer.INSTANCE.tickBackground(minecraft);
    }

    public static int yFor(ResourceKey<Level> dimension) {
        return SeedAtlasClientState.config().yFor(dimension);
    }

    /** Returns only already-computed overlay data; it never blocks the render thread. */
    public static BiomeSample biomeAtCached(
        ResourceKey<Level> level, int blockX, int blockZ
    ) {
        return SeedAtlasBiomeOverlayRenderer.INSTANCE.biomeAtCached(level, blockX, blockZ);
    }

    /** Localised biome name with a readable fallback for unknown engine ids. */
    public static Component biomeDisplayName(String biomeId) {
        String sanitized = biomeId == null ? "" : biomeId.replace('\r', ' ').replace('\n', ' ').trim();
        int separator = sanitized.indexOf(':');
        String namespace = separator > 0 ? sanitized.substring(0, separator) : "minecraft";
        String path = separator > 0 && separator < sanitized.length() - 1
            ? sanitized.substring(separator + 1) : sanitized;
        String[] words = path.replace('/', '_').split("_");
        StringBuilder fallback = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) continue;
            if (!fallback.isEmpty()) fallback.append(' ');
            fallback.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return Component.translatableWithFallback(
            "biome." + namespace + "." + path,
            fallback.isEmpty() ? path : fallback.toString()
        );
    }

    /**
     * Every biome of the running Minecraft version, sorted by resource name. Requires a native
     * session, so the list is empty while no seed is configured.
     */
    public static List<BiomeSample> availableBiomes() {
        List<BiomeSample> cached = availableBiomes;
        if (cached != null) {
            return cached;
        }
        List<BiomeSample> found = withSession(session -> {
            List<BiomeSample> biomes = new ArrayList<>();
            for (int id = 0; id < BiomeHighlighter.BIOME_ID_LIMIT; ++id) {
                String name = session.biomeName(id);
                if (name == null || name.contains("unknown_")) {
                    continue;
                }
                biomes.add(new BiomeSample(id, name, session.biomeColor(id)));
            }
            biomes.sort(Comparator.comparing(BiomeSample::name));
            return List.copyOf(biomes);
        });
        if (found == null || found.isEmpty()) {
            return List.of();
        }
        availableBiomes = found;
        return found;
    }

    public static <T> T withSession(SessionOperation<T> operation) {
        OptionalLong seed = SeedAtlasClientState.activeSeed();
        if (seed.isEmpty()) {
            return null;
        }
        SeedAtlasNative nativeSession;
        synchronized (NATIVE_LOCK) {
            nativeSession = session(seed.getAsLong(), SeedAtlasClientState.config().largeBiomes());
            if (nativeSession == null) {
                return null;
            }
        }
        try {
            return operation.apply(nativeSession);
        } catch (RuntimeException | LinkageError ignored) {
            return null;
        }
    }

    public static Dimension nativeDimension(ResourceKey<Level> dimension) {
        if (Level.OVERWORLD.equals(dimension)) {
            return Dimension.OVERWORLD;
        }
        if (Level.NETHER.equals(dimension)) {
            return Dimension.NETHER;
        }
        if (Level.END.equals(dimension)) {
            return Dimension.END;
        }
        return null;
    }

    public static int configuredWorkerThreads() {
        int configured = SeedAtlasClientState.config().workerThreads();
        if (configured > 0) {
            return Math.clamp(configured, 1, 8);
        }
        return Math.clamp(Runtime.getRuntime().availableProcessors() - 2, 2, 8);
    }

    public static double observedMapScale() {
        return observedMapScale;
    }

    /** Exposed for diagnostics and the settings status text. */
    public static int currentBiomeSampleStep() {
        return SeedAtlasBiomeOverlayRenderer.INSTANCE.currentSampleStep();
    }

    private static SeedAtlasNative session(long seed, boolean largeBiomes) {
        if (session != null && sessionSeed == seed && sessionLargeBiomes == largeBiomes) {
            return session;
        }
        closeSession();
        try {
            WorldType worldType = largeBiomes ? WorldType.LARGE_BIOMES : WorldType.NORMAL;
            session = SeedAtlasNative.open(seed, worldType);
            sessionSeed = seed;
            sessionLargeBiomes = largeBiomes;
            return session;
        } catch (RuntimeException | LinkageError ignored) {
            session = null;
            return null;
        }
    }

    private static void closeSession() {
        if (session == null) {
            return;
        }
        try {
            session.close();
        } catch (Exception ignored) {
            // Native cleanup cannot be allowed to take the client down.
        } finally {
            session = null;
        }
    }

    private static String contextDigest(String worldId, String multiworldId) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(worldId.getBytes(StandardCharsets.UTF_8));
            digest.update((byte)0);
            if (multiworldId == null) {
                digest.update((byte)0);
            } else {
                digest.update((byte)1);
                digest.update(multiworldId.getBytes(StandardCharsets.UTF_8));
            }
            return HexFormat.of().formatHex(digest.digest(), 0, 16);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    @FunctionalInterface
    public interface SessionOperation<T> {
        T apply(SeedAtlasNative session);
    }
}
