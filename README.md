# ScreenshotRouter

ScreenshotRouter is a local-only Android APP that observes `MediaStore.Images` only while a user-started foreground service is active. When it detects a recent screenshot-like image, it shows an app-owned swipe overlay if `SYSTEM_ALERT_WINDOW` is granted; otherwise it posts a notification with left/right routing actions.

## Privacy behavior

- No `INTERNET` permission.
- No analytics, Firebase, crash reporting, OCR, or network dependencies.
- No `AccessibilityService`.
- No `MANAGE_EXTERNAL_STORAGE`.
- Monitoring is explicit and visible through a foreground service notification and/or app-owned overlay.
- Monitoring is refused if full image access is missing, or if neither overlay nor notification route UI is available.
- Logs are local and store only minimal metadata/status text.
- Auto Backup is disabled so local DataStore preferences/logs are not cloud-backed by Android backup.

## Build

The project uses Gradle Kotlin DSL, Kotlin, Jetpack Compose Material 3, Coroutines, and DataStore Preferences.

The included `gradlew` script first uses a normal Gradle wrapper jar if one is added, then falls back to a system `gradle`, then tries to download Gradle 8.10.2. In locked-down environments without DNS/network and without Gradle installed, validation commands will fail before source compilation.

Recommended local commands:

```bash
./gradlew test
./gradlew lint
./gradlew assembleDebug
```

The module detects the highest installed Android SDK under `ANDROID_HOME`/`ANDROID_SDK_ROOT` and uses that as `compileSdk`; if no SDK is detected it defaults to 35. You can override with:

```bash
./gradlew assembleDebug -Pandroid.compileSdk=35 -Pandroid.targetSdk=35
```

## Permissions used

- `POST_NOTIFICATIONS`: foreground/status notifications and fallback action buttons on Android 13+.
- `READ_MEDIA_IMAGES`: MediaStore image metadata/read access on Android 13+.
- `READ_MEDIA_VISUAL_USER_SELECTED`: detects/handles Android 14+ selected-photo state; the UI explains partial access limitations.
- `READ_EXTERNAL_STORAGE` with `maxSdkVersion=32`: older Android MediaStore read compatibility.
- `SYSTEM_ALERT_WINDOW`: optional app-owned swipe overlay.
- `FOREGROUND_SERVICE` and `FOREGROUND_SERVICE_DATA_SYNC`: visible user-started monitoring service that observes and copies user-created media.

## MVP limitations

- Move mode is copy-first behavior. Original deletion is attempted only through Android's explicit delete approval flow after a verified copy; without approval the original is kept.
- App-managed MediaStore destinations are supported on Android 10+. On Android 8/9, choose a SAF folder.
- Automatic detection depends on MediaStore visibility and a full image/media permission grant. Android 14+ selected-photo-only grants are treated as insufficient for monitoring future screenshots.
- Overlay cards are best-effort and will not draw over lock screen, permission dialogs, or protected system surfaces.
