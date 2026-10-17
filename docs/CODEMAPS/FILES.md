# File Codemap

**Last Updated:** 2026-10-11
**Entry Points:** `app/src/main/java/com/water0/hydration/Water0Application.kt`, `app/src/main/java/com/water0/hydration/presentation/MainActivity.kt`
**Scope:** 56 main-source Kotlin/Java files + 11 unit tests + 1 androidTest; paths verified against `find app/src` on 2026-10-11.

## Directory Structure

```
app/src/
├── main/
│   ├── java/com/water0/hydration/
│   │   ├── Water0Application.kt
│   │   ├── data/
│   │   │   ├── fitness/      # FitnessDataSource.kt, HcAppContract.java
│   │   │   ├── local/        # HydrationDatabase.kt, dao/ x3, entity/ x3
│   │   │   ├── ml/           # OnDeviceForecast.kt
│   │   │   ├── preferences/  # (empty — placeholder)
│   │   │   └── repository/   # HydrationRepository.kt, HydrationRepositoryImpl.kt
│   │   ├── di/               # DatabaseModule.kt (AppContainer)
│   │   ├── domain/
│   │   │   ├── engine/       # RecommendationEngine.kt
│   │   │   ├── model/        # (empty — placeholder)
│   │   │   └── usecase/      # x7 use cases
│   │   ├── presentation/
│   │   │   ├── MainActivity.kt
│   │   │   ├── health/       # HealthRationaleActivity.kt
│   │   │   ├── history/      # HistoryScreen.kt, HistoryViewModel.kt
│   │   │   ├── home/         # HomeScreen.kt, HomeViewModel.kt, LogScreen.kt, components/ x8
│   │   │   ├── navigation/   # BottomNav.kt
│   │   │   ├── notification/ # NotificationWorker/Scheduler/Display, Boot/WaterLog receivers
│   │   │   ├── onboarding/   # OnboardingScreen.kt
│   │   │   └── settings/     # SettingsScreen.kt, SettingsViewModel.kt
│   │   └── ui/theme/         # AuroraBackground, Color, DesignTokens, GlassChrome/Config/Prefs,
│   │                         # GlassColors, GlassHelpers, LensCard, LiquidGlassTheme,
│   │                         # PressGlass, Type, liquidglass/LiquidGlass.kt
│   ├── assets/               # sequence_tcn.tflite
│   └── res/                  # drawable, font, layout, mipmap-*, values, xml
│       └── AndroidManifest.xml (in src/main/)
├── androidTest/java/com/water0/hydration/  # HomeScreenTest.kt
└── test/java/com/water0/hydration/          # 14 unit-test files
ml_training/  # generate_data, train_model/sequence/tcn/nhanes, convert_sequence,
              # simulate_coach, load_nhanes, requirements.txt, data/, model/, reports/
```

## File Purposes

### Shell + DI (3)

| File | Purpose |
|------|---------|
| `Water0Application.kt` | App init: 2 notification channels + per-launch `bg_seed` |
| `presentation/MainActivity.kt` | Single activity: pager nav, dock, snackbar, theme host |
| `di/DatabaseModule.kt` | `AppContainer` manual-DI singleton graph |

### Data — local (8)

| File | Purpose |
|------|---------|
| `data/local/HydrationDatabase.kt` | Room v4 `water0_database` + migrations 1→2/2→3/3→4 + `Converters` |
| `data/local/dao/HydrationEntryDao.kt` | Entry CRUD + today/for-date/range Flows + effective-total SQL |
| `data/local/dao/UserProfileDao.kt` | Singleton profile row access |
| `data/local/dao/UserBehaviorDao.kt` | Singleton behavior row access |
| `data/local/entity/HydrationEntry.kt` | Log row + `DrinkType` factors + `EntrySource` |
| `data/local/entity/UserProfile.kt` | Profile row + goal-factor enums + `baseWaterNeedMl` |
| `data/local/entity/UserBehavior.kt` | Behavior row + `updateOnLog`/`onReminderShown` |

(_8th file: `Converters` lives inside `HydrationDatabase.kt`. DAO+entity+DB files total 7 + `AppContainer` consumer wiring in repository._)

### Data — repository / ml / fitness (5)

| File | Purpose |
|------|---------|
| `data/repository/HydrationRepository.kt` | Data-seam interface (entries, profile, behavior, fitness) |
| `data/repository/HydrationRepositoryImpl.kt` | DAO delegation + behavior + manual-workout flag |
| `data/ml/OnDeviceForecast.kt` | TFLite advisor over 7 daily totals |
| `data/fitness/FitnessDataSource.kt` | Optional Health Connect sleep/workout reads + `SleepWindow` |
| `data/fitness/HcAppContract.java` | Health Connect contract helper (only `.java` in main) |

### Domain (8)

| File | Purpose |
|------|---------|
| `domain/engine/RecommendationEngine.kt` | Pure goal/status/plan/nudge math |
| `domain/usecase/LogHydrationUseCase.kt` | Insert log (supports backdating) |
| `domain/usecase/GetTodayProgressUseCase.kt` | Today `HydrationStatus` Flow |
| `domain/usecase/GetHistoryUseCase.kt` | Ranged history + goal-met Flow |
| `domain/usecase/CalculateRecommendationUseCase.kt` | Range recommendations |
| `domain/usecase/DeleteHydrationUseCase.kt` | Delete entry |
| `domain/usecase/ExportTrainingDataUseCase.kt` | 90-day training CSV export |
| `domain/usecase/ExportDrinkWindowUseCase.kt` | Drink-window CSV export |

### Presentation — home (11)

| File | Purpose |
|------|---------|
| `presentation/home/HomeScreen.kt` | Dashboard frame, hero card, recs section |
| `presentation/home/HomeViewModel.kt` | Home `UiState` + log/delete/rec intents |
| `presentation/home/LogScreen.kt` | Drink-type + amount + backdate logging |
| `presentation/home/components/TankMath.kt` | Tumbler geometry/level math |
| `presentation/home/components/PaceTimeline.kt` | Week pace timeline card |
| `presentation/home/components/TimelineMath.kt` | Week/window/guide/cursor math |
| `presentation/home/components/QuickAddButtons.kt` | Quick-add drops + custom ml |
| `presentation/home/components/AddWaterDialog.kt` | Amount dialog |
| `presentation/home/components/RecommendationCard.kt` | Range-rec card |
| `presentation/home/components/DrinkColors.kt` | Drink colors/layers |
| `presentation/home/components/EntryListItem.kt` | Log row |

### Presentation — rest (11)

| File | Purpose |
|------|---------|
| `presentation/history/HistoryScreen.kt` | Logs + 7-day graph UI + VM factory |
| `presentation/history/HistoryViewModel.kt` | 7/14/30d history state |
| `presentation/settings/SettingsScreen.kt` | Profile/Glass/Fitness/data/permission UI + VM factory |
| `presentation/settings/SettingsViewModel.kt` | Settings state + export + prefs |
| `presentation/onboarding/OnboardingScreen.kt` | First-run body/schedule/reminder flow |
| `presentation/navigation/BottomNav.kt` | Custom bottom dock |
| `presentation/notification/NotificationWorker.kt` | Reminder/status work + reschedule |
| `presentation/notification/NotificationScheduler.kt` | `ensureScheduled/poke/cancel` |
| `presentation/notification/NotificationDisplay.kt` | Shade rendering (`showStatusNotifications`) |
| `presentation/notification/BootReceiver.kt` | Post-reboot reschedule |
| `presentation/notification/WaterLogReceiver.kt` | Shade +250 quick-log |
| `presentation/health/HealthRationaleActivity.kt` | Health Connect rationale screens |

### Theme (13)

| File | Purpose |
|------|---------|
| `ui/theme/LiquidGlassTheme.kt` | `MaterialTheme` wrapper |
| `ui/theme/GlassColors.kt` | Dark/light glass color schemes |
| `ui/theme/GlassHelpers.kt` | Glass/dock/card-style helpers |
| `ui/theme/AuroraBackground.kt` | Living background: bubbles/orbs/jellyfish |
| `ui/theme/LensCard.kt` | Refractive lens card + composition locals |
| `ui/theme/liquidglass/LiquidGlass.kt` | `LiquidGlassBox` renderer |
| `ui/theme/GlassChrome.kt` | Glass snackbar |
| `ui/theme/GlassConfig.kt` | Lens config model |
| `ui/theme/GlassPrefs.kt` | Glass prefs persistence |
| `ui/theme/DesignTokens.kt` | Spacing/radii/icon/section/card tokens |
| `ui/theme/Color.kt` | `Water0` palette |
| `ui/theme/Type.kt` | Typography |
| `ui/theme/PressGlass.kt` | Press-interaction modifier |

### Tests (12)

Unit (`app/src/test/java/com/water0/hydration/`): `AbsorbedMlTest`, `DayPlanTest`, `ExportDrinkWindowUseCaseTest`, `ExportTrainingDataUseCaseTest`, `FitnessDataSourceTest`, `HomeViewModelTest`, `LiquidGlassMappingTest`, `RecommendationEngineTest`, `UseCaseTest`, `UserProfileSexTest`, `FakeHydrationRepository` (helper). UI (`app/src/androidTest/`): `HomeScreenTest`.

### Non-Android areas

| Path | Purpose |
|------|---------|
| `ml_training/*.py` (8) | Offline simulator + trainers (`generate_data`, `train_model/sequence/tcn/nhanes`, `convert_sequence`, `simulate_coach`, `load_nhanes`) → `model/` → `assets/sequence_tcn.tflite` |
| `ml_training/{DATA_SOURCES,LEARNING_GUIDE,README}.md`, `nhanes_*.json`, `data/`, `reports/` | Sources, calibration metrics, datasets |
| `design/GLASS.md` | Glass design language |
| `learning-guide/` | Phased HTML walkthroughs + `DEVLOG.md` + interview refs |
| `skills/glass-ui-verification` | Glass UI check skill |
| `scripts/verify.sh` | `./gradlew :app:testDebugUnitTest :app:assembleDebug` wrapper |
| `gradle/libs.versions.toml` | Version catalog (single source for dep versions) |
| `docs/RELEASE.md` | Release notes |

## Key Files (start here)

1. `presentation/MainActivity.kt` — UI root
2. `di/DatabaseModule.kt` — dependency graph
3. `data/repository/HydrationRepository.kt` — data seam
4. `domain/engine/RecommendationEngine.kt` — core math
5. `data/local/HydrationDatabase.kt` — schema + migrations
6. `presentation/notification/NotificationWorker.kt` — reminder brain
7. `gradle/libs.versions.toml` — dependency versions

## Related Areas

- [INDEX.md](INDEX.md) — orientation
- [ARCHITECTURE.md](ARCHITECTURE.md) — data flow + dependencies
- [MODULES.md](MODULES.md) — module APIs + usage
