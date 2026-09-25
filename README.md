# Water0 - Hydration Tracker

Offline-first Android hydration tracker with a liquid-glass UI. No account, no tracking — your data never leaves the phone unless you export it yourself.

## Features (v0.2)
- Hero tumbler tank — pours in on log, red flash and level glide on delete, unmixed drink layers, foam cap
- Anchored ambient tank behind every tab (hero on Home, dimmed elsewhere — no travel, crossfade only)
- 4-tab glass shell (Home / Update / Logs / Settings): floating top lenses, full-width dock with a jelly blob that trails your finger, swipe or hold-and-scrub to navigate
- Time-of-day goal judgment — chugging the whole day at 1am warns instead of celebrating
- Range recommendations ("drink around 500 ml"); tapping one opens Update with the amount prefilled
- Stats panel pages (headline, breakdown, today's sips) — swipe the card or tap the dots
- Persistent status line ("500 ml behind pace", day progress bar, one-tap +250 log); buzzing reminder only with a real nudge
- Backdated logging (Now / 1h ago / 3h ago / pick a time) + quick-add drops sized by amount + custom ml
- Logs with per-day cards and a sliding glass 7d/14d/30d switch; working delete everywhere
- Settings: typed weight entry, goal breakdown, opt-in reminders (permission asked only when enabling), Glass Lab (blur/tint/dock, live), training-data export
- Pitch-black dark mode and white light mode, each with its own living background (lifespan glow orbs + rising bubbles that spawn, drift, and die — nothing loops)
- TV Girl tinted lenses (blue top bar, pink dock), `>.<` launcher icon and stats quips

## Tech Stack
- Kotlin, Jetpack Compose, Material3
- Room Database, DataStore, WorkManager
- Manual DI via AppContainer
- JUnit4 + MockK unit tests, Compose UI tests, GitHub Actions CI
- Python (offline): NHANES grounding + personal-data retraining, noise-robust training numbers, optional TFLite export — see `ml_training/`

## Building
```bash
./gradlew assembleDebug
```

## Machine learning (Python, offline)

`ml_training/` holds the Python side, grounded in real data: CDC NHANES
2017-2018 (4,931 adults) calibrates the goal formula, and your own
opt-in app export (Settings → Your data) retrains personal models.
The tiny temporal-conv net (`sequence_tcn.tflite`, 7.6 KB) ships in the
app as a Settings advisor line; the aggregate MLP stays unshipped.
The Android app itself stays fully offline.

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