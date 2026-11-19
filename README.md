# Fleasion Mobile — 0.7.0 alpha02

Fleasion Mobile restores the full phone-first workspace inside the native Android app while keeping the web/PWA version available.

## Workspace restored in the Android app
- Multiple profiles: create, select and rename.
- Replacement-rule editor: add, edit, enable/disable, delete with confirmation.
- Rule search and profile/rule counters.
- JSON import, export and validation.
- Separate FastFlag profile manager: import PC JSON profiles, edit, validate, switch, save and export compatible key/value JSON.
- FastFlag structural validation for names and scalar values, with explicit unknown-support warnings; profiles are stored locally and are not applied to Roblox.
- Local profile storage, dark/light themes and offline bundled workspace.
- FPS quick check for the Fleasion browser animation only, plus low-end, balanced and performance guidance.
- Device diagnostics and optional Shizuku connection in a separate native tab.

The native APK bundles the same workspace source as the hosted PWA, so core profile/rule tools do not depend on a network connection. JSON import uses Android's document picker and export uses Android's save-file picker.

## Android compatibility
- Native minimum: Android 7.0 (API 24).
- Target SDK: 35.
- Shizuku API: 13.1.5.
- Runtime capability checks and feature-specific fallbacks are required. The hosted PWA remains available on older Android versions that cannot install this native build.

See [ANDROID_COMPANION.md](ANDROID_COMPANION.md) and [ANDROID_CAPABILITY_MATRIX.md](ANDROID_CAPABILITY_MATRIX.md).

## Build
GitHub Actions **Android debug build** builds the debug APK on Android-project changes. Download it from the workflow run's Artifacts section.

For a local build with JDK 17 and Gradle 8.9:

```sh
gradle :app:assembleDebug
```

## Important limitations
FastFlag profiles are a local profile editor/import-export feature only. The app does not apply flags to Roblox, and a valid JSON profile does not prove a flag is supported or active. The FPS quick check measures browser animation callbacks in Fleasion only; it does not read Roblox's in-game FPS. This companion does not modify Roblox protected files, inject code, automate gameplay or bypass platform protections. Shizuku does not automatically grant root. No FPS increase or target is guaranteed.

PC-only capabilities such as live process interception, desktop cache access, and direct game runtime integrations cannot simply be copied to Android. Android equivalents must be separately designed, permission-checked and tested.

Fleasion Mobile is an independent companion project and is not affiliated with Roblox, Shizuku or the original Fleasion project.
