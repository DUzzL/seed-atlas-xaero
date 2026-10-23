# Seed Atlas for Xaero's World Map

<iframe width="560" height="315" src="https://www.youtube-nocookie.com/embed/85zehFZezjs" title="YouTube video player" frameborder="0" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share" allowfullscreen></iframe>

A client-side Fabric mod that renders [Seed Atlas](https://github.com/DUzzL/Seed-Atlas)
biomes and structure markers in the fullscreen Xaero's World Map.

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
