#!/data/data/com.termux/files/usr/bin/sh
set -eu
echo "AT106_110 FINAL TERMUX CHECKS"
sh termux/01_static_checks.sh
sh termux/02_privacy_checks.sh
sh termux/03_qa_checks.sh
sh termux/05_make_local_manifest.sh
sh termux/04_optional_android_build.sh
echo "AT110_TERMUX_CHECKS_COMPLETE"
