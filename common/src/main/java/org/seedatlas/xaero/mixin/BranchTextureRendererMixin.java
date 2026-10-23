package org.seedatlas.xaero.mixin;

import net.minecraft.client.renderer.rendertype.RenderType;
import org.seedatlas.xaero.integration.ExploredTerrainRenderTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import xaero.map.region.texture.BranchTextureRenderer;

/** Keep unknown quadrants transparent, and unknown texels out of the average, when Xaero builds zoomed-out textures. */
@Mixin(value = BranchTextureRenderer.class, remap = false)
abstract class BranchTextureRendererMixin {
    @ModifyArg(method = {"ensureRenderTarget", "render"}, at = @At(
        value = "INVOKE",
        target = "Lxaero/lib/client/graphics/util/TextureUtils;clearRenderTarget(Lcom/mojang/blaze3d/pipeline/RenderTarget;I)V"
    ), index = 1)
    private int seedAtlas$transparentUnknownTerrain(int color) {
        // 26.1.2 clears with packed ARGB rather than the newer black4f field.
        return 0;
    }

    @ModifyArg(method = "render", at = @At(
        value = "INVOKE",
        target = "Lxaero/map/graphics/renderer/multitexture/MultiTextureRenderTypeRendererProvider;getRenderer(Lnet/minecraft/client/renderer/rendertype/RenderType;)Lxaero/map/graphics/renderer/multitexture/MultiTextureRenderTypeRenderer;"
    ))
    private RenderType seedAtlas$exploredOnlyAverage(RenderType type) {
        return ExploredTerrainRenderTypes.replace(type);
    }
}
