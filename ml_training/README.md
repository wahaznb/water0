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
`RecommendationEngine`** (`35ml × weight × activity + climate bonus`), so
models stay consistent with rule-based v1.

> **Data honesty:** the training CSV is synthetic — no public dataset of
> timestamped personal drink logs exists. Real grounding comes from
> NHANES/WWEIA (population intake stats) and Kaggle hydration sets; see
> `DATA_SOURCES.md` for links and how each source is (or will be) used.

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

Expected output (defaults): regression MAE ≈ 600 ml at R² ≈ 0.34,
goal-met accuracy ≈ 0.72 (majority baseline ≈ 0.62). Exact numbers vary
with `--seed`. Day-to-day intake is deliberately noisy — the model
captures *habit-level* differences between users; the rest is irreducible
daily randomness. That limitation is itself the point: it motivates
blending the model with the rule engine rather than trusting it blindly.

## What gets trained

Same features the app knows at prediction time (`weight_kg`,
`activity`, `climate`, `day_of_week`, `is_weekend`, `goal_ml`,
`prev_day_total_ml`, `avg_7d_ml`, `streak_days`, …):

| Task | Target | Model |
|------|--------|-------|
| Regression | tomorrow's total intake (ml) | HistGradientBoosting → optional 2-layer Keras MLP → `hydration_goal.tflite` |
| Classification | will the user meet their goal (0/1) | HistGradientBoosting |

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
