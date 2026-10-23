# In-game smoke test - Minecraft 26.1.2

Date: 2026-10-23. Windows x64, Java 25, Fabric Loader 0.19.5, Xaero World Map 1.47.1, XaeroLib 1.8.3.

- Started the client with `gradlew.bat runClient` in a separate test directory.
- Loaded a fresh singleplayer world.
- Opened Xaero's fullscreen map with the visible biome/terrain seed layer.
- No observed mod, mixin or shader failures during startup and map rendering.

This short interactive test covers Fabric and the Overworld. Forge/NeoForge and Nether/End were not tested interactively. Build and Java checks for all three loaders passed separately. Realms authentication errors are expected in the local Fabric development profile and did not prevent singleplayer testing.

## Final settings buttons

All versions (26.1.2, 26.2, 26.3) use compact 20x20 GUI-pixel buttons directly in the top-left corner: M at (0, 0), SA at (22, 0). Their tooltips display the full menu names. The bottom-left SA button still toggles the layer.

The earlier compact layout was visually inspected in 26.1.2. The final change to Y=0 was compiled for all loaders, and the Fabric Xaero compatibility checks passed on all versions. Screenshots and log snapshots from the local smoke test are in the ignored `build/ingame-smoke/` directory.
