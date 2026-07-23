#include "seedatlas_xaero.h"

#include <assert.h>
#include <stdint.h>
#include <stdio.h>
#include <string.h>

int main(void)
{
    sax_context *context;
    int32_t biome = -1;
    int32_t ids[SAX_CHUNK_SAMPLE_COUNT];
    uint32_t colors[SAX_CHUNK_SAMPLE_COUNT];
    int32_t sampled_ids[SAX_CHUNK_SAMPLE_COUNT];
    uint32_t sampled_colors[SAX_CHUNK_SAMPLE_COUNT];
    int32_t results[64 * SAX_RESULT_STRIDE];
    int32_t position[3];
    char version[64];
    char name[64];
    int count;
    int i;
    const int32_t dimensions[] = {
        SAX_DIM_OVERWORLD, SAX_DIM_NETHER, SAX_DIM_END};
    const int32_t heights[] = {255, 64, 128};

    assert(sax_abi_version() == SAX_ABI_VERSION);
    assert((sax_capabilities() & SAX_CAP_BIOMES) != 0);
    assert((sax_capabilities() & SAX_CAP_BIOME_SAMPLING) != 0);
    assert((sax_capabilities() & SAX_CAP_BIOME_AREAS) != 0);
    assert(sax_engine_version(version, sizeof(version)) > 1);
    assert(strstr(version, "26.2") != NULL);

    context = sax_create(INT64_C(8371904829), SAX_WORLD_NORMAL);
    assert(context != NULL);
    assert(sax_biome_at(context, SAX_DIM_OVERWORLD, 0, 255, 0, &biome) == SAX_OK);
    assert(biome >= 0);
    assert(sax_biome_name(biome, name, sizeof(name)) > 1);
    assert(sax_biome_color(biome) != 0);

    assert(sax_biome_chunk(context, SAX_DIM_OVERWORLD, 0, 0, 255,
                           ids, colors) == SAX_OK);
    for (i = 0; i < SAX_CHUNK_SAMPLE_COUNT; ++i) {
        assert(ids[i] >= 0);
        assert((colors[i] & UINT32_C(0xff000000)) == UINT32_C(0xff000000));
    }

    for (int step = 1; step <= 16; step *= 2) {
        int x, z;
        assert(sax_biome_chunk_sampled(context, SAX_DIM_OVERWORLD,
                                       0, 0, 255, step,
                                       sampled_ids, sampled_colors) == SAX_OK);
        for (z = 0; z < 16; ++z) {
            for (x = 0; x < 16; ++x) {
                int sample_index = ((z / step) * step + step / 2) * 16 +
                                   (x / step) * step + step / 2;
                int index = z * 16 + x;
                assert(sampled_ids[index] == ids[sample_index]);
                assert(sampled_colors[index] == colors[sample_index]);
            }
        }
    }
    assert(sax_biome_chunk_sampled(context, SAX_DIM_OVERWORLD,
                                   0, 0, 255, 3,
                                   sampled_ids, sampled_colors) ==
           SAX_ERR_ARGUMENT);

    /* Compact areas preserve exact center sampling at block/detail levels. */
    {
        int32_t area_ids[32 * 24];
        uint32_t area_colors[32 * 24];
        int x, z;
        assert(sax_biome_area_sampled(context, SAX_DIM_OVERWORLD,
                   -117, 43, 255, 1, 32, 24, area_ids, area_colors,
                   32 * 24) == SAX_OK);
        for (z = 0; z < 24; ++z) {
            for (x = 0; x < 32; ++x) {
                int index = z * 32 + x;
                assert(sax_biome_at(context, SAX_DIM_OVERWORLD,
                           -117 + x, 255, 43 + z, &biome) == SAX_OK);
                assert(area_ids[index] == biome);
                assert(area_colors[index] == sax_biome_color(biome));
            }
        }

        assert(sax_biome_area_sampled(context, SAX_DIM_OVERWORLD,
                   -117, 43, 255, 2, 16, 12, area_ids, area_colors,
                   32 * 24) == SAX_OK);
        for (z = 0; z < 12; ++z) {
            for (x = 0; x < 16; ++x) {
                int index = z * 16 + x;
                assert(sax_biome_at(context, SAX_DIM_OVERWORLD,
                           -117 + x * 2 + 1, 255,
                           43 + z * 2 + 1, &biome) == SAX_OK);
                assert(area_ids[index] == biome);
                assert(area_colors[index] == sax_biome_color(biome));
            }
        }
    }

    /* Seed Atlas-style scaled levels stay compact across all dimensions. */
    for (i = 0; i < 3; ++i) {
        int step;
        for (step = 4; step <= 1024; step *= 2) {
            int j;
            assert(sax_biome_area_sampled(context, dimensions[i],
                       -2048, 1024, heights[i], step, 16, 8,
                       sampled_ids, sampled_colors,
                       SAX_CHUNK_SAMPLE_COUNT) == SAX_OK);
            for (j = 0; j < 16 * 8; ++j) {
                assert(sampled_ids[j] >= 0);
                assert((sampled_colors[j] & UINT32_C(0xff000000)) ==
                       UINT32_C(0xff000000));
            }
        }
    }
    assert(sax_biome_area_sampled(context, SAX_DIM_OVERWORLD,
               1, 0, 255, 4, 16, 16, sampled_ids, sampled_colors,
               SAX_CHUNK_SAMPLE_COUNT) == SAX_ERR_ARGUMENT);
    assert(sax_biome_area_sampled(context, SAX_DIM_OVERWORLD,
               0, 0, 255, 3, 16, 16, sampled_ids, sampled_colors,
               SAX_CHUNK_SAMPLE_COUNT) == SAX_ERR_ARGUMENT);
    assert(sax_biome_area_sampled(context, SAX_DIM_OVERWORLD,
               0, 0, 255, 4, 16, 16, sampled_ids, sampled_colors,
               SAX_CHUNK_SAMPLE_COUNT - 1) == SAX_ERR_ARGUMENT);
    assert(sax_biome_area_sampled(context, SAX_DIM_OVERWORLD,
               INT32_MAX - 3, 0, 255, 4, 2, 1,
               sampled_ids, sampled_colors,
               SAX_CHUNK_SAMPLE_COUNT) == SAX_ERR_RANGE_TOO_LARGE);
    assert(sax_biome_area_sampled(context, SAX_DIM_OVERWORLD,
               INT32_MIN, 0, 255, 1, 1, 1,
               sampled_ids, sampled_colors,
               SAX_CHUNK_SAMPLE_COUNT) == SAX_ERR_RANGE_TOO_LARGE);
    assert(sax_biome_area_sampled(context, SAX_DIM_OVERWORLD,
               0, 0, 255, 1, 4096, 1025,
               sampled_ids, sampled_colors,
               INT32_MAX) == SAX_ERR_RANGE_TOO_LARGE);

    /* Exercise negative coordinates and all supported dimensions. */
    for (i = 0; i < 3; ++i) {
        int step, x, z;
        assert(sax_biome_chunk(context, dimensions[i], -7, 3, heights[i],
                               ids, colors) == SAX_OK);
        for (step = 2; step <= 16; step *= 2) {
            assert(sax_biome_chunk_sampled(context, dimensions[i],
                                           -7, 3, heights[i], step,
                                           sampled_ids, sampled_colors) == SAX_OK);
            for (z = 0; z < 16; ++z) {
                for (x = 0; x < 16; ++x) {
                    int sample_index = ((z / step) * step + step / 2) * 16 +
                                       (x / step) * step + step / 2;
                    int index = z * 16 + x;
                    assert(sampled_ids[index] == ids[sample_index]);
                    assert(sampled_colors[index] == colors[sample_index]);
                    if (x % step == 0 && z % step == 0) {
                        assert(sax_biome_at(context, dimensions[i],
                                           -7 * 16 + x + step / 2,
                                           heights[i],
                                           3 * 16 + z + step / 2,
                                           &biome) == SAX_OK);
                        assert(sampled_ids[index] == biome);
                    }
                }
            }
        }
    }

    /* Known 26.2 generation attempt from seedatlas-engine tests_versions.c. */
    count = sax_scan_structures(context, SAX_RUINED_PORTAL,
                                272, 48, 272, 48, 0, results, 64);
    assert(count == 1);
    assert(results[1] == 272 && results[3] == 48);

    assert(sax_spawn(context, 0, position) == SAX_OK);
    count = sax_scan_strongholds(context, -30000, -30000, 30000, 30000,
                                 0, results, 64);
    assert(count > 0);
    count = sax_scan_slime_chunks(context, -256, -256, 255, 255,
                                  results, 64);
    assert(count > 0);

    sax_destroy(context);
    puts("seedatlas_xaero native ABI tests passed");
    return 0;
}
