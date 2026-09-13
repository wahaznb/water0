# Changelog

All notable changes to Water0 are documented here. Format follows
[Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [Unreleased]

### Added
- Profile sex (female 31ml/kg / male 33ml/kg, narrowed from 35 on NHANES
  evidence) with Room v1→v2 migration,
  settings radio, and goal breakdown by sex
- Swipeable pager shell (hold-and-slide across tabs) with
  glass bottom bar and motion-blur transition
- Glass Lab in Settings: blur/tint/bevel sliders (defaults 23dp / 31% / 5%),
  applied live, no restart — kept after audit (restart flow deleted)
- Edge-to-edge display with full-bleed living background (kills grey
  system bars); Tetris line-clear delete animation in the Log tab
- Compact centered dock (no footer): fixed-width tabs, gliding selection
  badge that follows the finger, lens height measured on the lens itself
- Material icons throughout (tabs, recommendation reasons, drink entries,
  delete, navigation); glass toast snackbars
- Four-tab navigation (Home / Update / Logs / Settings): hero water glass
  back on Home with transparent unmixed drink layers, Update tab with drink
  types and working delete, long-press-drag across tabs to navigate,
  droplet selection indicator on the glass bar
- Translucent liquid-glass cards everywhere (settings, recommendations,
  logs) over a dimmed mesh; header removed for a seamless top; toast
  lifted above the glass bar
- Real liquid-glass bottom bar: ported backdrop-lens technique (refraction +
  blur + rim highlight sampled from content, AGSL on API 33+, gradient
  fallback below), driven live by the Glass Lab settings
- About section in Settings (version, GitHub link, license)
- Real launcher icon (IconKitchen adaptive set + monochrome + store listing)
- iPhone-style tab bar: hold to magnify nearby tabs with haptic scrub,
  drag-select with springy droplet, release to navigate
- Motion pass: press bounce on quick-add, cascading card entrances,
  gliding status tint, animated glass numerals
- Top-layer-only waves; droplet pour with splash rings (replaces the bar
  stream); glass badge on the active tab (no dot); footer chrome removed
- Living background: swirling washes + rising bubbles reacting to progress,
  twinkle specks, film grain against banding; washes turned up loud
- Ambient mega-tank: one persistent tumbler behind every tab (cropped hero
  on Home, dim refractive backdrop elsewhere); tumbler silhouette with
  thick base + rim; icon-only dock tabs
- Home is tank + recommendations only (logging lives strictly in Update);
  tall side tank with stats column; chronological unmixed drink layers
- Opt-in training-data export (Settings → Your data, 90-day simulator-schema
  CSV, fully offline) + single-user time split in `train_model.py`
- Personal-pace nudge: recommendations adapt to the user's own 7-day rhythm
  (prorated by time of day, 3-day cold-start guard, suppressed at goal)
- Custom water amounts (preset chips + slider + exact ml field)
- Liquid-glass water fill replacing the progress ring (animated level + wave)
- True drink colors (alcohol red, soda black, juice sand) rendered as
  unmixed layers; realistic liquid (depth gradient, meniscus, bubbles,
  shimmer, overfill foam); pour-out delete (lip-pinned tilt + spill arc),
  static glass otherwise; standout mesh background
- `ml_training/LEARNING_GUIDE.md`: hands-on training walkthrough incl. the
  NHANES negative result; direct NHANES training retired as a direction

### Fixed
- Evening "small sip before bed" no longer shows after goal met (gated to
  90–99%); goal-met state shows a celebration instead of drink nudges
- Tab bar rendered at the top over the status bar (dock wrapper didn't
  span the overlay); dock now pinned to the bottom
- Bare Text()/Icon() rendered black on dark theme (Material3 doesn't set
  LocalContentColor); scheme color provided once at the theme root
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

## [0.1.0] - 2026-09-13

First tagged release (published as v0.1 — same precedence).
