import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.seedatlas.xaero.nativeapi.BiomeRegion;
import org.seedatlas.xaero.nativeapi.BlockBox;
import org.seedatlas.xaero.nativeapi.StructureType;
import org.seedatlas.xaero.nativeapi.BiomeSample;
import org.seedatlas.xaero.nativeapi.BiomeTile;
import org.seedatlas.xaero.nativeapi.Dimension;
import org.seedatlas.xaero.nativeapi.SeedAtlasNative;
import org.seedatlas.xaero.nativeapi.WorldType;

/** Manual Java 25 FFM smoke/concurrency test for native ABI 5. */
public final class BiomeAreaFfmTest {
    public static void main(String[] args) throws Exception {
        assert SeedAtlasNative.status().available();
        assert SeedAtlasNative.engineVersion().contains("ABI 5");
        assert SeedAtlasNative.engineVersion().contains("MC 26.2");
        try (SeedAtlasNative atlas = SeedAtlasNative.open(8371904829L, WorldType.NORMAL)) {
            var plains = atlas.biomeAt(Dimension.OVERWORLD, -1184, 100, 1248);
            assert plains.id() == 1;
            assert plains.name().equals("minecraft:plains");
            assert atlas.biomeName(188).equals("minecraft:unknown_188");
            assert atlas.biomeColor(188) == 0;
            assert atlas.biomeName(187).equals("minecraft:sulfur_caves");
            for (int id = 188; id < 256; id++) {
                assert atlas.biomeName(id).contains("unknown_") : "Newer biome in 26.2 picker: " + id;
            }
            BiomeRegion exact = atlas.biomeArea(
                Dimension.OVERWORLD, -517, 39, 255, 1, 96, 80);
            for (int z = 0; z < 80; z += 7) {
                for (int x = 0; x < 96; x += 11) {
                    BiomeSample point = atlas.biomeAt(
                        Dimension.OVERWORLD, -517 + x, 255, 39 + z);
                    assert exact.biomeIdAtSample(x, z) == point.id();
                    assert exact.biomeIdAtBlock(-517 + x, 39 + z) == point.id();
                    assert exact.argbAtSample(x, z) == point.argb();
                }
            }

            BiomeRegion sampled = atlas.biomeArea(
                Dimension.OVERWORLD, -517, 39, 255, 2, 48, 40);
            for (int z = 0; z < 40; z += 5) {
                for (int x = 0; x < 48; x += 7) {
                    BiomeSample point = atlas.biomeAt(
                        Dimension.OVERWORLD, -517 + x * 2 + 1,
                        255, 39 + z * 2 + 1);
                    assert sampled.biomeIdAtSample(x, z) == point.id();
                }
            }

            for (Dimension dimension : Dimension.values()) {
                int y = switch (dimension) {
                    case OVERWORLD -> 255;
                    case NETHER -> 64;
                    case END -> 128;
                };
                for (int step = 4; step <= 1024; step *= 2) {
                    BiomeRegion region = atlas.biomeArea(
                        dimension, -4096, 2048, y, step, 32, 24);
                    assert region.sampleStep() == step;
                    assert region.sampleWidth() == 32;
                    assert region.sampleHeight() == 24;
                    assert region.argbAtSample(31, 23) != 0;
                }
            }

            BiomeRegion chunkRegion = atlas.biomeArea(
                Dimension.OVERWORLD, -512, 256, 255, 4, 256, 256);
            BiomeTile tile = chunkRegion.biomeTile(-32, 16);
            int[] copied = new int[BiomeTile.SAMPLE_COUNT];
            chunkRegion.copyChunkArgb(-32, 16, copied);
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    assert copied[z * 16 + x] == tile.argb(x, z);
                }
            }
            int[] bulk = new int[chunkRegion.sampleWidth()
                * chunkRegion.sampleHeight() + 3];
            chunkRegion.copyArgbSamplesTo(bulk, 3);
            assert bulk[3] == chunkRegion.argbAtSample(0, 0);
            short[] compactIds = new short[chunkRegion.sampleWidth()
                * chunkRegion.sampleHeight()];
            chunkRegion.copyBiomeIdsToUnsignedShorts(compactIds, 0);
            assert Short.toUnsignedInt(compactIds[0]) ==
                chunkRegion.biomeIdAtSample(0, 0);

            boolean rejected = false;
            try {
                atlas.biomeArea(Dimension.OVERWORLD, 1, 0, 255, 4, 16, 16);
            } catch (IllegalArgumentException expected) {
                rejected = true;
            }
            assert rejected;

            try (ExecutorService executor = Executors.newFixedThreadPool(8)) {
                List<Callable<Integer>> calls = new ArrayList<>();
                for (int index = 0; index < 32; index++) {
                    int tileIndex = index;
                    calls.add(() -> atlas.biomeArea(Dimension.OVERWORLD,
                        -65536 + tileIndex * 256, -65536,
                        255, 1, 256, 256).argbAtSample(0, 0));
                }
                for (var result : executor.invokeAll(calls)) {
                    assert result.get() != 0;
                }
            }
        }
        System.out.println("seedatlas_xaero Java FFM ABI 5 tests passed");
    }
}
