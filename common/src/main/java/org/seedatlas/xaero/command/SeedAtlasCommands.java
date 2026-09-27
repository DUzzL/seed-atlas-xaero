package org.seedatlas.xaero.command;

import java.util.OptionalLong;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import java.util.function.Function;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import net.minecraft.network.chat.Component;
import org.seedatlas.xaero.config.SeedAtlasClientState;
import org.seedatlas.xaero.config.SeedAtlasConfig;
import org.seedatlas.xaero.nativeapi.NativeStatus;
import org.seedatlas.xaero.nativeapi.SeedAtlasNative;

/** Registers the entirely client-side {@code /seedatlas} command tree. */
public final class SeedAtlasCommands {
	private SeedAtlasCommands() {
	}

	public static <S> void register(CommandDispatcher<S> dispatcher, Function<S, SeedAtlasCommandSource> source) {
		dispatcher.register(LiteralArgumentBuilder.<S>literal("seedatlas")
			.executes(context -> status(source.apply(context.getSource())))
			.then(LiteralArgumentBuilder.<S>literal("seed")
				.requires(value -> source.apply(value).attended())
				.then(RequiredArgumentBuilder.<S, String>argument("seed", StringArgumentType.greedyString())
					.executes(context -> setSeed(source.apply(context.getSource()), StringArgumentType.getString(context, "seed")))))
			.then(LiteralArgumentBuilder.<S>literal("clear")
				.requires(value -> source.apply(value).attended())
				.executes(context -> clearSeed(source.apply(context.getSource()))))
			.then(LiteralArgumentBuilder.<S>literal("status")
				.executes(context -> status(source.apply(context.getSource()))))
			.then(LiteralArgumentBuilder.<S>literal("settings")
				.requires(value -> source.apply(value).attended())
				.executes(context -> openSettings(source.apply(context.getSource()))))
		);
	}

	private static int setSeed(final SeedAtlasCommandSource source, String input) {
		try {
			long seed = SeedAtlasClientState.setSeed(input);
			source.sendFeedback(Component.translatable(
				"command.seedatlas_xaero.seed_set",
				input.trim(),
				seed,
				SeedAtlasClientState.currentContextLabel()
			));
			return 1;
		} catch (IllegalArgumentException exception) {
			source.sendError(Component.translatable("command.seedatlas_xaero.seed_blank"));
			return 0;
		} catch (IllegalStateException exception) {
			source.sendError(Component.translatable("command.seedatlas_xaero.no_context"));
			return 0;
		}
	}

	private static int clearSeed(final SeedAtlasCommandSource source) {
		SeedAtlasClientState.refreshContext(source.getClient());
		if (SeedAtlasClientState.currentContextKey().isEmpty()) {
			source.sendError(Component.translatable("command.seedatlas_xaero.no_context"));
			return 0;
		}
		if (SeedAtlasClientState.clearSeed()) {
			source.sendFeedback(Component.translatable("command.seedatlas_xaero.seed_cleared"));
			return 1;
		}
		source.sendFeedback(Component.translatable("command.seedatlas_xaero.no_seed_to_clear"));
		return 0;
	}

	private static int status(final SeedAtlasCommandSource source) {
		SeedAtlasClientState.refreshContext(source.getClient());
		String contextLabel = SeedAtlasClientState.currentContextLabel();
		if (SeedAtlasClientState.currentContextKey().isEmpty()) {
			source.sendError(Component.translatable("command.seedatlas_xaero.no_context"));
			return 0;
		}

		source.sendFeedback(Component.translatable("command.seedatlas_xaero.status.context", contextLabel));
		OptionalLong seed = SeedAtlasClientState.activeSeed();
		if (seed.isPresent()) {
			String input = SeedAtlasClientState.activeSeedInput().orElse(Long.toString(seed.getAsLong()));
			source.sendFeedback(Component.translatable("command.seedatlas_xaero.status.seed", input, seed.getAsLong()));
		} else {
			source.sendFeedback(Component.translatable("command.seedatlas_xaero.status.no_seed"));
		}

		SeedAtlasConfig config = SeedAtlasClientState.config();
		source.sendFeedback(Component.translatable(
			"command.seedatlas_xaero.status.layer",
			onOff(config.layerEnabled()),
			Math.round(config.opacityFraction() * 100.0F)
		));
		source.sendFeedback(Component.translatable(
			"command.seedatlas_xaero.status.world_type",
			Component.translatable(
				config.largeBiomes()
					? "options.seedatlas_xaero.world_type.large"
					: "options.seedatlas_xaero.world_type.normal"
			)
		));
		source.sendFeedback(Component.translatable(
			"command.seedatlas_xaero.status.structures",
			onOff(config.structuresEnabled())
		));
		Component workerThreads = config.workerThreads() == 0
			? Component.translatable("options.seedatlas_xaero.worker_threads.auto")
			: Component.literal(Integer.toString(config.workerThreads()));
		source.sendFeedback(Component.translatable(
			"command.seedatlas_xaero.status.performance",
			Component.translatable("options.seedatlas_xaero.biome_resolution." + config.biomeResolution()),
			workerThreads,
			Component.translatable("options.seedatlas_xaero.prefetch_radius." + config.prefetchRadius())
		));
		NativeStatus nativeStatus = SeedAtlasNative.status();
		if (nativeStatus.available()) {
			source.sendFeedback(Component.translatable(
				"command.seedatlas_xaero.status.native",
				nativeStatus.engineVersion(),
				nativeStatus.platform()
			));
		} else {
			source.sendError(Component.translatable(
				"command.seedatlas_xaero.status.native_unavailable",
				nativeStatus.message()
			));
		}
		return 1;
	}

	private static Component onOff(final boolean enabled) {
		return Component.translatable(enabled ? "options.seedatlas_xaero.on" : "options.seedatlas_xaero.off");
	}

	private static int openSettings(final SeedAtlasCommandSource source) {
		// ChatScreen closes after command execution, so queue this for the next client task.
		source.getClient().schedule(() -> SeedAtlasClientState.openSettings(null));
		return 1;
	}
}
