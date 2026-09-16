package org.seedatlas.xaero.integration.marker;

import net.minecraft.client.Minecraft;
import xaero.lib.client.gui.widget.Tooltip;
import xaero.map.element.render.ElementReader;
import xaero.map.element.render.ElementRenderLocation;

final class SeedAtlasStructureReader extends ElementReader<
    SeedAtlasStructureMarker,
    SeedAtlasStructureContext,
    SeedAtlasStructureRenderer
> {
    @Override
    public boolean isHidden(SeedAtlasStructureMarker marker, SeedAtlasStructureContext context) {
        return context.dimension == null || !context.dimension.equals(marker.dimension());
    }

    @Override
    public double getRenderX(
        SeedAtlasStructureMarker marker, SeedAtlasStructureContext context, float partialTicks
    ) {
        return marker.x();
    }

    @Override
    public double getRenderY(
        SeedAtlasStructureMarker marker, SeedAtlasStructureContext context, float partialTicks
    ) {
        return marker.y();
    }

    @Override
    public boolean hasYCoordinate() {
        return true;
    }

    @Override
    public double getRenderZ(
        SeedAtlasStructureMarker marker, SeedAtlasStructureContext context, float partialTicks
    ) {
        return marker.z();
    }

    @Override
    public int getInteractionBoxLeft(
        SeedAtlasStructureMarker marker, SeedAtlasStructureContext context, float partialTicks
    ) {
        return -10;
    }

    @Override
    public int getInteractionBoxRight(
        SeedAtlasStructureMarker marker, SeedAtlasStructureContext context, float partialTicks
    ) {
        return 10;
    }

    @Override
    public int getInteractionBoxTop(
        SeedAtlasStructureMarker marker, SeedAtlasStructureContext context, float partialTicks
    ) {
        return -10;
    }

    @Override
    public int getInteractionBoxBottom(
        SeedAtlasStructureMarker marker, SeedAtlasStructureContext context, float partialTicks
    ) {
        return 10;
    }

    @Override
    public int getRenderBoxLeft(
        SeedAtlasStructureMarker marker, SeedAtlasStructureContext context, float partialTicks
    ) {
        return -11;
    }

    @Override
    public int getRenderBoxRight(
        SeedAtlasStructureMarker marker, SeedAtlasStructureContext context, float partialTicks
    ) {
        return 11;
    }

    @Override
    public int getRenderBoxTop(
        SeedAtlasStructureMarker marker, SeedAtlasStructureContext context, float partialTicks
    ) {
        return -11;
    }

    @Override
    public int getRenderBoxBottom(
        SeedAtlasStructureMarker marker, SeedAtlasStructureContext context, float partialTicks
    ) {
        return 11;
    }

    @Override
    public int getLeftSideLength(SeedAtlasStructureMarker marker, Minecraft minecraft) {
        return 10 + minecraft.font.width(marker.displayName());
    }

    @Override
    public String getMenuName(SeedAtlasStructureMarker marker) {
        return marker.displayName();
    }

    @Override
    public String getFilterName(SeedAtlasStructureMarker marker) {
        return marker.type().id();
    }

    @Override
    public int getMenuTextFillLeftPadding(SeedAtlasStructureMarker marker) {
        return 0;
    }

    @Override
    public int getRightClickTitleBackgroundColor(SeedAtlasStructureMarker marker) {
        return 0xCC1F2530;
    }

    @Override
    public boolean shouldScaleBoxWithOptionalScale() {
        return true;
    }

    @Override
    public float getBoxScale(
        ElementRenderLocation location, SeedAtlasStructureMarker marker,
        SeedAtlasStructureContext context
    ) {
        // Xaero applies optionalScale separately to both hit and render boxes.
        return context.iconScale;
    }

    @Override
    public boolean isInteractable(ElementRenderLocation location, SeedAtlasStructureMarker marker) {
        return location == ElementRenderLocation.WORLD_MAP;
    }

    @Override
    public Tooltip getTooltip(
        SeedAtlasStructureMarker marker, SeedAtlasStructureContext context, boolean overMenu
    ) {
        return new Tooltip(marker.tooltip(), true);
    }
}
