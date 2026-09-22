# Releasing Water0

## Preconditions (already true)

- Apache-2.0 `LICENSE`, no proprietary dependencies, no tracking —
  F-Droid compatible by construction.
- `versionCode` / `versionName` live in `app/build.gradle.kts`.
- Every push to `main` produces a signed-debug APK via CI artifacts
  (release signing not configured yet — see below).

## Release checklist

1. **Bump version** in `app/build.gradle.kts`
   (`versionCode + 1`, `versionName` per semver) and add a new dated
   version header on top of `CHANGELOG.md` (sections stay dated —
   no `[Unreleased]` bucket).
2. **App icon**: adaptive set lives in
   `app/src/main/res/mipmap-anydpi-v26/` (+ per-density
   foreground/background/monochrome); `android:icon` / `android:roundIcon`
   are already wired in the manifest. Replace the PNGs + store listing
   (`fastlane/metadata/.../images/icon.png`) when the mark changes.
3. **Tag + push**: `git tag vX.Y.Z && git push origin vX.Y.Z`.
4. **GitHub Release**: attach the CI-built `app-debug.apk`
   (Actions → CI run → Artifacts). Add release signing before any
   Play upload: create a keystore (keep it OUT of git), add a
   `release` signing config reading from env vars / `keystore.properties`.
5. **F-Droid submission**: F-Droid builds from source, so no binary is
   needed. Open a merge request against
   [fdroiddata](https://gitlab.com/fdroid/fdroiddata) adding
   `metadata/com.water0.hydration.yml` pointing at this repo's tags.
   The `fastlane/metadata/` texts in this repo can be reused verbatim
   for the listing.

## Anti-piracy / integrity notes (for later)

- Debug builds are fine for personal testing and open distribution.
- Before Play: enable R8 full mode review, add `android:allowBackup`
  review, and consider Play App Signing.
