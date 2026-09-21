# Water0 v0.1.1 — the glass update >.<

The whole app got more alive — and more honest. Same offline-first tracker, no account, no tracking.

## Highlights

- **Glass everywhere** — every tab floats its own lens up top, and the bottom dock is a jelly blob that trails your finger and settles into a circle
- **A tank with stage presence** — the big tumbler pours on log, tips right and spills on delete, and strikes a different pose on every tab
- **Smarter about time** — chug the whole day at 1am and the app raises an eyebrow instead of celebrating; finish honestly in the evening and it cheers
- **Nudges with ranges** — "drink around 500 ml", and tapping one opens Update with the amount already filled in
- **Forgot to log?** Backdate it — Now, 1h ago, 3h ago, or pick the time
- **Living pitch-black** — glows and bubbles are born, drift, and die on their own schedules. Nothing loops. Light mode gets a clean white field to match

## Install

One universal APK for every phone (arm64, old ARM, Chromebooks — zero native code, nothing to choose). Details in `../README.md`.

- File: `water0-v0.1.1-universal.apk`
- Check it: compare `sha256sum` output against `sha256.txt`

## Known rough edges

- Reminder timing still follows fixed adaptive intervals — the behavior-learning scheduler is next
- Very old phones (Android 8–12) get gradient fallbacks instead of the true glass lens

Full detail: `CHANGELOG.md` → `[0.1.1]`.
