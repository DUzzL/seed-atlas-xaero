package org.seedatlas.xaero.integration;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.seedatlas.xaero.integration.biome.SeedAtlasBiomeOverlayRenderer;
import org.seedatlas.xaero.integration.marker.SeedAtlasStructureRenderer;
import xaero.map.WorldMapSession;

/** Optional Minimap bridge; no Minimap classes are linked when that mod is absent. */
public final class SeedAtlasMinimapIntegration {
    private SeedAtlasMinimapIntegration() { }

    public static void render(PoseStack pose, ResourceKey<Level> dimension, Vec3 camera,
        int originX, int originZ, double size, double radius, boolean aboveTerrain) {
        var session = WorldMapSession.getCurrentSession();
        if (session == null || session.getMapProcessor() == null || !Double.isFinite(radius)
            || !Double.isFinite(size) || radius <= 0 || size <= 0) return;
        double scale = size / (2.0 * radius);
        int viewport = (int)Math.ceil(size);
        var renderers = session.getMapProcessor().getMultiTextureRenderTypeRenderers();
        SeedAtlasBiomeOverlayRenderer.MINIMAP.renderBackground(pose, dimension,
            camera.x, camera.z, scale, originX, originZ, renderers, aboveTerrain, viewport, viewport);
        if (aboveTerrain) {
            SeedAtlasStructureRenderer.MINIMAP.renderMinimap(pose, dimension,
                camera.x, camera.z, originX, originZ, scale, viewport, renderers);
        }
    }
}
