# SatQSO Progress

## Current State: 2026-08-30

SatQSO has a working Android prototype for planning amateur-radio satellite passes.

### Implemented

- Jetpack Compose pass list and pass detail screens.
- GPS location with manual latitude, longitude, and altitude fallback.
- Curated amateur-radio satellite catalog.
- Orekit-based local pass prediction from TLE data.
- Interpolated AOS/LOS boundaries rather than raw 60-second sample boundaries.
- AMSAT and CelesTrak TLE retrieval with resilient source fallback.
- Twelve-hour TLE cache and offline use of the last successful dataset.
- WorkManager background TLE refresh.
- Minimum-elevation and operating-mode filters.
- Unit tests for TLE parsing, pass prediction, boundary handling, filters, and location formatting.

### Verification

- `.\gradlew.bat :app:testDebugUnitTest` passes.
- `.\gradlew.bat :app:assembleDebug` passes.
- Debug APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.
- Runtime behavior on a physical device or emulator has not yet been manually verified.

### Remaining Work

- Verify runtime permission flow, location behavior, network fallback, cache loading, and Orekit resource loading on Android.
- Confirm curated satellite operating metadata against current AMSAT/ARISS sources.
- Add Room persistence if the data model grows beyond the current cache.
- Add azimuth/elevation timeline and sky-path visualization.
- Add compass/device orientation integration.
- Add real-time Doppler-corrected uplink/downlink frequencies.
- Add user settings for location source, preferred satellites, time format, and band/mode preferences.

### Development Note

Keep `README.md` user-facing, `PROJECT.md` architectural, `TODO.md` task-oriented, and this file focused on shipped functionality, verification, and the next priorities.
