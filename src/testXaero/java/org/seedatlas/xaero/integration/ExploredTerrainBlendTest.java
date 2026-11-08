package org.seedatlas.xaero.integration;

import com.mojang.renderpearl.api.pipeline.BlendFactor;
import com.mojang.renderpearl.api.pipeline.BlendFunction;

/** The Minimap starts with transparent texels; explored terrain must write its alpha. */
final class ExploredTerrainBlendTest {
    static void run() {
        check(ExploredTerrainRenderTypes.terrainBlend(true).equals(new BlendFunction(
            BlendFactor.ONE, BlendFactor.ONE_MINUS_SRC_ALPHA,
            BlendFactor.ONE, BlendFactor.ONE_MINUS_SRC_ALPHA)),
            "Minimap terrain must replace transparent texels and preserve seed terrain under transparent edges");
        check(ExploredTerrainRenderTypes.terrainBlend(false).equals(new BlendFunction(
            BlendFactor.ONE, BlendFactor.ZERO, BlendFactor.ZERO, BlendFactor.ONE)),
            "World Map must retain its existing destination-alpha behavior");
        System.out.println("Explored terrain blend checks passed");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
