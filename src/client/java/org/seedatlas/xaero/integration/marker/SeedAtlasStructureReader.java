package org.seedatlas.xaero.integration.marker;

import java.util.ArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.seedatlas.xaero.config.SeedAtlasClientState;
import xaero.map.gui.IRightClickableElement;
import xaero.map.gui.dropdown.rightclick.RightClickOption;
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
        return context.dimension == null || !context.dimension.equals(marker.dimension())
            || (SeedAtlasClientState.config().hideCompletedStructures() && SeedAtlasStructureState.isCompleted(marker));
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
        // Show horizontal map distance: many predicted structures have unknown Y.
        return false;
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
    public boolean isRightClickValid(SeedAtlasStructureMarker marker) {
        return SeedAtlasStructureState.key(marker) != null;
    }

    @Override
    public ArrayList<RightClickOption> getRightClickOptions(SeedAtlasStructureMarker marker, IRightClickableElement target) {
        var options = new ArrayList<RightClickOption>();
        options.add(new RightClickOption("", options.size(), target) {
            @Override
            public Component getDisplayName() {
                return Component.literal(marker.displayName()).withStyle(net.minecraft.ChatFormatting.GRAY);
            }

            @Override
            public void onAction(Screen screen) { }
        }.setActive(false));
        var key = SeedAtlasStructureState.key(marker);
        boolean completed = SeedAtlasClientState.config().isCompleted(key);
        options.add(new RightClickOption(completed ? "menu.seedatlas_xaero.mark_incomplete"
            : "menu.seedatlas_xaero.mark_completed", options.size(), target) {
            @Override
            public void onAction(Screen screen) {
                // Ignore stale menus if the world/seed changed while they were open.
                if (key != null && key.equals(SeedAtlasStructureState.key(marker))) {
                    SeedAtlasClientState.setStructureCompleted(key, !completed);
                }
            }
        }.setActive(key != null));
        options.add(new RightClickOption("menu.seedatlas_xaero.copy_coordinates", options.size(), target) {
            @Override
            public void onAction(Screen screen) {
                String coordinates = "X: " + marker.x()
                    + (marker.y() == Integer.MIN_VALUE ? "" : ", Y: " + marker.y()) + ", Z: " + marker.z();
                Minecraft.getInstance().keyboardHandler.setClipboard(coordinates);
            }
        });
        if (SeedAtlasWaypointBridge.available()) {
            options.add(new RightClickOption("menu.seedatlas_xaero.create_waypoint", options.size(), target) {
                @Override
                public void onAction(Screen screen) {
                    if (screen instanceof xaero.map.gui.GuiMap map && key != null
                        && key.equals(SeedAtlasStructureState.key(marker))) {
                        SeedAtlasWaypointBridge.create(map, marker);
                    }
                }
            });
        }
        return options;
    }

    @Override
    public Tooltip getTooltip(
        SeedAtlasStructureMarker marker, SeedAtlasStructureContext context, boolean overMenu
    ) {
        return new Tooltip(SeedAtlasStructureState.isCompleted(marker)
            ? marker.tooltip().copy().append(" · ").append(Component.translatable("tooltip.seedatlas_xaero.completed"))
            : marker.tooltip(), true);
    }
}
