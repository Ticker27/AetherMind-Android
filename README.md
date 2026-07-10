# AetherMind Integrated Execution Project

Single integrated Android project for the AetherMind Execution Core.

This project combines:

- C++17 NDK execution core under `app/src/main/cpp/`
- JNI bridge exported for `com.aethermind.execution.AetherExecutionNative`
- Kotlin dispatcher layer under `app/src/main/java/com/aethermind/execution/`
- AccessibilityService gesture executor and foreground package guard
- Emergency stop controller that clears the native queue

## Project structure

```text
AetherMind_Integrated/
├── settings.gradle.kts
├── build.gradle.kts
├── README.md
└── app/
    ├── build.gradle.kts
    ├── proguard-rules.pro
    └── src/main/
        ├── AndroidManifest.xml
        ├── cpp/
        │   ├── ActionCommandSchema.h
        │   ├── ExecutionBridge.h
        │   ├── ExecutionBridge.cpp
        │   ├── aether_execution_jni.cpp
        │   └── CMakeLists.txt
        ├── java/com/aethermind/execution/
        │   ├── Action/Kotlin execution bridge files
        │   ├── Accessibility gesture executor
        │   ├── Foreground package guard
        │   ├── Emergency stop controller
        │   └── MainActivity.kt
        └── res/xml/
            └── aether_accessibility_service.xml
```

## Build requirements

- Android Studio Ladybug or newer, or command-line Android Gradle Plugin 8.6.1 environment
- JDK 17
- Android SDK 35
- Android NDK `26.3.11579264`
- CMake `3.22.1`

## Build command

From the project root:

```bash
./gradlew :app:assembleDebug
```

If this archive is opened directly in Android Studio, sync Gradle first, then run the `app` configuration.

## Native integration check

JNI naming is aligned as follows:

| Kotlin class | Native export prefix |
|---|---|
| `com.aethermind.execution.AetherExecutionNative` | `Java_com_aethermind_execution_AetherExecutionNative_` |

Native library naming is aligned as follows:

| Kotlin load call | CMake target | APK library |
|---|---|---|
| `System.loadLibrary("aether_execution_core")` | `aether_execution_core` | `libaether_execution_core.so` |

Command ABI size is fixed at 24 bytes and checked by both native static assertions and Kotlin constants.

## Runtime permission setup

Before dispatching any real gesture:

1. Install the debug or release APK on the target Android device.
2. Open the app.
3. Tap **Open Accessibility Settings**.
4. Enable **AetherMind Execution Service**.
5. Start the dispatcher only for the intended target package:

```kotlin
AetherRuntime.startForTargetPackage("com.example.targetgame")
```

## Safety model

- The dispatcher checks the foreground package before every real gesture.
- If the foreground package is not the configured target package, it calls `nativeClearQueue()` and triggers emergency stop.
- `AetherEmergencyStopController.stop(...)` can be called from UI, service code, or fault handlers.
- The JNI layer never stores raw `DirectByteBuffer` pointers. It copies command bytes by value during the current JNI call only.
- The native queue is protected by a single `std::mutex`; push, pop, and clear all share the same lock.

## Important operational cautions

- Android Accessibility permission must be granted manually by the user. Apps cannot silently enable this service.
- Do not start the dispatcher until the AccessibilityService is connected.
- Keep `targetPackage` exact. A mismatch intentionally clears the native queue and stops execution.
- The current ABI only contains `x`, `y`, `type`, and `timestampNanos`. `SWIPE` uses `x/y` as the start point and the configured `swipeDeltaXPx/swipeDeltaYPx` as the end offset.
- Test first with a harmless internal test app before targeting any production app or game.

## Files intentionally not merged

The uploaded archive also contained a legacy `com.aether.renderer` trajectory-renderer project. It uses a different package namespace and JNI prefix. This integrated project keeps the execution-core package at `com.aethermind.execution` so that the Phase 1 JNI export names and Phase 2 Kotlin class paths remain consistent without altering the original execution logic.
