package org.seedatlas.xaero.integration.biome;

/**
 * Terrain look for seed-only tiles: hill-shaded relief, canopy dots, water depth tones and
 * biome-edge outlines. When the native engine supplies height estimates, relief is shaded from
 * them (an approximation of the 1.18+ terrain offset, not the exact surface: caves, trees,
 * structures and jagged peaks are not modelled). Otherwise relief falls back to smooth noise
 * over absolute block coordinates, which is decoration only. Either way adjacent tiles match.
 */
public final class BiomeTerrainStyler {
    private static final int LIMIT = 256;
    private static final byte WATER = 1, DEEP = 2, LAND = 3;
    /** Relief strength per biome id (0 = flat). */
    private static final int[] RELIEF = new int[LIMIT];
    private static final byte[] KIND = new byte[LIMIT];
    private static final boolean[] CANOPY = new boolean[LIMIT];
    static {
        java.util.Arrays.fill(KIND, LAND);
        java.util.Arrays.fill(RELIEF, 14);
        for (int id : new int[]{0, 10, 44, 45, 46, 47}) KIND[id] = WATER;
        for (int id : new int[]{24, 48, 49, 50}) KIND[id] = DEEP;
        for (int id : new int[]{7, 11}) KIND[id] = WATER;
        for (int id : new int[]{2, 17, 130, 35, 36, 163, 164, 38, 39, 37, 165, 166, 167}) RELIEF[id] = 20;
        for (int id : new int[]{3, 20, 34, 131, 162, 177, 178, 179, 180, 181, 182, 12, 13, 140}) RELIEF[id] = 38;
        for (int id : new int[]{180, 181, 182, 3, 34, 131, 162}) RELIEF[id] = 52;
        for (int id : new int[]{4, 5, 18, 19, 21, 22, 23, 27, 28, 29, 30, 31, 32, 33, 132, 133, 155, 156, 157, 158, 160, 161, 168, 169, 185, 184, 6, 134}) {
            CANOPY[id] = true;
            RELIEF[id] = 22;
        }
        for (int id : new int[]{16, 25, 26}) RELIEF[id] = 6; // beaches and shores stay flat
    }

    private BiomeTerrainStyler() { }

    /** Cosmetic-noise relief; see {@link #apply(int[], short[], int, int, int, int, long, float[])}. */
    public static void apply(int[] argb, short[] ids, int originX, int originZ, int step, int size, long seed) {
        apply(argb, ids, originX, originZ, step, size, seed, null);
    }

    /**
     * Rewrites row-major ARGB samples in place; alpha and biome ids are never changed.
     * {@code heights} is an optional (size+2)^2 block-height grid with a one-sample halo
     * (its [1,1] entry is sample [0,0]); a missing or wrong-sized grid selects the noise fallback.
     */
    public static void apply(int[] argb, short[] ids, int originX, int originZ, int step, int size, long seed,
                             float[] heights) {
        int count = Math.min(Math.min(argb.length, ids.length), size * size);
        if (count < size * size) return;
        int hw = size + 2;
        boolean real = heights != null && heights.length == hw * hw;
        // The engine's biomes are 4x4-block cells, so edges look like stairs. Warp each sample's
        // lookup by smooth noise to make edges organic (about +-5 blocks; none once cells are tiny).
        float warp = 5.0F / step;
        int[] colour = new int[count];
        short[] id = new short[count];
        for (int z = 0; z < size; ++z) {
            for (int x = 0; x < size; ++x) {
                int sx = x, sz = z;
                if (warp >= 0.6F) {
                    int wx = originX + x * step, wz = originZ + z * step;
                    sx = Math.max(0, Math.min(size - 1, x + Math.round(noise(wx / 9.0F, wz / 9.0F, seed + 7) * warp)));
                    sz = Math.max(0, Math.min(size - 1, z + Math.round(noise(wx / 9.0F, wz / 9.0F, seed + 8) * warp)));
                }
                colour[z * size + x] = argb[sz * size + sx];
                id[z * size + x] = ids[sz * size + sx];
            }
        }
        int d = Math.max(2, step);
        for (int z = 0; z < size; ++z) {
            for (int x = 0; x < size; ++x) {
                int i = z * size + x;
                int bid = id[i] & 0xFFFF;
                if (bid >= LIMIT || (ids[i] & 0xFFFF) >= LIMIT) {
                    continue; // unknown samples keep their original colour
                }
                int wx = originX + x * step;
                int wz = originZ + z * step;
                byte kind = KIND[bid];
                int delta;
                if (kind == LAND) {
                    if (real) {
                        // Light from the north-west: slopes facing it (ground rising toward the south-east) are bright.
                        float slope = (heights[(z + 2) * hw + x + 2] - heights[z * hw + x]) / (2.0F * step);
                        delta = Math.round(Math.max(-35.0F, Math.min(35.0F, slope * 90.0F)));
                        // Gentle elevation tint around sea level (about y=63).
                        delta += Math.round(Math.max(-12.0F, Math.min(12.0F, (heights[(z + 1) * hw + x + 1] - 63.0F) * 0.12F)));
                    } else {
                        int amp = RELIEF[bid];
                        // Light from the north-west: slope toward it is bright, away is dark.
                        float lit = height(wx - d, wz - d, seed) - height(wx + d, wz + d, seed);
                        delta = Math.round(lit * amp * 3.2F) + Math.round(height(wx, wz, seed) * amp * 0.5F);
                    }
                    delta += (int) (hash(wx / step, wz / step, seed) % 9) - 4; // per-block grain
                    if (CANOPY[bid] && step <= 4) {
                        long h = hash(Math.floorDiv(wx, step), Math.floorDiv(wz, step), seed ^ 0x5DEECE66DL);
                        if (h % 100 < 42) delta -= 30; // tree crown
                        else if (h % 100 < 50) delta += 12; // lit gap
                    }
                    delta -= 14; // Xaero's explored land reads darker and more muted
                } else {
                    if (real) {
                        // Water: darker where the estimated floor is further below sea level.
                        float below = Math.max(0.0F, Math.min(70.0F, 63.0F - heights[(z + 1) * hw + x + 1]));
                        delta = -16 - Math.round(below * 16.0F / 70.0F) - (kind == DEEP ? 10 : 0);
                    } else {
                        // Water: broad depth bands plus faint ripples.
                        float depth = height(wx * 2, wz * 2, seed ^ 0xABCDEFL);
                        delta = Math.round(depth * (kind == DEEP ? 14 : 9)) - (kind == DEEP ? 10 : 0);
                        delta -= 22;
                    }
                    delta += (int) (hash(wx / step, wz / step, seed) % 5) - 2;
                }
                // Biome-edge outline: lighter on the north-west side, darker on the south-east.
                if (x > 0 && z > 0 && (id[i - size - 1] & 0xFFFF) != bid) delta += 14;
                if (x + 1 < size && z + 1 < size && (id[i + size + 1] & 0xFFFF) != bid) delta -= 14;
                argb[i] = shade(colour[i], delta);
            }
        }
    }
    /** Smooth multi-octave noise in about [-1, 1]. */
    private static float height(int x, int z, long seed) {
        return 0.55F * noise(x / 40.0F, z / 40.0F, seed)
            + 0.30F * noise(x / 15.0F, z / 15.0F, seed + 1)
            + 0.15F * noise(x / 6.0F, z / 6.0F, seed + 2);
    }

    private static float noise(float x, float z, long seed) {
        int x0 = (int) Math.floor(x), z0 = (int) Math.floor(z);
        float fx = x - x0, fz = z - z0;
        fx = fx * fx * (3 - 2 * fx);
        fz = fz * fz * (3 - 2 * fz);
        float a = lattice(x0, z0, seed), b = lattice(x0 + 1, z0, seed);
        float c = lattice(x0, z0 + 1, seed), e = lattice(x0 + 1, z0 + 1, seed);
        return (a + (b - a) * fx) + ((c + (e - c) * fx) - (a + (b - a) * fx)) * fz;
    }

    private static float lattice(int x, int z, long seed) {
        return (hash(x, z, seed) % 2001) / 1000.0F - 1.0F;
    }

    private static int shade(int colour, int percent) {
        int r = channel(colour >> 16, percent);
        int g = channel(colour >> 8, percent);
        int b = channel(colour, percent);
        return (colour & 0xFF000000) | (r << 16) | (g << 8) | b;
    }

    private static int channel(int value, int percent) {
        return Math.max(0, Math.min(255, (value & 0xFF) * (100 + percent) / 100));
    }

    private static long hash(int x, int z, long seed) {
        long h = seed ^ (x * 0x9E3779B97F4A7C15L) ^ (z * 0xC2B2AE3D27D4EB4FL);
        h ^= h >>> 32; h *= 0xD6E8FEB86659FD93L; h ^= h >>> 32;
        return h & Long.MAX_VALUE;
    }
}
