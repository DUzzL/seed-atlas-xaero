package org.seedatlas.xaero;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import org.seedatlas.xaero.command.SeedAtlasCommands;
import org.seedatlas.xaero.config.SeedAtlasClientState;
import org.seedatlas.xaero.integration.SeedAtlasXaeroIntegration;
import org.seedatlas.xaero.integration.icon.StructureIcons;

/** Client-only entry point for Seed Atlas for Xaero's World Map. */
public final class SeedAtlasXaero implements ClientModInitializer {
	public static final String MOD_ID = "seedatlas_xaero";

	@Override
	public void onInitializeClient() {
		SeedAtlasClientState.initialize();
		StructureIcons.initialize();
		SeedAtlasXaeroIntegration.initialize();
		SeedAtlasClientState.save();
		SeedAtlasCommands.register();
		ClientTickEvents.END_CLIENT_TICK.register(SeedAtlasClientState::refreshContext);
		ClientTickEvents.END_CLIENT_TICK.register(SeedAtlasXaeroIntegration::tickBackground);
	}
}
