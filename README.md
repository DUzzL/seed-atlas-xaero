# Seed Atlas for Xaero's World Map

A client-side Fabric 26.2 extension that renders deterministic Seed Atlas
biomes and structure markers in the fullscreen Xaero's World Map.

## Requirements

- Fabric API
- Xaero's World Map (specific version)


The server does not need this mod. On multiplayer servers the world seed stays
on the client and is never sent to the server.

## Commands

- `/seedatlas seed <number-or-text>` sets the seed for the current Xaero map.
- `/seedatlas clear` removes it.
- `/seedatlas status` shows the active context and generator settings.
- `/seedatlas settings` opens the settings screen.

Singleplayer seeds are detected automatically. Multiplayer seeds are stored
per Xaero map/server context. Only vanilla normal and Large Biomes world
generation are supported; custom worldgen datapacks and modded dimensions
cannot be reconstructed from the seed alone.


Not affiliated with Xaero
