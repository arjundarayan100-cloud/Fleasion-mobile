# Fleasion Mobile — Native Android companion (0.7.0 alpha01)

The native APK now bundles the existing phone-first workspace and adds device diagnostics as a separate native section.

## Restored workspace

The app includes the same local workspace used by the hosted PWA:
- Multiple profiles with create, select and rename.
- Replacement rules with add/edit, enable/disable, search and delete confirmation.
- JSON import/export and JSON validation.
- Local browser-style storage, dark/light theme, counters and empty states.
- Browser animation FPS quick check and performance guidance.

The workspace is bundled in the APK at `app/src/main/assets/workspace`, so the profile/rule tools work offline. Import and export are connected to Android's document picker and save-file picker.

## Native Android section

- Device identity and ABI list.
- Read-only diagnostics for memory, display refresh-rate modes, battery, storage and thermal status where available.
- Shizuku binder detection and explicit permission request.
- Standard mode remains available without Shizuku.

## Compatibility

- Minimum native version: Android 7.0 (API 24), based on this project's Shizuku integration baseline.
- Target SDK: 35.
- Runtime checks and fallbacks are required; do not assume every OEM exposes identical controls.
- The PWA remains available for older Android versions that cannot install the native app.

## Build

GitHub Actions **Android debug build** builds the debug APK. For local build with JDK 17 and Gradle 8.9:

```sh
gradle :app:assembleDebug
```

## Next PC-parity milestones

1. Asset library with metadata, tags, notes, safe previews and portable collections.
2. FastFlags/config editor with schema validation and export-only behavior unless a documented supported Android API exists.
3. Preset browser with import, preview and validation.
4. Cache/storage viewer limited to Fleasion-owned files and user-selected folders.
5. Texture and 3D asset preview/conversion for formats supported by Android libraries.
6. Animation and audio preview for user-provided compatible files.
7. Activity logs, settings, backup/restore and compatibility diagnostics.

Desktop process interception, privileged access to Roblox's private files, injection, gameplay automation and anti-cheat bypass are not Android parity targets. Future system actions must be narrowly scoped, user-approved and reversible.

## Safety

The current native diagnostics are read-only. Shizuku permission alone is not root. The app does not modify Roblox protected files, inject code, automate gameplay or bypass platform protections. No in-game FPS increase is promised.
