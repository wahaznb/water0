# Water0 releases

Sideload-ready packages. Each version folder holds the installable APK,
its SHA-256 checksum, and release notes.

## Install

1. Copy the `water0-*.apk` to your phone (or download it from the
   [GitHub release](https://github.com/wahaznb/water0/releases)).
2. Open it → allow "install unknown apps" → accept the Play Protect
   prompt (expected: these are debug-signed, not Play-signed).
3. Requires Android 8.0+. Best on Android 13+, where the true
   backdrop-glass lens runs (graceful gradient fallback below).

## One APK for every phone

The filename says `universal` and means it: one APK installs on
**arm64-v8a** (every modern phone), old **armeabi-v7a** devices, and
**x86_64** (Chromebooks/emulators) — no per-ABI splits to hunt
through. The app logic is pure Kotlin; the APK additionally bundles
the tiny on-device model's CPU runtimes per architecture, so the file
is bigger but there is still nothing to choose.

Verify after download:

```bash
sha256sum water0-v0.1.1-universal.apk
# compare against sha256.txt in the version folder
```
