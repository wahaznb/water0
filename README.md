# Water0 - Hydration Tracker

Open-source Android app for tracking hydration with personalized recommendations.

## Features (v1.0)
- Log water intake with quick-add buttons (water, coffee, tea, juice, soda, alcohol, other)
- Daily progress ring with personalized goal (weight × activity × climate)
- Smart recommendations (morning nudge, catch-up, caffeine offset, hot weather, streaks)
- History with per-day totals, 7/14/30-day ranges, and delete
- Settings: profile, goal breakdown, reminders, quiet hours, units
- Adaptive reminder notifications (WorkManager, intervals adapt to progress and responsiveness)
- Local SQLite storage (Room)
- Offline-first, no tracking, no account

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