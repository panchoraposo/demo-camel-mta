#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

INPUT="${ROOT}/legacy"
OUT="${ROOT}/mta-output"

mkdir -p "${OUT}"

echo "Running MTA analysis"
echo "  input:  ${INPUT}"
echo "  output: ${OUT}"
echo
echo "Tip: run 'mta-cli analyze --list-targets' to confirm available targets."
echo

mta-cli analyze \
  --input "${INPUT}" \
  --output "${OUT}" \
  --target quarkus \
  --target jakarta-ee \
  --target camel

echo
echo "Done. Open the report under: ${OUT}"

