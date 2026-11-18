# Fleasion Mobile Companion — starter 0.1.0

This is a phone-friendly PWA prototype for managing asset replacement profiles.

## Run on Android
1. Extract the ZIP using your file manager.
2. Upload the folder to a static host that supports HTTPS (for example, a GitHub Pages repository).
3. Open the hosted `index.html` URL in Chrome.
4. Use Chrome's menu → **Add to Home screen** / **Install app**.

The app stores profiles in browser local storage and supports JSON import/export. Exported JSON is a portable companion format; it is not yet guaranteed to match every Fleasion 2.4.1 schema.

## Current limits
The original Fleasion project is a desktop application for Windows, macOS and Linux/Sober. Its live proxy, TLS certificate integration, Roblox process launching, cache interception and desktop OpenGL/audio components are not directly portable to a normal Android Roblox install. This prototype does not intercept or alter Roblox traffic.

## Next development stages
- Inspect and port exact Fleasion configuration schema and validation.
- Add local asset-library browsing and safe preview tools.
- Port compatible image/mesh conversions where Android browser APIs allow.
- Investigate Android-supported integration paths without patching Roblox or bypassing platform protections.
