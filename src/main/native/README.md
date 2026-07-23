# Seed Atlas Xaero native ABI

`seedatlas_xaero.h` is the stable primitive C ABI used by the Java 25 FFM
facade. It is fixed to vanilla Java Edition 26.2. The context stores only the
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

## Accuracy boundaries

- Biomes are deterministic vanilla 26.2 biome-noise results at the requested
  X/Y/Z. A seed cannot reconstruct datapack, modded, or custom-dimension
  generation.
- A checked structure result passed `isViableStructurePos`, i.e. the engine's
  biome-level check. It does not promise that terrain, jigsaw placement,
  post-processing, or a server datapack permits the final structure. In
  particular, mansion/temple terrain and End City surface-height checks are not
  part of this small ABI.
- End Ship markers are derived from predicted End City pieces after the biome
  check, but inherit the End City terrain limitation.
- End Island markers are decorator attempts and are marked approximate.
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
