# Fleasion Mobile — Native Android companion (0.6.0 alpha)

The repository contains a native Android companion alongside the existing 0.5.0 web companion.

## Compatibility strategy

- Minimum native app version: Android 7.0 (API 24), required by the selected Shizuku integration baseline.
- Target SDK: 35.
- Shizuku API: 13.1.5.
- Runtime capability checks are used. A feature absent on an OS version or device is shown as unavailable rather than assumed to work.
- The PWA remains available on older Android versions through a supported browser, even where the native app cannot be installed.

Android does not provide one universal API for every performance setting. Vendor, OS, hardware and Shizuku's ADB-backed versus root mode affect available operations. Shizuku does not grant root automatically.

## Current native alpha

- Manufacturer, model, Android release, API level, build and supported ABI identification.
- Read-only diagnostics for memory, display refresh-rate modes, battery level, internal storage and thermal status where Android exposes it.
- Shizuku binder connection detection.
- Explicit, user-initiated Shizuku permission request.
- Standard-mode operation when Shizuku is absent or not authorized.

Diagnostics are not an in-game FPS test and do not change device settings.

## Build

GitHub Actions workflow **Android debug build** runs on pushes affecting the Android project or by manual dispatch. Download the generated debug APK from the workflow run's Artifacts section.

For a local build with JDK 17 and Gradle 8.9:

```sh
gradle :app:assembleDebug
```

## Next milestones

1. Add an explicit capability registry for each proposed system action.
2. Add supported, reversible controls only when public Android APIs or documented Shizuku permissions allow them.
3. Add a user-controlled export/import bridge for PWA profiles and rules.
4. Add automated build and compatibility checks before publishing an APK.

## Safety and limitations

This alpha is a device-information and Shizuku-connection foundation, not an FPS booster. It does not change system settings, kill background apps, access protected app data, modify Roblox files, inject code, automate gameplay or bypass platform protections. No specific in-game FPS can be guaranteed. Future privileged actions must be visible, permission-checked, narrowly scoped and reversible where possible.

Fleasion Mobile is an independent companion project and is not affiliated with Roblox, Shizuku or the original Fleasion project.
