# Changelog

All notable changes to Water0 are documented here. Format follows
[Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [Unreleased]

### Added
- Profile sex (female 31ml/kg / male 33ml/kg, narrowed from 35 on NHANES
  evidence) with Room v1→v2 migration,
  settings radio, and goal breakdown by sex
- Swipeable pager shell (hold-and-slide across Home/History/Settings) with
  glass bottom bar and motion-blur transition
- Glass Lab in Settings: blur/tint/bevel sliders (defaults 23dp / 31% / 5%),
  apply-on-restart with live pending state
- Material icons throughout (tabs, recommendation reasons, drink entries,
  delete, navigation); glass toast snackbars
- Centered 280dp progress ring over static Canvas mesh gradient
- Single-Scaffold pager shell (content-only screens, shared glass snackbar)
- Opt-in training-data export (Settings → Your data, 90-day simulator-schema
  CSV, fully offline) + single-user time split in `train_model.py`
- Personal-pace nudge: recommendations adapt to the user's own 7-day rhythm
  (prorated by time of day, 3-day cold-start guard, suppressed at goal)
- Five-tab navigation (Home / Glass / Log / History / Settings): hero water
  glass that pours on log and tilts on delete, Log tab with drink types and
  working delete, droplet selection indicator on the glass bar
- Real liquid-glass bottom bar: ported backdrop-lens technique (refraction +
  blur + rim highlight sampled from content, AGSL on API 33+, gradient
  fallback below), driven by the Glass Lab settings
- Custom water amounts (preset chips + slider + exact ml field)
- Liquid-glass water fill replacing the progress ring (animated level + wave)
- Seamless transparent header across all three screens
- `ml_training/LEARNING_GUIDE.md`: hands-on training walkthrough incl. the
  NHANES negative result; direct NHANES training retired as a direction

### Fixed
- Evening "small sip before bed" no longer shows after goal met (gated to
  90–99%); goal-met state shows a celebration instead of drink nudges
- Background gradient banding: removed large-area blur blobs, subtle
  ≤10% hydration overlay only
- Time-flaky quiet-hours unit test (injected clock hour)

### Added
- History screen: per-day totals with progress, 7/14/30-day ranges,
  expandable day cards, working delete
- Settings screen: profile, live goal breakdown, reminders, quiet hours,
  sleep window, metric/imperial units
- Adaptive reminder notifications (WorkManager chain, quiet hours, boot
  reschedule, runtime permission flow)
- Offline Python ML pipeline (`ml_training/`): synthetic data generator,
  sklearn baselines, optional TFLite export, real-data source docs
- Unit tests (14) + Compose UI tests + GitHub Actions CI
- First-run seeding of default profile and behavior

### Fixed
- `activityExtra` in daily-goal math always computed as zero
  (operator-precedence bug, caught by tests)
- Fresh installs stuck on Loading (empty Room tables emit nothing)

## [1.0.0] - TBD

First tagged release. Planned: app icon artwork, release notes below.
