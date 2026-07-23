package org.seedatlas.xaero.command;

import java.util.OptionalLong;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;
import org.seedatlas.xaero.config.SeedAtlasClientState;
import org.seedatlas.xaero.config.SeedAtlasConfig;
import org.seedatlas.xaero.nativeapi.NativeStatus;
import org.seedatlas.xaero.nativeapi.SeedAtlasNative;

/** Registers the entirely client-side {@code /seedatlas} command tree. */
public final class SeedAtlasCommands {
	private SeedAtlasCommands() {
	}

	public static void register() {
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, buildContext) -> register(dispatcher));
	}

	static void register(final CommandDispatcher<FabricClientCommandSource> dispatcher) {
		dispatcher.register(ClientCommands.literal("seedatlas")
			.executes(SeedAtlasCommands::status)
			.then(ClientCommands.literal("seed")
				.requires(FabricClientCommandSource::attended)
				.then(ClientCommands.argument("seed", StringArgumentType.greedyString()).executes(SeedAtlasCommands::setSeed)))
			.then(ClientCommands.literal("clear")
				.requires(FabricClientCommandSource::attended)
				.executes(SeedAtlasCommands::clearSeed))
			.then(ClientCommands.literal("status").executes(SeedAtlasCommands::status))
			.then(ClientCommands.literal("settings")
				.requires(FabricClientCommandSource::attended)
				.executes(SeedAtlasCommands::openSettings))
		);
	}

	private static int setSeed(final CommandContext<FabricClientCommandSource> context) {
		String input = StringArgumentType.getString(context, "seed");
		try {
			long seed = SeedAtlasClientState.setSeed(input);
			context.getSource().sendFeedback(Component.translatable(
				"command.seedatlas_xaero.seed_set",
				input.trim(),
				seed,
				SeedAtlasClientState.currentContextLabel()
			));
			return 1;
		} catch (IllegalArgumentException exception) {
			context.getSource().sendError(Component.translatable("command.seedatlas_xaero.seed_blank"));
			return 0;
		} catch (IllegalStateException exception) {
			context.getSource().sendError(Component.translatable("command.seedatlas_xaero.no_context"));
			return 0;
		}
	}

	private static int clearSeed(final CommandContext<FabricClientCommandSource> context) {
		SeedAtlasClientState.refreshContext(context.getSource().getClient());
		if (SeedAtlasClientState.currentContextKey().isEmpty()) {
			context.getSource().sendError(Component.translatable("command.seedatlas_xaero.no_context"));
			return 0;
		}
		if (SeedAtlasClientState.clearSeed()) {
			context.getSource().sendFeedback(Component.translatable("command.seedatlas_xaero.seed_cleared"));
			return 1;
		}
		context.getSource().sendFeedback(Component.translatable("command.seedatlas_xaero.no_seed_to_clear"));
		return 0;
	}

	private static int status(final CommandContext<FabricClientCommandSource> context) {
		SeedAtlasClientState.refreshContext(context.getSource().getClient());
		String contextLabel = SeedAtlasClientState.currentContextLabel();
		if (SeedAtlasClientState.currentContextKey().isEmpty()) {
			context.getSource().sendError(Component.translatable("command.seedatlas_xaero.no_context"));
			return 0;
		}

		context.getSource().sendFeedback(Component.translatable("command.seedatlas_xaero.status.context", contextLabel));
		OptionalLong seed = SeedAtlasClientState.activeSeed();
		if (seed.isPresent()) {
			String input = SeedAtlasClientState.activeSeedInput().orElse(Long.toString(seed.getAsLong()));
			context.getSource().sendFeedback(Component.translatable("command.seedatlas_xaero.status.seed", input, seed.getAsLong()));
		} else {
			context.getSource().sendFeedback(Component.translatable("command.seedatlas_xaero.status.no_seed"));
		}

		SeedAtlasConfig config = SeedAtlasClientState.config();
		context.getSource().sendFeedback(Component.translatable(
			"command.seedatlas_xaero.status.layer",
			onOff(config.layerEnabled()),
			Math.round(config.opacityFraction() * 100.0F)
		));
		context.getSource().sendFeedback(Component.translatable(
			"command.seedatlas_xaero.status.world_type",
			Component.translatable(
				config.largeBiomes()
					? "options.seedatlas_xaero.world_type.large"
					: "options.seedatlas_xaero.world_type.normal"
			)
		));
		context.getSource().sendFeedback(Component.translatable(
			"command.seedatlas_xaero.status.structures",
			onOff(config.structuresEnabled())
		));
		Component workerThreads = config.workerThreads() == 0
			? Component.translatable("options.seedatlas_xaero.worker_threads.auto")
			: Component.literal(Integer.toString(config.workerThreads()));
		context.getSource().sendFeedback(Component.translatable(
			"command.seedatlas_xaero.status.performance",
			Component.translatable("options.seedatlas_xaero.biome_resolution." + config.biomeResolution()),
			workerThreads,
			Component.translatable("options.seedatlas_xaero.prefetch_radius." + config.prefetchRadius())
		));
		NativeStatus nativeStatus = SeedAtlasNative.status();
		if (nativeStatus.available()) {
			context.getSource().sendFeedback(Component.translatable(
				"command.seedatlas_xaero.status.native",
				nativeStatus.engineVersion(),
				nativeStatus.platform()
			));
		} else {
			context.getSource().sendError(Component.translatable(
				"command.seedatlas_xaero.status.native_unavailable",
				nativeStatus.message()
			));
		}
		return 1;
	}

	private static Component onOff(final boolean enabled) {
		return Component.translatable(enabled ? "options.seedatlas_xaero.on" : "options.seedatlas_xaero.off");
	}

	private static int openSettings(final CommandContext<FabricClientCommandSource> context) {
		// ChatScreen closes after command execution, so queue this for the next client task.
		context.getSource().getClient().schedule(() -> SeedAtlasClientState.openSettings(null));
		return 1;
	}
}
