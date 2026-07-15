#!/usr/bin/env bash
set -euo pipefail

NS="${NS:-demo-camel-mta}"
PRODUCER_DEPLOY="${PRODUCER_DEPLOY:-modern-queue-producer}"
CONSUMER1_DEPLOY="${CONSUMER1_DEPLOY:-modern-event-consumer}"
CONSUMER2_DEPLOY="${CONSUMER2_DEPLOY:-modern-kaoto-consumer}"

EXPECTED_CONTEXT="${EXPECTED_CONTEXT:-camel}"

TARGET_PRODUCER="${TARGET_PRODUCER:-5}"
TARGET_CONSUMERS="${TARGET_CONSUMERS:-3}"
DURATION_SECONDS="${DURATION_SECONDS:-120}"

SLEEP_BETWEEN="${SLEEP_BETWEEN:-2}"

usage() {
  cat <<'EOF'
Scale producer + consumers up, then restore originals.

Defaults:
  namespace: demo-camel-mta
  producer deployment: modern-queue-producer
  consumers deployments: modern-event-consumer, modern-kaoto-consumer
  scale up to: producer=5, consumers=3
  duration: 120s

Usage:
  bash scripts/scale-flow-demo.sh

Optional env vars:
  EXPECTED_CONTEXT=camel
  NS=demo-camel-mta
  TARGET_PRODUCER=8
  TARGET_CONSUMERS=5
  DURATION_SECONDS=180
  PRODUCER_DEPLOY=modern-queue-producer
  CONSUMER1_DEPLOY=modern-event-consumer
  CONSUMER2_DEPLOY=modern-kaoto-consumer
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

get_replicas() {
  local deploy="$1"
  local r
  r="$(oc -n "$NS" get deploy "$deploy" -o jsonpath='{.spec.replicas}' 2>/dev/null || true)"
  if [[ -z "$r" ]]; then
    echo "ERROR: deployment '$deploy' not found in namespace '$NS'." >&2
    exit 1
  fi
  echo "$r"
}

scale_and_wait() {
  local deploy="$1"
  local replicas="$2"
  echo "Scaling $NS/$deploy -> replicas=$replicas"
  oc -n "$NS" scale deploy "$deploy" --replicas="$replicas" >/dev/null
  oc -n "$NS" rollout status deploy/"$deploy" --timeout=5m
}

ORIG_PRODUCER="$(get_replicas "$PRODUCER_DEPLOY")"
ORIG_CONSUMER1="$(get_replicas "$CONSUMER1_DEPLOY")"
ORIG_CONSUMER2="$(get_replicas "$CONSUMER2_DEPLOY")"

restore() {
  set +e
  echo
  echo "Restoring original replica counts..."
  oc -n "$NS" scale deploy "$PRODUCER_DEPLOY" --replicas="$ORIG_PRODUCER" >/dev/null 2>&1 || true
  oc -n "$NS" scale deploy "$CONSUMER1_DEPLOY" --replicas="$ORIG_CONSUMER1" >/dev/null 2>&1 || true
  oc -n "$NS" scale deploy "$CONSUMER2_DEPLOY" --replicas="$ORIG_CONSUMER2" >/dev/null 2>&1 || true

  oc -n "$NS" rollout status deploy/"$PRODUCER_DEPLOY" --timeout=5m >/dev/null 2>&1 || true
  oc -n "$NS" rollout status deploy/"$CONSUMER1_DEPLOY" --timeout=5m >/dev/null 2>&1 || true
  oc -n "$NS" rollout status deploy/"$CONSUMER2_DEPLOY" --timeout=5m >/dev/null 2>&1 || true

  echo "Restored:"
  oc -n "$NS" get deploy "$PRODUCER_DEPLOY" "$CONSUMER1_DEPLOY" "$CONSUMER2_DEPLOY" -o custom-columns=NAME:.metadata.name,REPLICAS:.spec.replicas,AVAILABLE:.status.availableReplicas
}

trap restore EXIT INT TERM

echo "Current replica counts:"
echo "  $NS/$PRODUCER_DEPLOY=$ORIG_PRODUCER"
echo "  $NS/$CONSUMER1_DEPLOY=$ORIG_CONSUMER1"
echo "  $NS/$CONSUMER2_DEPLOY=$ORIG_CONSUMER2"
echo

scale_and_wait "$PRODUCER_DEPLOY" "$TARGET_PRODUCER"
sleep "$SLEEP_BETWEEN"
scale_and_wait "$CONSUMER1_DEPLOY" "$TARGET_CONSUMERS"
sleep "$SLEEP_BETWEEN"
scale_and_wait "$CONSUMER2_DEPLOY" "$TARGET_CONSUMERS"

echo
echo "Scaled up. Holding for ${DURATION_SECONDS}s..."
oc -n "$NS" get deploy "$PRODUCER_DEPLOY" "$CONSUMER1_DEPLOY" "$CONSUMER2_DEPLOY" -o custom-columns=NAME:.metadata.name,REPLICAS:.spec.replicas,AVAILABLE:.status.availableReplicas
sleep "$DURATION_SECONDS"

echo "Done (triggering restore)."
