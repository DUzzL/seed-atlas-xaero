# Native Seed Atlas builds

The CMake project in `src/main/native` links the bundled `seedatlas-engine`
sources into a dependency-free C ABI library. Build it on a CI matrix with:

- Windows x64 (MinGW GCC or clang, not MSVC `cl`):
  `./scripts/native/build-native.ps1 -RunTests`
- Linux x64: `RUN_TESTS=ON ./scripts/native/build-native.sh`
- macOS x64: `RUN_TESTS=ON ./scripts/native/build-native.sh`
- macOS arm64: `RUN_TESTS=ON ./scripts/native/build-native.sh`

The scripts install the result below:

`build/generated/native-resources/natives/<platform>/`

Before packaging, copy the generated library into the matching directory below
`src/main/resources/natives/`. The repository CI workflow performs this staging
step automatically. Runtime lookup expects exactly these paths in the final jar:

- `natives/windows-x86_64/seedatlas_xaero.dll`
- `natives/linux-x86_64/seedatlas_xaero.so`
- `natives/macos-x86_64/seedatlas_xaero.dylib`
- `natives/macos-aarch64/seedatlas_xaero.dylib`

For local Java/FFM testing, an unpackaged build can be selected with the JVM
property `-Dseedatlas_xaero.native.path=/absolute/path/to/the/library`.
