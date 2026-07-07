#!/data/data/com.termux/files/usr/bin/sh
set -eu
echo "[AT110] clean local build/cache folders"
rm -rf app/build build .gradle .gradle-termux .kotlin .cxx app/.cxx
find . -type d \( -name '.gradle' -o -name 'build' -o -name '.cxx' \) -prune -exec rm -rf {} + 2>/dev/null || true
echo "LOCAL_BUILD_CACHE_CLEANED"
