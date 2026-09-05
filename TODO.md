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

- [x] Add an inline "Tap to change" hint beside the GPS summary label so the tappable location control is discoverable.
- [x] Show the satellite image on the pass-detail radar only while the pass is active; retain the arc and endpoint dots before and after the pass.
- [x] Restyle the radar with a clean sky plot, separated status/countdown header, and aligned timing/tuning footer.
- [x] Move the compass up 6 dp and enlarge it approximately 5%, including its track and compass labels.
- [x] Enlarge and clip the map picker, add persistent zoom buttons, and retain pinch-to-zoom.
- [x] Restyle the GPS summary with clear location/time grouping, subdued labels, and a separate grid-reference footer.
- [x] Fade the list to disabled grayscale during refresh and show a bright refresh spinner, restoring the list when loading finishes.
- [x] Match the compact pass-card layout and readable countdown formatting from the home-screen reference.
- [x] Show pass dates, times, and full countdowns together without increasing the pass-card size.
- [x] Refine pass-card maximum elevation and direction with centered labels, consistent value typography, and a direction arrow.
- [x] Export Doppler tuning points as CHIRP-compatible split-memory CSV channels.
- [x] Add persisted display settings for screen-awake behavior, portrait lock, and time format.
- [ ] Add geographic ground-track visualization.
- [ ] Add Doppler-corrected uplink frequencies.
- [ ] Add preferences for location source, satellites, time format, bands, and modes.
