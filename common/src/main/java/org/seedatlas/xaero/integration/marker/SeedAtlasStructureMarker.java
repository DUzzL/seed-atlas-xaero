package org.seedatlas.xaero.integration.marker;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.seedatlas.xaero.config.MarkerType;

/** Immutable map element produced by a bounded native structure scan. */
public record SeedAtlasStructureMarker(
    MarkerType type,
    int x,
    int y,
    int z,
    ResourceKey<Level> dimension,
    boolean approximate,
    String biomeName,
    boolean specialLoot
) {
    public SeedAtlasStructureMarker(MarkerType type, int x, int y, int z,
                                    ResourceKey<Level> dimension, boolean approximate) {
        this(type, x, y, z, dimension, approximate, null, false);
    }

    public String textureId() {
        return type == MarkerType.ABANDONED_CAMP && specialLoot ? "camp_special" : type.id();
    }

    public Component tooltip() {
        String yText = y == Integer.MIN_VALUE ? "?" : Integer.toString(y);
        Component result = Component.empty().append(type.displayName());
        if (approximate) {
            result = result.copy().append(Component.translatableWithFallback(
                "tooltip.seedatlas_xaero.approximate", " (approx.)"));
        }
        if (biomeName != null) {
            result = result.copy().append(" · ").append(Component.translatable(
                "biome." + biomeName.replace(':', '.')));
        }
        if (specialLoot) {
            result = result.copy().append(" · ").append(Component.translatable(
                "tooltip.seedatlas_xaero.special_loot"));
        }
        return result.copy().append(Component.literal(
            " \u00b7 X=" + x + " \u00b7 Y=" + yText + " \u00b7 Z=" + z));
    }

    public String displayName() {
        return type.displayName().getString();
    }
}
