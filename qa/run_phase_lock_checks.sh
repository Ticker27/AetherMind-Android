#!/usr/bin/env sh
set -eu

echo "QA phase lock checks"

grep -q 'FROZEN_PHASE3' app/src/main/java/com/aether/renderer/Phase3Lock.kt
grep -q 'FROZEN_PHASE4' app/src/main/java/com/aether/renderer/Phase4Lock.kt
grep -q 'FROZEN_PHASE5' app/src/main/java/com/aether/renderer/Phase5Lock.kt
test -f app/src/main/java/com/aether/renderer/Phase7Performance.kt
grep -q 'FROZEN_PHASE8' app/src/main/java/com/aether/renderer/Phase8Stable.kt
grep -q 'FROZEN_PHASE9' app/src/main/java/com/aether/renderer/Phase9Stable.kt
grep -q 'PRODUCTION_LOCKED' app/src/main/java/com/aether/renderer/ProductionLock.kt
grep -q 'FINAL_EXTERNAL_TEST_LOCK' app/src/main/java/com/aether/renderer/FinalManifest.kt
grep -q 'AT120_125_STRATEGIC_REASONING_CORE' app/src/main/java/com/aether/renderer/FinalManifest.kt

echo "QA phase lock checks PASS"
