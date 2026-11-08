# Native prediction engine

The bundled Seed Atlas engine predicts vanilla biomes and structures for the Minecraft version targeted by this branch. Java accesses it through the primitive C ABI `seedatlas_sax.h`; incompatible native libraries are rejected during loading. The engine and that ABI come from the Seed Atlas repository (`seedatlas-engine/`, `seedatlas-sax/`, at the commit in `CMakeLists.txt`), which the Horizon mod builds as well, so both compute exactly what the Seed Atlas app does. `SEEDATLAS_SOURCE_DIR` points the build at a local checkout instead.

Predictions support Normal and Large Biomes worlds. Datapacks, custom dimensions, terrain changes and final structure placement can differ from the prediction. Approximate structures are identified in the map UI.

See [ENGINE_LICENSE](ENGINE_LICENSE) and the repository's [third-party notices](../../../THIRD_PARTY_NOTICES.md) for licensing and attribution.
