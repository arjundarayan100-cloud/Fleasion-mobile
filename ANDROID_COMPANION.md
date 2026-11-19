# Fleasion Mobile — Native Android companion (0.8.0 alpha01)

The native APK packages the existing Fleasion Mobile workspace in an Android WebView shell and adds a verified native Shizuku bridge plus read-only Android diagnostics. The PWA remains intact on the main branch.

## Included workspace modules

- Dashboard and local profile workspace.
- Replacement rules: create, edit, enable/disable, delete and export.
- FFlags Lab: JSON import, validation, per-profile save and export. Compatibility is not assumed and flags are not injected into Roblox.
- Asset Library: locally saved asset references and notes.
- Subplace Joiner: opens the public Roblox experience page.
- Modifications: Fleasion interface-only lightweight, compact and reduced-motion presets.
- Proxy & Misc guidance.
- FPS Lab: browser animation benchmark and performance guidance; it is not Roblox FPS.
- Device Lab: native model, Android/API, CPU core count, total/available RAM, screen, refresh rate, free storage and thermal status where Android exposes them.
- Shizuku: live binder detection and user-confirmed permission request.
- Settings, theme toggle and local storage.

## Native Android behavior

- Minimum Android: 7.0 (API 24).
- Compile / target SDK: 35.
- Kotlin / Java target: 17.
- Shizuku API: 13.1.5.
- Shizuku is optional. Workspace features and read-only device information remain available without it.
- JSON import uses the Android document picker. JSON export uses the Android save-document picker.
- Shizuku state is shown only when verified by the native API. Permission is never requested automatically.

## Build

The Fleasion Mobile native APK build GitHub Actions workflow builds the debug APK on pushes and pull requests that touch the Android project. It uploads an app-debug.apk workflow artifact.

Local build requirements: JDK 17 and Gradle 8.9.

```sh
gradle :app:assembleDebug
```

## Safety and limitations

This alpha restores the full companion workspace; it is not a privileged FPS booster. The current Shizuku bridge only checks connection and permission state. It does not kill other apps, clear another app's memory, modify system settings, access protected app data, modify Roblox files, inject code, automate gameplay or bypass platform protections. The FPS test measures Fleasion's own WebView animation only. No in-game FPS increase is promised.

Any future privileged operation must be visible, narrowly scoped, permission-checked, user-confirmed and reversible where possible. Unsupported operations must remain unavailable rather than pretending to work.

Fleasion Mobile is an independent companion project and is not affiliated with Roblox, Shizuku or the original Fleasion project.
