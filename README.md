# Water0 - Hydration Tracker

**Know your water. Stay ahead of thirst.**

[![License](https://img.shields.io/badge/license-Apache--2.0-blue)](LICENSE)
[![Platform](https://img.shields.io/badge/platform-Android-3DDC84)](https://github.com/wahaznb/water0)
[![Min SDK](https://img.shields.io/badge/minSdk-26-brightgreen)](https://github.com/wahaznb/water0)

---

Water0 is an offline-first Android hydration tracker with a liquid-glass UI.
Log what you drink, watch the day fill a tumbler, get nudged only when it
matters. No account, no tracking, no ads — your data never leaves the phone
unless you export it yourself.

## Features

### Home Dashboard

- **Hero tumbler tank** — the day as liquid on the left (~35% of the
  screen): pours in on log, red flash and level glide on delete,
  unmixed drink layers, foam cap
- **Hero card** — greeting + status chip, huge pace percentage (swipe the
  card or tap the dots for breakdown, then today's sips), day progress bar
- **Today's pace curve** — expected wake-to-sleep line, your actual intake
  as rising steps, now-marker, dashed goal line, one-line verdict
- **Rotating quips** — sometimes `>.<`

### Logging

- **Drink types** — water, coffee, tea, juice, soda, alcohol, other, each
  counting toward hydration differently
- **Quick-add drops** — sized by amount, plus custom ml entry
- **Recent shortcuts** — your own type + amount pairs from today, one tap
- **Backdated logging** — Now / 1h ago / 3h ago / pick a time (future
  clamped to now)
- **Range recommendations** — "drink around 500 ml"; tapping one opens
  Update with the amount prefilled (confirm logs it, walking away is a
  valid outcome the trackers notice)

### Smart Nudges

- **Time-of-day judgment** — chugging the whole day at 1am warns instead
  of celebrating; genuine evening finishes cheer
- **Recency sips** — dry spell grows the next glass (250 + 75/dry-hour,
  capped at 500, silent while on pace)
- **Personal pace** — compares today against your own 7-day rhythm, so
  slow starters who catch up in the evening get left alone
- **Ranges, not numbers** — every nudge bands ±20% snapped to 50s; nobody
  measures a glass to the milliliter

### Notifications

- **Status channel (silent)** — persistent line ("500 ml behind pace"),
  day progress bar, one-tap +250 log with instant toast; refreshes within
  seconds of every log
- **Reminder channel (buzz)** — fires only with a real nudge, never on pace
- **Quiet hours, opt-in permission** — asked only when enabling reminders;
  reminders re-schedule after reboot

### Logs and Trends

- **7-day bar graph** — totals with dashed goal line and per-day goal-met
  dots on top of Logs
- **Day cards** — Today, Yesterday, per-day totals, expandable entries,
  working delete everywhere; 7d / 14d / 30d range switch in the top lens

### Settings

- **Profile** — sex, typed weight, activity, climate, live goal breakdown
- **Glass Lab** — blur / tint / dock roundness sliders, live (defaults
  6 / 27% / 42dp)
- **Fitness** — Health Connect workouts shape recs, last night's sleep
  display, manual "Just worked out" path when no provider exists
- **Your data** — opt-in 90-day training CSV export in the simulator schema
- **Permissions and why** — every permission listed with its reason;
  network is not requested at all

### Onboarding

- **Guided first run** — body (goal math), schedule (by-now pace), reminders;
  each section says what it powers
- **Ask, don't assume** — weight required, age skippable, activity and
  climate picked (never silently defaulted); everything editable later

## Architecture

```
com.water0.hydration
├── presentation/    # Compose screens (home, history, settings, onboarding)
│   ├── home/        # HomeScreen, LogScreen, AmbientTank, components
│   ├── history/     # Logs + week graph
│   ├── settings/    # Profile, Glass Lab, Fitness, export
│   └── notification/ # Worker chain, shade rendering, quick-log receiver
├── domain/          # RecommendationEngine + one-verb use cases
├── data/            # Room entities/DAOs, repository, TFLite forecast
├── di/              # Manual AppContainer (no DI framework)
└── ui/theme/        # Liquid-glass theme, lenses, living background
```

Single-activity MVVM: Compose UI → ViewModels → use cases → Room
repository observed as Flows. Manual DI via `AppContainer`.

## Tech Stack

| Component | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose, Material3 |
| Database | Room (SQLite) |
| Preferences | DataStore + SharedPreferences |
| Background | WorkManager (self-perpetuating reminder chain) |
| On-device ML | TensorFlow Lite (7.6 KB temporal-conv advisor) |
| Offline ML | Python: scikit-learn baselines, Keras → TFLite |
| Tests | JUnit4 + MockK (46 unit), Compose UI test, GitHub Actions CI |
| Min SDK | 26 (Android 8) |
| License | Apache 2.0 |

## Hydration Science

The daily goal is `31/33 ml/kg × weight × activity + climate bonus`,
sex-aware, narrowed against CDC NHANES 2017–2018 data (4,931 adults):
population total-water mean 2,870 ml/d vs simulator 2,650 ml/d, and the
modeled sex gap (2 ml/kg) sits near the empirical gap (1.3 ml/kg).

The shipped on-device net reads the week's shape (7 daily totals in, one
number out): MAE 484 ml vs 555 for aggregates and 837 for guessing the
goal — a ~40% error cut for 7.6 KB. It advises; rules decide. A direct
NHANES training attempt scored R² −0.08 (demographics can't predict a
day's intake — day noise dominates), kept in the repo as an honest
negative result. Details in `ml_training/`.

## Getting Started

### Prerequisites

- **JDK 17**
- **Android SDK, platform 34** (`sdk.dir` in `local.properties`)

### Build and Run

```bash
# Unit tests + debug APK:
./gradlew :app:testDebugUnitTest :app:assembleDebug

# Install on a connected device:
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Python side (optional, offline):

```bash
cd ml_training
pip install -r requirements.txt
python generate_data.py --users 200 --days 60 --seed 42
python train_model.py --data data/hydration_logs.csv --out-dir model
```

## Design

Water0 speaks liquid glass, not flat Material:

- **Lenses, not bars** — top plates and bottom dock refract living content
- **Living background** — glow orbs and bubbles spawn, drift, and die on
  their own schedules; nothing loops, nothing syncs
- **Calm motion** — crossfades over travel, fade over pop; deletes vanish,
  water settles
- **Honest numbers** — ranges over fake precision, pace over raw percent

## Contributing

Fork, branch, keep the architecture (UI → use cases → Room), keep time
injected in testable code, keep the 46 unit tests green, open a pull
request.

## License

Apache 2.0 — see [LICENSE](LICENSE).
