# Android capability matrix

Fleasion Mobile uses runtime checks rather than assuming every Android device exposes the same APIs.

| Capability | Baseline | Fallback |
|---|---:|---|
| Device identity and ABI list | API 21+ | Show unavailable when fields are blank |
| Memory and storage diagnostics | API 18+ | Mark unavailable on error |
| Display refresh-rate modes | API 23+ | Show current rate when supported |
| Native Shizuku integration | API 24+ in this project | Use the PWA on older devices |
| Thermal status | API 29+ | Show not exposed on older Android versions |
| Battery percentage | Battery status broadcast | Show unavailable if no level is reported |

## Rules for future controls

- Check `Build.VERSION.SDK_INT` and the relevant service/API at runtime.
- Check device and vendor support before presenting an action as available.
- Request Shizuku permission only after a user action and only for a feature that needs it.
- Explain exactly what a control changes before running it.
- Prefer public Android APIs; never assume ADB-backed Shizuku has root privileges.
- Provide an undo path for every reversible setting change.
- If a capability is unknown, disable the control instead of guessing.
- Do not modify Roblox protected files, inject code, automate gameplay or bypass anti-cheat systems.

The PWA and native APK are separate delivery options. The PWA can cover older Android versions where the native Shizuku baseline is unavailable.
