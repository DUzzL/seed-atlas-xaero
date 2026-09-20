# Seed Atlas for Xaero's World Map

![Replace this with a description](https://cdn.modrinth.com/data/cached_images/2253717c2e6ea430744b8fd6078c749cc40fe906.png)

A client-side Fabric mod that renders [Seed Atlas](https://github.com/DUzzL/Seed-Atlas)
biomes and structure markers in the fullscreen Xaero's World Map.

## Requirements

- Minecraft **26.2**, Fabric Loader, Java 25
- Fabric API for 26.2
- Xaero's World Map **1.46.1 for Fabric 26.2**

This backport uses Seed Atlas engine commit
`30f024b724aca816b7ccd4ada640ccb22125ff60` with `MC_26_2` generation.
26.3-only Abandoned Camps and Dappled Forest are excluded from generation
and from the structure/biome selectors.



## Commands

- `/seedatlas seed <number-or-text>` sets the seed for the current Xaero map.
- `/seedatlas clear` removes it.
- `/seedatlas status` shows the active context and generator settings.
- `/seedatlas settings` opens the settings screen.

Singleplayer seeds are detected automatically. Multiplayer seeds are stored
per Xaero map/server context. Only vanilla normal and Large Biomes world
generation are supported; custom worldgen datapacks and modded dimensions
are currently not supported.


Not affiliated with Xaero
