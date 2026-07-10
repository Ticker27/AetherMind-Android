# AetherMind Ready-To-Build Android Project

This repository is a single integrated Android project with the original AetherMind renderer/native core plus the Phase 1/Phase 2 execution layer.

## Project layout

```text
settings.gradle.kts
build.gradle.kts
.github/workflows/build-apk.yml
app/
  build.gradle.kts
  src/main/AndroidManifest.xml
  src/main/cpp/
    CMakeLists.txt
    aether_core/              # original AetherMind native core: include/src/cmake
    trajectory/               # original trajectory JNI bridge
    execution/                # Phase 1 C++ ABI/JNI execution core
  src/main/java/
    com/aether/renderer/      # original Android renderer/runtime layer
    com/aethermind/execution/ # Phase 2 Kotlin dispatcher/execution layer
  src/main/res/xml/
    aether_accessibility_service.xml
```

## Native libraries built by CMake

| Library | Kotlin loader | Purpose |
|---|---|---|
| `libaether_jni.so` | `System.loadLibrary("aether_jni")` | Original renderer / trajectory / integration JNI |
| `libaether_execution_core.so` | `System.loadLibrary("aether_execution_core")` | Phase 1 execution queue ABI/JNI bridge |

## Integration checks

| Item | Expected value |
|---|---|
| Gradle module | `:app` |
| `rootProject.name` | `AetherMind` |
| Android namespace | `com.aether.renderer` |
| App ID | `com.aether.renderer` |
| Execution Kotlin package | `com.aethermind.execution` |
| Execution JNI class | `com.aethermind.execution.AetherExecutionNative` |
| Execution JNI prefix | `Java_com_aethermind_execution_AetherExecutionNative_*` |
| Execution native library | `aether_execution_core` |
| CMake file | `app/src/main/cpp/CMakeLists.txt` |

## Build locally

Requirements:

- JDK 17
- Android SDK platform 35
- Android Build Tools 35.0.0
- Android NDK `26.3.11579264`
- CMake `3.22.1`
- Gradle 8.10.2 or GitHub Actions workflow included in this repo

Commands:

```bash
gradle --no-daemon projects
gradle --no-daemon :app:assembleDebug --stacktrace
gradle --no-daemon :app:assembleRelease --stacktrace
```

Debug APK output:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Release APK output:

```text
app/build/outputs/apk/release/app-release-unsigned.apk
```

## GitHub Actions

The workflow is in:

```text
.github/workflows/build-apk.yml
```

It performs these checks before building:

- `settings.gradle.kts` exists at repository root
- `include(":app")` is present
- `app/build.gradle.kts` exists
- `app/src/main/cpp/CMakeLists.txt` exists
- execution service is registered in `AndroidManifest.xml`
- `aether_execution_core` target exists in CMake

Important: push the project contents directly to the repository root. Do not create an extra nested folder such as `AetherMind/AetherMind/...`, otherwise Gradle may not see the `:app` module.

## Accessibility permission

The APK includes Accessibility services. Android will not enable them automatically. On device:

1. Install the APK.
2. Open **Settings > Accessibility**.
3. Enable the AetherMind service you intend to use.
4. Confirm the permission dialog.

Gesture dispatch requires:

```xml
android:canPerformGestures="true"
```

This is already set in:

```text
app/src/main/res/xml/aether_accessibility_service.xml
```

## Runtime safety notes

- The execution dispatcher checks the foreground package before executing a command.
- If the foreground package does not match the configured target package, it calls `nativeClearQueue()` and triggers emergency stop.
- `AetherEmergencyStopController.stop()` clears the native execution queue by default.
- The C++ execution queue does not store JVM raw pointers. DirectByteBuffer memory is copied by value during the JNI call only.

## Do not change these unless you also update JNI names

- `com.aethermind.execution.AetherExecutionNative`
- `System.loadLibrary("aether_execution_core")`
- C++ JNI exports in `app/src/main/cpp/execution/aether_execution_jni.cpp`
