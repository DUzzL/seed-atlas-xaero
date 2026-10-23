# Seed Atlas for Xaero's World Map ? Minecraft 26.1.2

Version **2.0.0** for Minecraft **26.1.2**. See [BACKPORT.md](BACKPORT.md) for build and validation details.

A client-side addon that predicts biomes and structures from a world seed and displays them in Xaero's fullscreen World Map.

## Installation

Release versions use `2.0.0-mc<Minecraft version>-<loader>`, for example `2.0.0-mc26.1.2-fabric`. The same version is embedded in the mod metadata and used in the JAR filename.

Install the Seed Atlas file for your **Minecraft version and mod loader** (Fabric, Forge or NeoForge), together with the matching [Xaero's World Map](https://modrinth.com/mod/xaeros-world-map) and its required dependencies. Fabric also requires Fabric API. See the selected release for compatible versions and requirements.

Newer Xaero's World Map releases work without a Seed Atlas update. If one changes something Seed Atlas relies on, Seed Atlas switches itself off and says so in chat, while Xaero's map keeps working; a Seed Atlas update then restores the layer.

Seed Atlas is only needed on the client. Native libraries are included for Windows x64, Linux x64, macOS x64 and macOS Apple Silicon.

## Usage

Singleplayer seeds are detected automatically. On multiplayer, set a known seed with `/seedatlas seed <number-or-text>`; it is saved for the current Xaero map/server context.

- `/seedatlas settings` opens the settings.
- `/seedatlas status` shows the active seed and generation settings.
- `/seedatlas dimension-seed <nether|end> <seed>` sets a separate seed for the Nether or End, for servers that generate them from another seed; `/seedatlas clear <nether|end>` returns that dimension to the main seed.
- `/seedatlas version <version|auto>` sets the Minecraft version the world or server generates with, e.g. `26.1` when you join a 26.1 server with a 26.1.2 client; `auto` follows the running game.
- `/seedatlas clear` removes the saved seeds.

Use **SA** at the bottom left of the world map to toggle the layer. In the top-left corner, the compact **M** button opens Xaero's map settings and **SA** opens Seed Atlas settings; hover over either button for its full name. Settings include structure filters, biome highlighting, opacity and performance controls. Structure context menus let you mark locations as completed and create waypoints when Xaero's Minimap is installed; **Completed** in the structure settings fades completed markers from 100 % (default) down to 0 % (hidden). A completed structure stays completed when another Minecraft version moves it by up to 12 blocks. **Reset completed** clears all completed structures of the world or server, and the button beside each structure type clears just that type.

**World** in the settings chooses the world type and the Minecraft version, per world or server. The list holds only versions whose world generation differs, from 1.1 to 26.1.2 (for example 1.21.5 – 26.1.2 is one entry). Biomes and structures follow the chosen version; the terrain look needs 1.18 or newer and uses the running game's terrain. Older versions show plain biome colours.

Most markers show a Minecraft block, item or head (a rail for mineshafts, an eye of ender for strongholds, raw iron or copper for ore veins, a villager head for villages); these textures come from the game or the active resource pack and are not part of the mod.

**Overlay explored** (off by default) also draws the seed map over explored terrain; **Layer opacity** blends the two. Biome focus is always drawn on top.

## Accuracy

Predictions cover vanilla Normal and Large Biomes worlds of Minecraft 1.1 to the version a release is built for. Custom world generation and modded dimensions are not supported. Some structures and spawn positions are approximate, so a marker does not guarantee a structure.

The terrain look shows predicted ground, not explored blocks:

- **Overworld:** close-up views (up to 4 blocks per sample) use vanilla base terrain and aquifers for ground height and water. Grass, leaves and water use each biome's vanilla tint, as on Xaero's map. Surface rules, caves, trees and structures are not generated; forest tree crowns are decorative. A quick preview appears first and is refined in place, which can take a few seconds the first time.
- **End:** island outlines, end-stone colours and shading follow the generator density, up to 8 blocks per sample. Distant views, and the moment before the heights arrive, outline the islands from the End biomes.
- **Nether:** cave floors and lava seas come from terrain density and vanilla cave carvers, up to 4 blocks per sample. Xaero's Full cave mode scans from below the roof; other modes use the configured Nether Y.

In every dimension the map first shows the biomes and adds the terrain shortly after for the visible area. Distant views use coarser samples without fine shading. With the seed layer enabled and a seed available, the map zooms out to one pixel per 512 blocks. Beyond its tile budget, the biome layer pauses and returns when you zoom back in.

[Report an issue](https://github.com/DUzzL/seed-atlas-xaero/issues) with your Minecraft version, loader, mod versions and relevant logs.

Not affiliated with Xaero. See [LICENSE](LICENSE), [third-party notices](THIRD_PARTY_NOTICES.md) and [legal notice](LEGAL_NOTICE.md).
