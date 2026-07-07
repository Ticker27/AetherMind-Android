#!/usr/bin/env sh
set -eu
SO_PATH="${1:-app/build/intermediates/merged_native_libs/release/mergeReleaseNativeLibs/out/lib/arm64-v8a/libaether_jni.so}"
if [ ! -f "$SO_PATH" ]; then
  echo "Missing shared library: $SO_PATH" >&2
  echo "Run ./gradlew clean :app:assembleRelease first, or pass the .so path as argument." >&2
  exit 1
fi
NM_BIN="${LLVM_NM:-llvm-nm}"
$NM_BIN -D "$SO_PATH" | grep 'Java_com_aether_renderer_AetherIntegrationLoop_nativeRunFrame'
$NM_BIN -D "$SO_PATH" | grep 'Java_com_aether_renderer_AetherIntegrationLoop_nativeOnKeyEvent'
$NM_BIN -D "$SO_PATH" | grep 'Java_com_aether_renderer_AetherIntegrationLoop_nativeOnTouchEvent'
$NM_BIN -D "$SO_PATH" | grep 'Java_com_aether_renderer_AetherIntegrationLoop_nativeHudVisible'
$NM_BIN -D "$SO_PATH" | grep 'Java_com_aether_renderer_AetherIntegrationLoop_nativeAiActive'
$NM_BIN -D "$SO_PATH" | grep 'Java_com_aether_renderer_AetherIntegrationLoop_nativeLatestHudIntent'
$NM_BIN -D "$SO_PATH" | grep 'Java_com_aether_renderer_NativeTrajectoryBridge_nativeStateSize'
$NM_BIN -D "$SO_PATH" | grep 'Java_com_aether_renderer_NativeTrajectoryBridge_nativeCopyLatestState'
echo "JNI_SYMBOL_CHECK=PASS"
