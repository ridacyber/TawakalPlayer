
# TawakalPlayer

A kiosk-mode Android Quran audio player — built as a physical, single-purpose device rather than a typical app. Once locked in, the app can never be exited, closed, or navigated away from.

## Overview

TawakalPlayer runs on a dedicated Android device (originally a $30 secondhand Samsung Galaxy A10e), locked into Android's **Device Owner** and **Lock Task Mode** via `DevicePolicyManager`. The device is mounted inside a wooden box, functioning as a single-purpose audio player rather than a general-purpose phone.

## Features

- **True lockdown** — Device Owner + Lock Task Mode prevent exiting, closing, or navigating away from the app
- **Hidden exit gesture** — a concealed 7-tap gesture gated behind a PIN is the only way out of lock mode
- **Reactive UI** — built with Kotlin and Jetpack Compose, fully driven by Compose state with no manual view updates
- **Custom playback** — audio handled via Media3 ExoPlayer, including a hand-built progress bar via manual position polling (ExoPlayer doesn't expose this directly)
- **Multiple reciters** — supports Abdul Basit, Khalid Al-Jalil, and Yasir al-Dawsari
- **Fuzzy surah search** — Levenshtein-distance-based search for quick, typo-tolerant navigation

## Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose
- **Audio:** Media3 ExoPlayer
- **Device Management:** Android Device Policy APIs (`DevicePolicyManager`)

## How It Works

The app is provisioned as the device's **Device Owner**, which unlocks system-level control not available to normal apps — including the ability to enter Lock Task Mode, a state that pins the app to the foreground and disables home/recents/back navigation entirely. Exiting requires a hidden gesture (7 taps in a specific area) followed by a PIN prompt.

Debugging was done entirely through `logcat` and `dumpsys`, with no browser-style dev console available. Notable bugs traced this way included a stale closure breaking state updates and a silent network failure handler, both found during extended overnight testing.

## Setup

> ⚠️ Requires a dedicated/test Android device. Device Owner provisioning cannot be done on a device with an existing Google account signed in — factory reset first.

1. Factory reset the target device (no accounts signed in)
2. Enable Developer Options and USB Debugging
3. Connect via ADB and set the app as Device Owner:

   ```
   adb shell dpm set-device-owner com.yourpackage.tawakalplayer/.DeviceAdminReceiver
   ```

4. Build and install the app via Android Studio or a signed APK
5. Launch — the app will automatically enter Lock Task Mode


## License

MIT — see [LICENSE](LICENSE) for details.

## Author

Built by [Rida Rahim](https://github.com/ridacyber)
