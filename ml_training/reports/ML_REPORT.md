# Water0 ML report: coaching effect + live model (2026-10-04)

Question: does the app's coaching actually move goal-hit rates, and does
the on-device model earn its place? Measured on this machine, today,
after the post-migration venv rebuild (Python 3.14, numpy/pandas/sklearn
fresh; TensorFlow unavailable — Keras retraining not re-run).

## 1. Coach simulation: with AI vs without (`simulate_coach.py`)

Paired design — same synthetic users live both arms, so inter-user
variance cancels. The coached arm runs the app's real rules, not a
strawman:

- goal: `RecommendationEngine.calculateDailyGoal` (per-kg × activity +
  climate, by sex)
- nudge sizing: `suggestSipMl` (250 + (gap−1)×75, snapped to 50s, 150–500)
- gating: `recencySuggestion` (behind prorated pace, gap ≥ 45 min,
  active window) + morning start (250 ml if < 250 by wake+2)
- cadence: `calculateNextReminderInterval` (60 min base; ×0.9 if
  response > 0.7; ×1.5 if < 0.3; ×0.7 behind; ×1.3 ahead; quiet skip)
- habits: `generate_data.py` hourly probs, weekend dip, NHANES-calibrated
  discipline, day noise

The one free parameter is **responsiveness** — P(a nudge becomes a
drink). It is assumed, not measured, so we sweep it and report the
range. 300 users × 60 days × 3 seeds per cell:

| responsiveness | control met | coached met | lift | nudges/day |
|---|---|---|---|---|
| 0.2 (skeptic) | 0.391 | 0.536 | **+14.5pp** | 5.8 |
| 0.5 (mid) | 0.393 | 0.762 | **+36.9pp** | 5.2 |
| 0.8 (eager) | 0.395 | 0.878 | **+48.3pp** | 4.2 |

Control ≈ 0.39 matches the generator's own ~0.34–0.47 band (consistent,
not tuned to impress). Reading: even skeptical users gain +14pp for
~6 nudges/day; the RANGE is the result, not a point. Eager users
saturate near 88% — the app can't beat bedtime.

Threats (stated, not buried): responsiveness is the whole effect and
it is assumed; no fatigue, habituation, or notification-blindness;
simulated users drink like the generator by construction. Real proof
needs the user's own export (pre/post with washout — a self-A/B is
impossible, so claim direction, not magnitude, until then).

Reproduce (from repo root): `cd ml_training && .venv/bin/python
simulate_coach.py --users 300 --days 60 --seeds 3` →
`ml_training/reports/coach_simulation.json` (committed).

## 2. Model zoo (filed metrics, unchanged)

| model | task | metric | ships? |
|---|---|---|---|
| sklearn HGB | tomorrow's intake | MAE 555 ml (568 noisy), R² 0.22 | no (server-side ref) |
| sklearn HGB | goal met y/n | acc 0.75 (0.74 noisy; majority 0.65) | no |
| sequence TCN | 7-day → tomorrow | MAE ≈ 484 (2026-09-22 run) | **yes, advisor** |
| aggregate MLP | tomorrow's intake | MAE 665+ | no — worse is not shipped |
| NHANES direct | intake from demographics | R² −0.08 | no — negative kept |

Day noise dominates single recalls; habit features (prev day, 7-day
avg, streak) carry all signal. Rules decide, models whisper.

## 3. Live model, measured today

- Integrity: `sha256(app/src/main/assets/sequence_tcn.tflite) ==
  sha256(ml_training/model/sequence_tcn.tflite)` =
  `e164c848…bb8962`. Shipped == trained, 7,800 bytes.
- I/O: `[1,7,1]` float32 in (7 daily totals ÷ 4000), `[1,1]` out. The
  app feeds exactly 7 floats — element counts match.
- Sanity (ai-edge-litert, XNNPACK): flat 2000s → 1967; rising
  1500→3200 → 2263; noisy week (mean 2411) → 2385. Smoothed-habit
  behavior, no surprises.
- Latency, 300 runs desktop CPU: mean 0.005 ms, p50 0.005, p99 0.012.
  Same order as the 2026-09-22 record (0.002 ms). Phone silicon will
  differ; the net is 7.8 KB — there is no regime where this matters.
- Role: advisor-only line in Settings, null-safe (`OnDeviceForecast`:
  any failure is silence, never a crash, never a driver).

## 4. Workable usecase (what "ML works" means here)

1. Population model ships today as a whisper (done, verified above).
2. Personal retraining unlocks at ~100 logged days: Settings → export
   CSV → `train_model.py --data <csv>` (auto time-split: recent 20%
   held out). Current user: 3 days — not yet, by the README's own rule.
3. Coaching effect is simulated (+14 to +48pp) pending real-data
   confirmation; notification load ~5/day at the mid cell.

## 5. F-Droid relevance

Inference is fully on-device (no network, no accounts). No app
dependencies were added: `ai-edge-litert` is a dev-bench tool only.
Simulator, report JSON, and this report commit as text; binaries,
venvs, and datasets stay gitignored.
