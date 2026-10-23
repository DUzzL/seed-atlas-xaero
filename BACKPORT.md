# Minecraft 26.2 backport

Independent source copy of `seed-atlas-xaero` at `d85c31892aaa91ba63ec9b752658d40513520d7c`.
Git history, build caches, logs and showcase media were excluded from the source copy.

## Target versions

| Component | Version |
| --- | --- |
| Minecraft | 26.2 (exact release) |
| Java | 25 |
| Fabric Loader / Loom | 0.19.5 / 1.17.21 |
| Fabric API | 0.161.0+26.2 |
| Forge | 65.1.5 |
| NeoForge | 26.2.0.93 |
| Xaero World Map / XaeroLib | 1.47.1 / 1.8.3 |

Artifacts include `mc26.2` in their version to distinguish them from the original build.

## Adaptations

- Compact 20x20 map-settings buttons (`M` / `SA`) sit in the top-left corner, with full menu names in tooltips.

- Loader dependencies, mod metadata, artifact names and CI target this release.
- Default world generation and the selectable versions stop at 26.2; newer generation settings from a 26.3 config fall back to automatic.
- Render pipelines use this release's Blaze3D API.
- The Overworld sampler shares vanilla cell interpolation and aquifers across sampled columns; the existing cache, cancellation and concurrency behavior is retained.
- Nether cave geometry uses the older configured carvers and a recording subclass. A zero-section chunk shell supplies coordinates without loading a world or generating terrain.
- The 26.3-only Dappled Forest is excluded from the vanilla surface palette.
- Native libraries must support explicit Minecraft version selection; older single-version libraries are rejected instead of silently predicting 26.3.

## Build and validation

On Windows:

```powershell
.\gradlew.bat build
.\gradlew.bat -p forge build
.\gradlew.bat -p neoforge build
```

On Linux/macOS, use `./gradlew` instead. The mod JARs are in `build/libs`, `forge/build/libs`, and `neoforge/build/libs`.

Validation on Windows x64 / Java 25:

- Forge and NeoForge builds pass, including native FFM, biome highlights and viewport behavior, icon geometry, command registration, completion persistence, cache migration and Xaero compatibility across 130 reachable classes.
- Fabric build passes all checks, including seven Nether cave test groups and nine Overworld sampler test groups (206 columns compared against vanilla, cache bounds, cancellation and concurrent reads).
- Native C ABI test passes. The separate native Nether reference executable was blocked by Windows application control; Java Nether geometry/native integration checks pass against the actual target release.
- Short Fabric in-game smoke test completed on Windows: fresh singleplayer world and visible biome/terrain seed layer. See [INGAME_TEST.md](INGAME_TEST.md). Forge/NeoForge and Nether/End were not tested interactively.

## Native libraries

The C engine is unchanged and retains upstream Minecraft 1.1?26.3 support. Java explicitly requests the target release. Upstream 26.3 fixtures remain explicit engine regression tests, and extra assertions verify that the backport's default context excludes Dappled Forest and abandoned camps.

Windows x64 was rebuilt locally from the copied C source and the engine revision pinned in CMake (`fc2c628693bc72130d7c16a21636f50d57553ddf`). Linux x64 and both macOS architectures come from the successful [upstream build for this exact source commit](https://github.com/DUzzL/seed-atlas-xaero/actions/runs/39878341383); archive SHA-256 digests were checked before extraction. These unchanged engine binaries were tested by that upstream CI run on their respective platforms; the backported Java code was tested locally on Windows.

| Platform | Origin | Library SHA-256 |
| --- | --- | --- |
| linux-x86_64 | GitHub artifact 12469422297 | `d7e5b68e7a42d415dc281a306c69de0b04fcd2cdb39bb0d03faecd2692678527` |
| macos-aarch64 | GitHub artifact 12469551914 | `7a91e437f74952987942d10886763276522d8bb587fb5331f16a91825deb97fa` |
| macos-x86_64 | GitHub artifact 12469402554 | `e188007101e9cd42a5ad70946047f183b94c3ae5c9d4a315628f995d8582a5b6` |
| windows-x86_64 | Local CMake/GCC build | `94ef9080796ae0607b79caab1e13ce24704420b1c1bacd5f514a2b63d394cd7f` |
