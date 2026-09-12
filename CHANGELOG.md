# Changelog

All notable changes to Water0 are documented here. Format follows
[Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [Unreleased]

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
