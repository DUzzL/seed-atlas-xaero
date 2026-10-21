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
public final class SeedAtlasWaypointBridge {
    private static final Logger LOGGER = LoggerFactory.getLogger("seedatlas_xaero");
    private static boolean incompatible;

    /**
     * Named waypoint at the exact coordinates in the current waypoint world. Built reflectively
     * because World Map does not expose Minimap's waypoint classes at compile time.
     */
    public static boolean createNamed(int x, int z, String name, boolean temporary) {
        if (!available()) return false;
        try {
            Object support = SupportMods.xaeroMinimap;
            Object world = support.getClass().getMethod("getWaypointWorld").invoke(support);
            if (world == null) return false;
            Class<?> waypointClass = Class.forName("xaero.common.minimap.waypoints.Waypoint");
            Class<?> colorClass = Class.forName("xaero.hud.minimap.waypoint.WaypointColor");
            Class<?> purposeClass = Class.forName("xaero.hud.minimap.waypoint.WaypointPurpose");
            Class<?> worldClass = Class.forName("xaero.hud.minimap.world.MinimapWorld");
            Object waypoint = waypointClass.getConstructor(int.class, int.class, int.class, String.class,
                String.class, colorClass, purposeClass, boolean.class, boolean.class).newInstance(
                x, 32767, z, name, name.isEmpty() ? "X" : name.substring(0, 1).toUpperCase(),
                colorClass.getMethod("getRandom").invoke(null), purposeClass.getField("NORMAL").get(null),
                temporary, false);
            Object set = worldClass.getMethod("getCurrentWaypointSet").invoke(world);
            set.getClass().getMethod("add", waypointClass).invoke(set, waypoint);
            if (!temporary) {
                Object module = Class.forName("xaero.hud.minimap.BuiltInHudModules").getField("MINIMAP").get(null);
                Object session = module.getClass().getMethod("getCurrentSession").invoke(module);
                Object io = session.getClass().getMethod("getWorldManagerIO").invoke(session);
                io.getClass().getMethod("saveWorld", worldClass).invoke(io, world);
            }
            SupportMods.xaeroMinimap.requestWaypointsRefresh();
            return true;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError error) {
            LOGGER.warn("Could not create Seed Atlas waypoint", error);
            return false;
        }
    }


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

    static void createTemporary(GuiMap map, SeedAtlasStructureMarker marker) {
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
            SupportMods.xaeroMinimap.createTempWaypoint(marker.x(),
                marker.y() == Integer.MIN_VALUE ? 32767 : marker.y(), marker.z(),
                dimension.calculateDimScale(processor.getWorldDimensionTypeRegistry()), true);
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
