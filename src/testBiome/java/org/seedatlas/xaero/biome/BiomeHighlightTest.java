package org.seedatlas.xaero.biome;

import java.util.Set;
import java.util.ArrayList;
import org.seedatlas.xaero.integration.StateRevisionTracker;
import org.seedatlas.xaero.integration.biome.BiomeHighlighter;

/** Verifies the Seed Atlas style highlight colours without opening a Minecraft client. */
public final class BiomeHighlightTest {
    public static void main(String[] args) {
        int[] argb = {0xFF3E6B3E, 0xFF2C4C6B, 0xFF808080};
        short[] ids = {1, 4, 1};

        BiomeHighlighter.apply(argb, ids, BiomeHighlighter.mask(true, Set.of(1)));
        check(argb[0] == 0xFF568356, "Highlighted biome keeps its colour and brightens by 24");
        check(argb[1] == 0xFF202020, "Unselected biome turns into the dark Seed Atlas grey");
        check(argb[2] == 0xFF989898, "Every highlighted sample is brightened");

        int[] untouched = {0xFF3E6B3E};
        BiomeHighlighter.apply(untouched, new short[]{1}, BiomeHighlighter.mask(false, Set.of(1)));
        check(untouched[0] == 0xFF3E6B3E, "Disabled highlighting keeps the plain biome colours");

        check(BiomeHighlighter.mask(true, Set.of()).length == 0, "Empty selection yields an empty mask");
        boolean[] mask = BiomeHighlighter.mask(true, Set.of(3, 200, 999, -5));
        check(mask.length == BiomeHighlighter.BIOME_ID_LIMIT, "Mask covers every biome id");
        check(mask[3] && mask[200] && !mask[2], "Mask marks exactly the selected ids");

        int[] alpha = {0x803E6B3E};
        BiomeHighlighter.apply(alpha, new short[]{1}, BiomeHighlighter.mask(true, Set.of(1)));
        check((alpha[0] & 0xFF000000) == 0x80000000, "Overlay alpha is preserved");

        // Reproduce selecting biomes in a paused settings screen: ticks must not consume
        // those revisions, and closing the screen must apply the latest selection once.
        var tracker = new StateRevisionTracker();
        var applied = new ArrayList<Long>();
        tracker.synchronize(1, false, applied::add);
        tracker.synchronize(2, true, applied::add);
        tracker.synchronize(3, true, applied::add);
        check(applied.equals(java.util.List.of(1L)), "Menus must not start tile work");
        tracker.synchronize(3, false, applied::add);
        tracker.synchronize(3, false, applied::add);
        check(applied.equals(java.util.List.of(1L, 3L)), "Resume must apply the newest pending selection exactly once");
        try {
            tracker.synchronize(4, false, revision -> { throw new IllegalStateException("retry"); });
        } catch (IllegalStateException expected) { }
        tracker.synchronize(4, false, applied::add);
        check(applied.equals(java.util.List.of(1L, 3L, 4L)), "Failed updates must not be acknowledged");

        int[] saturated = {0xFFFFFFF0, 0x406AB135};
        BiomeHighlighter.apply(saturated, new short[]{4, (short) 65535}, BiomeHighlighter.mask(true, Set.of(4)));
        check(saturated[0] == 0xFFFFFFFF, "Brightening saturates without wrapping");
        check((saturated[1] >>> 24) == 0x40 && ((saturated[1] >>> 16) & 255) == (saturated[1] & 255),
            "Unknown biome samples stay neutral and preserve alpha");
        System.out.println("Biome highlight colour and deferred revision tests passed");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
