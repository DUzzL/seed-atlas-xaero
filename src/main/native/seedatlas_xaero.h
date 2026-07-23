#ifndef SEEDATLAS_XAERO_H
#define SEEDATLAS_XAERO_H

#include <stdint.h>

#if defined(_WIN32)
#  if defined(SEEDATLAS_XAERO_BUILD)
#    define SAX_API __declspec(dllexport)
#  else
#    define SAX_API __declspec(dllimport)
#  endif
#elif defined(__GNUC__) || defined(__clang__)
#  define SAX_API __attribute__((visibility("default")))
#else
#  define SAX_API
#endif

#ifdef __cplusplus
extern "C" {
#endif

/*
 * Stable, allocation-free (apart from the opaque context) C ABI used by the
 * Java FFM binding. Keep primitive widths and output array layouts stable.
 */
enum sax_error {
    SAX_OK = 0,
    SAX_ERR_ARGUMENT = -1,
    SAX_ERR_ALLOCATION = -2,
    SAX_ERR_UNSUPPORTED = -3,
    SAX_ERR_RANGE_TOO_LARGE = -4,
    SAX_ERR_GENERATION = -5
};

enum sax_capability {
    SAX_CAP_BIOMES = 1u << 0,
    SAX_CAP_STRUCTURES = 1u << 1,
    SAX_CAP_STRONGHOLDS = 1u << 2,
    SAX_CAP_SPAWN = 1u << 3,
    SAX_CAP_SLIME = 1u << 4,
    SAX_CAP_ORE_VEINS = 1u << 5,
    SAX_CAP_END_SHIPS = 1u << 6,
    SAX_CAP_BIOME_SAMPLING = 1u << 7,
    SAX_CAP_BIOME_AREAS = 1u << 8
};

enum sax_dimension {
    SAX_DIM_NETHER = -1,
    SAX_DIM_OVERWORLD = 0,
    SAX_DIM_END = 1
};

enum sax_world_flags {
    SAX_WORLD_NORMAL = 0,
    SAX_WORLD_LARGE_BIOMES = 1u << 0
};

/* Engine StructureType values are retained verbatim for 1..24. */
enum sax_structure_type {
    SAX_DESERT_PYRAMID = 1,
    SAX_JUNGLE_TEMPLE = 2,
    SAX_SWAMP_HUT = 3,
    SAX_IGLOO = 4,
    SAX_VILLAGE = 5,
    SAX_OCEAN_RUIN = 6,
    SAX_SHIPWRECK = 7,
    SAX_MONUMENT = 8,
    SAX_MANSION = 9,
    SAX_OUTPOST = 10,
    SAX_RUINED_PORTAL = 11,
    SAX_RUINED_PORTAL_NETHER = 12,
    SAX_ANCIENT_CITY = 13,
    SAX_BURIED_TREASURE = 14,
    SAX_MINESHAFT = 15,
    SAX_DESERT_WELL = 16,
    SAX_AMETHYST_GEODE = 17,
    SAX_NETHER_FORTRESS = 18,
    SAX_BASTION = 19,
    SAX_END_CITY = 20,
    SAX_END_GATEWAY = 21,
    SAX_END_ISLAND = 22,
    SAX_TRAIL_RUINS = 23,
    SAX_TRIAL_CHAMBERS = 24,

    /* Wrapper-only marker types. */
    SAX_STRONGHOLD = 1001,
    SAX_SPAWN = 1002,
    SAX_SLIME_CHUNK = 1003,
    SAX_ORE_VEIN_COPPER = 1004,
    SAX_ORE_VEIN_IRON = 1005,
    SAX_END_SHIP = 1006,
    SAX_ORE_VEIN = 1007
};

enum sax_result_flags {
    SAX_RESULT_BIOME_CHECKED = 1u << 0,
    SAX_RESULT_BIOME_VIABLE = 1u << 1,
    SAX_RESULT_APPROXIMATE = 1u << 2,
    SAX_RESULT_FIXED = 1u << 3
};

enum {
    SAX_ABI_VERSION = 3,
    SAX_CHUNK_SAMPLE_COUNT = 16 * 16,
    SAX_MAX_BIOME_AREA_SAMPLES = 4 * 1024 * 1024,
    SAX_RESULT_STRIDE = 6,
    SAX_UNKNOWN_Y = INT32_MIN
};

typedef struct sax_context sax_context;

SAX_API int32_t sax_abi_version(void);
SAX_API uint32_t sax_capabilities(void);

/* Copies a NUL-terminated version into out. Returns required bytes incl. NUL. */
SAX_API int32_t sax_engine_version(char *out, int32_t capacity);

SAX_API sax_context *sax_create(int64_t seed, uint32_t world_flags);
SAX_API void sax_destroy(sax_context *context);

/* Returns SAX_OK and writes a biome id, or a negative sax_error. */
SAX_API int32_t sax_biome_at(const sax_context *context, int32_t dimension,
                            int32_t x, int32_t y, int32_t z,
                            int32_t *out_biome_id);

/*
 * Generates the 16x16 block plane belonging to (chunk_x, chunk_z).
 * Output is indexed as local_z * 16 + local_x. Either output may be NULL.
 * Colors are opaque ARGB (0xFFRRGGBB).
 */
SAX_API int32_t sax_biome_chunk(const sax_context *context, int32_t dimension,
                               int32_t chunk_x, int32_t chunk_z, int32_t y,
                               int32_t out_ids[SAX_CHUNK_SAMPLE_COUNT],
                               uint32_t out_argb[SAX_CHUNK_SAMPLE_COUNT]);

/*
 * Generates the same 16x16 output plane with reduced sampling density.
 * sample_step must be one of 1, 2, 4, 8, or 16. At step 1 this uses the
 * normal bulk generator. Larger steps sample the center block of each
 * step-sized cell and fill that complete cell with the sampled biome.
 */
SAX_API int32_t sax_biome_chunk_sampled(
    const sax_context *context, int32_t dimension,
    int32_t chunk_x, int32_t chunk_z, int32_t y, int32_t sample_step,
    int32_t out_ids[SAX_CHUNK_SAMPLE_COUNT],
    uint32_t out_argb[SAX_CHUNK_SAMPLE_COUNT]);

/*
 * Generates one compact, regularly sampled biome area for map rendering.
 * The block origin is the north-west corner of sample (0, 0). The output
 * contains sample_width * sample_height entries in row-major order and is
 * intentionally not expanded to block resolution. Each entry represents a
 * sample_step by sample_step block cell.
 *
 * sample_step must be a power of two in [1, 1024]. Origins for steps >= 4
 * must be aligned to sample_step, matching Seed Atlas' native map levels.
 * Steps 1 and 2 retain block-resolution/Voronoi semantics; steps >= 4 use
 * cubiomes' scaled map generator, like the Seed Atlas desktop renderer.
 * Either output may be NULL, but not both. output_capacity is measured in
 * entries and must cover sample_width * sample_height.
 */
SAX_API int32_t sax_biome_area_sampled(
    const sax_context *context, int32_t dimension,
    int32_t origin_block_x, int32_t origin_block_z, int32_t y,
    int32_t sample_step, int32_t sample_width, int32_t sample_height,
    int32_t *out_ids, uint32_t *out_argb, int32_t output_capacity);

/* Returns required bytes including NUL; 0 means the id has no known name. */
SAX_API int32_t sax_biome_name(int32_t biome_id, char *out, int32_t capacity);
SAX_API uint32_t sax_biome_color(int32_t biome_id);

/*
 * Finds starts/markers whose X/Z coordinate is inside the inclusive block
 * bounding box. When check_biome != 0, non-viable candidates are omitted.
 *
 * out_results contains SAX_RESULT_STRIDE int32 values per result:
 *   [type, block_x, block_y_or_SAX_UNKNOWN_Y, block_z, flags, detail]
 *
 * Returns the number written, capacity + 1 when truncated, or sax_error.
 */
SAX_API int32_t sax_scan_structures(const sax_context *context,
                                    int32_t structure_type,
                                    int32_t min_x, int32_t min_z,
                                    int32_t max_x, int32_t max_z,
                                    int32_t check_biome,
                                    int32_t *out_results,
                                    int32_t capacity);

/* Exact biome-relocated locations when check_biome != 0, ring estimates else. */
SAX_API int32_t sax_scan_strongholds(const sax_context *context,
                                     int32_t min_x, int32_t min_z,
                                     int32_t max_x, int32_t max_z,
                                     int32_t check_biome,
                                     int32_t *out_results,
                                     int32_t capacity);

/* detailed != 0 runs the slower biome search (still cannot validate grass). */
SAX_API int32_t sax_spawn(const sax_context *context, int32_t detailed,
                          int32_t out_xyz[3]);

SAX_API int32_t sax_scan_slime_chunks(const sax_context *context,
                                      int32_t min_x, int32_t min_z,
                                      int32_t max_x, int32_t max_z,
                                      int32_t *out_results,
                                      int32_t capacity);

#ifdef __cplusplus
}
#endif

#endif
