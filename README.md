# Fleasion Mobile Companion — v0.5.0

A phone-first Progressive Web App for organizing local profiles and replacement-rule notes, with JSON import/export and a safe device performance helper. This is an independent companion prototype, not an official Fleasion or Roblox app.

## What's in the v0.5.0 web companion
- FPS quick check that estimates this app's browser animation rate over five seconds.
- Low-end, Balanced and Performance-priority guidance for Android devices.
- Profile and replacement-rule management, enable/disable, search, import/export, validation, theme and local storage.

## Native Android companion (0.6.0 alpha)

A native Android companion is being developed alongside the PWA. It includes device identification, read-only device diagnostics and optional Shizuku connection/permission handling.

- Native minimum: Android 7.0 (API 24).
- Target SDK: 35.
- Shizuku API: 13.1.5.
- Older Android devices can continue using the hosted PWA in a supported browser.
- Features are gated by runtime OS, hardware and service checks.

See [ANDROID_COMPANION.md](ANDROID_COMPANION.md) and [ANDROID_CAPABILITY_MATRIX.md](ANDROID_CAPABILITY_MATRIX.md) for details.

## Build and install

The **Android debug build** GitHub Actions workflow builds a debug APK. Download it from the workflow run's Artifacts section. A successful workflow run is required before treating the APK as install-ready.

For a local build with JDK 17 and Gradle 8.9:

```sh
gradle :app:assembleDebug
```

## Performance helper limitations

The web quick check measures animation callbacks in the Fleasion webpage only. It cannot read Roblox's internal frame rate or improve a Roblox game by itself. It does not install unofficial FFlags, modify Roblox files, inject code, automate gameplay or bypass platform protections. No FPS target is guaranteed.

## Data and compatibility

Profiles are stored in the browser's local storage. Use Export to keep a portable JSON copy and Import to add a profile on another device. The export format is a companion format and is not guaranteed to match every Fleasion release or configuration schema.

Fleasion Mobile is an independent community prototype and is not affiliated with Roblox, Shizuku or the original Fleasion project.
