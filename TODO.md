# SatQSO TODO

## Phase 0: Project Setup

- [x] Inspect existing Android project.
- [x] Verify baseline debug build.
- [x] Add project memory files: `PROJECT.md`, `TODO.md`, `PROGRESS.md`.
- [ ] Initialize Git if no repository exists.
- [ ] Commit baseline documentation.

## Phase 1: First Functional Slice

- [ ] Add Gradle dependencies for lifecycle ViewModel, coroutines, permissions/location support, networking, and tests.
- [ ] Create domain models for satellites, operating modes, observer location, orbital elements, and pass summaries.
- [ ] Add curated satellite catalog.
- [ ] Add location permission and current-location provider.
- [ ] Add TLE retrieval/parsing for the curated catalog.
- [ ] Add local pass calculation for today.
- [ ] Build Compose pass list screen with loading, permission, error, and empty states.
- [ ] Add focused unit tests for parsing and pass-summary formatting/calculation boundaries.
- [ ] Build and test.
- [ ] Commit Phase 1 slice.

## Later Phases

- [ ] Add Room persistence for satellite metadata and orbital elements.
- [ ] Add WorkManager periodic TLE/GP refresh.
- [ ] Add filtering by FM Voice, SSB/CW, APRS, SSTV, Digital/Data, and Telemetry.
- [ ] Add detailed pass screen with AOS, maximum elevation, LOS, azimuth/elevation timeline, path visualization, and operating notes.
- [ ] Add compass/device orientation integration.
- [ ] Add real-time Doppler-corrected uplink/downlink display.
- [ ] Add settings for location source, preferred satellites, elevation cutoff, time format, and band/mode preferences.
