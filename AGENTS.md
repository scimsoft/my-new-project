# AGENTS.md

## Cursor Cloud specific instructions

This is a **TapShare** Android application — an NFC-based sharing app built with Kotlin and Jetpack Compose.

### Prerequisites

- **Java 17+** (JDK 21 is available in the Cloud VM)
- **Android SDK** installed at `~/android-sdk` with `platforms;android-34` and `build-tools;34.0.0`
- `ANDROID_HOME` must be set: `export ANDROID_HOME=~/android-sdk`

### Build & run

```bash
export ANDROID_HOME=~/android-sdk
./gradlew assembleDebug          # Build debug APK
./gradlew test                   # Run all unit tests
./gradlew lintDebug              # Run Android lint checks
```

The debug APK is output at `app/build/outputs/apk/debug/app-debug.apk`.

### Key notes

- This is a pure Android project — it cannot be "run" in the traditional sense on a headless VM. Build verification is done via `./gradlew assembleDebug`.
- NFC and WiFi Direct features require physical Android hardware; they cannot be tested in an emulator or CI.
- The project uses **Jetpack Compose** for UI, not XML layouts.
- Share intent receiver (`ShareReceiverActivity`) handles `ACTION_SEND` and `ACTION_SEND_MULTIPLE` from any app.
- The original `index.js` / `project_config.json` files are the repo's original scaffold and are unrelated to the Android app.
