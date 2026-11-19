# Fleasion Mobile — Native Android companion (0.6.0 alpha)

This repository now contains the first native Android app foundation alongside the existing 0.5.0 web companion.

## Scope of this alpha

- Generic Android device and OS identification (manufacturer, model, release, API level and build).
- Shizuku binder connection detection.
- Explicit, user-initiated Shizuku permission request.
- Standard-mode operation when Shizuku is absent or not authorized.
- GitHub Actions debug APK build.

## Compatibility

- Minimum Android version: Android 7.0 (API 24).
- Target SDK: 35.
- Shizuku API: 13.1.5.
- Android features must be checked at runtime. Shizuku does not grant root automatically; ADB-backed Shizuku has fewer privileges than root, and available operations vary by Android version and device vendor.

## Build

The GitHub Actions workflow **Android debug build** runs on pushes affecting the Android project or by manual dispatch. Download the generated APK from the workflow run's Artifacts section.

For a local build with JDK 17 and Gradle 8.9:

```sh
gradle :app:assembleDebug
```

## Next milestones

1. Add a capability registry that checks each supported Android feature before displaying its control.
2. Add read-only device diagnostics and refresh-rate reporting.
3. Add carefully scoped, reversible system controls only where public Android APIs or documented Shizuku permissions support them.
4. Connect the native app and existing PWA data through an explicit export/import bridge.

## Safety and limitations

This alpha is a connection and device-information foundation, not an FPS booster. It does not change system settings, kill background apps, access protected app data, modify Roblox files, inject code, or automate gameplay. No specific in-game FPS can be guaranteed. Every future privileged action must be visible, permission-checked, documented, and reversible where possible.

Fleasion Mobile is an independent companion project and is not affiliated with Roblox, Shizuku, or the original Fleasion project.
