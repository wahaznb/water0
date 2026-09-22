# Changelog

All notable changes to Water0 are documented here. Format follows
[Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [0.2] - 2026-09-21

### Added
- Floating top lens on every tab: big centered Water0 brand on Home,
  Update/Logs/Settings titles, Logs 7d/14d/30d as a sliding glass
  segmented bar beside the title
- Full-width bottom dock with a finger-trailing jelly blob (asymmetric
  morph while scrubbing, circle at rest, no tap animations);
  hold-and-scrub with haptics; slower glide + page transitions
- Ambient mega-tank with a pose per tab (hero low-left on Home,
  low-right on Update, upper-left on Logs, small-center on Settings)
  reached on slow 1.1s glides with fading opacity
- Time-of-day goal judgment: chugging the whole day early warns instead
  of celebrating; genuine evening finishes still celebrate (tests incl.
  the 1am case)
- Range nudges ("drink around 500 ml", ±20% to 50s); tapping Add opens
  Update with the midpoint prefilled — confirm logs it, walking away
  starves the pace trackers
- Backdated logging (Now / 1h ago / 3h ago / time picker, future clamped
  to now)
- Stats panel pages (headline, breakdown, today's sips grouped by type)
  with dots + rotating quips (sometimes >.<)
- Light mode with its own luminous field; pitch-black dark field
- Lifespan background: glow orbs (10s lives) + bubbles (own 20–45s
  risetimes, curves, rhythms) — spawn, drift, die, nothing loops
- TV Girl lens tints (blue #0351A3 top, pink #EE2689 dock); blue plate
  titles in dark, black in light
- Training noise augmentation (human-estimate jitter + round grids,
  clean + noisy metrics reported)
- Typed weight entry (kg/lb range-checked); notification permission only
  when enabling reminders
- release/ folder: universal APK + sha256 + notes; new launcher icon +
  store listing

### Changed
- Home info zone reordered (recommendations up, stats panel down, one
  shared tank-side edge); cards carry the angle in silhouette (left edge
  slants with the tumbler wall)
- Quick-add: one drop language scaled by amount, stacked vertical;
  custom-amount dialog accepts prefill
- Tumbler: square-cut mouth, pour-out tips right with falling drops,
  foam cap only when overfull (overflow stream deleted), sunk lower
  on Home
- Range chips became a segmented switch; lists flow under both floating
  lenses with scrollable top gutters
- Bubbles ×1.8 size/brightness, then fully independent lives; dock
  icons 34dp; light cards black-tinted; panel sits tighter under the lens
- README rewritten for v0.2

### Fixed
- 7d/14d/30d switch invisible (self-measurement collapsed to zero)
- Quick-add amounts clipped out of narrow buttons
- Evening "sip before bed" showing after goal met; goal celebrated at
  any hour
- Ghost lens plate on Home; records sliding under plates; blob
  miscentered on edge tabs
- Bottom cards (About GitHub link included) unreachable under the
  floating dock — scrollable bottom gutters on every tab
- Default black text/icons on dark theme; gradient banding; time-flaky
  quiet-hours test
- Pour shoving the glass down (fixed rain zone now, glass never moves);
  delete tilting the tank (rows flash red and vanish, level glides down)
- Housekeeping: dead Slosh animation code removed, one shared dark-theme
  check, stray build APKs cleaned (release APK is the only kept binary)
- Recency-sized sips: dry spell grows the next glass (250 + 75/dry-hour,
  50s grid, 500 cap — never chug coaching), silent while on pace;
  persistent notification shows current vs prorated by-now target +
  the same sip (refreshes each tick, clears overnight)
- Normal buzzing reminder alongside the persistent line, only with a
  real recency nudge (same card as Home shows — on pace means silence)
- Pour droplets dance on Home only: logging elsewhere just moves the
  level, nothing banked, nothing replayed on return

## [0.1.0] - 2026-09-13

First tagged release (published as v0.1 — same precedence).

### Added
- History screen: per-day totals with progress, 7/14/30-day ranges,
  expandable day cards, working delete
- Settings screen: profile, live goal breakdown, reminders, quiet hours,
  sleep window, metric/imperial units
- Adaptive reminder notifications (WorkManager chain, quiet hours, boot
  reschedule, runtime permission flow)
- Offline Python ML pipeline (`ml_training/`): synthetic data generator,
  sklearn baselines, optional TFLite export, real-data source docs
- Unit tests + Compose UI tests + GitHub Actions CI
- First-run seeding of default profile and behavior

### Fixed
- `activityExtra` in daily-goal math always computed as zero
  (operator-precedence bug, caught by tests)
- Fresh installs stuck on Loading (empty Room tables emit nothing)
