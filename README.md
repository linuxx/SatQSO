# SatQSO

SatQSO is an Android satellite-pass planner for amateur radio operators. It calculates visible passes from current or manually entered coordinates and presents satellite, timing, elevation, direction, frequency, and operating-mode details.

## Features

- Orekit-based pass prediction with accurate AOS and LOS boundaries.
- AMSAT and CelesTrak TLE data with a 12-hour cache and source fallback.
- Background TLE refresh through WorkManager.
- Offline fallback to the most recent cached orbital data.
- GPS location with manual latitude, longitude, and altitude fallback.
- Maidenhead locator and live local-time display.
- Minimum-elevation and operating-mode filters.
- Pass detail view with satellite-specific radio information.
- High-resolution launcher icon and dark space-themed UI.

## Requirements

- Android Studio or the Android SDK.
- JDK 11.
- Android SDK platform 37.
- A device or emulator running Android 7.0 (API 24) or newer.

The app needs location permission for GPS-based predictions and internet access to download current TLE data. Manual coordinates and cached TLEs allow it to remain useful without either service.

## Build

From the project directory:

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
```

To install the debug build on a connected device:

```powershell
.\gradlew.bat installDebug
```

The generated APK is located at `app/build/outputs/apk/debug/app-debug.apk`.

## Project layout

- `app/src/main/java/.../data` — TLE caching, refresh scheduling, and pass data sources.
- `app/src/main/java/.../domain` — satellite models and Orekit prediction logic.
- `app/src/main/java/.../location` — GPS and manual location handling.
- `app/src/main/java/.../ui` — Compose screens, filters, theming, and formatting.
- `app/src/test` — unit tests for caching, prediction boundaries, filters, and location formatting.

## Orbital data

TLE data is fetched from AMSAT and CelesTrak. Cached data is considered fresh for 12 hours; when a network request fails, the app uses the newest available stale cache rather than failing immediately.
