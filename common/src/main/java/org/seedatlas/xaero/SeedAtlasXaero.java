package org.seedatlas.xaero;

import java.nio.file.Path;
import net.minecraft.client.Minecraft;
import org.seedatlas.xaero.config.SeedAtlasClientState;
import org.seedatlas.xaero.integration.SeedAtlasXaeroIntegration;

/** Shared client lifecycle, called by each loader's client entry point. */
public final class SeedAtlasXaero {
    public static final String MOD_ID = "seedatlas_xaero";

    private SeedAtlasXaero() { }

    public static void initialize(Path configDirectory) {
        SeedAtlasClientState.initialize(configDirectory);
        SeedAtlasXaeroIntegration.initialize();
        SeedAtlasClientState.save();
    }

    public static void tick(Minecraft client) {
        SeedAtlasClientState.refreshContext(client);
        SeedAtlasXaeroIntegration.tickBackground(client);
    }
}
