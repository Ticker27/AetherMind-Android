#!/data/data/com.termux/files/usr/bin/bash
set -e

mkdir -p build
cd build

cmake -G Ninja ..
ninja

echo
echo "Build complete."
echo "Run with:"
echo "./build/aether_console"
