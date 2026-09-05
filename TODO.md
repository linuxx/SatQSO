# SatQSO TODO

## Runtime Checks

- [ ] Test decimal-coordinate entry on a physical device.
- [ ] Test complete network failure with a populated TLE cache.
- [ ] Test cached pass loading and stale GPS refresh after the one-hour freshness window.
- [ ] Confirm packaged Orekit time data loads in a clean install.

## Data

- [ ] Verify satellite frequencies, tones, modes, and operating notes against current AMSAT and ARISS sources.
- [ ] Define a maintenance process for the curated satellite catalog.

## Features

- [x] Match the compact pass-card layout and readable countdown formatting from the home-screen reference.
- [x] Export Doppler tuning points as CHIRP-compatible split-memory CSV channels.
- [x] Add persisted display settings for screen-awake behavior, portrait lock, and time format.
- [ ] Add geographic ground-track visualization.
- [ ] Add Doppler-corrected uplink frequencies.
- [ ] Add preferences for location source, satellites, time format, bands, and modes.
