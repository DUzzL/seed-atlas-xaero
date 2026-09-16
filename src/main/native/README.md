# Seed Atlas Xaero native ABI

`seedatlas_xaero.h` is the stable primitive C ABI used by the Java 25 FFM
facade. It is fixed to vanilla Java Edition 26.3. The context stores only the
seed, Normal/Large Biomes flag, and immutable pre-seeded dimension generators,
so independent calls may run concurrently without repeating generator setup.

ABI 2 added reduced-density biome tiles through
`sax_biome_chunk_sampled`. Sampling steps 2, 4, 8, and 16 calculate only one
biome per corresponding square and expand it to the fixed 16x16 output plane;
step 1 keeps the optimized full-resolution bulk generator.

ABI 3 adds `sax_biome_area_sampled`, the fast map-rendering path. It returns a
compact sample matrix for a general block-aligned area in one FFM/native call.
Steps 1/2 use block-level generation; powers of two from 4 through 1024 use the
same scaled `Range`/`genBiomes` levels as Seed Atlas desktop. A 256x256 texture
therefore always costs 65,536 entries regardless of the represented world
area, and callers can keep a coarser parent tile visible while finer work runs.

ABI 4 selects Minecraft 26.3 for all generators, adds Dappled Forest (188)
and Abandoned Camp (25), and prevents old 26.2 native binaries from loading.
The primitive function signatures and six-int result stride remain unchanged.
For checked camps, `detail & 0xff` is the biome and `detail & 0x100` indicates
special loot. Unchecked camps have detail -1 and no predicted variant.
Generation comes from Seed Atlas commit
`fc2c628693bc72130d7c16a21636f50d57553ddf`, downloaded and SHA-256 verified
by CMake unless `SEEDATLAS_ENGINE_DIR` is explicitly set.

## Accuracy boundaries

- Biomes are deterministic vanilla 26.3 biome-noise results at the requested
  X/Y/Z. A seed cannot reconstruct datapack, modded, or custom-dimension
  generation.
- A checked structure result passed `isViableStructurePos`, i.e. the engine's
  biome-level check. It does not promise that terrain, jigsaw placement,
  post-processing, or a server datapack permits the final structure. Desert
  pyramids, jungle temples, and mansions have additional Overworld surface
  checks that the engine cannot reproduce exactly; their markers are retained
  but marked approximate. The available depth-noise heuristic is deliberately
  not used as a hard filter because it can hide real structures. Desert wells
  and amethyst geodes are also marked approximate because their final block
  and terrain gates are unavailable. End City results additionally pass the
  engine's exact End surface-height check (`isViableEndCityTerrain`).
- Abandoned Camps use Seed Atlas' rotated start-piece biome position and
  approximate projected surface height, including its negative-coordinate
  boundary corrections. Special-loot markers use `getVariant`, not template
  name guesses. Camps remain marked approximate because the engine does not
  reproduce the final vanilla surface/jigsaw/block checks.
- End Ship markers are derived from predicted End City pieces after the biome
  and surface-height checks.
- End Island markers use the placed-feature RNG, include the generated Y and
  radius, and return both islands when an attempt creates two. Checked results
  additionally require the `small_end_islands` biome; unchecked results remain
  raw decorator attempts.
- Ore-vein markers reproduce Seed Atlas' coarse projection: at most one sampled
  vein body per 128x128 marker tile. They do not enumerate ore blocks and do
  not account for terrain exposure.
- `ESTIMATED` spawn uses the climate estimate. `DETAILED_BIOME_SEARCH` is slow
  and still cannot validate the grass blocks used by final Vanilla placement.
- Strongholds with biome checking use the engine's biome relocation and refer
  to the start chunk, not an exposed portal-room coordinate. Without the check,
  their locations are ring estimates.
- Fixed End Gateway markers describe the 20 dragon-fight gateway positions in
  unlock order; a gateway does not exist until its corresponding fight has
  completed. Scattered outer gateways are also returned.
- Slime chunks are seed-exact for vanilla and are independent of biome.

A single scan is capped at 4,194,304 region/chunk cells. Callers should split
large view bounds into map tiles and honor the Java `ScanResult.truncated()`
flag. The Java facade additionally caps a result buffer at 100,000 entries.

Java 25 treats linker access as restricted. Launchers should pass
`--enable-native-access=ALL-UNNAMED`; current Java 25 otherwise emits a warning,
and later Java releases may deny the call by default.
