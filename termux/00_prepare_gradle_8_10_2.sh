#!/data/data/com.termux/files/usr/bin/sh
set -eu
ROOT=$(pwd)
GRADLE_VERSION=8.10.2
TOOLS_DIR="$ROOT/.termux-tools"
GRADLE_DIR="$TOOLS_DIR/gradle-$GRADLE_VERSION"
GRADLE_BIN="$GRADLE_DIR/bin/gradle"
ZIP_FILE="$TOOLS_DIR/gradle-$GRADLE_VERSION-bin.zip"
URL="https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip"

echo "[AT110] prepare Gradle $GRADLE_VERSION"
mkdir -p "$TOOLS_DIR"

if [ -x "$GRADLE_BIN" ]; then
  echo "OK local Gradle exists: $GRADLE_BIN"
  "$GRADLE_BIN" --version | sed -n '1,12p'
  exit 0
fi

if ! command -v unzip >/dev/null 2>&1; then
  echo "FAIL unzip not found. Install: pkg install unzip"
  exit 1
fi

if [ ! -f "$ZIP_FILE" ]; then
  if command -v curl >/dev/null 2>&1; then
    echo "Downloading $URL"
    curl -L --fail -o "$ZIP_FILE" "$URL"
  elif command -v wget >/dev/null 2>&1; then
    echo "Downloading $URL"
    wget -O "$ZIP_FILE" "$URL"
  else
    echo "FAIL curl/wget not found. Install: pkg install curl"
    exit 1
  fi
fi

rm -rf "$GRADLE_DIR"
unzip -q "$ZIP_FILE" -d "$TOOLS_DIR"
chmod +x "$GRADLE_BIN"
"$GRADLE_BIN" --version | sed -n '1,12p'
echo "GRADLE8_READY=$GRADLE_BIN"
