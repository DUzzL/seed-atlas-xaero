package org.seedatlas.xaero.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.seedatlas.xaero.integration.SeedAtlasMinimapIntegration;
import org.seedatlas.xaero.integration.SeedAtlasXaeroIntegration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** No terrain-layer callback exists here; draw inside Xaero's FBO so its rotation/mask still apply. */
// shortcut: Safe Mode has no FBO hooks; add its software-texture path before claiming Safe Mode support.
@Pseudo
@Mixin(targets = "xaero.common.minimap.render.MinimapFBORenderer", remap = false)
abstract class MinimapFBORendererMixin {
    @Unique private boolean seedAtlas$frameActive;

    @Inject(method = "renderChunksToFBO", at = @At(value = "INVOKE", target =
        "Lxaero/lib/client/graphics/XaeroBufferProvider;getBuffer(Lnet/minecraft/client/renderer/rendertype/RenderType;)Lcom/mojang/blaze3d/vertex/BufferBuilder;", ordinal = 0))
    private void seedAtlas$background(CallbackInfo ci,
        @Local(name = "matrixStack", argsOnly = true) PoseStack pose,
        @Local(name = "renderPos", argsOnly = true) Vec3 camera,
        @Local(name = "mapDimension", argsOnly = true) ResourceKey<Level> dimension,
        @Local(name = "xFloored") int originX, @Local(name = "zFloored") int originZ,
        @Local(name = "maxVisibleLength") double size, @Local(name = "radiusBlocks") double radius) {
        this.seedAtlas$frameActive = SeedAtlasXaeroIntegration.beginMinimapFrame();
        if (this.seedAtlas$frameActive) {
            SeedAtlasMinimapIntegration.render(pose, dimension, camera, originX, originZ, size, radius, false);
        }
    }

    @Inject(method = "renderChunksToFBO", at = @At(value = "FIELD", target =
        "Lxaero/hud/minimap/common/config/option/MinimapProfiledConfigOptions;CHUNK_GRID:Lxaero/lib/common/config/option/RangeConfigOption;", ordinal = 0))
    private void seedAtlas$highlightsAndMarkers(CallbackInfo ci,
        @Local(name = "matrixStack", argsOnly = true) PoseStack pose,
        @Local(name = "renderPos", argsOnly = true) Vec3 camera,
        @Local(name = "mapDimension", argsOnly = true) ResourceKey<Level> dimension,
        @Local(name = "xFloored") int originX, @Local(name = "zFloored") int originZ,
        @Local(name = "maxVisibleLength") double size, @Local(name = "radiusBlocks") double radius) {
        if (this.seedAtlas$frameActive) {
            SeedAtlasMinimapIntegration.render(pose, dimension, camera, originX, originZ, size, radius, true);
        }
    }
}
