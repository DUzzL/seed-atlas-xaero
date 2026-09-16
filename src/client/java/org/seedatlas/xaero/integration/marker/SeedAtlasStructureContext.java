package org.seedatlas.xaero.integration.marker;

import java.util.List;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import xaero.map.graphics.renderer.multitexture.MultiTextureRenderTypeRenderer;

final class SeedAtlasStructureContext {
    volatile List<SeedAtlasStructureMarker> markers = List.of();
    volatile ResourceKey<Level> dimension;
    MultiTextureRenderTypeRenderer batchedIcons;
    float iconScale = 1.0F;

    void updateZoom(double mapScale) {
        // Keep overview markers readable, then grow smoothly from 20 to 60
        // pixels (before Xaero's screen-size scaling) as the map zooms in.
        iconScale = (float)Math.clamp(Math.sqrt(mapScale / 0.25), 1.0, 3.0);
    }
}
