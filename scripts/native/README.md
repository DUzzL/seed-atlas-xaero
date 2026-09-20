# Native Seed Atlas builds

The CMake project in `src/main/native` links the `seedatlas-engine` sources
into a C ABI library requiring only platform system libraries. CMake downloads
Seed Atlas commit `30f024b724aca816b7ccd4ada640ccb22125ff60` and verifies its
SHA-256. Set `SEEDATLAS_ENGINE_DIR` to use an explicit local engine checkout
instead. Build it with:

- Windows x64 (MinGW GCC or clang, not MSVC `cl`):
  `./scripts/native/build-native.ps1 -RunTests`
- Linux x64: `RUN_TESTS=ON bash scripts/native/build-native.sh`
- macOS x64: `RUN_TESTS=ON bash scripts/native/build-native.sh`
- macOS arm64: `RUN_TESTS=ON bash scripts/native/build-native.sh`

The scripts install the result below:

`build/generated/native-resources/natives/<platform>/`

Before packaging, copy every generated library into the matching directory
below `src/main/resources/natives/`. The repository CI compiles and tests the
native source on all four platforms and runs Java FFM regression tests against
both checked-in and freshly built libraries. The final CI jar uses the freshly
built libraries from all four runners, preventing stale platform resources.
Runtime lookup expects exactly these paths in the final jar:

- `natives/windows-x86_64/seedatlas_xaero.dll`
- `natives/linux-x86_64/seedatlas_xaero.so`
- `natives/macos-x86_64/seedatlas_xaero.dylib`
- `natives/macos-aarch64/seedatlas_xaero.dylib`

For local Java/FFM testing, an unpackaged build can be selected with the JVM
property `-Dseedatlas_xaero.native.path=/absolute/path/to/the/library`.

Run `bash ./gradlew build` with Java 25 to build the mod and run the bundled
native FFM tests, or `bash ./gradlew nativeSmokeTest` for just those tests.
The smoke test uses the actual jar's host library, including extraction and
ABI validation. The C test suite also checks 26.2 Plains instead of Dappled
Forest, rejection of camps, Large Biomes, and the existing structure regressions.

For Windows PowerShell builds select a MinGW generator/compiler, for example
`$env:CMAKE_GENERATOR = 'MinGW Makefiles'`, with MinGW GCC on PATH. The CI uses
MSYS2 MinGW64 with Ninja. Visual Studio's MSVC C compiler is unsupported.
