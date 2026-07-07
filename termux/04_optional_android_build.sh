#!/data/data/com.termux/files/usr/bin/sh
set -eu
ROOT=$(pwd)
echo "[AT110] optional Android build"

if [ -z "${ANDROID_HOME:-}" ]; then
  echo "SKIP ANDROID_HOME not set"
  exit 0
fi

if [ ! -d "$ANDROID_HOME" ]; then
  echo "SKIP ANDROID_HOME path not found: $ANDROID_HOME"
  exit 0
fi

GRADLE_VERSION=8.10.2
LOCAL_GRADLE="$ROOT/.termux-tools/gradle-$GRADLE_VERSION/bin/gradle"
GRADLE_CMD=""

if [ -x "$LOCAL_GRADLE" ]; then
  GRADLE_CMD="$LOCAL_GRADLE"
elif command -v gradle >/dev/null 2>&1; then
  SYS_VER=$(gradle --version 2>/dev/null | awk '/Gradle / {print $2; exit}')
  SYS_MAJOR=$(printf '%s' "$SYS_VER" | cut -d. -f1)
  if [ "${SYS_MAJOR:-0}" -ge 9 ] 2>/dev/null; then
    echo "WARN system Gradle $SYS_VER detected. Android Gradle Plugin 8.6.1 should be built with Gradle 8.x."
    sh termux/00_prepare_gradle_8_10_2.sh
    GRADLE_CMD="$LOCAL_GRADLE"
  else
    GRADLE_CMD="gradle"
  fi
else
  sh termux/00_prepare_gradle_8_10_2.sh
  GRADLE_CMD="$LOCAL_GRADLE"
fi

if [ ! -x "$GRADLE_CMD" ] && [ "$GRADLE_CMD" != "gradle" ]; then
  echo "FAIL Gradle command not executable: $GRADLE_CMD"
  exit 1
fi

echo "Using Gradle: $GRADLE_CMD"
$GRADLE_CMD --version | sed -n '1,12p'

export GRADLE_USER_HOME="$ROOT/.gradle-termux"
$GRADLE_CMD --no-daemon clean :app:assembleDebug --stacktrace

if [ -f app/build/outputs/apk/debug/app-debug.apk ]; then
  mkdir -p dist
  cp app/build/outputs/apk/debug/app-debug.apk dist/AT110_termux_debug.apk
  echo "ANDROID_DEBUG_BUILD_PASS dist/AT110_termux_debug.apk"
else
  echo "WARN build completed but default APK path not found"
fi
