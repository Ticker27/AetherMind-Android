#!/usr/bin/env sh
set -eu

ROOT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
BUILD_DIR="$ROOT_DIR/build"

find "$ROOT_DIR" \( -name build -type d -o -name '.ninja*' -o -name 'build.ninja' -o -name 'CMakeCache.txt' -o -name 'CMakeFiles' -type d \) -prune -exec rm -rf {} +
find "$ROOT_DIR" -type f -exec touch {} +

cmake -S "$ROOT_DIR" -B "$BUILD_DIR" -G "Unix Makefiles" -DCMAKE_BUILD_TYPE=Release
cmake --build "$BUILD_DIR" --parallel
"$BUILD_DIR/aether_console"
