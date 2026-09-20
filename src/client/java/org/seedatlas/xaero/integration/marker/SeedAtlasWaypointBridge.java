package org.seedatlas.xaero.integration.marker;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xaero.map.WorldMap;
import xaero.map.common.config.option.WorldMapProfiledConfigOptions;
import xaero.map.gui.GuiMap;
import xaero.map.mods.SupportMods;

/** Uses World Map's own optional integration, never Minimap classes or version strings. */
final class SeedAtlasWaypointBridge {
    private static final Logger LOGGER = LoggerFactory.getLogger("seedatlas_xaero");
    private static boolean incompatible;

    static boolean available() {
        if (incompatible) return false;
        try {
            return SupportMods.minimap() && Boolean.TRUE.equals(WorldMap.INSTANCE.getConfigs()
                .getClientConfigManager().getEffective(WorldMapProfiledConfigOptions.WAYPOINTS));
        } catch (LinkageError error) {
            disable(error);
            return false;
        }
    }

    static void create(GuiMap map, SeedAtlasStructureMarker marker) {
        if (!available()) {
            unavailable(map);
            return;
        }
        try {
            var processor = map.getMapProcessor();
            var dimension = processor.getMapWorld().getCurrentDimension();
            if (dimension == null || !marker.dimension().equals(dimension.getDimId())) {
                unavailable(map);
                return;
            }
            // Xaero captures the source waypoint world on the actual right click.
            // Supply exact structure coordinates, not the nearby mouse/terrain pixel.
            // Its bridge expects 32767 for unknown Y, rather than our MIN_VALUE.
            SupportMods.xaeroMinimap.createWaypoint(map, marker.x(),
                marker.y() == Integer.MIN_VALUE ? 32767 : marker.y(), marker.z(),
                dimension.calculateDimScale(processor.getWorldDimensionTypeRegistry()), true);
            if (Minecraft.getInstance().gui.screen() == map) unavailable(map);
        } catch (LinkageError error) {
            disable(error);
            unavailable(map);
        }
    }

    private static void disable(LinkageError error) {
        incompatible = true;
        LOGGER.warn("Disabling optional waypoint integration: World Map's bridge is incompatible", error);
    }

    private static void unavailable(GuiMap map) {
        map.getMapProcessor().getMessageBox().addMessage(
            Component.translatable("message.seedatlas_xaero.waypoint_unavailable"));
    }
}
