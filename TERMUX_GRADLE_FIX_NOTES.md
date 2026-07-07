# AT110 Termux Gradle Fix

The previous Termux build log shows Gradle 9.x running against Android Gradle Plugin 8.6.1. This package keeps AGP 8.6.1 and forces the Termux optional build to use Gradle 8.10.2.

Run:

```sh
unzip AT110_termux_gradle8_fix.zip -d AetherMind_AT110_FIX
cd AetherMind_AT110_FIX
sh termux/06_clean_build_cache.sh
sh termux/run_all_termux_checks.sh
```

If Android SDK is available, `termux/04_optional_android_build.sh` will build debug APK using local Gradle 8.10.2.
