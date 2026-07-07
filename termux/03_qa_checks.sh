#!/data/data/com.termux/files/usr/bin/sh
set -eu
echo "[AT110] QA checks"
if [ -x qa/run_all_qa_checks.sh ]; then
  sh qa/run_all_qa_checks.sh
else
  echo "WARN qa/run_all_qa_checks.sh not executable; running with sh"
  sh qa/run_all_qa_checks.sh
fi
echo "QA_CHECKS_PASS"
