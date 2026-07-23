#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
project_dir="$(cd -- "$script_dir/../.." && pwd)"
native_source="$project_dir/src/main/native"

configuration="${CONFIGURATION:-Release}"
run_tests="${RUN_TESTS:-ON}"
platform="${1:-}"

if [[ -z "$platform" ]]; then
    os="$(uname -s)"
    architecture="$(uname -m)"
    case "$os/$architecture" in
        Linux/x86_64) platform="linux-x86_64" ;;
        Darwin/x86_64) platform="macos-x86_64" ;;
        Darwin/arm64|Darwin/aarch64) platform="macos-aarch64" ;;
        MINGW*/*|MSYS*/*|CYGWIN*/*) platform="windows-x86_64" ;;
        *) echo "Unsupported native host: $os / $architecture" >&2; exit 2 ;;
    esac
fi

case "$platform" in
    windows-x86_64|linux-x86_64|macos-x86_64|macos-aarch64) ;;
    *) echo "Unsupported resource platform: $platform" >&2; exit 2 ;;
esac

build_dir="$project_dir/build/native/$platform"
resource_root="$project_dir/build/generated/native-resources/natives/$platform"

cmake -S "$native_source" -B "$build_dir" \
    -DCMAKE_BUILD_TYPE="$configuration" \
    -DCMAKE_INSTALL_PREFIX="$resource_root" \
    -DBUILD_TESTING="$run_tests"
cmake --build "$build_dir" --config "$configuration" --parallel

if [[ "$run_tests" == "ON" ]]; then
    ctest --test-dir "$build_dir" -C "$configuration" --output-on-failure
fi

cmake --install "$build_dir" --config "$configuration"
echo "Native resource generated at $resource_root"
