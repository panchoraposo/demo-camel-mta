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
let sources = [];
let lastTick = performance.now();
let posPx = 0;
let dir = 1;
let aliveCountLast = 0;
let lastSnapshotAtMs = 0;

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
  elConsumed.textContent = String(state.consumed ?? 0);
  elWatts.textContent = String(state.last?.watts ?? state.totalWatts ?? 0);
  elSpeed.textContent = `${Math.round((state.speedFactor ?? 0) * 100)}%`;
  const now = performance.now();
  const aliveCount = Array.from(consumers.values()).filter((v) => now - v.lastSeenMs < 30000).length;
  aliveCountLast = aliveCount;
  elConsumers.textContent = String(aliveCount);
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
  updateUi();

  const id = snap?.consumer ?? "consumer";
  consumers.set(id, { lastSeenMs: performance.now(), lastSnapshot: snap });
  layoutTurbines();
}

async function getConfig() {
  const res = await fetch("/api/config");
  if (!res.ok) throw new Error(`config http ${res.status}`);
  return await res.json();
}

function connectSseFanout() {
  if (!consumerBaseUrl) return;
  const base = consumerBaseUrl.replace(/\/$/, "");
  const url = `${base}/api/stream`;

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
        startPolling();
      }
    };
  }

  if (sources.length === 0) {
    setMode("polling");
    startPolling();
    return;
  }
}

let polling = false;
async function startPolling() {
  if (polling || !consumerBaseUrl) return;
  polling = true;
  const url = `${consumerBaseUrl.replace(/\/$/, "")}/api/snapshot`;

  while (polling) {
    try {
      const res = await fetch(url, { cache: "no-store" });
      if (res.ok) {
        onSnapshot(await res.json());
      }
    } catch {
      // ignore transient errors
    }
    await new Promise((r) => setTimeout(r, 250));
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
  connectSseFanout();
  requestAnimationFrame(animate);
}

main().catch(() => setMode("error"));

