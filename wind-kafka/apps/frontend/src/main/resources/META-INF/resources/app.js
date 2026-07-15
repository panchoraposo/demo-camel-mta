let state = {
  consumed: 0,
  totalWatts: 0,
  speedFactor: 0,
  distance: 0,
};

const elConsumed = document.getElementById("consumed");
const elWatts = document.getElementById("watts");
const elSpeed = document.getElementById("speed");
const elMode = document.getElementById("mode");
const elConsumers = document.getElementById("consumers");
const car = document.getElementById("car");

const scene = document.querySelector(".scene");
const turbineField = document.getElementById("turbine-field");

let consumerBaseUrl = null;
let consumerStreams = 1;
let snapshotUrl = null;
let streamUrl = null;
let sources = [];
let lastTick = performance.now();
let posPx = 0;
let dir = 1;
let aliveCountLast = 0;
let lastSnapshotAtMs = 0;
let uiConsumedMonotonic = 0;
let uiWattsEma = 0;
let uiSpeedEma = 0;
let lastUiUpdateAtMs = 0;
let lastLayoutAtMs = 0;
let lastLayoutKey = "";

// Track unique consumers observed over SSE/polling (useful when consumer is scaled on OpenShift).
const consumers = new Map(); // id -> { lastSeenMs, lastSnapshot }
const turbineByConsumer = new Map(); // id -> { root, blades }

function setMode(m) {
  elMode.textContent = m;
}

function clamp(v, min, max) {
  return Math.max(min, Math.min(max, v));
}

function updateUi() {
  const now = performance.now();
  const alive = Array.from(consumers.entries())
    .filter(([, v]) => now - v.lastSeenMs < 30000)
    .map(([, v]) => v);

  aliveCountLast = alive.length;
  elConsumers.textContent = String(aliveCountLast);

  // Aggregate across pods to avoid "jumping back to 0" when we hit a different replica.
  // Use per-consumer max to keep the total monotonic for the session.
  let sumConsumed = 0;
  let sumWatts = 0;
  let sumSpeed = 0;
  let speedN = 0;

  for (const v of alive) {
    const snap = v.lastSnapshot || {};
    const consumed = Number(snap.consumed || 0);
    v.maxConsumed = Math.max(Number(v.maxConsumed || 0), consumed);
    sumConsumed += v.maxConsumed;

    const watts = Number((snap.last && snap.last.watts) || snap.totalWatts || 0);
    sumWatts += watts;

    const sf = Number(snap.speedFactor || 0);
    sumSpeed += sf;
    speedN += 1;
  }

  uiConsumedMonotonic = Math.max(uiConsumedMonotonic, sumConsumed);
  const rawWatts = sumWatts;
  const rawSpeed = speedN ? sumSpeed / speedN : 0;

  // Smooth numbers to keep movement fluid.
  uiWattsEma = uiWattsEma === 0 ? rawWatts : uiWattsEma * 0.85 + rawWatts * 0.15;
  uiSpeedEma = uiSpeedEma === 0 ? rawSpeed : uiSpeedEma * 0.85 + rawSpeed * 0.15;

  state.consumed = uiConsumedMonotonic;
  state.totalWatts = uiWattsEma;
  state.speedFactor = uiSpeedEma;

  elConsumed.textContent = String(Math.round(uiConsumedMonotonic));
  elWatts.textContent = String(Math.round(uiWattsEma));
  elSpeed.textContent = `${Math.round(uiSpeedEma * 100)}%`;
}

function mkTurbineEl(sizeClass) {
  const root = document.createElement("div");
  root.className = `turbine ${sizeClass}`.trim();

  const mast = document.createElement("div");
  mast.className = "mast";
  const hub = document.createElement("div");
  hub.className = "hub";
  root.appendChild(mast);
  root.appendChild(hub);

  const blades = [];
  ["b1", "b2", "b3"].forEach((b) => {
    const blade = document.createElement("div");
    blade.className = `blade ${b}`;
    blades.push(blade);
    root.appendChild(blade);
  });

  return { root, blades };
}

function layoutTurbines() {
  // Use only "recently seen" consumers to avoid accumulating forever.
  const now = performance.now();
  const alive = Array.from(consumers.entries())
    .filter(([, v]) => now - v.lastSeenMs < 30000)
    .map(([id]) => id);

  // Deterministic order so layout doesn't jump too much
  alive.sort();

  // Ensure DOM exists for each alive consumer
  alive.forEach((id, idx) => {
    if (turbineByConsumer.has(id)) return;
    const sizeClass = idx % 6 === 0 ? "" : idx % 3 === 0 ? "small" : "tiny";
    const t = mkTurbineEl(sizeClass);
    turbineByConsumer.set(id, t);
    turbineField.appendChild(t.root);
  });

  // Remove turbines for consumers not alive anymore
  for (const [id, t] of turbineByConsumer.entries()) {
    if (!alive.includes(id)) {
      try {
        t.root.remove();
      } catch {}
      turbineByConsumer.delete(id);
    }
  }

  // Position turbines across the scene width.
  const w = scene.clientWidth;
  const n = alive.length || 1;
  const left = w * 0.04;
  const right = w * 0.04;
  const span = Math.max(1, w - left - right);
  alive.forEach((id, i) => {
    const t = turbineByConsumer.get(id);
    if (!t) return;
    const x = left + (span * (i + 0.5)) / n;
    t.root.style.left = `${Math.round(x - 75)}px`; // approx half of base width
  });
}

function applyMotion(dtMs) {
  const now = performance.now();
  const stale = lastSnapshotAtMs > 0 && now - lastSnapshotAtMs > 5000;

  const w = scene.clientWidth;
  // Make the visible track longer before looping.
  const leftPad = w * 0.08;
  const rightPad = w * 0.03;
  const trackLen = Math.max(240, w - leftPad - rightPad - 20);

  // Car speed based on speedFactor (0..1). Convert to px/sec.
  const shouldStop = aliveCountLast <= 0 || stale;
  const pxPerSec = shouldStop ? 0 : 25 + (state.speedFactor ?? 0) * 360;
  posPx += dir * (pxPerSec * dtMs) / 1000;

  // Ping-pong instead of looping to feel like a longer run.
  if (posPx > trackLen) {
    posPx = trackLen;
    dir = -1;
  } else if (posPx < 0) {
    posPx = 0;
    dir = 1;
  }

  car.style.transform = `translateX(${posPx}px)`;

  // Turbines spin rate (global intensity derived from latest snapshot)
  const rpm = shouldStop ? 0 : 10 + (state.speedFactor ?? 0) * 190;
  const degPerMs = (rpm * 360) / 60000;
  const rot = ((now * degPerMs) % 360);
  for (const [, t] of turbineByConsumer.entries()) {
    t.blades.forEach((b, i) => {
      const base = i * 120;
      b.style.transform = `translateX(-50%) rotate(${base + rot}deg)`;
    });
  }
}

function onSnapshot(snap) {
  state = snap;
  lastSnapshotAtMs = performance.now();
  const id = snap?.consumer ?? "consumer";
  const prev = consumers.get(id) || {};
  consumers.set(id, { ...prev, lastSeenMs: performance.now(), lastSnapshot: snap });

  // Throttle UI + layout work (polling fanout can be noisy).
  const now = performance.now();
  if (now - lastUiUpdateAtMs >= 120) {
    lastUiUpdateAtMs = now;
    updateUi();
  }

  // Layout only when the alive set changes, and at most once per second.
  if (now - lastLayoutAtMs >= 1000) {
    const aliveIds = Array.from(consumers.entries())
      .filter(([, v]) => now - v.lastSeenMs < 30000)
      .map(([cid]) => cid)
      .sort();
    const key = aliveIds.join("|");
    if (key !== lastLayoutKey) {
      lastLayoutKey = key;
      lastLayoutAtMs = now;
      layoutTurbines();
    }
  }
}

async function getConfig() {
  const res = await fetch("/api/config");
  if (!res.ok) throw new Error(`config http ${res.status}`);
  return await res.json();
}

function stopPolling() {
  polling = false;
}

function connectSseFanout() {
  if (!streamUrl) return;
  const url = streamUrl;

  // Close old sources
  sources.forEach((s) => {
    try {
      s.close();
    } catch {}
  });
  sources = [];

  const n = Math.max(1, Math.min(24, Number(consumerStreams || 1)));
  let opened = 0;
  let failed = 0;

  for (let i = 0; i < n; i++) {
    let s;
    try {
      // Add a query param to avoid overly aggressive proxy caching and to help LB distribute.
      s = new EventSource(`${url}?c=${i}-${Math.random().toString(16).slice(2)}`);
    } catch {
      s = null;
    }
    if (!s) {
      failed++;
      continue;
    }
    sources.push(s);

    s.onopen = () => {
      opened++;
      setMode(`sse x${opened}`);
    };
    s.onmessage = (ev) => {
      try {
        onSnapshot(JSON.parse(ev.data));
      } catch {
        // ignore malformed events
      }
    };
    s.onerror = () => {
      failed++;
      try {
        s.close();
      } catch {}
      // If everything fails, fallback to polling
      if (failed >= n) {
        setMode("polling");
        startPollingFanout();
      }
    };
  }

  if (sources.length === 0) {
    setMode("polling");
    startPollingFanout();
    return;
  }
}

let polling = false;
async function startPollingOne(url, idx) {
  while (polling) {
    try {
      const res = await fetch(`${url}?c=${idx}-${Math.random().toString(16).slice(2)}`, { cache: "no-store" });
      if (res.ok) {
        onSnapshot(await res.json());
      }
    } catch {
      // ignore transient errors
    }
    // Lower request rate to reduce UI "jank" when fanout is high.
    await new Promise((r) => setTimeout(r, 500));
  }
}

function startPollingFanout() {
  if (polling || !snapshotUrl) return;
  polling = true;

  const n = Math.max(1, Math.min(24, Number(consumerStreams || 1)));
  for (let i = 0; i < n; i++) {
    startPollingOne(snapshotUrl, i);
  }
}

function animate(now) {
  const dt = clamp(now - lastTick, 0, 80);
  lastTick = now;
  applyMotion(dt);
  requestAnimationFrame(animate);
}

async function main() {
  setMode("connecting");
  const cfg = await getConfig();
  consumerBaseUrl = cfg.consumerBaseUrl;
  consumerStreams = cfg.consumerStreams ?? 1;
  snapshotUrl = cfg.snapshotUrl || (consumerBaseUrl ? `${consumerBaseUrl.replace(/\/$/, "")}/api/snapshot` : null);
  streamUrl = cfg.streamUrl || (consumerBaseUrl ? `${consumerBaseUrl.replace(/\/$/, "")}/api/stream` : null);

  // Prefer SSE if available; otherwise use polling fanout (works behind frontend proxy).
  if (streamUrl) {
    connectSseFanout();
  } else {
    setMode("polling");
    startPollingFanout();
  }
  requestAnimationFrame(animate);
}

main().catch(() => setMode("error"));

