# Seed Atlas for Xaero's World Map

A client-side addon that predicts biomes and structures from a world seed and displays them in Xaero's fullscreen World Map.

## Installation

Install the Seed Atlas file for your **Minecraft version and mod loader** (Fabric, Forge or NeoForge), together with the matching [Xaero's World Map](https://modrinth.com/mod/xaeros-world-map) and its required dependencies. Fabric also requires Fabric API. See the selected release for compatible versions and requirements.

Seed Atlas is only needed on the client. Native libraries are included for Windows x64, Linux x64, macOS x64 and macOS Apple Silicon.

## Usage

Singleplayer seeds are detected automatically. On multiplayer, set a known seed with `/seedatlas seed <number-or-text>`; it is saved for the current Xaero map/server context.

- `/seedatlas settings` opens the settings.
- `/seedatlas status` shows the active seed and generation settings.
- `/seedatlas clear` removes the saved seed.

Use **SA** on the world map to toggle the layer and **…** to open settings. Settings include structure filters, biome highlighting, opacity and performance controls. Structure context menus let you mark locations as completed and create waypoints when Xaero's Minimap is installed.

**Overlay explored** draws the seed map over Xaero's explored terrain as well; it is off by default. Use **Layer opacity** to blend the two maps. Biome focus still draws above explored terrain independently of this option.

Distant views use coarser biome samples and omit fine terrain shading. With unlimited-zoom addons, Seed Atlas suspends its biome overlay beyond its safe tile/coordinate budget and resumes when you zoom back in; Xaero's own zoom remains unrestricted.

## Accuracy

Predictions support vanilla Normal and Large Biomes worlds for the Minecraft version of the installed release. Custom world generation and modded dimensions are not supported. Some structures and spawn positions are approximate; a predicted marker does not guarantee that the structure exists in the world. Detailed Overworld terrain (up to 4 blocks per sample) uses vanilla base-terrain density and aquifers for ground height and water placement, with surface colours inferred from biome and temperature. This does not run the full surface-rule, carver or decoration pipeline, so trees, structures and some surface materials still differ from explored terrain. Grass, leaves and water use each biome's vanilla tint, as on Xaero's explored map, so biomes keep their own colours; tree crowns in forest biomes are drawn from block coordinates and do not predict actual trees. Overworld tiles first display a fast preview using the same surface palette, then refine visible close-up tiles in place. Generated samples are cached, but first-time detail can take seconds; coarser views and unavailable generators retain approximate native heights. In the End, island heights come from the generator density at zoom levels up to 8 blocks per pixel. The Nether predicts cave floors and lava seas from terrain density and seed-derived vanilla cave-carver masks at detailed zoom levels (up to 4 blocks per sample) when supported by the native library, not decorative blocks or structures. Carver geometry is calculated without loading or generating world chunks; tiny upper caves can expose floors that a density-only scan misses. Xaero's Full cave mode scans below the Nether roof rather than the configured Nether biome Y; other modes retain the configured Y. Predictions are not a block-for-block replacement for explored terrain.

[Report an issue](https://github.com/DUzzL/seed-atlas-xaero/issues) with your Minecraft version, loader, mod versions and relevant logs.

Not affiliated with Xaero. See [LICENSE](LICENSE), [third-party notices](THIRD_PARTY_NOTICES.md) and [legal notice](LEGAL_NOTICE.md).
