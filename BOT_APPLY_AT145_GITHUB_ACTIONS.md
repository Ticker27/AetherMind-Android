# BOT APPLY COMMAND - AT145 Pool Geometry Integration

Input status found during verification:
- The uploaded `AetherMind-Android-main.zip` contains the Android app and workflow, but it does not contain `AetherMindCore/`.
- `app/build.gradle.kts` expects `../AetherMindCore/android_bridge/cpp/CMakeLists.txt`.
- Therefore, GitHub Actions will fail unless `AetherMindCore/` is present at repository root.

Use this fixed source package as the repository root content.

## Required root layout

```text
.github/workflows/build-apk.yml
app/
AetherMindCore/
BOT_LABEL.txt
build.gradle.kts
settings.gradle.kts
```

## Safety invariants to preserve

```text
android:canPerformGestures="false"
devKey: null
AUTO_CONTROL_ENABLED: false
EXECUTION_ENABLED: false
SKILL_MODE: PROPOSE_ONLY
RAW_UI_TEXT_EXPORTED: false
```

## Do not modify

```text
AetherMindCore/android_bridge/cpp/aether_trajectory_jni.cpp
AetherMindCore/include/aether/security/OmnisGate.h
```

## Build command in GitHub Actions

The existing workflow uses:

```bash
gradle --no-daemon :app:assembleDebug
gradle --no-daemon :app:assembleRelease
```

Expected APK names after workflow rename:

```text
out/AT140_final_debug.apk
out/AT140_final_release.apk
```
