#!/data/data/com.termux/files/usr/bin/sh
set -eu
echo "[AT110] local manifest"
OUT=TERMUX_LOCAL_MANIFEST.sha256
find . -type f \
  ! -path './.git/*' \
  ! -path './app/build/*' \
  ! -path './build/*' \
  ! -name "$OUT" \
  -print | sort | xargs sha256sum > "$OUT"
echo "WROTE $OUT"
