package org.seedatlas.xaero.integration.biome;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterLists;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.block.state.BlockState;

/** Seed-only vanilla base terrain; never reads a world, constructs a chunk, or adds vegetation. */
public final class OverworldTerrainSampler {
    private static final int TILE_SIZE = 16;
    private static final int MAX_DIMENSION = 262;
    // 16x16 samples each, ~3 KB: enough to keep a dozen detailed map tiles for zooming and panning back.
    private static final int MAX_CACHED_TILES = 4096;
    // The Overworld top slide fixes density to -0.078125 from 64 blocks below the world top up, and
    // fluid only fills air below sea level, so every block from there up is air.
    private static final int TOP_SLIDE_END_DEPTH = 64;
    private static final int MAX_CONTEXTS = 2;
    private static final LinkedHashMap<TileKey, SurfaceGrid> CACHE = new LinkedHashMap<>(16, 0.75f, true);
    private static final LinkedHashMap<WorldKey, Context> CONTEXTS = new LinkedHashMap<>(4, 0.75f, true);
    private static final ConcurrentHashMap<String, Holder<Biome>> PREVIEW_BIOMES = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<TileKey, CompletableFuture<SurfaceGrid>> GENERATING = new ConcurrentHashMap<>();

    private OverworldTerrainSampler() { }

    /**
     * Row-major arrays, sampled at origin + index * step + (step - 1) / 2, including any caller halo.
     * Heights are the highest non-air base block Y (including fluid); floors are the highest solid Y.
     * These match base columns, not surface rules, carvers, structures, features, ice or snow layers.
     * Colours infer surface material from the base block, surface biome and temperature, not exact blocks.
     * Grass and leaves use the biome's vanilla tint; forest crowns are decoration, not predicted trees.
     * Minecraft must already be bootstrapped. Steps 1/2/4/8/16 and dimensions 1..262 are supported;
     * coarse steps still evaluate real vanilla density and are substantially more expensive per sample.
     */
    public static SurfaceGrid sample(long seed, boolean largeBiomes, int originX, int originZ,
                                     int step, int width, int height) {
        return sample(seed, largeBiomes, originX, originZ, step, width, height, () -> true);
    }

    /** Cancels stale work between output rows and before each tile it would generate. Safe to call from several threads. */
    public static SurfaceGrid sample(long seed, boolean largeBiomes, int originX, int originZ,
                                     int step, int width, int height, BooleanSupplier stillCurrent) {
        if (step < 1 || step > 16 || (step & (step - 1)) != 0) {
            throw new IllegalArgumentException("step must be one of 1, 2, 4, 8, or 16");
        }
        if (width < 1 || height < 1 || width > MAX_DIMENSION || height > MAX_DIMENSION) {
            throw new IllegalArgumentException("Surface dimensions must be between 1 and " + MAX_DIMENSION);
        }
        if ((long) originX + (long) width * step - 1 > Integer.MAX_VALUE
                || (long) originZ + (long) height * step - 1 > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Surface area exceeds block coordinate limits");
        }
        Objects.requireNonNull(stillCurrent, "stillCurrent");
        var world = new WorldKey(seed, largeBiomes);
        var result = new SurfaceGrid(new float[width * height], new float[width * height], new int[width * height]);
        int span = TILE_SIZE * step;
        for (int z = 0; z < height; ++z) {
            requireCurrent(stillCurrent);
            int blockZ = originZ + z * step + (step - 1) / 2;
            for (int x = 0; x < width; ++x) {
                int blockX = originX + x * step + (step - 1) / 2;
                var key = new TileKey(world, step, Math.floorDiv(blockX, span), Math.floorDiv(blockZ, span),
                        Math.floorMod(blockX, step), Math.floorMod(blockZ, step));
                var tile = tile(key, stillCurrent);
                int source = (Math.floorMod(blockZ, span) / step) * TILE_SIZE + Math.floorMod(blockX, span) / step;
                int target = z * width + x;
                result.heights[target] = tile.heights[source];
                result.floors[target] = tile.floors[source];
                result.colors[target] = tile.colors[source];
            }
        }
        return result;
    }

    public record SurfaceGrid(float[] heights, float[] floors, int[] colors) { }

    /** Cheap LOD preview using the same material palette, without evaluating terrain density. */
    static int previewColor(String name, int x, int y, int z, boolean water, int step) {
        if (name == null) return -1;
        var biome = PREVIEW_BIOMES.computeIfAbsent(name, value -> Vanilla.LOOKUP.lookupOrThrow(Registries.BIOME)
                .get(ResourceKey.create(Registries.BIOME, Identifier.parse(value))).orElse(null));
        return biome == null ? -1 : surfaceColor(biome, x, y, z, water, 63, step);
    }

    private static SurfaceGrid tile(TileKey key, BooleanSupplier stillCurrent) {
        synchronized (CACHE) {
            var cached = CACHE.get(key);
            if (cached != null) return cached;
        }
        requireCurrent(stillCurrent);
        // RandomState is shared by vanilla's own parallel chunk generation, so workers generate
        // different tiles at once; one that another worker is already generating is awaited instead.
        var mine = new CompletableFuture<SurfaceGrid>();
        var running = GENERATING.putIfAbsent(key, mine);
        if (running != null) return await(running);
        try {
            SurfaceGrid generated;
            synchronized (CACHE) {
                generated = CACHE.get(key);
            }
            if (generated == null) {
                generated = context(key.world).generate(key);
                synchronized (CACHE) {
                    if (CACHE.size() == MAX_CACHED_TILES) CACHE.remove(CACHE.keySet().iterator().next());
                    CACHE.put(key, generated);
                }
            }
            mine.complete(generated);
            return generated;
        } catch (RuntimeException | Error error) {
            mine.completeExceptionally(error);
            throw error;
        } finally {
            GENERATING.remove(key, mine);
        }
    }

    private static SurfaceGrid await(CompletableFuture<SurfaceGrid> running) {
        try {
            return running.join();
        } catch (CompletionException wrapped) {
            if (wrapped.getCause() instanceof RuntimeException cause) throw cause;
            if (wrapped.getCause() instanceof Error cause) throw cause;
            throw wrapped;
        }
    }

    private static Context context(WorldKey world) {
        synchronized (CONTEXTS) {
            var context = CONTEXTS.get(world);
            if (context == null) {
                context = new Context(world);
                if (CONTEXTS.size() == MAX_CONTEXTS) CONTEXTS.remove(CONTEXTS.keySet().iterator().next());
                CONTEXTS.put(world, context);
            }
            return context;
        }
    }

    private static void requireCurrent(BooleanSupplier stillCurrent) {
        if (!stillCurrent.getAsBoolean()) throw new CancellationException("Overworld sample is no longer current");
    }

    private record WorldKey(long seed, boolean largeBiomes) { }
    private record TileKey(WorldKey world, int step, int x, int z, int phaseX, int phaseZ) { }

    private static final class Context {
        private final NoiseGeneratorSettings settings;
        private final RandomState state;
        private final Aquifer.FluidPicker fluids;

        private Context(WorldKey key) {
            settings = Vanilla.LOOKUP.lookupOrThrow(Registries.NOISE_SETTINGS).getOrThrow(
                    key.largeBiomes ? NoiseGeneratorSettings.LARGE_BIOMES : NoiseGeneratorSettings.OVERWORLD).value();
            state = RandomState.create(settings, Vanilla.LOOKUP.lookupOrThrow(Registries.NOISE), key.seed);
            var water = new Aquifer.FluidStatus(settings.seaLevel(), settings.defaultFluid());
            var lava = new Aquifer.FluidStatus(-54, Blocks.LAVA.defaultBlockState());
            // Same built-in Overworld global picker as NoiseBasedChunkGenerator (without debug fluid disabling).
            fluids = (x, y, z) -> y < Math.min(-54, settings.seaLevel()) ? lava : water;
        }

        private SurfaceGrid generate(TileKey key) {
            var noiseSettings = settings.noiseSettings();
            int minY = noiseSettings.minY();
            int cellWidth = noiseSettings.getCellWidth();
            int cellHeight = noiseSettings.getCellHeight();
            int group = Math.max(1, cellWidth / key.step);
            int originX = key.x * TILE_SIZE * key.step + key.phaseX;
            int originZ = key.z * TILE_SIZE * key.step + key.phaseZ;
            int columns = TILE_SIZE * TILE_SIZE;
            int[] tops = new int[columns];
            int[] floors = new int[columns];
            boolean[] fluid = new boolean[columns];
            boolean[] done = new boolean[columns];
            Arrays.fill(tops, minY - 1);
            Arrays.fill(floors, minY - 1);
            // Before 26.3, vanilla exposes cell interpolation instead of density volumes.
            // Share each cell's noise and aquifer across all sampled columns inside it.
            for (int gz = 0; gz < TILE_SIZE; gz += group) {
                for (int gx = 0; gx < TILE_SIZE; gx += group) {
                    int cellX = Math.floorDiv(originX + gx * key.step, cellWidth) * cellWidth;
                    int cellZ = Math.floorDiv(originZ + gz * key.step, cellWidth) * cellWidth;
                    var noise = new SurfaceNoise(state, cellX, cellZ, settings, fluids);
                    noise.initializeForFirstCellX();
                    noise.advanceCellX(0);
                    int remaining = group * group;
                    try {
                        int topCell = (noiseSettings.height() - TOP_SLIDE_END_DEPTH) / cellHeight - 1;
                        for (int cy = topCell; cy >= 0 && remaining > 0; --cy) {
                            noise.selectCellYZ(cy, 0);
                            for (int dy = cellHeight - 1; dy >= 0 && remaining > 0; --dy) {
                                int blockY = minY + cy * cellHeight + dy;
                                noise.updateForY(blockY, (double) dy / cellHeight);
                                for (int x = gx; x < gx + group; ++x) {
                                    int blockX = originX + x * key.step;
                                    noise.updateForX(blockX, (double) (blockX - cellX) / cellWidth);
                                    for (int z = gz; z < gz + group; ++z) {
                                        int index = z * TILE_SIZE + x;
                                        if (done[index]) continue;
                                        int blockZ = originZ + z * key.step;
                                        noise.updateForZ(blockZ, (double) (blockZ - cellZ) / cellWidth);
                                        var block = noise.sample();
                                        if (block == null) block = settings.defaultBlock();
                                        if (block.isAir()) continue;
                                        if (tops[index] < minY) {
                                            tops[index] = blockY;
                                            fluid[index] = !block.getFluidState().isEmpty();
                                        }
                                        if (block.getFluidState().isEmpty()) {
                                            floors[index] = blockY;
                                            done[index] = true;
                                            --remaining;
                                        }
                                    }
                                }
                            }
                        }
                    } finally {
                        noise.stopInterpolation();
                    }
                }
            }
            var result = new SurfaceGrid(new float[columns], new float[columns], new int[columns]);
            for (int z = 0; z < TILE_SIZE; ++z) {
                int blockZ = originZ + z * key.step;
                for (int x = 0; x < TILE_SIZE; ++x) {
                    int blockX = originX + x * key.step;
                    int index = z * TILE_SIZE + x;
                    int top = tops[index];
                    result.heights[index] = top;
                    result.floors[index] = floors[index];
                    var biome = Vanilla.BIOMES.getNoiseBiome(blockX >> 2, top >> 2, blockZ >> 2, state.sampler());
                    result.colors[index] = surfaceColor(biome, blockX, top, blockZ, fluid[index], settings.seaLevel(), key.step);
                }
            }
            return result;
        }
    }

    /** Exposes vanilla's protected material sampler without a mixin or a world chunk. */
    private static final class SurfaceNoise extends NoiseChunk {
        private static final DensityFunctions.BeardifierOrMarker NO_STRUCTURES = new DensityFunctions.BeardifierOrMarker() {
            @Override public double compute(net.minecraft.world.level.levelgen.DensityFunction.FunctionContext context) { return 0; }
            @Override public double minValue() { return 0; }
            @Override public double maxValue() { return 0; }
        };
        private SurfaceNoise(RandomState state, int x, int z, NoiseGeneratorSettings settings,
                             Aquifer.FluidPicker fluids) {
            super(1, state, x, z, settings.noiseSettings(), NO_STRUCTURES,
                    settings, fluids, Blender.empty());
        }

        private BlockState sample() { return getInterpolatedState(); }
    }

    /** Surface colour of one sample; see {@link BiomeSurfacePalette}. */
    static int surfaceColor(Holder<Biome> biome, int x, int y, int z, boolean fluid, int seaLevel, int step) {
        return BiomeSurfacePalette.surfaceColor(biome, x, y, z, fluid, seaLevel, step);
    }

    private static final class Vanilla {
        private static final HolderLookup.Provider LOOKUP = VanillaRegistries.createLookup();
        private static final MultiNoiseBiomeSource BIOMES = MultiNoiseBiomeSource.createFromPreset(
                LOOKUP.lookupOrThrow(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST)
                        .getOrThrow(MultiNoiseBiomeSourceParameterLists.OVERWORLD));
    }
}
