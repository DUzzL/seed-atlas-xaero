package org.seedatlas.xaero.integration.marker;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.seedatlas.xaero.integration.SeedAtlasXaeroIntegration;
import xaero.lib.client.graphics.XaeroBufferProvider;
import xaero.map.element.MapElementGraphics;
import xaero.map.element.render.ElementRenderInfo;
import xaero.map.element.render.ElementRenderLocation;
import xaero.map.element.render.ElementRenderer;
import xaero.map.graphics.CustomRenderTypes;
import xaero.map.graphics.MapRenderHelper;
import xaero.map.graphics.renderer.multitexture.MultiTextureRenderTypeRendererProvider;

/** Xaero map-element renderer backed by Seed Atlas' original structure icons. */
public final class SeedAtlasStructureRenderer extends ElementRenderer<
    SeedAtlasStructureMarker,
    SeedAtlasStructureContext,
    SeedAtlasStructureRenderer
> {
    public static final SeedAtlasStructureRenderer INSTANCE = new SeedAtlasStructureRenderer();

    private final SeedAtlasStructureMarkerSource source;

    private SeedAtlasStructureRenderer() {
        this(new SeedAtlasStructureContext(), new SeedAtlasStructureProvider(), new SeedAtlasStructureReader());
    }

    private SeedAtlasStructureRenderer(
        SeedAtlasStructureContext context,
        SeedAtlasStructureProvider provider,
        SeedAtlasStructureReader reader
    ) {
        super(context, provider, reader);
        this.source = new SeedAtlasStructureMarkerSource();
    }

    @Override
    public void preRender(
        ElementRenderInfo renderInfo,
        XaeroBufferProvider xaeroBufferProvider,
        MultiTextureRenderTypeRendererProvider rendererProvider,
        boolean shadow
    ) {
        if (!SeedAtlasXaeroIntegration.isLayerActive()) {
            this.context.markers = List.of();
            this.context.batchedIcons = null;
            return;
        }
        this.context.dimension = renderInfo.mapDimension;
        this.context.batchedIcons = rendererProvider.getRenderer(CustomRenderTypes.GUI_BILINEAR_PRE);
        Minecraft minecraft = Minecraft.getInstance();
        this.source.request(
            this.context,
            renderInfo.mapDimension,
            renderInfo.renderPos.x,
            renderInfo.renderPos.z,
            renderInfo.scale,
            minecraft.getWindow().getWidth(),
            minecraft.getWindow().getHeight()
        );
    }

    @Override
    public void postRender(
        ElementRenderInfo renderInfo,
        XaeroBufferProvider xaeroBufferProvider,
        MultiTextureRenderTypeRendererProvider rendererProvider,
        boolean shadow
    ) {
        if (this.context.batchedIcons != null) {
            rendererProvider.draw(this.context.batchedIcons);
        }
    }

    @Override
    public void renderElementShadow(
        SeedAtlasStructureMarker marker,
        boolean hovered,
        float optionalScale,
        double partialX,
        double partialY,
        ElementRenderInfo renderInfo,
        MapElementGraphics graphics,
        XaeroBufferProvider xaeroBufferProvider,
        MultiTextureRenderTypeRendererProvider rendererProvider
    ) {
    }

    @Override
    public boolean renderElement(
        SeedAtlasStructureMarker marker,
        boolean hovered,
        double optionalDepth,
        float optionalScale,
        double partialX,
        double partialY,
        ElementRenderInfo renderInfo,
        MapElementGraphics graphics,
        XaeroBufferProvider xaeroBufferProvider,
        MultiTextureRenderTypeRendererProvider rendererProvider
    ) {
        PoseStack pose = graphics.pose();
        pose.translate(partialX, partialY, optionalDepth);
        pose.scale(optionalScale, optionalScale, 1.0F);
        if (hovered) {
            graphics.fill(-11, -11, 11, 11, 0x99000000);
        }
        Identifier texture = Identifier.fromNamespaceAndPath(
            "seedatlas_xaero", "textures/structure/" + marker.type().id() + ".png");
        MapRenderHelper.blitIntoMultiTextureRenderer(
            pose.last().pose(),
            this.context.batchedIcons,
            -10.0F,
            -10.0F,
            0,
            0,
            20,
            20,
            1.0F,
            1.0F,
            1.0F,
            1.0F,
            20,
            20,
            Minecraft.getInstance().getTextureManager().getTexture(texture).getTextureView()
        );
        return false;
    }

    @Override
    public boolean shouldRender(ElementRenderLocation location, boolean shadow) {
        return location == ElementRenderLocation.WORLD_MAP
            && !shadow
            && SeedAtlasXaeroIntegration.isLayerActive();
    }

    @Override
    public int getOrder() {
        return 175;
    }

    @Override
    public boolean shouldBeDimScaled() {
        return false;
    }
}
