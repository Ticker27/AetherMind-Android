#!/data/data/com.termux/files/usr/bin/bash
set -e

./scripts/build_termux.sh
./build/aether_console

echo
echo "Core check completed."
