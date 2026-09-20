package org.seedatlas.xaero.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import org.seedatlas.xaero.config.SeedAtlasClientState;
import org.seedatlas.xaero.integration.SeedAtlasXaeroIntegration;
import org.seedatlas.xaero.integration.biome.SeedAtlasBiomeOverlayRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.lib.client.gui.widget.Tooltip;
import xaero.map.gui.GuiMap;
import xaero.map.gui.TooltipButton;

/** Full-map-only controls and lifecycle gate for the seed layer. */
@Mixin(value = GuiMap.class, remap = false)
abstract class GuiMapMixin {
    @Shadow
    private double cameraX;
    @Shadow
    private double cameraZ;
    @Shadow
    private double scale;

    @Unique
    private Button seedAtlas$layerButton;
    @Unique
    private Button seedAtlas$settingsButton;
    @Unique
    private long seedAtlas$displayedRevision = Long.MIN_VALUE;

    // Draw before Xaero reserves both renderers in its shared provider.
    @Inject(method = "extractRenderState", at = @At(
        value = "INVOKE",
        target = "Lxaero/map/MapProcessor;getMultiTextureRenderTypeRenderers()Lxaero/map/graphics/renderer/multitexture/MultiTextureRenderTypeRendererProvider;"
    ))
    private void seedAtlas$renderBiomeBackground(
        CallbackInfo ci,
        @Local(name = "matrixStack") PoseStack pose,
        @Local(name = "flooredCameraX") int flooredCameraX,
        @Local(name = "flooredCameraZ") int flooredCameraZ
    ) {
        GuiMap map = (GuiMap)(Object)this;
        var processor = map.getMapProcessor();
        SeedAtlasBiomeOverlayRenderer.INSTANCE.renderBackground(
            pose, processor.getMapWorld().getCurrentDimension().getDimId(),
            this.cameraX, this.cameraZ, this.scale, flooredCameraX, flooredCameraZ,
            processor.getMultiTextureRenderTypeRenderers(), false
        );
    }

    // Both terrain batches have been flushed and released. Use the same FBO and pose,
    // before grids, waypoints and structure markers, so explored blocks cannot hide focus.
    @Inject(method = "extractRenderState", at = @At(
        value = "INVOKE",
        target = "Lxaero/map/graphics/renderer/multitexture/MultiTextureRenderTypeRendererProvider;draw(Lxaero/map/graphics/renderer/multitexture/MultiTextureRenderTypeRenderer;)V",
        ordinal = 1, shift = At.Shift.AFTER
    ))
    private void seedAtlas$renderBiomeFocus(
        CallbackInfo ci,
        @Local(name = "matrixStack") PoseStack pose,
        @Local(name = "flooredCameraX") int flooredCameraX,
        @Local(name = "flooredCameraZ") int flooredCameraZ
    ) {
        GuiMap map = (GuiMap)(Object)this;
        var processor = map.getMapProcessor();
        SeedAtlasBiomeOverlayRenderer.INSTANCE.renderBackground(
            pose, processor.getMapWorld().getCurrentDimension().getDimId(),
            this.cameraX, this.cameraZ, this.scale, flooredCameraX, flooredCameraZ,
            processor.getMultiTextureRenderTypeRenderers(), true
        );
    }

    @Inject(method = "init", at = @At("RETURN"))
    private void seedAtlas$addLayerControls(CallbackInfo ci) {
        GuiMap map = (GuiMap)(Object)this;
        SeedAtlasClientState.refreshContext(Minecraft.getInstance());
        SeedAtlasXaeroIntegration.setFullMapActive(true);

        // Xaero reserves x=0 in 20px rows along the left edge. Its own buttons
        // occupy h-40/-60/-100/-120; h-20 and h-80 are intentionally free.
        // Matching those slots avoids the centered coordinate text at y=2.
        int x = 0;
        int layerY = Math.max(0, map.height - 20);
        this.seedAtlas$layerButton = new TooltipButton(x, layerY, 20, 20,
            compactLayerButtonText(), button -> {
            SeedAtlasClientState.toggleLayer();
            button.setMessage(compactLayerButtonText());
            SeedAtlasXaeroIntegration.synchronizeRevision();
        }, () -> new Tooltip(layerButtonText()));
        map.addButton(this.seedAtlas$layerButton);

        int settingsY = Math.max(0, map.height - 80);
        this.seedAtlas$settingsButton = new TooltipButton(x, settingsY, 20, 20,
            Component.literal("..."), button -> SeedAtlasClientState.openSettings(map),
            () -> new Tooltip(Component.translatable("screen.seedatlas_xaero.settings")));
        map.addButton(this.seedAtlas$settingsButton);

        this.seedAtlas$displayedRevision = SeedAtlasClientState.revision();
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void seedAtlas$refreshState(CallbackInfo ci) {
        SeedAtlasClientState.refreshContext(Minecraft.getInstance());
        SeedAtlasXaeroIntegration.synchronizeRevision();
        seedAtlas$observeXaeroView();
        if (this.seedAtlas$layerButton != null) {
            this.seedAtlas$layerButton.visible = !GuiMap.hiddenUI;
        }
        if (this.seedAtlas$settingsButton != null) {
            this.seedAtlas$settingsButton.visible = !GuiMap.hiddenUI;
        }
        long revision = SeedAtlasClientState.revision();
        if (revision == this.seedAtlas$displayedRevision) {
            return;
        }
        this.seedAtlas$displayedRevision = revision;
        if (this.seedAtlas$layerButton != null) {
            this.seedAtlas$layerButton.setMessage(compactLayerButtonText());
        }
    }

    @Inject(method = "removed", at = @At("HEAD"))
    private void seedAtlas$disableFullMapLayer(CallbackInfo ci) {
        SeedAtlasXaeroIntegration.setFullMapActive(false);
    }

    @Unique
    private static Component layerButtonText() {
        Component state = Component.translatable(
            SeedAtlasClientState.config().layerEnabled()
                ? "options.seedatlas_xaero.on"
                : "options.seedatlas_xaero.off");
        return Component.translatable("options.seedatlas_xaero.layer", state);
    }

    @Unique
    private static Component compactLayerButtonText() {
        return Component.literal("SA").withStyle(
            SeedAtlasClientState.config().layerEnabled()
                ? ChatFormatting.GREEN : ChatFormatting.GRAY);
    }

    @Unique
    private void seedAtlas$observeXaeroView() {
        try {
            GuiMap map = (GuiMap)(Object)this;
            if (map.getMapProcessor() == null
                || map.getMapProcessor().getMapWorld() == null
                || map.getMapProcessor().getMapWorld().getCurrentDimension() == null) {
                return;
            }
            SeedAtlasXaeroIntegration.observeMapView(
                map.getMapProcessor().getMapWorld().getCurrentDimension().getDimId(),
                this.cameraX,
                this.cameraZ,
                this.scale,
                Minecraft.getInstance().getWindow().getWidth(),
                Minecraft.getInstance().getWindow().getHeight()
            );
        } catch (RuntimeException ignored) {
            // GuiMap can swap dimensions while a frame is being prepared.
        }
    }
}
