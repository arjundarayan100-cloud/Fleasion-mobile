# Fleasion Mobile — Native Android companion (0.8.0 alpha01)

This native Android app is the Shizuku-capable companion to the Fleasion Mobile PWA.

## Compatibility

- Minimum Android: 7.0 (API 24).
- Compile / target SDK: 35.
- Kotlin / Java target: 17.
- Shizuku API: 13.1.5.
- Shizuku is optional. Read-only diagnostics remain available without it.
- Shizuku does not grant root automatically; ADB-backed and root-backed modes have different capabilities.

## Included in this alpha

- Native Android application shell and device overview.
- Manufacturer, model, Android release, API level, build and supported ABI details.
- Read-only diagnostics for available/total memory, low-memory state, display refresh-rate modes, battery level, free internal storage and thermal status where exposed by Android.
- Live Shizuku binder connection detection.
- User-initiated Shizuku permission request, preceded by an explanation.
- Graceful standard mode when Shizuku is not installed, stopped or permission is denied.

## Build

The **Android debug build** GitHub Actions workflow builds the APK on pushes and pull requests that touch the Android project. It uploads `app-debug.apk` as a workflow artifact.

Local build requirements: JDK 17 and Gradle 8.9.

```sh
gradle :app:assembleDebug
```

## Next milestones

1. Add a native workspace for profiles and FFlags JSON import/export.
2. Add a capability registry that verifies each Android operation against OS version, service mode and permission state.
3. Add narrowly scoped, reversible Android controls only where documented APIs support them.
4. Test on multiple Android versions and OEM devices before a public release.

## Safety and limitations

This alpha is a device-information and Shizuku-connection foundation, not an FPS booster. It does not kill other apps, clear another app's memory, access protected app data, modify Roblox files, inject code, automate gameplay or bypass platform protections. Diagnostics are not in-game FPS measurements and no FPS increase is guaranteed.

Any future privileged operation must be visible, narrowly scoped, permission-checked, user-confirmed and reversible where possible. Unsupported operations must remain unavailable rather than pretending to work.

Fleasion Mobile is an independent companion project and is not affiliated with Roblox, Shizuku or the original Fleasion project.
