package org.seedatlas.xaero.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.storage.LevelResource;
import org.seedatlas.xaero.gui.SeedAtlasSettingsScreen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stable client facade shared by commands, settings and the Xaero/native integration.
 *
 * <p>All mutations persist immediately and advance {@link #revision()}, allowing
 * renderers to invalidate cached tiles without depending on implementation details.</p>
 */
public final class SeedAtlasClientState {
	private static final Logger LOGGER = LoggerFactory.getLogger("seedatlas_xaero");
	private static final Object LOCK = new Object();
	private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("seedatlas_xaero.json");
	private static final AtomicLong REVISION = new AtomicLong();
	private static final CopyOnWriteArrayList<StateListener> LISTENERS = new CopyOnWriteArrayList<>();
	private static final WorldContext NO_CONTEXT = new WorldContext("", "");

	private static volatile SeedAtlasConfig config = new SeedAtlasConfig();
	private static volatile boolean initialized;
	private static volatile WorldContext activeContext = NO_CONTEXT;
	private static volatile OptionalLong activeSeed = OptionalLong.empty();
	private static volatile String activeSeedInput;
	private static volatile String autoDetectSuppressedContext;
	private static volatile ContextProvider contextProvider;
	private static volatile WorldContext externalContext;

	private SeedAtlasClientState() {
	}

	public static void initialize() {
		if (initialized) {
			return;
		}
		synchronized (LOCK) {
			if (initialized) {
				return;
			}
			try {
				config = SeedAtlasConfigIO.load(CONFIG_PATH);
			} catch (IOException exception) {
				LOGGER.error("Could not load {}; using defaults", CONFIG_PATH, exception);
				config = new SeedAtlasConfig();
			}
			initialized = true;
		}
	}

	public static SeedAtlasConfig config() {
		initialize();
		return config;
	}

	public static Path configPath() {
		return CONFIG_PATH;
	}

	public static OptionalLong activeSeed() {
		initialize();
		return activeSeed;
	}

	public static Optional<String> activeSeedInput() {
		initialize();
		return Optional.ofNullable(activeSeedInput);
	}

	public static String currentContextKey() {
		initialize();
		return activeContext.key();
	}

	public static String currentContextLabel() {
		initialize();
		return activeContext.label();
	}

	public static long revision() {
		return REVISION.get();
	}

	/** Called from the client tick; cheap when the selected world has not changed. */
	public static void refreshContext(final Minecraft minecraft) {
		initialize();
		WorldContext nextContext = resolveContext(minecraft);
		OptionalLong detectedSingleplayerSeed = detectSingleplayerSeed(minecraft);
		if (nextContext.key().equals(autoDetectSuppressedContext)) {
			detectedSingleplayerSeed = OptionalLong.empty();
		}
		boolean contextChanged;
		boolean profileChanged = false;

		synchronized (LOCK) {
			contextChanged = !activeContext.key().equals(nextContext.key());
			if (contextChanged && !nextContext.key().equals(autoDetectSuppressedContext)) {
				autoDetectSuppressedContext = null;
			}
			if (!contextChanged && (activeSeed.isPresent() || detectedSingleplayerSeed.isEmpty())) {
				return;
			}

			activeContext = nextContext;
			if (nextContext.key().isEmpty()) {
				activeSeed = OptionalLong.empty();
				activeSeedInput = null;
			} else {
				SeedAtlasConfig.SeedProfile profile = config.seedProfile(nextContext.key());
				if (detectedSingleplayerSeed.isPresent() && (profile == null || profile.seed() != detectedSingleplayerSeed.getAsLong())) {
					long seed = detectedSingleplayerSeed.getAsLong();
					profile = new SeedAtlasConfig.SeedProfile(Long.toString(seed), seed, nextContext.label());
					profileChanged = config.putSeedProfile(nextContext.key(), profile);
				}

				if (profile == null) {
					activeSeed = OptionalLong.empty();
					activeSeedInput = null;
				} else {
					activeSeed = OptionalLong.of(profile.seed());
					activeSeedInput = profile.input();
				}
			}
		}

		if (profileChanged) {
			save();
		}
		if (contextChanged || profileChanged) {
			publish(ChangeReason.CONTEXT);
		}
	}

	/**
	 * Overrides fallback server/world identification. Xaero integration can set a
	 * map-specific context directly and clear it when its session closes.
	 */
	public static void setExternalContext(final WorldContext context) {
		externalContext = context;
		refreshContext(Minecraft.getInstance());
	}

	public static void clearExternalContext() {
		externalContext = null;
		refreshContext(Minecraft.getInstance());
	}

	public static void setContextProvider(final ContextProvider provider) {
		contextProvider = provider;
		refreshContext(Minecraft.getInstance());
	}

	/** Parses exactly like vanilla: a long when possible, otherwise Java's text hash. */
	public static long parseSeed(final String input) {
		if (input == null) {
			throw new IllegalArgumentException("Seed must not be null");
		}
		return WorldOptions.parseSeed(input).orElseThrow(() -> new IllegalArgumentException("Seed must not be blank"));
	}

	public static long setSeed(final String input) {
		initialize();
		String normalized = Objects.requireNonNull(input, "input").trim();
		long seed = parseSeed(normalized);
		refreshContext(Minecraft.getInstance());
		WorldContext context = activeContext;
		if (context.key().isEmpty()) {
			throw new IllegalStateException("No active world or server context");
		}

		synchronized (LOCK) {
			config.putSeedProfile(context.key(), new SeedAtlasConfig.SeedProfile(normalized, seed, context.label()));
			activeSeed = OptionalLong.of(seed);
			activeSeedInput = normalized;
			autoDetectSuppressedContext = null;
		}
		saveAndPublish(ChangeReason.SEED);
		return seed;
	}

	public static boolean clearSeed() {
		initialize();
		refreshContext(Minecraft.getInstance());
		WorldContext context = activeContext;
		if (context.key().isEmpty()) {
			return false;
		}

		boolean changed;
		synchronized (LOCK) {
			changed = config.removeSeedProfile(context.key());
			activeSeed = OptionalLong.empty();
			activeSeedInput = null;
			autoDetectSuppressedContext = context.key();
		}
		if (changed) {
			saveAndPublish(ChangeReason.SEED);
		}
		return changed;
	}

	public static void toggleLayer() {
		setLayerEnabled(!config().layerEnabled());
	}

	public static void setLayerEnabled(final boolean enabled) {
		if (config().setLayerEnabled(enabled)) {
			saveAndPublish(ChangeReason.DISPLAY);
		}
	}

	public static void setOpacity(final int opacity) {
		if (config().setOpacity(opacity)) {
			saveAndPublish(ChangeReason.DISPLAY);
		}
	}

	public static void setStructuresEnabled(final boolean enabled) {
		if (config().setStructuresEnabled(enabled)) {
			saveAndPublish(ChangeReason.MARKERS);
		}
	}

	public static void setBiomeResolution(final int resolution) {
		if (config().setBiomeResolution(resolution)) {
			saveAndPublish(ChangeReason.PERFORMANCE);
		}
	}

	public static void setWorkerThreads(final int workerThreads) {
		if (config().setWorkerThreads(workerThreads)) {
			saveAndPublish(ChangeReason.PERFORMANCE);
		}
	}

	public static void setPrefetchRadius(final int prefetchRadius) {
		if (config().setPrefetchRadius(prefetchRadius)) {
			saveAndPublish(ChangeReason.PERFORMANCE);
		}
	}

	public static void setLargeBiomes(final boolean largeBiomes) {
		if (config().setLargeBiomes(largeBiomes)) {
			saveAndPublish(ChangeReason.WORLD_GENERATION);
		}
	}

	public static void setOverworldY(final int y) {
		if (config().setOverworldY(y)) {
			saveAndPublish(ChangeReason.WORLD_GENERATION);
		}
	}

	public static void setNetherY(final int y) {
		if (config().setNetherY(y)) {
			saveAndPublish(ChangeReason.WORLD_GENERATION);
		}
	}

	public static void setEndY(final int y) {
		if (config().setEndY(y)) {
			saveAndPublish(ChangeReason.WORLD_GENERATION);
		}
	}

	public static void setYFor(final ResourceKey<Level> dimension, final int y) {
		if (Level.NETHER.equals(dimension)) {
			setNetherY(y);
		} else if (Level.END.equals(dimension)) {
			setEndY(y);
		} else {
			setOverworldY(y);
		}
	}

	public static void setMarkerEnabled(final String markerId, final boolean enabled) {
		if (config().setMarkerEnabled(markerId, enabled)) {
			saveAndPublish(ChangeReason.MARKERS);
		}
	}

	public static void openSettings(final Screen parent) {
		initialize();
		// Pause native/GPU work immediately so opening settings never keeps the
		// map pipeline spinning underneath the menu.
		org.seedatlas.xaero.integration.SeedAtlasXaeroIntegration.setHeavyWorkPaused(true);
		Minecraft.getInstance().gui.setScreen(new SeedAtlasSettingsScreen(parent));
	}

	public static void save() {
		initialize();
		try {
			SeedAtlasConfigIO.save(CONFIG_PATH, config);
		} catch (IOException exception) {
			LOGGER.error("Could not save {}", CONFIG_PATH, exception);
		}
	}

	public static void reload() {
		initialize();
		try {
			SeedAtlasConfig loaded = SeedAtlasConfigIO.load(CONFIG_PATH);
			synchronized (LOCK) {
				config = loaded;
				autoDetectSuppressedContext = null;
				loadActiveProfileLocked();
			}
			publish(ChangeReason.RELOAD);
		} catch (IOException exception) {
			LOGGER.error("Could not reload {}", CONFIG_PATH, exception);
		}
	}

	public static Subscription addListener(final StateListener listener) {
		Objects.requireNonNull(listener, "listener");
		LISTENERS.add(listener);
		return () -> LISTENERS.remove(listener);
	}

	private static void loadActiveProfileLocked() {
		if (activeContext.key().isEmpty()) {
			activeSeed = OptionalLong.empty();
			activeSeedInput = null;
			return;
		}
		SeedAtlasConfig.SeedProfile profile = config.seedProfile(activeContext.key());
		activeSeed = profile == null ? OptionalLong.empty() : OptionalLong.of(profile.seed());
		activeSeedInput = profile == null ? null : profile.input();
	}

	private static void saveAndPublish(final ChangeReason reason) {
		save();
		publish(reason);
	}

	private static void publish(final ChangeReason reason) {
		long newRevision = REVISION.incrementAndGet();
		for (StateListener listener : LISTENERS) {
			try {
				listener.onStateChanged(newRevision, reason);
			} catch (RuntimeException exception) {
				LOGGER.error("Seed Atlas state listener failed", exception);
			}
		}
	}

	private static WorldContext resolveContext(final Minecraft minecraft) {
		WorldContext override = externalContext;
		if (override != null) {
			return override;
		}

		ContextProvider provider = contextProvider;
		if (provider != null) {
			try {
				WorldContext supplied = provider.resolve(minecraft);
				if (supplied != null && !supplied.key().isBlank()) {
					return supplied;
				}
			} catch (RuntimeException exception) {
				LOGGER.error("Seed Atlas context provider failed", exception);
			}
		}

		IntegratedServer integratedServer = minecraft.getSingleplayerServer();
		if (integratedServer != null) {
			String label = integratedServer.getWorldData().getLevelName();
			String path = integratedServer.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize().toString();
			return new WorldContext("singleplayer:" + digest(path), label);
		}

		ServerData server = minecraft.getCurrentServer();
		if (server != null && server.ip != null && !server.ip.isBlank()) {
			String address = server.ip.trim().toLowerCase(java.util.Locale.ROOT);
			String label = server.name == null || server.name.isBlank() ? server.ip : server.name + " (" + server.ip + ")";
			return new WorldContext("server:" + digest(address), label);
		}

		return NO_CONTEXT;
	}

	private static OptionalLong detectSingleplayerSeed(final Minecraft minecraft) {
		IntegratedServer server = minecraft.getSingleplayerServer();
		if (server == null) {
			return OptionalLong.empty();
		}
		return OptionalLong.of(server.getWorldGenSettings().options().seed());
	}

	private static String digest(final String input) {
		try {
			byte[] hash = MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(hash, 0, 16);
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 is unavailable", exception);
		}
	}

	public enum ChangeReason {
		CONTEXT,
		SEED,
		DISPLAY,
		WORLD_GENERATION,
		MARKERS,
		PERFORMANCE,
		RELOAD
	}

	@FunctionalInterface
	public interface StateListener {
		void onStateChanged(long revision, ChangeReason reason);
	}

	@FunctionalInterface
	public interface ContextProvider {
		WorldContext resolve(Minecraft minecraft);
	}

	@FunctionalInterface
	public interface Subscription extends AutoCloseable {
		@Override
		void close();
	}

	public record WorldContext(String key, String label) {
		public WorldContext {
			key = Objects.requireNonNull(key, "key").trim();
			label = Objects.requireNonNullElse(label, "").trim();
			if (key.length() > 256) {
				key = "external:" + digest(key);
			}
		}
	}
}
