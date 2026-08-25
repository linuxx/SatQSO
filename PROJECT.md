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

## First Vertical Slice

Scope:

- Request foreground location permission.
- Obtain the user's current location.
- Include a small curated set of popular active amateur-radio satellites.
- Obtain and parse orbital elements.
- Calculate today's visible passes locally on-device.
- Display a polished Compose list with satellite name, mode, AOS time, LOS time, maximum elevation, and direction.

Out of scope for the first slice:

- Room persistence.
- WorkManager refresh scheduling.
- Detailed pass screen.
- Live Doppler correction.
- Compass and sky-path rendering.
- Full satellite catalog management.

## Current Baseline

As of 2026-08-25:

- Project is a stock Android Compose app under package `com.thenetworkings.satqso`.
- Build uses AGP 9.3.2, Kotlin Compose plugin 2.2.10, compile SDK 37, target SDK 37, min SDK 24.
- Baseline build verified with `.\gradlew.bat :app:assembleDebug`.

## Important Decisions

- Orbital calculations should run locally using TLE/GP data and an SGP4-compatible implementation.
- Avoid server-dependent pass prediction.
- Major architecture or dependency choices that materially affect long-term maintainability should be confirmed before proceeding.
