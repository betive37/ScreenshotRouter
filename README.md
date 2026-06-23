# ScreenshotRouter MVP

ScreenshotRouter is a local-only Android MVP that observes `MediaStore.Images` only while a user-started foreground service is active. When it detects a recent screenshot-like image, it shows an app-owned swipe overlay if `SYSTEM_ALERT_WINDOW` is granted; otherwise it posts a notification with left/right routing actions.

## Privacy behavior

- No `INTERNET` permission.
- No analytics, Firebase, crash reporting, OCR, or network dependencies.
- No `AccessibilityService`.
- No `MANAGE_EXTERNAL_STORAGE`.
- Monitoring is explicit and visible through a foreground service notification.
- Logs are local and store only minimal metadata/status text.

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

- Move mode is honest copy-first behavior. The app attempts `ContentResolver.delete()` only after a verified copy. If Android requires a user-approved delete flow, the result reports that the copy succeeded but the original could not be deleted automatically. A future Activity-mediated `MediaStore.createDeleteRequest()` flow can be added.
- App-managed MediaStore destinations are supported on Android 10+. On Android 8/9, choose a SAF folder.
- Automatic detection depends on MediaStore visibility and the user’s media permission grant. Android 14+ selected-photo-only grants may not expose future screenshots.
- Overlay cards are best-effort and will not draw over lock screen, permission dialogs, or protected system surfaces.
