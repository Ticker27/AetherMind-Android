#!/data/data/com.termux/files/usr/bin/sh
set -eu

echo "[AT120-125] static checks"

need_file() {
  if [ ! -f "$1" ]; then
    echo "FAIL missing $1"
    exit 1
  fi
  echo "OK $1"
}

need_file settings.gradle.kts
need_file build.gradle.kts
need_file app/build.gradle.kts
need_file BOT_LABEL.txt
need_file BUILD_NOTES.txt
need_file app/src/main/java/com/aether/renderer/FinalManifest.kt
need_file app/src/main/java/com/aether/renderer/AetherRuntimeBus.kt
need_file app/src/main/java/com/aether/renderer/StrategicStateModel.kt
need_file app/src/main/java/com/aether/renderer/StrategicIntentEngine.kt
need_file app/src/main/java/com/aether/renderer/SkillBoundary.kt
need_file app/src/main/java/com/aether/renderer/PlannerCandidateGenerator.kt
need_file app/src/main/java/com/aether/renderer/StrategicCostModel.kt
need_file app/src/main/java/com/aether/renderer/PlanExplanation.kt
need_file app/src/main/java/com/aether/renderer/StrategicReasoningCore.kt
need_file app/src/main/java/com/aether/renderer/StrategicReasoningLock.kt
need_file AetherMindCore/android_bridge/cpp/CMakeLists.txt

grep -q 'LABEL: AT120_125_STRATEGIC_REASONING_CORE' BOT_LABEL.txt
grep -q 'versionCode = 1250' app/build.gradle.kts
grep -q 'versionName = "11.5.0"' app/build.gradle.kts
grep -q 'AUTO_CONTROL_ENABLED: FALSE' BOT_LABEL.txt
grep -q 'ACTION_EXECUTION_ENABLED: FALSE' BOT_LABEL.txt
grep -q 'SKILL_LAYER_ENABLED: FALSE' BOT_LABEL.txt
grep -q 'RAW_UI_TEXT_EXPORTED: FALSE' BOT_LABEL.txt
grep -q 'EXTERNAL_EXPORT_ALLOWED: FALSE' BOT_LABEL.txt
grep -q 'strategicReasoningFlow' app/src/main/java/com/aether/renderer/AetherRuntimeBus.kt
grep -q 'ACTION_EXECUTION_ENABLED = false' app/src/main/java/com/aether/renderer/StrategicReasoningLock.kt

if grep -R 'AETHER_ARCHITECT_2026\|TODO_COMPILER_ERROR' app/src/main/java >/dev/null 2>&1; then
  echo "FAIL bad marker found"
  exit 1
fi

echo "STATIC_CHECKS_PASS"
