#!/usr/bin/env sh
set -eu

echo "QA layout checks"

test -f app/src/main/res/layout/activity_main.xml
test -f app/src/main/res/layout/layout_aether_hud.xml

grep -q 'btnExportSanitized' app/src/main/res/layout/activity_main.xml
grep -q 'btnResetRuntimePrivacy' app/src/main/res/layout/activity_main.xml
grep -q 'btnFreezeProfile' app/src/main/res/layout/activity_main.xml
grep -q 'hudIntentText' app/src/main/res/layout/layout_aether_hud.xml
grep -q 'hudDetailText' app/src/main/res/layout/layout_aether_hud.xml

echo "QA layout checks PASS"
