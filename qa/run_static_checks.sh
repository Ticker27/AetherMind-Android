#!/usr/bin/env sh
set -eu

echo "QA static checks"

test -f settings.gradle.kts
test -f app/build.gradle.kts
test -f app/src/main/java/com/aether/renderer/AetherRuntimeBus.kt
test -f app/src/main/java/com/aether/renderer/MainActivity.kt
test -f app/src/main/java/com/aether/renderer/QaManifest.kt
test -f app/src/main/java/com/aether/renderer/RegressionTestPlan.kt
test -f app/src/main/java/com/aether/renderer/ScenarioTestCatalog.kt
test -f app/src/main/java/com/aether/renderer/BuildMatrix.kt
test -f app/src/main/java/com/aether/renderer/CrashDiagnostics.kt
test -f app/src/main/java/com/aether/renderer/Phase9Stable.kt
test -f app/src/main/java/com/aether/renderer/ReleaseCandidate.kt
test -f app/src/main/java/com/aether/renderer/RuntimeMonitoring.kt
test -f app/src/main/java/com/aether/renderer/UpdateChannel.kt
test -f app/src/main/java/com/aether/renderer/RollbackSystem.kt
test -f app/src/main/java/com/aether/renderer/MaintenanceMode.kt
test -f app/src/main/java/com/aether/renderer/ProductionLock.kt
test -f app/src/main/java/com/aether/renderer/FinalManifest.kt
test -f app/src/main/java/com/aether/renderer/BaselineVerifier.kt
test -f app/src/main/java/com/aether/renderer/TermuxTestHarness.kt
test -f app/src/main/java/com/aether/renderer/Phase10ExternalTestLock.kt
test -f app/src/main/java/com/aether/renderer/UiReadability.kt
test -f app/src/main/java/com/aether/renderer/DashboardOverlayGate.kt
test -f app/src/main/java/com/aether/renderer/StrategicStateModel.kt
test -f app/src/main/java/com/aether/renderer/StrategicIntentEngine.kt
test -f app/src/main/java/com/aether/renderer/SkillBoundary.kt
test -f app/src/main/java/com/aether/renderer/PlannerCandidateGenerator.kt
test -f app/src/main/java/com/aether/renderer/StrategicCostModel.kt
test -f app/src/main/java/com/aether/renderer/PlanExplanation.kt
test -f app/src/main/java/com/aether/renderer/StrategicReasoningCore.kt
test -f app/src/main/java/com/aether/renderer/StrategicReasoningLock.kt
grep -q "strategicReasoningFlow" app/src/main/java/com/aether/renderer/AetherRuntimeBus.kt
grep -q "ACTION_EXECUTION_ENABLED = false" app/src/main/java/com/aether/renderer/StrategicReasoningLock.kt

grep -q 'versionCode = 1250' app/build.gradle.kts
grep -q 'versionName = "11.5.0"' app/build.gradle.kts
grep -q 'AT90_95_QA_LOCK' app/src/main/java/com/aether/renderer/QaManifest.kt
grep -q 'FROZEN_PHASE9' app/src/main/java/com/aether/renderer/Phase9Stable.kt
grep -q 'AT120_125_STRATEGIC_REASONING_CORE' app/src/main/java/com/aether/renderer/FinalManifest.kt
grep -q 'FINAL_EXTERNAL_TEST_LOCK' app/src/main/java/com/aether/renderer/FinalManifest.kt
grep -q 'releaseCandidateFlow' app/src/main/java/com/aether/renderer/AetherRuntimeBus.kt
grep -q 'productionLockFlow' app/src/main/java/com/aether/renderer/AetherRuntimeBus.kt
grep -q 'ACTIVE_ANALYSIS' app/src/main/java/com/aether/renderer/UiReadability.kt
grep -q 'CONTEXT_POSITIONING' app/src/main/res/layout/activity_main.xml

grep -R 'AETHER_ARCHITECT_2026' app/src/main/java && exit 1 || true
grep -R 'TODO_COMPILER_ERROR' app/src/main/java && exit 1 || true

echo "QA static checks PASS"
