# TapShare

Share anything on your phone to another phone by touching them together.

## Overview

TapShare is an Android app that uses **NFC** (Near Field Communication) for device discovery and **WiFi Direct** for high-speed data transfer. Simply select what you want to share, touch your phone to another phone running TapShare, and the content transfers automatically.

## Features

- **Tap to share** — Touch phones together to instantly share content
- **Share anything** — Text, URLs, images, videos, files, contacts
- **Share from any app** — Appears in Android's share sheet so you can share from any app
- **High-speed transfer** — Uses WiFi Direct for fast file transfers (NFC for handshake only)
- **Modern UI** — Built with Jetpack Compose and Material Design 3
- **Progress tracking** — Real-time transfer progress with notifications

## How It Works

1. **Select content** — Choose text, a photo, a file, or share from any app
2. **Touch phones** — Bring the backs of both phones together (NFC)
3. **Auto-transfer** — Content transfers over WiFi Direct at high speed

## Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose with Material Design 3
- **NFC:** Android NFC API for device discovery and NDEF message exchange
- **Transfer:** WiFi Direct (WiFi P2P) for peer-to-peer file transfer
- **Architecture:** MVVM with ViewModels and StateFlow
- **Min SDK:** 24 (Android 7.0)
- **Target SDK:** 34 (Android 14)

## Building

```bash
# Build debug APK
./gradlew assembleDebug

# Run tests
./gradlew test

# Run lint
./gradlew lintDebug
```

The APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.

## Permissions

| Permission | Purpose |
|---|---|
| NFC | Device discovery via tap |
| WiFi State | WiFi Direct connections |
| Location | Required for WiFi Direct peer discovery |
| Nearby WiFi Devices | Android 13+ WiFi Direct |
| Storage/Media | Reading files to share |
| Foreground Service | Background file transfer |
| Notifications | Transfer progress and received file alerts |

## Project Structure

```
app/src/main/java/com/tapshare/app/
├── MainActivity.kt              # Main entry point with Compose navigation
├── ShareReceiverActivity.kt     # Handles share intents from other apps
├── MainViewModel.kt             # App state management
├── TapShareApp.kt               # Application class
├── model/
│   └── ShareItem.kt             # Data models
├── nfc/
│   └── NfcManager.kt            # NFC operations
├── transfer/
│   ├── WifiDirectManager.kt     # WiFi Direct P2P connections
│   └── FileTransferService.kt   # Background file transfer service
├── ui/
│   ├── theme/                   # Material Design 3 theme
│   ├── components/              # Reusable UI components
│   └── screens/                 # App screens (Home, Send, Receive, ShareText)
└── util/
    └── ContentUtils.kt          # File and content utilities
```

## License

ISC
