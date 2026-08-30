# SatQSO TODO

## Phase 0: Project Setup

- [x] Inspect existing Android project.
- [x] Verify baseline debug build.
- [x] Configure and verify a signed, shrunk release APK.
- [x] Add project memory files: `PROJECT.md`, `TODO.md`, `PROGRESS.md`.
- [x] Initialize Git if no repository exists.
- [x] Commit baseline documentation.

## Phase 1: First Functional Slice

- [x] Add Gradle dependencies for lifecycle ViewModel, coroutines, permissions/location support, networking, and tests.
- [x] Create domain models for satellites, operating modes, observer location, orbital elements, and pass summaries.
- [x] Add curated satellite catalog.
- [x] Add location permission and current-location provider.
- [x] Add TLE retrieval/parsing for the curated catalog.
- [x] Add rolling local pass calculation for the selected look-ahead window.
- [x] Build Compose pass list screen with loading, permission, error, and empty states.
- [x] Add focused unit tests for parsing and pass calculation smoke coverage.
- [x] Build and test.
- [x] Commit Phase 1 slice.

## Phase 1 Follow-Up Hardening

- [ ] Complete instrumentation/manual device verification for manual location, network fallback, offline cache, and Orekit resource loading.
- [x] Manually verify phone launch, GPS display, Maidenhead formats, filters, and pass details.
- [x] Improve pass boundary accuracy by interpolating AOS/LOS instead of using 60-second samples.
- [ ] Confirm curated satellite operating metadata against current AMSAT/ARISS source material.
- [x] Add a fallback location entry path for emulators and devices without a recent fused location fix.
- [x] Add an interactive map location picker with tap-to-place and Reset to GPS.
- [x] Indicate when the active observer location is a modified map pin.
- [x] Refresh pass details with a rotating compass radar, live readout, and compact telemetry panels.
- [x] Add fine compass sub-ticks and enlarge the detail-title Maidenhead grid.
- [x] Add subtle clustered constellation detail inside the radar and remove redundant compass heading text.
- [x] Add user-selectable minimum elevation cutoff.
- [x] Persist the last successful TLE set locally so pass calculation works completely offline after first launch.
- [x] Show only upcoming passes in a rolling window with configurable look-ahead and start countdowns.
- [x] Add 24-hour, 2-day, 4-day, and 7-day look-ahead choices and keep the display awake during use.

## Later Phases

- [ ] Add Room persistence for satellite metadata and orbital elements.
- [x] Add WorkManager periodic TLE/GP refresh.
- [x] Add filtering by FM Voice, SSB/CW, APRS, SSTV, Digital/Data, and Telemetry.
- [x] Add detailed pass screen with AOS, maximum elevation, LOS, azimuths, frequencies, and operating notes.
- [x] Add azimuth/elevation timeline to the pass detail screen.
- [x] Add polar sky-path visualization to the pass detail screen.
- [ ] Add map-style sky-path visualization to the pass detail screen.
- [x] Add compass/device orientation integration.
- [ ] Add real-time Doppler-corrected uplink/downlink display.
- [ ] Add settings for location source, preferred satellites, elevation cutoff, time format, and band/mode preferences.
