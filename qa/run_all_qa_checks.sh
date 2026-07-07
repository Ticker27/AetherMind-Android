#!/usr/bin/env sh
set -eu

sh qa/run_static_checks.sh
sh qa/run_privacy_checks.sh
sh qa/run_layout_checks.sh
sh qa/run_phase_lock_checks.sh

echo "ALL QA CHECKS PASS"
