# Fleasion Android — Native Edition

This is the native Android app track. The existing Fleasion Mobile website remains in the repository root and is not replaced by this project.

## Current first milestone

- Native Android launcher app
- Device and Android version display
- Shizuku binder detection
- Shizuku permission request/status
- GitHub Actions debug APK build

## Build

The project uses Android Gradle Plugin 8.7.3, Gradle 8.9, Kotlin 2.0.21, Java 17, and compile SDK 35.

From the repository root, run:

```bash
gradle assembleDebug
```

The APK is created at:

```
app/build/outputs/apk/debug/app-debug.apk
```

A GitHub Actions workflow builds the debug APK when relevant files are pushed to the `native-android` branch. Download it from the workflow run's Artifacts section after a successful build.

## Shizuku

Install and start Shizuku separately, then open Fleasion and grant its permission when prompted. This first milestone only checks the Shizuku connection and permission. It does not execute shell commands or modify other apps.

Future file features will use Android's supported storage access and explicit user-selected locations. Fleasion will not bypass Android sandboxing or access Roblox's protected private data.
