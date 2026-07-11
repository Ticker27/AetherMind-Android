# AT163 Consolidated Build Pack

This pack merges AT159, AT160, AT161, and AT162 into one project tree so the CI/bot should not race against an older commit.

## Included AT set

| AT | Included feature |
|---|---|
| AT159 | Two-window pool overlay: non-touch Canvas overlay + touchable Floating Menu |
| AT160 | Accessibility Eye wired into `AccessibilityStateMapper` and `AetherRuntimeBus` |
| AT161 | AI Skill Level menu: Basic, Smart, Pro, backed by C++ runtime/JNI |
| AT162 | Skill-adaptive guarded Auto Play policy and Kotlin dispatcher bridge |

## Runtime separation

```text
AccessibilityService = eye/context observer
C++ aether_jni = policy, skill level, runtime switches
Compose Canvas = visual aim guide overlay
Floating Menu = user controls
Execution bridge = guarded dispatcher, only active when Auto Play is explicitly enabled
```

## Critical files that must be committed together

```text
app/src/main/java/com/aethermind/ui/AetherFloatingMenu.kt
app/src/main/java/com/aethermind/ui/AetherDevOverlayService.kt
app/src/main/java/com/aethermind/ui/overlay/AetherAimCanvas.kt
app/src/main/java/com/aethermind/ui/overlay/MockPoolOverlayProvider.kt
app/src/main/java/com/aethermind/ui/overlay/OverlayUiState.kt
app/src/main/java/com/aethermind/ui/overlay/TrajectoryPathRenderer.kt
app/src/main/java/com/aethermind/execution/AetherAccessibilityService.kt
app/src/main/java/com/aethermind/execution/AutoPlayController.kt
app/src/main/java/com/aethermind/execution/AccessibilityGestureExecutor.kt
app/src/main/java/com/aethermind/execution/AetherRuntime.kt
app/src/main/java/com/aether/renderer/AetherIntegrationLoop.kt
app/src/main/java/com/aether/renderer/MainActivity.kt
app/src/main/cpp/aether_core/src/runtime/IntegrationLoop.cpp
app/src/main/AndroidManifest.xml
app/build.gradle.kts
build.gradle.kts
gradle.properties
```

## Bot/CI command

```bash
gradle --no-daemon clean :app:assembleDebug --stacktrace
```

## Commit command

Use `git add` with exact paths below. Do not run `git rm AutoPlayController.kt`; it is a required file.

```bash
git add \
  AT163_CONSOLIDATED_WORKFLOW.md \
  app/src/main/java/com/aethermind/ui/AetherFloatingMenu.kt \
  app/src/main/java/com/aethermind/ui/AetherDevOverlayService.kt \
  app/src/main/java/com/aethermind/ui/overlay/AetherAimCanvas.kt \
  app/src/main/java/com/aethermind/ui/overlay/MockPoolOverlayProvider.kt \
  app/src/main/java/com/aethermind/ui/overlay/OverlayUiState.kt \
  app/src/main/java/com/aethermind/ui/overlay/TrajectoryPathRenderer.kt \
  app/src/main/java/com/aethermind/execution/AetherAccessibilityService.kt \
  app/src/main/java/com/aethermind/execution/AutoPlayController.kt \
  app/src/main/java/com/aethermind/execution/AccessibilityGestureExecutor.kt \
  app/src/main/java/com/aethermind/execution/AetherRuntime.kt \
  app/src/main/java/com/aether/renderer/AetherIntegrationLoop.kt \
  app/src/main/java/com/aether/renderer/MainActivity.kt \
  app/src/main/cpp/aether_core/src/runtime/IntegrationLoop.cpp \
  app/src/main/AndroidManifest.xml \
  app/build.gradle.kts \
  build.gradle.kts \
  gradle.properties

git commit -m "AT163: Consolidate overlay eye skill and autoplay stack"
git push
```

## Notes from validation

- The earlier issue from the screenshot was likely caused by a bad `git add` / stale commit run. This pack includes `MainActivity.kt` and `AutoPlayController.kt` together.
- `AutoPlayController.kt` must be present in `app/src/main/java/com/aethermind/execution/`.
- `MainActivity.kt` must include `nativeAutoPlayEnabled()` status so the local fix is not left in the working tree.
- Android build was not executed in the sandbox because the mounted project has no `gradle`/`gradlew`; static validation and C++ syntax validation were executed.
