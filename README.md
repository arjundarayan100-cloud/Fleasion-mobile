# Fleasion Mobile Companion — v0.3.0

A phone-friendly Progressive Web App for organizing local profiles and replacement-rule notes, with JSON import/export. It is an independent companion prototype, not an official Fleasion or Roblox app.

## Install on Android
1. Open the hosted app over HTTPS in Chrome.
2. Open Chrome's menu and choose **Install app** or **Add to Home screen**.
3. Launch Fleasion from your home screen. The interface is designed for narrow phone screens and can cache its app shell for offline reopening.

## v0.3.0 improvements
- Larger touch targets, safe-area spacing, horizontal tab scrolling and improved phone layouts.
- Better keyboard focus and dialog sizing.
- Dashboard counts update from the selected profile data.
- Local-storage failures are reported instead of silently claiming data was saved.
- Theme preference is remembered when browser storage is available.
- JSON import validates rule fields and limits file size; replacement targets are restricted to numeric IDs, HTTPS URLs or relative local paths.
- Service-worker cache version updated to refresh installed copies.

## Data and compatibility
Profiles are stored in this browser's local storage. Use **Export current** to keep a portable JSON backup and **Import JSON** to add a profile on another device. The export format is a companion format and is not guaranteed to match every Fleasion release or configuration schema.

## Important limitations
This app edits and exports local data only. It does not intercept Roblox traffic, modify a running Roblox client, bypass platform protections, or provide desktop-only proxy, TLS certificate, process-launching, OpenGL or audio features. A rule appearing in this companion does not mean it is applied in Roblox.

## Project
This is an independent community prototype and is not affiliated with Roblox or the original Fleasion project.