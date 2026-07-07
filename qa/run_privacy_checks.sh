#!/usr/bin/env sh
set -eu

echo "QA privacy checks"

grep -q 'RAW_UI_TEXT_EXPORTED: FALSE' BOT_LABEL.txt
grep -q 'AUTO_CONTROL_ENABLED: FALSE' BOT_LABEL.txt
grep -q 'TELEMETRY_ONLY: TRUE' BOT_LABEL.txt
grep -q 'rawUiTextExported: Boolean = false' app/src/main/java/com/aether/renderer/QaManifest.kt
grep -q 'rawUiTextIncluded: Boolean = false' app/src/main/java/com/aether/renderer/CrashDiagnostics.kt
grep -q 'RAW_UI_TEXT_EXPORTED: Boolean = false' app/src/main/java/com/aether/renderer/ProductionLock.kt
grep -q 'AUTO_CONTROL_ENABLED: Boolean = false' app/src/main/java/com/aether/renderer/ProductionLock.kt

echo "QA privacy checks PASS"
