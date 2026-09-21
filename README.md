# Water0 - Hydration Tracker

Offline-first Android hydration tracker with a liquid-glass UI. No account, no tracking — your data never leaves the phone unless you export it yourself.

## Features (v0.2)
- Hero tumbler tank — pours in on log, tips right and spills on delete, unmixed drink layers, foam cap, splash drops
- Ambient mega-tank behind every tab, gliding to a pose per tab
- 4-tab glass shell (Home / Update / Logs / Settings): floating top lenses, full-width dock with a jelly blob that trails your finger, swipe or hold-and-scrub to navigate
- Time-of-day goal judgment — chugging the whole day at 1am warns instead of celebrating
- Range recommendations ("drink around 500 ml"); tapping one opens Update with the amount prefilled
- Backdated logging (Now / 1h ago / 3h ago / pick a time) + quick-add drops sized by amount + custom ml
- Logs with per-day cards and a sliding glass 7d/14d/30d switch; working delete everywhere
- Settings: typed weight entry, goal breakdown, opt-in reminders (permission asked only when enabling), Glass Lab (blur/tint/bevel, live), training-data export
- Pitch-black dark mode and white light mode, each with its own living background (lifespan glow orbs + rising bubbles that spawn, drift, and die — nothing loops)
- TV Girl tinted lenses (blue top bar, pink dock), `>.<` launcher icon and stats quips

## Tech Stack
- Kotlin, Jetpack Compose, Material3
- Room Database, DataStore, WorkManager
- Manual DI via AppContainer (Hilt returns in v0.2)
- JUnit4 + MockK unit tests, Compose UI tests, GitHub Actions CI
- Python (offline): NHANES grounding + personal-data retraining, optional TFLite export — see `ml_training/`

## Building
```bash
./gradlew assembleDebug
```

## Machine learning (Python, offline)

`ml_training/` holds the Python side, grounded in real data: CDC NHANES
2017-2018 (4,931 adults) calibrates the goal formula, and your own
opt-in app export (Settings → Your data) retrains personal models
(scikit-learn baselines + optional Keras → TFLite export for future
on-device use). The Android app itself stays fully offline.

```bash
cd ml_training
pip install -r requirements.txt
python load_nhanes.py
python train_model.py --data ~/water0-training-*.csv --out-dir model_personal
```

See `ml_training/README.md` for the pipeline, Android integration plan,
and methodology notes.

## License
Apache 2.0 - see LICENSE file