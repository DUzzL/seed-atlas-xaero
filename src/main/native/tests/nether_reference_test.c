/* Include the implementation to check internal densities without adding a test ABI. */
#include "../seedatlas_xaero.c"
#include <assert.h>
#include <stdio.h>
#include "nether_vanilla_26_3.h"

int main(void)
{
    double max_base_error = 0.0, max_density_error = 0.0;
    const int layers[] = {64, 100, 127};
    const size_t count = sizeof(nether_vanilla) / sizeof(nether_vanilla[0]);
    for (size_t i = 0; i < count; ++i) {
        sax_context *context = sax_create(nether_vanilla[i].seed, SAX_WORLD_NORMAL);
        int x = nether_vanilla[i].x, z = nether_vanilla[i].z;
        int cx = floor_div(x, 4), cz = floor_div(z, 4);
        double columns[4][17], col[17];
        double tx = (x - cx * 4) / 4.0, tz = (z - cz * 4) / 4.0;
        assert(context);
        for (int corner = 0; corner < 4; ++corner)
            nether_column(&context->nether_noise, cx + (corner & 1), cz + (corner >> 1), 16, columns[corner]);
        for (int j = 0; j <= 16; ++j) {
            double base = sampleSurfaceNoise(&context->nether_noise, cx, j, cz) / 128.0;
            double error = fabs(base - nether_vanilla[i].base[j]);
            if (error > max_base_error) max_base_error = error;
            assert(error < 2e-6); /* Vanilla 26.3 Perlin/FBM evaluation uses floats. */
            double top = columns[0][j] + (columns[1][j] - columns[0][j]) * tx;
            double bottom = columns[2][j] + (columns[3][j] - columns[2][j]) * tx;
            col[j] = top + (bottom - top) * tz;
        }
        for (int j = 0; j <= 16; ++j) {
            int y = j == 16 ? 127 : j * 8;
            double value = col[y >> 3] + (col[(y >> 3) + 1] - col[y >> 3]) * ((y & 7) / 8.0);
            double clamped = fmax(-1.0, fmin(1.0, value));
            double squeezed = clamped / 2.0 - clamped * clamped * clamped / 24.0;
            double error = fabs(squeezed - nether_vanilla[i].density[j]);
            if (error > max_density_error) max_density_error = error;
            assert(error < 2e-6);
        }
        const int expected[] = {nether_vanilla[i].floor64, nether_vanilla[i].floor100, nether_vanilla[i].floor127};
        for (int layer = 0; layer < 3; ++layer) {
            float height;
            assert(sax_nether_floor_area_sampled(context, x, z, layers[layer], 1, 1, 1, &height, 1) == SAX_OK);
            assert(height == expected[layer]);
        }
        sax_destroy(context);
    }
    printf("Vanilla 26.3: %zu base values, %zu final densities, %zu cave floors; max errors %.9g / %.9g\n",
           count * 17, count * 17, count * 3, max_base_error, max_density_error);
    sax_context *reported = sax_create(INT64_C(2842062689604764280), SAX_WORLD_NORMAL);
    float raw, carved_height;
    /* Actual vanilla carver opens Y102/103 here; the exposed floor is 101, not 62. */
    uint64_t mask[2] = {0, (UINT64_C(1) << (102 - 64)) | (UINT64_C(1) << (103 - 64))};
    assert(sax_nether_floor_area_sampled(reported, -200, 14, 127, 1, 1, 1, &raw, 1) == SAX_OK);
    assert(raw == 62);
    assert(sax_nether_floor_area_carved_sampled(reported, -200, 14, 127, 1, 1, 1,
        mask, 2, &carved_height, 1) == SAX_OK);
    assert(carved_height == 101);
    assert(sax_nether_floor_area_carved_sampled(reported, -200, 14, 127, 1, 1, 1,
        mask, 1, &carved_height, 1) == SAX_ERR_ARGUMENT);
    assert(sax_nether_floor_area_carved_sampled(reported, -200, 14, 127, 1, 1, 1,
        NULL, 2, &carved_height, 1) == SAX_ERR_ARGUMENT);
    mask[1] = 0;
    assert(sax_nether_floor_area_carved_sampled(reported, -200, 14, 127, 1, 1, 1,
        mask, 2, &carved_height, 1) == SAX_OK);
    assert(carved_height == raw);
    sax_destroy(reported);
    puts("Nether carved-air floor and mask bounds tests passed");
    return 0;
}
