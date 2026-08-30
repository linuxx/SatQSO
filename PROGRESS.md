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
- Azimuth/elevation timeline on the pass detail screen.
- Polar sky-path visualization in the pass detail screen.
- Device heading readout using the rotation-vector sensor.
- Time-synchronized satellite marker that follows the sky path and timeline.
- Live pass-state countdown showing start, end, and completion status.
- Home screen shows only upcoming passes in a rolling time window, with selectable look-ahead and per-pass start countdowns.
- Look-ahead options are 24 hours, 2 days, 4 days, and 7 days; the activity keeps the display awake while open.
- Unit tests for TLE parsing, pass prediction, boundary handling, filters, and location formatting.

### Verification

- `.\gradlew.bat :app:testDebugUnitTest` passes.
- `.\gradlew.bat :app:assembleDebug` passes.
- Debug APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.
- Release build is configured with R8/resource shrinking and local keystore signing; the generated APK is `app/build/outputs/apk/release/app-release.apk`.
- Release verification completed with `testDebugUnitTest`, `assembleDebug`, and `assembleRelease`.
- Manual phone smoke test passes for launch, GPS display, both Maidenhead formats, filters, and pass details.
- Manual location-entry and offline-network paths still need runtime verification.

### Remaining Work

- Complete runtime verification for manual location entry, network fallback, cache loading, and Orekit resource loading on Android.
- Confirm curated satellite operating metadata against current AMSAT/ARISS sources.
- Add Room persistence if the data model grows beyond the current cache.
- Add map-style sky-path visualization with geographic context.
- Add real-time Doppler-corrected uplink/downlink frequencies.
- Add user settings for location source, preferred satellites, time format, and band/mode preferences.

### Development Note

Keep `README.md` user-facing, `PROJECT.md` architectural, `TODO.md` task-oriented, and this file focused on shipped functionality, verification, and the next priorities.
