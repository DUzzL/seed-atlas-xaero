package org.seedatlas.xaero.integration.marker;

import org.seedatlas.xaero.config.SeedAtlasClientState;
import org.seedatlas.xaero.config.SeedAtlasConfig.StructureKey;

/** Persistent progress belongs to a world and seed, never to a cached marker object. */
final class SeedAtlasStructureState {
    static StructureKey key(SeedAtlasStructureMarker marker) {
        var seed = SeedAtlasClientState.activeSeed();
        String world = SeedAtlasClientState.currentContextKey();
        if (seed.isEmpty() || world.isBlank()) return null;
        return new StructureKey(world, seed.getAsLong(), SeedAtlasClientState.config().largeBiomes(),
            marker.dimension().identifier().toString(), marker.type().id(), marker.x(), marker.z());
    }

    static boolean isCompleted(SeedAtlasStructureMarker marker) {
        return SeedAtlasClientState.config().isCompleted(key(marker));
    }
}
