#!/usr/bin/env bash
set -euo pipefail

EXPECTED_CONTEXT="${EXPECTED_CONTEXT:-camel}"
NS="${NS:-wind-kafka}"
CONSUMER_DEPLOY="${CONSUMER_DEPLOY:-wind-kafka-consumer}"
FRONTEND_DEPLOY="${FRONTEND_DEPLOY:-wind-kafka-frontend}"

TARGET_CONSUMERS="${TARGET_CONSUMERS:-}"

usage() {
  cat <<'EOF'
Scale Wind Kafka consumer replicas (and keep UI in sync).

Usage:
  bash scripts/scale-wind-kafka-consumer.sh <replicas>

Optional env vars:
  EXPECTED_CONTEXT=camel
  NS=wind-kafka
  CONSUMER_DEPLOY=wind-kafka-consumer
  FRONTEND_DEPLOY=wind-kafka-frontend

Notes:
  - Also sets DEMO_CONSUMER_STREAMS in the frontend Deployment to match <replicas>
    (capped at 24) to keep turbine discovery responsive without overloading the UI.
EOF
}

if [[ "${1:-}" == "-h" || "${1:-}" == "--help" ]]; then
  usage
  exit 0
fi

need() {
  command -v "$1" >/dev/null 2>&1 || { echo "Missing '$1' in PATH" >&2; exit 1; }
}

need oc

if [[ -z "${TARGET_CONSUMERS}" ]]; then
  TARGET_CONSUMERS="${1:-}"
fi

if [[ -z "${TARGET_CONSUMERS}" || ! "${TARGET_CONSUMERS}" =~ ^[0-9]+$ ]]; then
  echo "ERROR: replicas must be a non-negative integer." >&2
  usage >&2
  exit 1
fi

CTX="$(oc config current-context 2>/dev/null || true)"
if [[ -z "$CTX" ]]; then
  echo "ERROR: unable to determine current oc context." >&2
  exit 1
fi
if [[ -n "${EXPECTED_CONTEXT}" && "$CTX" != "${EXPECTED_CONTEXT}" ]]; then
  echo "ERROR: refusing to scale because current context is '$CTX' but EXPECTED_CONTEXT is '$EXPECTED_CONTEXT'." >&2
  echo "Tip: set EXPECTED_CONTEXT='$CTX' or run: oc config use-context $EXPECTED_CONTEXT" >&2
  exit 1
fi

cap_streams() {
  local n="$1"
  if (( n < 1 )); then
    echo "1"
  elif (( n > 24 )); then
    echo "24"
  else
    echo "$n"
  fi
}

STREAMS="$(cap_streams "$TARGET_CONSUMERS")"

echo "Scaling $NS/$CONSUMER_DEPLOY -> replicas=$TARGET_CONSUMERS"
oc -n "$NS" scale deploy "$CONSUMER_DEPLOY" --replicas="$TARGET_CONSUMERS" >/dev/null
oc -n "$NS" rollout status deploy/"$CONSUMER_DEPLOY" --timeout=10m

echo "Updating $NS/$FRONTEND_DEPLOY env DEMO_CONSUMER_STREAMS=$STREAMS"
oc -n "$NS" set env deploy/"$FRONTEND_DEPLOY" "DEMO_CONSUMER_STREAMS=$STREAMS" >/dev/null
oc -n "$NS" rollout status deploy/"$FRONTEND_DEPLOY" --timeout=10m

echo "Done."
oc -n "$NS" get deploy "$CONSUMER_DEPLOY" "$FRONTEND_DEPLOY" -o custom-columns=NAME:.metadata.name,REPLICAS:.spec.replicas,AVAILABLE:.status.availableReplicas

