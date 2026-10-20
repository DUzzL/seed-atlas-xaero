package org.seedatlas.xaero.biome;

import java.util.Set;
import java.util.ArrayList;
import org.seedatlas.xaero.integration.StateRevisionTracker;
import org.seedatlas.xaero.integration.biome.BiomeHighlighter;
import org.seedatlas.xaero.integration.biome.BiomeTerrainStyler;

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
        // Terrain styler: deterministic, seam-free across tiles (negative coords), alpha/unknown preserved.
        int n = 24;
        short[] fid = new short[n * n];
        java.util.Arrays.fill(fid, (short) 4);
        int[] a = new int[n * n];
        java.util.Arrays.fill(a, 0xC03E6B3E);
        int[] b = a.clone();
        BiomeTerrainStyler.apply(a, fid, -20, -36, 2, n, 99L);
        BiomeTerrainStyler.apply(b, fid, -20, -36, 2, n, 99L);
        check(java.util.Arrays.equals(a, b), "Terrain styling is deterministic");
        int[] left = new int[n * n];
        java.util.Arrays.fill(left, 0xC03E6B3E);
        BiomeTerrainStyler.apply(left, fid, -20 - n * 2, -36, 2, n, 99L);
        int[] whole = new int[n * 2 * n * 2];
        short[] wid = new short[n * 2 * n * 2];
        java.util.Arrays.fill(wid, (short) 4);
        java.util.Arrays.fill(whole, 0xC03E6B3E);
        BiomeTerrainStyler.apply(whole, wid, -20 - n * 2, -36, 2, n * 2, 99L);
        boolean seam = true;
        for (int z = 6; z < n - 6; ++z) for (int x = 6; x < n - 6; ++x) seam &= left[z * n + x] == whole[z * n * 2 + x];
        check(seam, "Tile interiors match a larger tile at the same world position");
        boolean alphaKept = true;
        for (int v : a) alphaKept &= (v >>> 24) == 0xC0;
        check(alphaKept, "Terrain styling preserves alpha");
        int[] unknown = {0xFF123456};
        BiomeTerrainStyler.apply(unknown, new short[]{(short) 65535}, 0, 0, 1, 1, 1L);
        check(unknown[0] == 0xFF123456, "Unknown biome samples keep their colour");
        // Real heights: with light from the north-west, ground rising toward the south-east is bright.
        int hn = 8, hw = hn + 2;
        float[] ramp = new float[hw * hw];
        for (int z = 0; z < hw; ++z) for (int x = 0; x < hw; ++x) ramp[z * hw + x] = 63 + (x + z) * 6;
        short[] hid = new short[hn * hn];
        java.util.Arrays.fill(hid, (short) 4);
        int[] lit = new int[hn * hn];
        java.util.Arrays.fill(lit, 0xC03E6B3E);
        BiomeTerrainStyler.apply(lit, hid, 0, 0, 4, hn, 5L, ramp);
        int[] shaded = new int[hn * hn];
        java.util.Arrays.fill(shaded, 0xC03E6B3E);
        float[] rampAway = new float[hw * hw];
        for (int i = 0; i < ramp.length; ++i) rampAway[i] = 2 * 63 - ramp[i];
        BiomeTerrainStyler.apply(shaded, hid, 0, 0, 4, hn, 5L, rampAway);
        long litSum = 0, shadedSum = 0;
        for (int i = 0; i < hn * hn; ++i) { litSum += (lit[i] >> 8) & 255; shadedSum += (shaded[i] >> 8) & 255; }
        check(litSum > shadedSum, "Slopes facing the north-west light are brighter than slopes facing away");
        check((lit[0] >>> 24) == 0xC0, "Height shading preserves alpha");
        int[] fallbackA = new int[hn * hn], fallbackB = new int[hn * hn];
        java.util.Arrays.fill(fallbackA, 0xC03E6B3E);
        java.util.Arrays.fill(fallbackB, 0xC03E6B3E);
        BiomeTerrainStyler.apply(fallbackA, hid, 0, 0, 4, hn, 5L);
        BiomeTerrainStyler.apply(fallbackB, hid, 0, 0, 4, hn, 5L, new float[3]);
        check(java.util.Arrays.equals(fallbackA, fallbackB), "A wrong-sized height grid falls back to the noise relief");
        System.out.println("Biome highlight colour and deferred revision tests passed");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
