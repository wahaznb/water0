# Learning Guide: Training Water0's Models (Real Data)

Hands-on companion to `README.md`. Goal: you run every step yourself and
understand what each number means. Budget ~1 hour. Environment: your own
`.venv` (Python 3.11), built in Step 1.

## Step 0 — Orient (5 min)

Read these two files first — everything else hangs off them:

- `generate_data.py` — the synthetic simulator. Note `daily_goal()`
  (31/33 ml/kg × activity + climate, the app's formula), `HOURLY_P`
  (when people drink), and `discipline` (per-user fraction of need).
  Every row it emits is one user-day.
- `train_model.py` — `FEATURES` is the model's entire worldview: only
  those columns exist for it. `split()` is BY USER (`GroupShuffleSplit`)
  so no user appears in both train and test — otherwise the model just
  memorizes personal habits and test scores lie.

Key vocabulary: **MAE** (mean absolute error, average miss in ml),
**R²** (fraction of variance explained; 1.0 = perfect, 0.0 = "just guess
the mean", negative = worse than guessing the mean), **accuracy** on the
met-goal classifier (always compare against the *majority baseline*: the
score of always predicting the commoner class).

## Step 1 — Environment (5 min)

```bash
cd ml_training
uv venv .venv --python 3.11
uv pip install --python .venv/bin/python -r requirements.txt
.venv/bin/python -c "import sklearn, pandas; print(sklearn.__version__)"
# No uv? Plain pip works too (the repo-root `.venv` already has
# everything, TF included): `python -m pip install -r requirements.txt`
```

Why not system Python: ours is 3.14 and sklearn has no build for it.
Why pinned versions in `requirements.txt`: unpinned resolves once
drifted into a scipy/numpy import hang. Versions move as a set here —
same rule as the Android side (AGP/Kotlin/compose compiler).

## Step 2 — Generate synthetic data (5 min)

```bash
.venv/bin/python generate_data.py --users 200 --days 60 --seed 42 --out data/hydration_logs.csv
head -n 3 data/hydration_logs.csv
```

Watch the `goal-met rate` (≈0.33–0.35). That number is a calibration
anchor, not a trophy: NHANES population truth is ≈0.28–0.30 (see
`nhanes_calibration.json`). If yours reads ~0.47, your
`generate_data.py` predates the discipline fix (0.90 → 0.80).

Things to try: `--seed 7` (numbers wobble — single-split metrics are
noisy), `--users 50` (small data, worse scores — feel the data hunger).

## Step 3 — Train the baselines (10 min)

```bash
.venv/bin/python train_model.py --data data/hydration_logs.csv --out-dir model
cat model/metrics.json
```

Baselines (seed 42): MAE ≈ 555 ml, R² ≈ 0.22, accuracy ≈ 0.75.
Interpretation that matters: habits explain a fraction of tomorrow's
intake; the rest is irreducible daily randomness.
That limitation is the whole product argument — it is why the app
blends `0.5 × rules + 0.5 × model` instead of trusting the model.

## Step 3b — Noise: humans estimate (5 min)

Re-read your `metrics.json`: alongside every clean number sits a
`_noisy` twin (MAE 568, accuracy 0.74). Those come from the
augmentation in `main()`: every train row gets a jittered twin
(±150 ml Gaussian, snapped to 50 ml grids — the round numbers people
actually type), labels recomputed. Try it:

```bash
.venv/bin/python train_model.py --noise-ml 0  # exact numbers only
```

Watch clean MAE stay ~555 while the (unreported now) robustness story
vanishes: without noisy twins you can't tell memorization from
habits. The 13-ml clean/noisy gap is the number that says the model
learned shapes, not digits.

Note what trains in seconds on CPU: `HistGradientBoosting` on 12k rows
× 12 features. The TFLite path (optional, needs `pip install
tensorflow`, Python ≤ 3.11) compares 3 MLP sizes × 3 quantization
levels and exports the smallest model within 5% of the best MAE under
a hard 100 KB budget.

## Step 4 — Real data: NHANES (20 min, the important one)

```bash
.venv/bin/python load_nhanes.py --out data/nhanes_daily.csv
cat nhanes_calibration.json
```

What the loader does: downloads CDC 2017-2018 `.XPT` files (diet recall
+ demographics + body measures + activity), merges on `SEQN`, keeps
reliable adult recalls (n=4,931), maps columns onto app concepts.
Variable names were verified against the official CDC codebook
(`DR1_320Z` plain water, `DR1TMOIS` total moisture, `DR1DRSTZ`,
`DR1DAY`, `RIAGENDR`, `BMXWT`) — never trust remembered variable
names; the loader's header comment says which ones are verified.

Two traps it handles, learn both:
1. **CDC returns HTTP 200 with a "Page Not Found" HTML page** for wrong
   URL casing. The loader validates the XPORT magic bytes
   (`HEADER RECORD`) and fails loud instead of parsing HTML.
2. **Compare like with like.** Plain water alone understates intake
   ~40% (food moisture excluded). The fair comparator for the
   simulator's totals is `DR1TMOIS` (all moisture): NHANES 2,870 ml/d
   vs simulator ~2,650 — within ~10%. Plain-vs-total comparison would
   "prove" the simulator wrong by construction.

## Step 5 — The negative result (10 min, do not skip)

```bash
.venv/bin/python train_nhanes.py --data data/nhanes_daily.csv
cat nhanes_model_metrics.json
```

Result: R² **−0.08**, accuracy below majority baseline. The model
learned nothing; only `age_yr` carries signal (see permutation
importance). This is a finding, not a failure, and it teaches three
things:

1. **Baselines first.** Without the mean/majority baselines printed
   beside the metrics, R² −0.08 looks like "a number" instead of
   "worse than guessing".
2. **One day's intake is day noise.** What someone drank is dominated
   by what they ate and which weekday got recalled — demographics
   can't see any of that.
3. **The sequence task needs sequences.** Everything that makes the
   simulator model work (`prev_day_total_ml`, `avg_7d_ml`,
   `streak_days`) structurally doesn't exist in single-recall data.
   That's why the NHANES model is deliberately not shipped, and why
   the only dataset that can ever train this task is longitudinal —
   the app's own opt-in export (Settings → Your data).

## Step 6 — Your own data (ongoing)

Log in the app daily → Settings → Your data → Export training CSV →
`python train_model.py --data <export> --out-dir model_personal`.
Single-user files auto-switch to a time split (most recent 20% of days
held out — the future is what's tested). Under ~100 days expect noisy
metrics; the value is personal calibration, not beating population
baselines.

## Experiments checklist

- [ ] Drop `goal_ml` from `FEATURES` in a copy of `train_model.py`
      (it leaks the formula's opinion into features). Watch R².
- [ ] `--seed 7` vs `--seed 42`: feel single-split noise.
- [ ] Halve `--users`: feel data hunger.
- [ ] `train_nhanes.py` with only `age_yr`: confirm the rest was dead
      weight (R² unchanged).
- [ ] Read a `DaySummary` → export-CSV row trace for one of your own
      logged days and verify streak/prev/avg by hand.
