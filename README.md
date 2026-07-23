# Seed Atlas for Xaero's World Map

A client-side Fabric 26.2 extension that renders deterministic Seed Atlas
biomes and structure markers in the fullscreen Xaero's World Map.

## Requirements

- Minecraft 26.2
- Fabric Loader 0.19.3 or newer
- Fabric API
- Xaero's World Map 1.44.x
- Java 25

The server does not need this mod. On multiplayer servers the world seed stays
on the client and is never sent to the server.

## Commands

- `/seedatlas seed <number-or-text>` sets the seed for the current Xaero map.
- `/seedatlas clear` removes it.
- `/seedatlas status` shows the active context and generator settings.
- `/seedatlas settings` opens the settings screen.

Singleplayer seeds are detected automatically. Multiplayer seeds are stored
per Xaero map/server context. Only vanilla 26.2 normal and Large Biomes world
generation are supported; custom worldgen datapacks and modded dimensions
cannot be reconstructed from the seed alone.

## Building

Native libraries for Windows x86-64, Linux x86-64, macOS x86-64 and macOS
ARM64 must be generated before the final Gradle build. The repository workflow
`.github/workflows/seedatlas-xaero.yml` builds and tests Windows with
MinGW-w64, Linux natively, and a universal macOS dylib containing both x86-64
and ARM64 slices. It verifies the macOS architectures with `lipo`, copies the
four native resources into their runtime paths, and packages them into one
Fabric jar with Java 25.

Local native build commands and the exact resource paths are documented in
[`scripts/native/README.md`](scripts/native/README.md).

For a Java-only compile check:

```text
gradlew build
```

This compile check uses the native resources already present under
`src/main/resources`; use the repository workflow for a release-ready jar.

## License

Copyright (C) DUzzL and Seed Atlas contributors.

This extension is licensed under GNU GPLv3. See the repository-level
`LICENSE`, `LEGAL_NOTICE.md`, and `THIRD_PARTY_NOTICES.md`. Xaero's World Map is
an external required dependency and is not redistributed by this project.

This is an unofficial extension and is not affiliated with Xaero, Mojang, or
Microsoft.
