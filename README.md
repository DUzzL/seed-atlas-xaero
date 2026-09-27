# Seed Atlas for Xaero's World Map

A client-side mod that renders Seed Atlas biomes and structure markers in the fullscreen Xaero's World Map.

## Minecraft 26.3 loaders

The repository now contains Fabric, Forge and NeoForge builds. The Java code, assets, mixins and native libraries in `common/` are shared. Each loader has its own client entry point and build configuration. Forge and NeoForge are standalone Gradle builds so their development plugins and Gradle versions stay isolated from Fabric Loom.

| Loader | Development version | Build from the repository root | Output |
| --- | --- | --- | --- |
| Fabric | Loader 0.19.5 / API 0.160.6+26.3 | `./gradlew build` | `build/libs/` |
| Forge | 66.0.4 | `./forge/gradlew -p forge build` | `forge/build/libs/` |
| NeoForge | 26.3.0.10-beta | `./neoforge/gradlew -p neoforge build` | `neoforge/build/libs/` |

All builds require a 64-bit JDK 25. `gradle.properties` in the repository root defines the shared Minecraft, mod and Xaero versions. The loader-specific properties define the Forge/NeoForge versions.

### Xaero version overrides

The shared `xaero_world_map_version` remains the default for all three loaders. To choose versions independently, uncomment the corresponding keys in the root `gradle.properties`:

```properties
xaero_world_map_version=1.46.4
xaero_world_map_version_fabric=1.46.4
xaero_world_map_version_forge=1.46.4
xaero_world_map_version_neoforge=1.46.4
```

Each override affects only its named loader; omit it to inherit the shared default. Forge and NeoForge also accept a local `xaero_world_map_version` in their own `gradle.properties` files. Their precedence is: local loader-specific key, local generic key, root loader-specific key, root shared default. Gradle `-P` arguments override the same key from a properties file, for example:

```sh
./gradlew build -Pxaero_world_map_version_fabric=1.46.4
./forge/gradlew -p forge build -Pxaero_world_map_version=1.46.4
./neoforge/gradlew -p neoforge build -Pxaero_world_map_version=1.46.4
```

The selected version controls both the Maven dependency and the exact Xaero requirement recorded in the mod JAR. Switching versions requires checking the shared mixins against that Xaero release; this configuration does not establish compatibility with other releases. The verified default remains 1.46.4.

ForgeGradle also uses a Java 8 bootstrap toolchain, which its Foojay resolver downloads automatically if needed. Forge's four mixins compile to Java 21 bytecode for its bundled Mixin 0.8.7; the rest of the mod still requires Java 25. Its development build removes stale nested-library metadata from a local copy of Xaero's thin Maven artifact and loads XaeroLib separately. Installed Xaero JARs are not modified.

Install **one** loader-specific Seed Atlas JAR together with **Xaero's World Map 1.46.4** and its required **XaeroLib** for the same loader and Minecraft version. Fabric additionally requires Fabric API. Xaero's mods are external dependencies and are never bundled into Seed Atlas.

The bundled C engine uses Java's Foreign Function & Memory API. To explicitly permit native access, add this JVM argument to the Minecraft launcher's Java arguments:

```text
--enable-native-access=ALL-UNNAMED
```

The development client runs include this argument. Forge and NeoForge load the mod into the dynamically created `seedatlas_xaero` module. Java 25 currently permits its native calls but can log a native-access warning: naming that module in the startup flag does not enable access for a module created later. The port targets Java 25; behavior on future Java versions has not been verified.

## Development and checks

Launch a development client with `./gradlew runClient`, `./forge/gradlew -p forge runClient`, or `./neoforge/gradlew -p neoforge runClient`.

Every `build` runs the existing native FFM, progress persistence, icon layout and biome highlight checks, plus a shared client command registration check. The GitHub workflow builds all three loaders and rebuilds native resources on Windows, Linux and both macOS architectures.

## Commands

- `/seedatlas seed <number-or-text>` sets the seed for the current Xaero map.
- `/seedatlas clear` removes it.
- `/seedatlas status` shows the active context and generator settings.
- `/seedatlas settings` opens the settings screen.

Singleplayer seeds are detected automatically. Multiplayer seeds are stored per Xaero map/server context. Only vanilla Minecraft 26.3 normal and Large Biomes generation are supported; custom worldgen datapacks and modded dimensions are not supported. Switching the mod loader does not extend the engine's world-generation coverage.

Not affiliated with Xaero.
