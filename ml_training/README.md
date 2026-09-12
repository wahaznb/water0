# Water0 ML Training (Python)

Offline model training for Water0's on-device AI (v0.2). The Android app
stays fully offline: models are trained here, exported to TFLite, and the
resulting `.tflite` file is copied into `app/src/main/assets/`.

## Pipeline

```
generate_data.py  →  data/hydration_logs.csv  →  train_model.py  →  model/
   (synthetic,          (one row per               (sklearn baselines
    mirrors the          user-day)                   + optional TFLite)
    app's formula)
```

The simulator uses the **same goal formula as the app's
`RecommendationEngine`** (`31/35ml × weight × activity + climate bonus`,
by sex), so models stay consistent with rule-based v1.

> **Data honesty:** the training CSV is synthetic — no public dataset of
> timestamped personal drink logs exists. Real grounding comes from
> NHANES/WWEIA (population intake stats) and Kaggle hydration sets; see
> `DATA_SOURCES.md` for links and how each source is (or will be) used.

## Real-data grounding (NHANES)
`load_nhanes.py` downloads CDC NHANES 2017-2018 files, builds
per-person daily totals + demographics (`data/nhanes_daily.csv`), and
calibrates the simulator (`nhanes_calibration.json`):

```bash
python load_nhanes.py --out data/nhanes_daily.csv
python load_nhanes.py --no-download   # reuse cached data/nhanes_raw/*.XPT
```

One recall per person means no sequences — so NHANES grounds the
simulator, it doesn't replace it. Current result (n=4,931 adults):
total-water mean 2,870 ml/d vs simulator 2,687 ml/d. Details in
`DATA_SOURCES.md`.

Direct training was also tried (`train_nhanes.py`, cross-sectional
task): R² −0.08, i.e. single-recall demographics can't predict intake —
day noise dominates and habit features don't exist in that data. The
NHANES model is deliberately not shipped; the simulator remains the
trainer. See `DATA_SOURCES.md` for the full negative result.

## Personal data (your own longitudinal logs)

The app exports opt-in training rows (Settings → Your data → Export
training CSV): 90 days in this pipeline's schema, streaks pre-update
(no leakage), profile snapshot documented per row. Retrain on it:

```bash
python train_model.py --data ~/water0-training-20260913.csv --out-dir model_personal
```

Single-user CSVs automatically get a time split (most recent 20% of
days held out) instead of the group split — the future is what's
tested. Expect noisy metrics under ~100 days; the value is personal
calibration, not beating the population baselines.

## Setup

```bash
cd ml_training
python -m venv .venv && source .venv/bin/activate
pip install -r requirements.txt
# optional, only for the TFLite export step:
# pip install tensorflow
```

## Run

```bash
python generate_data.py --users 200 --days 60 --seed 42
python train_model.py --data data/hydration_logs.csv --out-dir model
```

Expected output (defaults, seed 42): regression MAE ≈ 507 ml at R² ≈ 0.35,
goal-met accuracy ≈ 0.77 (majority baseline ≈ 0.66). Exact numbers vary
with `--seed`. Day-to-day intake is deliberately noisy — the model
captures *habit-level* differences between users; the rest is irreducible
daily randomness. That limitation is itself the point: it motivates
blending the model with the rule engine rather than trusting it blindly.

## What gets trained

Same features the app knows at prediction time (`weight_kg`,
`activity`, `climate`, `sex`, `day_of_week`, `is_weekend`, `goal_ml`,
`prev_day_total_ml`, `avg_7d_ml`, `streak_days`, …):

| Task | Target | Model |
|------|--------|-------|
| Regression | tomorrow's total intake (ml) | HistGradientBoosting → model selection over tiny MLPs → `hydration_goal.tflite` |
| Classification | will the user meet their goal (0/1) | HistGradientBoosting (stays server-side/offline analysis) |

## Model selection (no performance sacrificed)

`train_model.py` compares 3 MLP sizes (tiny 16-8 / small 32-16 / base
64-32) × 3 quantization levels (dynamic / float16 / full-int8) and
exports the winner to `model/`:

- **Winner rule:** smallest model within 5% of the best test MAE.
- **Hard budget:** must be ≤ 100 KB (typical winner: int8, ~10–30 KB).
- Each candidate reports test MAE, size, and per-inference latency to
  `model/model_selection.json`.
- Full-int8 uses a representative dataset so mobile DSPs/NPUs can run it;
  `model/scaler.json` stores the preprocessing the app must replicate.

Needs `pip install tensorflow` (Python ≤ 3.11 recommended) — without it
the script trains/evaluates the sklearn baselines and skips export.

The split is **by user** (`GroupShuffleSplit`): no user appears in both
train and test, otherwise the model just memorizes personal habits and
test scores lie.

## Android integration (v0.2)

1. Copy `model/hydration_goal.tflite` + `model/scaler.json` into `app/src/main/assets/`.
2. Load with the TensorFlow Lite Task Library / Interpreter API.
3. Apply the saved scaler (`(x − mean) / scale`) before inference.
4. Blend with the rule engine: `final = 0.5 × rule + 0.5 × model`, then clamp to [1000, 5000] ml.

## Talking about this (interviews)

- *Why synthetic data?* No real user data exists yet; the simulator encodes domain knowledge (hourly drink probabilities, weekend dip, personal discipline) and the app's own goal formula.
- *Why split by user?* Random row splits leak a user's habits into test → inflated scores. Group split measures generalization to *new* users.
- *Why TFLite?* On-device inference: works offline, no latency, no privacy risk — matches the app's offline-first promise.
- *Why keep the rule engine?* Transparent fallback when the model is uncertain; the model refines, rules guarantee sane bounds.
