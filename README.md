# Seed Atlas for Xaero's World Map

[![Watch the Seed Atlas showcase on YouTube](https://i.ytimg.com/vi/85zehFZezjs/maxresdefault.jpg)](https://www.youtube.com/watch?v=85zehFZezjs)

A client-side Fabric mod that renders [Seed Atlas](https://github.com/DUzzL/Seed-Atlas)
biomes and structure markers in the fullscreen Xaero's World Map.

With Xaero's Minimap installed, the standard framebuffer minimap also shows the
seed terrain, biome highlights, and structure markers, using the same settings
and completion state as World Map. World Map is still required; Minimap is optional.
The minimap hooks are checked against Minimap 26.5.1 and disable themselves if
an installed version changes the render API. Xaero's Safe Mode renderer is not
currently supported by the seed layers.
Minimap structure icons rotate with the terrain and follow Xaero's final minimap
scaling; they do not yet have the upright, fixed-screen-size treatment of native
Minimap waypoints. Seed layers pause while a GUI screen is open; their separate
minimap texture cache also uses additional GPU memory.

In **Biome Highlights**, **View Selected** filters the list to enabled biomes.
It works together with search; switching the filter off does not change the
selected biomes.

## Requirements

- Fabric API
- Xaero's World Map (specific version depending on release)



## Commands

- `/seedatlas seed <number-or-text>` sets the seed for the current Xaero map.
- `/seedatlas clear` removes it.
- `/seedatlas status` shows the active context and generator settings.
- `/seedatlas settings` opens the settings screen.
- `/seedatlas dimension-seed <nether|end> <number-or-text>` sets a separate seed for the Nether or End.
- `/seedatlas clear <nether|end>` removes the separate seed for that dimension and returns it to the main seed.
- `/seedatlas version <version|auto>` sets the Minecraft version used for world generation; `auto` follows the running game. Use `/seedatlas version` to show the current setting.

Singleplayer seeds are detected automatically. Multiplayer seeds are stored
per Xaero map/server context. Only vanilla normal and Large Biomes world
generation are supported; custom worldgen datapacks and modded dimensions
are currently not supported.


Not affiliated with Xaero
