package org.seedatlas.xaero.integration.biome;

import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterLists;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.densityfunction.DensityVolume;
import net.minecraft.world.level.levelgen.densityfunction.SamplerContext;

/** Seed-only vanilla base terrain; never reads a world, constructs a chunk, or adds vegetation. */
public final class OverworldTerrainSampler {
    private static final int TILE_SIZE = 16;
    private static final int MAX_DIMENSION = 262;
    private static final int MAX_CACHED_TILES = 512;
    private static final int MAX_CONTEXTS = 2;
    private static final LinkedHashMap<TileKey, SurfaceGrid> CACHE = new LinkedHashMap<>(16, 0.75f, true);
    private static final LinkedHashMap<WorldKey, Context> CONTEXTS = new LinkedHashMap<>(4, 0.75f, true);
    private static final ConcurrentHashMap<String, Holder<Biome>> PREVIEW_BIOMES = new ConcurrentHashMap<>();

    private OverworldTerrainSampler() { }

    /**
     * Row-major arrays, sampled at origin + index * step + (step - 1) / 2, including any caller halo.
     * Heights are the highest non-air base block Y (including fluid); floors are the highest solid Y.
     * These match base columns, not surface rules, carvers, structures, features, ice or snow layers.
     * Colours infer surface material from the base block, surface biome and temperature, not exact blocks.
     * Grass without a built-in biome override uses a fixed green, not the temperature/downfall colormap.
     * Minecraft must already be bootstrapped. Steps 1/2/4/8/16 and dimensions 1..262 are supported;
     * coarse steps still evaluate real vanilla density and are substantially more expensive per sample.
     */
    public static SurfaceGrid sample(long seed, boolean largeBiomes, int originX, int originZ,
                                     int step, int width, int height) {
        return sample(seed, largeBiomes, originX, originZ, step, width, height, () -> true);
    }

    /** Cancels stale work between output rows and before each density batch, including after waiting for its context. */
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
    static int previewColor(String name, int x, int y, int z, boolean water) {
        if (name == null) return -1;
        var biome = PREVIEW_BIOMES.computeIfAbsent(name, value -> Vanilla.LOOKUP.lookupOrThrow(Registries.BIOME)
                .get(ResourceKey.create(Registries.BIOME, Identifier.parse(value))).orElse(null));
        return biome == null ? -1 : surfaceColor(biome, x, y, z, water, 63);
    }

    private static SurfaceGrid tile(TileKey key, BooleanSupplier stillCurrent) {
        synchronized (CACHE) {
            var cached = CACHE.get(key);
            if (cached != null) return cached;
        }
        Context context;
        synchronized (CONTEXTS) {
            context = CONTEXTS.get(key.world);
            if (context == null) {
                context = new Context(key.world);
                if (CONTEXTS.size() == MAX_CONTEXTS) CONTEXTS.remove(CONTEXTS.keySet().iterator().next());
                CONTEXTS.put(key.world, context);
            }
        }
        // RandomState and its sampler caches are mutable; serialize generation per seed/mode, not cache hits.
        synchronized (context) {
            requireCurrent(stillCurrent);
            synchronized (CACHE) {
                var cached = CACHE.get(key);
                if (cached != null) return cached;
            }
            var generated = context.generate(key);
            synchronized (CACHE) {
                if (CACHE.size() == MAX_CACHED_TILES) CACHE.remove(CACHE.keySet().iterator().next());
                CACHE.put(key, generated);
            }
            return generated;
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
            state = RandomState.create(Vanilla.LOOKUP.lookupOrThrow(Registries.NOISE), key.seed, settings);
            var water = new Aquifer.FluidStatus(settings.seaLevel(), settings.defaultFluid());
            var lava = new Aquifer.FluidStatus(-54, Blocks.LAVA.defaultBlockState());
            // Same built-in Overworld global picker as NoiseBasedChunkGenerator (without debug fluid disabling).
            fluids = (x, y, z) -> y < Math.min(-54, settings.seaLevel()) ? lava : water;
        }

        private SurfaceGrid generate(TileKey key) {
            var noiseSettings = settings.noiseSettings();
            var volume = new DensityVolume(TILE_SIZE, noiseSettings.height(), TILE_SIZE,
                    key.x * TILE_SIZE * key.step + key.phaseX, noiseSettings.minY(),
                    key.z * TILE_SIZE * key.step + key.phaseZ, key.step, 1, key.step);
            var result = new SurfaceGrid(new float[TILE_SIZE * TILE_SIZE], new float[TILE_SIZE * TILE_SIZE],
                    new int[TILE_SIZE * TILE_SIZE]);
            try (var noise = new NoiseChunk(state, null, settings, fluids, Blender.empty(), volume);
                 var density = noise.cachingSamplers().get(settings.noiseRouter().finalDensity()).sampleVolume(volume)) {
                var resolver = Vanilla.BIOMES.createResolver(state.createClimateSampler(SamplerContext.EMPTY_UNCACHED));
                for (int z = 0; z < TILE_SIZE; ++z) {
                    int blockZ = volume.blockZ(z);
                    for (int x = 0; x < TILE_SIZE; ++x) {
                        int blockX = volume.blockX(x);
                        int index = z * TILE_SIZE + x;
                        int top = noiseSettings.minY() - 1;
                        int floor = top;
                        boolean fluid = false;
                        for (int y = volume.sizeY() - 1; y >= 0; --y) {
                            int blockY = volume.blockY(y);
                            var block = noise.aquifer().computeSubstance(blockX, blockY, blockZ,
                                    density.get(volume.indexUnchecked(x, y, z)));
                            if (block == null) block = settings.defaultBlock();
                            if (block.isAir()) continue;
                            if (top < noiseSettings.minY()) {
                                top = blockY;
                                fluid = !block.getFluidState().isEmpty();
                            }
                            if (block.getFluidState().isEmpty()) {
                                floor = blockY;
                                break;
                            }
                        }
                        result.heights[index] = top;
                        result.floors[index] = floor;
                        var biome = resolver.getNoiseBiome(blockX >> 2, top >> 2, blockZ >> 2);
                        result.colors[index] = surfaceColor(biome, blockX, top, blockZ, fluid, settings.seaLevel());
                    }
                }
            }
            return result;
        }
    }

    static int surfaceColor(Holder<Biome> biome, int x, int y, int z, boolean fluid, int seaLevel) {
        if (fluid) return biome.value().getWaterColor() & 0xffffff;
        // shortcut: infer top material without chunk-dependent material rules; upgrade only with a chunk-free rule API.
        if (biome.value().coldEnoughToSnow(new BlockPos(x, y, z), seaLevel)) return 0xe8eff0;
        if (biome.is(Biomes.DESERT) || biome.is(Biomes.BEACH)) return 0xd8cf9b;
        if (biome.is(Biomes.STONY_SHORE) || biome.is(Biomes.STONY_PEAKS)) return 0x858585;
        if (biome.is(Biomes.BADLANDS) || biome.is(Biomes.ERODED_BADLANDS)
                || biome.is(Biomes.WOODED_BADLANDS)) return 0xb06b48;
        if (biome.is(Biomes.MUSHROOM_FIELDS)) return 0x8b7f90;
        if (biome.is(Biomes.MANGROVE_SWAMP)) return 0x514b40;
        var effects = biome.value().getSpecialEffects();
        // Use immutable biome tints, not the client/resource-pack grass colormap, which may not be loaded on workers.
        int grass = effects.grassColorOverride().orElse(0x91bd59);
        return effects.grassColorModifier().modifyColor(x, z, grass) & 0xffffff;
    }

    private static final class Vanilla {
        private static final HolderLookup.Provider LOOKUP = VanillaRegistries.createWorldLookup();
        private static final MultiNoiseBiomeSource BIOMES = MultiNoiseBiomeSource.createFromPreset(
                LOOKUP.lookupOrThrow(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST)
                        .getOrThrow(MultiNoiseBiomeSourceParameterLists.OVERWORLD));
    }
}
