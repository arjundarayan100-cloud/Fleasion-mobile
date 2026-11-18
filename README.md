# Fleasion Mobile Companion — v0.5.0

A phone-first Progressive Web App for organizing local profiles and replacement-rule notes, with JSON import/export and a safe device performance helper. This is an independent companion prototype, not an official Fleasion or Roblox app.

## What's new in v0.5.0
- Added an FPS quick check that estimates this app's browser animation rate over five seconds.
- Added Low-end, Balanced and Performance-priority guidance for Android devices.
- Clarified that the browser check is not a measurement of Roblox in-game FPS.
- Refreshed cached asset versions so installed copies can receive the update.
- Existing profile, rule, enable/disable, search, import/export, validation, theme and local-storage features remain available.

## Install/update on Android
1. Open the hosted app over HTTPS in Chrome.
2. Refresh the page or close and reopen the installed app.
3. If the old version remains, clear the site's cached files in Chrome and reload.
4. Use Chrome's menu and choose **Install app** or **Add to Home screen** if it is not already installed.

## Performance helper limitations
The quick check measures animation callbacks in the Fleasion webpage only. It cannot read Roblox's internal frame rate or improve a Roblox game by itself. The guide provides ordinary in-game graphics and device-care suggestions. It does not install unofficial FFlags, modify Roblox files, inject code, automate gameplay, or bypass platform protections. No FPS target (including 30–50 FPS) is guaranteed; results depend on device capability, game complexity, temperature and network conditions.

## Data and compatibility
Profiles are stored in this browser's local storage. Use **Export** to keep a portable JSON copy and **Import** to add a profile on another device. The export format is a companion format and is not guaranteed to match every Fleasion release or configuration schema.

## Project
Fleasion Mobile is an independent community prototype and is not affiliated with Roblox or the original Fleasion project.
