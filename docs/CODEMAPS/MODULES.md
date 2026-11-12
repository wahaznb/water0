# Module Codemap

**Last Updated:** 2026-10-11
**Entry Points:** `Water0Application.kt`, `presentation/MainActivity.kt`
**Base package:** `com.water0.hydration` under `app/src/main/java/`

## App Shell

### App entry

**Purpose**: Process init — notification channels, per-launch background seed.

**Location**: `app/src/main/java/com/water0/hydration/`

**Key Files**:
- `Water0Application.kt` — creates `Hydration status` (LOW/silent) + `Hydration reminders` (DEFAULT/buzz) channels; mints random `bg_seed` into `water0_prefs` each process start

**Dependencies**: `presentation.notification.NotificationWorker` (channel IDs)

**Exports**:
- `Water0Application` — `Application` subclass (manifest `android:name`)
- `KEY_BG_SEED` — prefs key for the living-background seed

## DI

### Manual container

**Purpose**: Singleton graph without a DI framework (Hilt disabled in `app/build.gradle.kts`).

**Location**: `app/src/main/java/com/water0/hydration/di/`

**Key Files**:
- `DatabaseModule.kt` — `object AppContainer` with double-checked `getDatabase / getRepository / getRecommendationEngine / get*UseCase / getNotificationScheduler`

**Dependencies**: data.local, data.repository, domain.engine, domain.usecase, notification

**Exports**:
- `AppContainer` — the only DI surface; ViewModels/factories call `get*` directly

**Usage Example**:
```kotlin
val repo = AppContainer.getRepository(context)
val logUseCase = AppContainer.getLogHydrationUseCase(context)
```

## Data Layer

### Entities

**Purpose**: Room tables + hydration/profile math helpers.

**Location**: `app/src/main/java/com/water0/hydration/data/local/entity/`

**Key Files**:
- `HydrationEntry.kt` — `hydration_entries`; `DrinkType` (WATER 1.0, COFFEE 0.6, TEA 0.8, JUICE 0.9, SODA 0.5, ALCOHOL −0.5, OTHER 0.7), `EntrySource` (MANUAL/HEALTH_CONNECT), `effectiveHydrationMl`
- `UserProfile.kt` — `user_profile` (id=1); `Sex` (FEMALE 31 / MALE 33 ml/kg), `ActivityLevel` (×1.0–1.4), `Climate` (+0/200/500/800 ml), `baseWaterNeedMl`; nullable `ageYr` (null = skipped)
- `UserBehavior.kt` — `user_behavior` (id=1); `updateOnLog` (bounded last-20 histories, 30-min response rule, day streaks), `onReminderShown`

**Exports**: `HydrationEntry`, `UserProfile`, `UserBehavior` (+ nested enums)

### DAOs + Database

**Purpose**: Queries and Room database v4 with migrations.

**Location**: `app/src/main/java/com/water0/hydration/data/local/`

**Key Files**:
- `dao/HydrationEntryDao.kt` — insert/update/delete, today/for-date/in-range Flows, `getTodayTotalEffectiveMl` (SQL CASE mirrors `hydrationFactor`), count, recent-N
- `dao/UserProfileDao.kt` — singleton-row (id=1) Flow + suspend getters
- `dao/UserBehaviorDao.kt` — singleton-row (id=1) Flow + suspend getters
- `HydrationDatabase.kt` — `@Database` v4 (`water0_database`), `MIGRATION_1_2` (sex), `MIGRATION_2_3` (ageYr), `MIGRATION_3_4` (responses/lastReminderMs), `Converters`

**Dependencies**: entities, Room 2.6.1

### Repository

**Purpose**: Single data seam for ViewModels/use cases; owns behavior + fitness wiring.

**Location**: `app/src/main/java/com/water0/hydration/data/repository/`

**Key Files**:
- `HydrationRepository.kt` — interface: entry CRUD + today totals, profile/behavior Flows + suspend accessors, `onWaterLogged/onReminderShown`, fitness (`getLastNightSleep`, `hadRecentWorkout`, `markManualWorkout`, `hasManualWorkoutBoost`)
- `HydrationRepositoryImpl.kt` — DAO delegation + behavior updates + fitness/manual-workout flag

**Dependencies**: DAOs, `FitnessDataSource`, appContext prefs

**Usage Example**:
```kotlin
repository.getTodayEntries().collect { entries -> /* render */ }
repository.insertEntry(HydrationEntry(amountMl = 250, type = DrinkType.WATER))
```

### On-device ML

**Purpose**: TFLite advisor (advises; rules decide).

**Location**: `app/src/main/java/com/water0/hydration/data/ml/` (+ `app/src/main/assets/`)

**Key Files**:
- `OnDeviceForecast.kt` — `class OnDeviceForecast(appContext)`; loads model, maps 7 daily totals → one adjustment number
- `assets/sequence_tcn.tflite` — shipped 7.6 KB temporal-conv net

**Dependencies**: TensorFlow Lite 2.15.0

### Fitness (optional plugin)

**Purpose**: Health Connect workouts/sleep; null/false when unavailable, never throws.

**Location**: `app/src/main/java/com/water0/hydration/data/fitness/`

**Key Files**:
- `FitnessDataSource.kt` — `class FitnessDataSource(context)`; reads `ExerciseSessionRecord`/`SleepSessionRecord`; `data class SleepWindow(bedTimeMs, wakeTimeMs)`
- `HcAppContract.java` — Health Connect app contract helper

**Dependencies**: Health Connect client 1.1.0; manifest `READ_SLEEP`/`READ_EXERCISE` + queries block

## Domain Layer

### Recommendation engine

**Purpose**: Pure, testable hydration math (no Android imports).

**Location**: `app/src/main/java/com/water0/hydration/domain/engine/`

**Key Files**:
- `RecommendationEngine.kt` — `DailyGoal` (+`breakdown()`), `HydrationStatus` (`BEHIND/ON_TRACK/AHEAD/OVER`), `SipSlot`, `dayStartMillis` (wake-anchored day), `windowSpanHours` (overnight-aware), `recommendedBedtime`, `sipPlan`, recency/personal-pace/range-banding logic

**Dependencies**: entities only

**Usage Example**:
```kotlin
val engine = AppContainer.getRecommendationEngine()
val start = RecommendationEngine.dayStartMillis(profile.wakeUpHour, profile.sleepHour, nowMs)
```

### Use cases (7, one verb each)

**Purpose**: Thin operations over repository + engine.

**Location**: `app/src/main/java/com/water0/hydration/domain/usecase/`

**Key Files**:
- `LogHydrationUseCase.kt` — insert log (+ backdated timestamps; future clamped)
- `GetTodayProgressUseCase.kt` — today entries + profile → `HydrationStatus` Flow
- `GetHistoryUseCase.kt` — date-range history + per-day goal-met Flow
- `CalculateRecommendationUseCase.kt` — engine-backed range recs
- `DeleteHydrationUseCase.kt` — delete by id
- `ExportTrainingDataUseCase.kt` — opt-in 90-day simulator-schema CSV
- `ExportDrinkWindowUseCase.kt` — drink-window variant of the export

**Dependencies**: `HydrationRepository`, `RecommendationEngine`

## Presentation Layer

### Home

**Purpose**: Dashboard (tumbler tank, pace timeline, quips), logging, recs.

**Location**: `app/src/main/java/com/water0/hydration/presentation/home/`

**Key Files**:
- `HomeScreen.kt` — `HomeScreen`, `HomeUiFrame`, `RecommendationsSection`, `hydrationTintFor`
- `HomeViewModel.kt` — `class HomeViewModel` + `UiState` (progress, recs, recent shortcuts)
- `LogScreen.kt` — `LogScreen` (drink types, quick-add, backdated Now/1h/3h/pick)
- `components/TankMath.kt` — tank level/geometry math
- `components/PaceTimeline.kt` — scrollable week pace card (cursor, zoom, canvas)
- `components/TimelineMath.kt` — wake-day windows, curved guide, absorption, cursor math
- `components/QuickAddButtons.kt` — sized drops + custom ml
- `components/AddWaterDialog.kt` — amount entry dialog
- `components/RecommendationCard.kt` — range rec display
- `components/DrinkColors.kt` — per-type colors/layers
- `components/EntryListItem.kt` — log row

**Dependencies**: use cases via `AppContainer`, engine, theme

### History

**Purpose**: 7-day bar graph + day cards with expandable entries and delete.

**Location**: `app/src/main/java/com/water0/hydration/presentation/history/`

**Key Files**:
- `HistoryScreen.kt` — `HistoryScreen` + `HistoryViewModelFactory`
- `HistoryViewModel.kt` — `class HistoryViewModel` (7d/14d/30d ranges)

### Settings

**Purpose**: Profile, Glass Lab sliders, Fitness, data export, permission reasons.

**Location**: `app/src/main/java/com/water0/hydration/presentation/settings/`

**Key Files**:
- `SettingsScreen.kt` — `SettingsScreen` + `SettingsViewModelFactory` (+ section composables)
- `SettingsViewModel.kt` — `class SettingsViewModel` (goal breakdown, glass prefs, export)

### Onboarding

**Purpose**: First-run body/schedule/reminders flow; asks, never assumes.

**Location**: `app/src/main/java/com/water0/hydration/presentation/onboarding/`

**Key Files**:
- `OnboardingScreen.kt` — `OnboardingScreen` (weight required, age skippable)

### Navigation

**Purpose**: Lens-style bottom dock.

**Location**: `app/src/main/java/com/water0/hydration/presentation/navigation/`

**Key Files**:
- `BottomNav.kt` — custom dock (drag/long-press interactions, animated)

### Notifications

**Purpose**: Silent status line + buzz-only reminders, self-perpetuating chain.

**Location**: `app/src/main/java/com/water0/hydration/presentation/notification/`

**Key Files**:
- `NotificationWorker.kt` — `class NotificationWorker`; `doWork`, quiet-hours check, `constraints()`, `nextRequest()`, `scheduleNext()`; `CHANNEL_ID` + `STATUS_CHANNEL_ID`
- `NotificationScheduler.kt` — `class NotificationScheduler`; `ensureScheduled/poke/cancel`
- `NotificationDisplay.kt` — `showStatusNotifications()` (progress bar, +250 action, toast)
- `BootReceiver.kt` — re-schedule after reboot
- `WaterLogReceiver.kt` — shade +250 quick-log → repository

**Dependencies**: WorkManager 2.9.0, repository, engine

### Health rationale

**Purpose**: Health Connect permission-contract screens.

**Location**: `app/src/main/java/com/water0/hydration/presentation/health/`

**Key Files**:
- `HealthRationaleActivity.kt` — rationale UI; manifest `VIEW_PERMISSION_USAGE` + `HEALTH_PERMISSIONS` category

### Shell activity

**Purpose**: Single-activity host (pager + dock + snackbar + theme).

**Location**: `app/src/main/java/com/water0/hydration/presentation/`

**Key Files**:
- `MainActivity.kt` — `ComponentActivity`, `HorizontalPager` nav, bottom dock, `GlassSnackbar`

## Theme (liquid glass)

**Purpose**: Lenses/dock/living background/calm motion/honest numbers.

**Location**: `app/src/main/java/com/water0/hydration/ui/theme/` (+ `liquidglass/`)

**Key Files**:
- `LiquidGlassTheme.kt` — `MaterialTheme` wrapper + `LocalContentColor` fix
- `GlassColors.kt` — `GlassColors.dark` / `.light` schemes
- `GlassHelpers.kt` — `Glass`, `DockMetrics`, `CardStyle`, `glassCardContainer/Border`, `isDarkScheme()`
- `AuroraBackground.kt` — living background: bubbles, glow orbs, jellyfish, grain
- `LensCard.kt` — `LensCard`, `LocalLensScope/Config/Visible`, `isLensActive`
- `liquidglass/LiquidGlass.kt` — `LiquidGlassBox`, `toLiquidParams`, `LocalGlassScope`
- `GlassChrome.kt` — `GlassSnackbar`
- `GlassConfig.kt` / `GlassPrefs.kt` — blur/tint/roundness (defaults 6 / 27% / 42dp) over SharedPreferences
- `DesignTokens.kt` — `Spacing`, `Radii`, `IconSize`, `SectionHeader`, `glassCard()`
- `Color.kt` (`Water0`), `Type.kt` (Typography), `PressGlass.kt` (`prismalPress`)

## Placeholders (empty — no sources)

- `data/preferences/` — empty directory, no `.kt` files (prefs currently live in `water0_prefs` SharedPreferences + `GlassPrefs` + DataStore dep)
- `domain/model/` — empty directory, no `.kt` files (domain models currently nested in engine/entities)

## Related Areas

- [ARCHITECTURE.md](ARCHITECTURE.md) — system overview + data flow
- [FILES.md](FILES.md) — full file inventory
- [INDEX.md](INDEX.md) — orientation
