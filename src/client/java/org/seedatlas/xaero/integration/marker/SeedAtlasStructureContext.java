package org.seedatlas.xaero.integration.marker;

import java.util.List;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import xaero.map.graphics.renderer.multitexture.MultiTextureRenderTypeRenderer;

final class SeedAtlasStructureContext {
    volatile List<SeedAtlasStructureMarker> markers = List.of();
    volatile ResourceKey<Level> dimension;
    MultiTextureRenderTypeRenderer batchedIcons;
}
