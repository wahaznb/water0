# hy2ox1 Codemaps — Index

**Last Updated:** 2026-10-11
**Entry Points:** `app/src/main/java/com/water0/hydration/Water0Application.kt`, `app/src/main/java/com/water0/hydration/presentation/MainActivity.kt`
**Project:** Water0 hydration tracker — offline-first Android, Kotlin + Compose + Room, manual DI, minSdk 26, version `0.3-beta` (versionCode 4)

## Map

| Codemap | Contents |
|---------|----------|
| [ARCHITECTURE.md](ARCHITECTURE.md) | High-level system overview, component relationships, data flow |
| [MODULES.md](MODULES.md) | Per-module purpose, key files, dependencies, exports, usage |
| [FILES.md](FILES.md) | Directory tree, file purposes, key files, tests, non-Android areas |

## Quick Orientation

```
com.water0.hydration
├── presentation/    # Compose screens (home, history, settings, onboarding)
│   ├── home/        # HomeScreen, LogScreen, AmbientTank, components
│   ├── history/     # Logs + week graph (HistoryScreen, HistoryViewModel)
│   ├── settings/    # Profile, Glass Lab, Fitness, export
│   ├── health/      # HealthRationaleActivity (Health Connect contract)
│   ├── navigation/  # BottomNav custom dock
│   └── notification/ # Worker chain, shade rendering, quick-log receiver
├── domain/          # RecommendationEngine + one-verb use cases
├── data/            # Room entities/DAOs, repository, TFLite forecast, fitness
├── di/              # Manual AppContainer (no DI framework; Hilt disabled)
└── ui/theme/        # Liquid-glass theme, lenses, living background
```

Single-activity MVVM: Compose UI → ViewModels → use cases → Room
repository observed as Flows. Manual DI via `AppContainer` (`di/DatabaseModule.kt`).

## Verified Facts (from source, 2026-10-11)

- Single launcher activity: `presentation.MainActivity` (manifest `MAIN`/`LAUNCHER`).
- `Water0Application` creates two notification channels (`STATUS_CHANNEL_ID` silent, `CHANNEL_ID` buzz) and mints a per-process `bg_seed` pref.
- Room `HydrationDatabase` v4, 3 entities, 3 DAOs, migrations 1→2 (sex), 2→3 (ageYr), 3→4 (responses/lastReminderMs).
- 7 one-verb use cases in `domain/usecase/`; pure `RecommendationEngine` in `domain/engine/`.
- `assets/sequence_tcn.tflite` consumed by `data/ml/OnDeviceForecast.kt`.
- Health Connect is optional: `data/fitness/FitnessDataSource.kt` + `HcAppContract.java`, `READ_SLEEP`/`READ_EXERCISE` permissions, rationale activity in manifest.
- Empty placeholder packages exist and contain no sources: `data/preferences/`, `domain/model/`.
- Tests: 11 unit-test files under `app/src/test/`, 1 Compose test under `app/src/androidTest/`.
- Offline ML sidecar: `ml_training/` (8 Python scripts, `requirements.txt`).

## Related Docs

- `README.md` — features, architecture sketch, build/run
- `docs/RELEASE.md` — release notes
- `design/GLASS.md` — glass design language
- `ml_training/README.md`, `DATA_SOURCES.md`, `LEARNING_GUIDE.md` — ML sidecar
- `learning-guide/` — phased walkthroughs (data layer, domain/home/settings, notifications/tests/ML)
