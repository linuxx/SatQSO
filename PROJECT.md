# SatQSO Project Notes

SatQSO is a native Android ham-radio satellite operating assistant written in Kotlin with Jetpack Compose and Material 3.

## Product Goal

Show amateur-radio satellite passes for the user's location, with filtering by operating mode and detailed pass information for operating in the field. The app should eventually support real-time Doppler-corrected frequencies, compass/device orientation, path visualization, tones, and operating guidance.

## Architecture Direction

Keep the app split into testable layers:

- `data`: persisted satellite metadata, orbital elements, TLE update sources, and Room storage.
- `domain`: satellite catalog, pass prediction, TLE parsing, SGP4/orbital calculation abstractions, filters, and use cases.
- `location`: Android permission handling and current observer location.
- `ui`: Compose screens and state models only; no orbital math or Android location calls in composables.
- `worker`: WorkManager jobs for periodic orbital-data refresh.

Domain code should be usable from unit tests without Android framework dependencies.

## Completed First Vertical Slice

Scope:

- Request foreground location permission.
- Obtain the user's current location.
- Include a small curated set of popular active amateur-radio satellites.
- Obtain and parse orbital elements.
- Calculate today's visible passes locally on-device.
- Display a polished Compose list with satellite name, mode, AOS time, LOS time, maximum elevation, and direction.

The following deferred items have since been added:

- WorkManager refresh scheduling.
- Detailed pass screen.
- Improved AOS/LOS boundary interpolation.
- Offline TLE caching.
- Manual location fallback.
- Minimum-elevation and operating-mode filters.

Still deferred:

- Room persistence for structured satellite/orbital data.
- Live Doppler correction.
- Compass and sky-path rendering.
- Full satellite catalog management.

## Current Baseline

As of 2026-08-30:

- Project has a working pass-planning app under package `com.thenetworkings.satqso`.
- The app supports GPS or manually entered coordinates, current local time display, cached TLEs, scheduled refresh, pass filters, and pass details.
- Location selection supports GPS, decimal entry, and an embedded OpenStreetMap picker with a tap-to-place pin and GPS reset.
- Build uses AGP 9.3.2, Kotlin Compose plugin 2.2.10, compile SDK 37, target SDK 37, min SDK 24.
- Build verified with `.\gradlew.bat :app:assembleDebug`.
- Release builds use R8/resource shrinking and a local, ignored keystore configuration from `keystore.properties`.
- Unit tests verified with `.\gradlew.bat :app:testDebugUnitTest`.
- Manual phone smoke testing covers launch, GPS display, Maidenhead formats, filters, and pass details; manual-location and offline-cache paths remain outstanding.

## Important Decisions

- Orbital calculations should run locally using TLE/GP data and an SGP4-compatible implementation.
- Avoid server-dependent pass prediction.
- Orekit is the selected SGP4/TLE engine for the first implementation. It is wrapped behind `PassPredictor`.
- Orekit requires time-scale data. The app currently packages `orekit-data/tai-utc.dat` as a Java resource and initializes Orekit through `OrekitData`.
- The app uses AMSAT `nasabare.txt` plus CelesTrak GP TLE endpoints for the `amateur` and `stations` groups. Fetching is best-effort across sources so a single unreachable feed does not fail pass calculation.
- Major architecture or dependency choices that materially affect long-term maintainability should be confirmed before proceeding.
