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
    boolean approximate
) {
    public Component tooltip() {
        String yText = y == Integer.MIN_VALUE ? "?" : Integer.toString(y);
        Component result = Component.empty().append(type.displayName());
        if (approximate) {
            result = result.copy().append(Component.translatableWithFallback(
                "tooltip.seedatlas_xaero.approximate", " (approx.)"));
        }
        return result.copy().append(Component.literal(
            " \u00b7 X=" + x + " \u00b7 Y=" + yText + " \u00b7 Z=" + z));
    }

    public String displayName() {
        return type.displayName().getString();
    }
}
