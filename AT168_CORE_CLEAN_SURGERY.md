# AT168 Core Clean Surgery

Goal: cut the project back to the heart of Aether Core: observe, build WorldState, validate, propose, explain. No gameplay execution, no auto-play, no learning from mock data.

## Surgical cuts

### Native execution hard lock
- `IntegrationLoop::executeAutoPlayFrame()` now delegates to `classifyOnlyFrame()`.
- `nativeRunFrame()` now calls `classifyOnlyFrame()` directly.
- `integrationLoopSetAutoPlayEnabled()` always stores `false` and returns `false`.
- Auto-play interval/power exports return `0`.
- Native bridge health reports `Exec=LOCKED | Mode=PROPOSE_ONLY | Auto=DISABLED_NATIVE`.

### Execution queue locked
- `ExecutionBridge::pushCommand()` validates ABI input but never accepts commands.
- It clears the queue and returns `ERROR_EXECUTION_LOCKED`.
- Kotlin status mapping includes `ERROR_EXECUTION_LOCKED`.

### Kotlin auto-play removed from behavior path
- `AutoPlayController` is now a no-op compatibility shell.
- `VisionShotIntegration` is now a locked compatibility shell.
- Overlay button cannot arm auto-play; it clears legacy queues and reports `LOCKED`.
- UI wording changed from `Aim` to `Proposal` where the overlay is only a diagnostic proposal.

### WorldState contract repaired
- Added explicit `WorldStateMeta` with source, sequence, timestamp, coordinate space and authority flags.
- Added `PerceptionSource`: Unknown / Mock / SyntheticTest / RealVision / AccessibilityOnly.
- Added canonical table/ball/pocket/quality structures while preserving legacy `objects` for older planner compatibility.
- Added `WorldStateValidator` and `ValidationReport`.

### Brain core cleaned
- `AetherMind` no longer executes or records skill memory.
- It builds WorldState, validates it, optionally ranks proposals, and always emits propose-only explanation.
- Executed-action fields are force-cleared.
- Memory is explicitly reported as disabled for diagnostic/propose-only mode.

### Vision coordinate bug fixed
- Native `BallOutput` no longer stores Q16 coordinates in `int16_t`.
- Coordinates now use `int32_t xQ16/yQ16` to avoid truncating most positions to zero.

## Verified locally

Pure C++ compilation check was run for:
- all `aether_core` source files listed in `CoreSources.cmake`
- `execution/ExecutionBridge.cpp`
- `vision/VisionProcessorImpl.cpp`

Android/Gradle build was not run in this container because no Gradle wrapper/Android SDK build environment is present.

## Intended runtime truth

Expected HUD/bridge truth after this patch:

```text
Aether Core: DIAGNOSTIC_LOCK
Vision: mock/real observation only
Proposal: ON/OFF
Auto Play: LOCKED / propose-only
Native Bridge: OK | Eye=... | Exec=LOCKED | Mode=PROPOSE_ONLY | Auto=DISABLED_NATIVE
```

The next correct milestone is not auto-play. It is real WorldState generation and structured explanation quality.
