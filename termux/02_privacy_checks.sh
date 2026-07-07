#!/data/data/com.termux/files/usr/bin/sh
set -eu
echo "[AT110] privacy checks"

grep -q 'RAW_UI_TEXT_STORED: FALSE' BOT_LABEL.txt
grep -q 'RAW_UI_TEXT_EXPORTED: FALSE' BOT_LABEL.txt
grep -q 'EXTERNAL_EXPORT_ALLOWED: FALSE' BOT_LABEL.txt
grep -q 'TELEMETRY_ONLY: TRUE' BOT_LABEL.txt
grep -q 'AUTO_CONTROL_ENABLED: FALSE' BOT_LABEL.txt

if grep -R 'rawText\|raw_ui_text\|exportRaw\|sendToServer\|uploadTelemetry' app/src/main/java >/dev/null 2>&1; then
  echo "WARN possible raw/export token found; review manually"
else
  echo "OK no obvious raw/export token"
fi

echo "PRIVACY_CHECKS_PASS"
