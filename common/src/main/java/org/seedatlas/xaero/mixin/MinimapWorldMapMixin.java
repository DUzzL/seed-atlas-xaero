package org.seedatlas.xaero.mixin;

import net.minecraft.client.renderer.rendertype.RenderType;
import org.seedatlas.xaero.integration.ExploredTerrainRenderTypes;
import org.seedatlas.xaero.integration.SeedAtlasXaeroIntegration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** The shared map textures need the same unexplored-texel filtering in both maps. */
@Pseudo
@Mixin(targets = "xaero.common.mods.SupportXaeroWorldmap", remap = false)
abstract class MinimapWorldMapMixin {
    @ModifyArg(method = "drawMinimap", at = @At(value = "INVOKE", target =
        "Lxaero/common/graphics/renderer/multitexture/MultiTextureRenderTypeRendererProvider;getRenderer(Ljava/util/function/Consumer;Lnet/minecraft/client/renderer/rendertype/RenderType;)Lxaero/common/graphics/renderer/multitexture/MultiTextureRenderTypeRenderer;"), index = 1)
    private RenderType seedAtlas$exploredTerrain(RenderType original) {
        return SeedAtlasXaeroIntegration.isLayerActive() ? ExploredTerrainRenderTypes.minimap() : original;
    }
}
