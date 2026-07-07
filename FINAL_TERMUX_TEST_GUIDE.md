# AT106_110 Final Termux Test Guide

Use this package as a full source root.

## On Termux

```sh
unzip AT106_110_final_termux_lock.zip -d AetherMind_AT110
cd AetherMind_AT110
sh termux/run_all_termux_checks.sh
```

## Optional Android build in Termux

Android builds require Android SDK, NDK, CMake, Java 17, and Gradle. If these are not installed, the optional build script will skip with a clear message.

```sh
sh termux/04_optional_android_build.sh
```

## Authoritative build

Cloud/GitHub Actions is still the primary APK build environment:

```sh
gradle clean :app:assembleDebug :app:assembleRelease --stacktrace
```

## Safety

This release remains telemetry-only. It does not enable auto-control, raw UI text export, or external data export.


## Termux Gradle fix

Use `sh termux/06_clean_build_cache.sh` then `sh termux/run_all_termux_checks.sh`. The optional Android build now uses local Gradle 8.10.2 when system Gradle is 9.x.
