# Seed Atlas for Xaero's World Map

A client-side Fabric 26.3 extension that renders deterministic Seed Atlas
biomes and structure markers in the fullscreen Xaero's World Map.

## Requirements

- Minecraft 26.3, Java 25, Fabric Loader 0.19.5 or newer
- Fabric API for 26.3 (built with 0.160.6+26.3)
- Xaero's World Map 1.46.2 for Fabric 26.3


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

## Marker zoom fix (0.4.1)

Structure icons now grow smoothly with map zoom, from their original overview
size up to three times larger. Hover highlights, tooltip hit areas, and
screen-edge visibility use the same scale. Xaero's screen-size scaling is
preserved.

## Minecraft 26.3 update (0.4.0)

World generation follows Seed Atlas commit
`fc2c628693bc72130d7c16a21636f50d57553ddf`. This adds Dappled Forest with
Seed Atlas' biome colors and Abandoned Camp markers. Camps can be toggled in
the marker settings and use the original normal/special-loot icons, with
biome and loot information in their tooltips. Existing seed profiles,
settings, commands, overlays, and dimension handling remain supported.

Camp positions use Seed Atlas' approximate projected surface height and
remain labelled approximate, like other structures with unavailable final
terrain checks. Only vanilla 26.3 generation is predicted; older chunks in
upgraded worlds can still reflect their original Minecraft version.

## Build

Use Java 25 and `bash ./gradlew build`. The build includes a Java FFM smoke
test against the native library packaged for the current host. Gradle 9.6.0,
Loom 1.17.21, and Xaero's World Map 1.46.2 are pinned in the build settings.
Native resources are bundled for Windows x64, Linux x64, macOS Intel, and
macOS Apple Silicon. See `scripts/native/README.md` to rebuild them from the
pinned engine sources. GitHub Actions rebuilds and tests all four platforms.
