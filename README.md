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

## Accuracy

Predictions support vanilla Normal and Large Biomes worlds for the Minecraft version of the installed release. Custom world generation and modded dimensions are not supported. Some structures and spawn positions are approximate; a predicted marker does not guarantee that the structure exists in the world.

[Report an issue](https://github.com/DUzzL/seed-atlas-xaero/issues) with your Minecraft version, loader, mod versions and relevant logs.

Not affiliated with Xaero. See [LICENSE](LICENSE), [third-party notices](THIRD_PARTY_NOTICES.md) and [legal notice](LEGAL_NOTICE.md).
