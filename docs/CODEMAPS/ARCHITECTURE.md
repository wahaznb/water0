# Architecture Codemap

**Last Updated:** 2026-10-11
**Entry Points:** `app/src/main/java/com/water0/hydration/Water0Application.kt`, `app/src/main/java/com/water0/hydration/presentation/MainActivity.kt`

## Architecture

Single-activity MVVM, offline-first, no network permission. Compose screens
observe `Flow`s from ViewModels; ViewModels call one-verb use cases; use
cases call `HydrationRepository`; the repository fronts Room DAOs and
optional fitness/ML plugins. DI is a hand-written singleton container
(`AppContainer` in `di/DatabaseModule.kt` — Hilt is commented out in
`app/build.gradle.kts`).

```
┌──────────────┐   Flows/State   ┌─────────────┐  invoke  ┌──────────────┐  calls  ┌──────────────┐
│  Compose UI  │◄────────────────│ ViewModels  │─────────►│  Use Cases   │────────►│  Repository  │
│  Screens +   │                 │ Home/       │          │  7 one-verb  │         │  Impl + iface│
│  components  │────────────────►│ History/    │          │  classes     │         │              │
└──────────────┘   user events   └─────────────┘          └──────────────┘         └──────┬───────┘
                                                                                        │
                                                                     ┌──────────────────┼──────────────────┐
                                                                     ▼                  ▼                  ▼
                                                              ┌─────────────┐  ┌────────────────┐  ┌───────────────┐
                                                              │ Room (v4)   │  │ Fitness plugin │  │ TFLite advisor│
                                                              │ 3 entities  │  │ Health Connect │  │ sequence_tcn  │
                                                              │ 3 DAOs      │  │ optional/null  │  │ advises only  │
                                                              └─────────────┘  └────────────────┘  └───────────────┘

Notification side-chain (WorkManager, self-perpetuating):
  NotificationScheduler.ensureScheduled/poke ─► NotificationWorker.doWork
    ─► NotificationDisplay.showStatusNotifications ─► status shade + reminder buzz
  BootReceiver (re-schedule after reboot) · WaterLogReceiver (+250 ml quick-log from shade)

Fitness side-input (never throws, null/false when unavailable):
  FitnessDataSource ─► HydrationRepository.getLastNightSleep/hadRecentWorkout
  + manual-workout flag fallback (Prefs, not Room) for degoogled phones
```

## Key Modules

| Module | Purpose | Exports | Dependencies |
|--------|---------|---------|--------------|
| `presentation/` | Single activity, 4 screen areas, notification chain | `MainActivity`, `HomeScreen`, `LogScreen`, `HistoryScreen`, `SettingsScreen`, `OnboardingScreen`, `BottomNav` | ViewModels → `AppContainer` use cases; theme |
| `domain/engine/` | Pure hydration math + plan/schedule helpers | `RecommendationEngine` (`DailyGoal`, `HydrationStatus`, `SipSlot`, `dayStartMillis`, `windowSpanHours`, `recommendedBedtime`, `sipPlan`) | Entities only (no Android) |
| `domain/usecase/` | One-verb operations (7) | `LogHydration`, `GetTodayProgress`, `GetHistory`, `CalculateRecommendation`, `DeleteHydration`, `ExportTrainingData`, `ExportDrinkWindow` | `HydrationRepository`, `RecommendationEngine` |
| `data/local/` | Room v4: entities, DAOs, DB + migrations | `HydrationDatabase`, `MIGRATION_1_2/2_3/3_4`, `Converters`, 3 entities, 3 DAOs | Room, entities |
| `data/repository/` | Repository interface + impl, behavior + fitness wiring | `HydrationRepository`, `HydrationRepositoryImpl` | DAOs, `FitnessDataSource`, prefs/appContext |
| `data/ml/` | On-device TFLite advisor | `OnDeviceForecast` | `assets/sequence_tcn.tflite`, TFLite 2.15.0 |
| `data/fitness/` | Optional Health Connect reads | `FitnessDataSource`, `SleepWindow`, `HcAppContract` | Health Connect client 1.1.0 |
| `di/` | Manual DI container | `AppContainer` (getDatabase/Repository/Engine/UseCases/Scheduler) | Everything above |
| `ui/theme/` | Liquid-glass design system | `LiquidGlassTheme`, `LensCard`, `GlassConfig`, `GlassPrefs`, `AuroraBackground`, tokens | Compose Material3 |
| `ml_training/` | Offline Python sidecar (not shipped) | `generate_data.py`, `train_model.py`, `train_sequence.py`, `train_tcn.py`, `train_nhanes.py`, `convert_sequence.py`, `simulate_coach.py`, `load_nhanes.py` | scikit-learn, Keras → TFLite |

## Data Flow

1. **Log:** `LogScreen`/quick-add → `HomeViewModel` → `LogHydrationUseCase` → `repository.insertEntry` → Room; `onWaterLogged` updates `UserBehavior`; `NotificationScheduler.poke()` refreshes shade within seconds.
2. **Observe:** `GetTodayProgressUseCase` combines `getTodayEntries()` + `getUserProfile()` Flows with `RecommendationEngine` → `HydrationStatus` → `HomeScreen` (tank, pace curve, verdict).
3. **History:** `GetHistoryUseCase` ranges entries per day, marks goal-met dots → `HistoryScreen` 7/14/30d.
4. **Nudge:** `NotificationWorker` reads profile + progress + behavior + sleep/workout, asks engine for banded range (±20% snapped to 50s), shows silent status always, buzzes only on real nudge and outside quiet hours; then `scheduleNext` re-enqueues (self-perpetuating chain).
5. **Export:** `ExportTrainingDataUseCase` / `ExportDrinkWindowUseCase` emit 90-day simulator-schema CSV (opt-in, Settings → Your data).
6. **ML advise:** `OnDeviceForecast` reads 7 daily totals → one adjustment number; rules decide (MAE 484 ml vs 555 aggregates, 7.6 KB model).

## External Dependencies

- Jetpack Compose BOM 2024.06.00 + Material3 1.2.1 + activity-compose 1.8.2 — UI
- Room 2.6.1 (runtime/ktx/compiler) — persistence
- Lifecycle 2.7.0, core-ktx 1.12.0, coroutines 1.7.3 — async/state
- WorkManager 2.9.0 — reminder chain
- DataStore preferences 1.0.0 + SharedPreferences (`water0_prefs`, `GlassPrefs`) — settings
- TensorFlow Lite 2.15.0 — on-device advisor
- Health Connect client 1.1.0 — optional workouts/sleep (API 34+)
- JUnit 4.13.2 + MockK 1.13.12 + coroutines-test — unit tests; Espresso 3.5.1 + Compose ui-test — UI test
- AGP 8.9.1, Kotlin 1.9.23, compile/targetSdk 36, minSdk 26 — toolchain

No network stack: manifest requests no `INTERNET`; permissions are `POST_NOTIFICATIONS`, `RECEIVE_BOOT_COMPLETED`, `health.READ_SLEEP/READ_EXERCISE` only.

## Related Areas

- [Module Map](MODULES.md) — per-module files, exports, usage
- [File Map](FILES.md) — directory tree and test inventory
- [Index](INDEX.md) — orientation + verified facts
