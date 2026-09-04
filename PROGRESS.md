# SatQSO Progress

Release: `1.2.2` (version code `5`)

## Shipped

- Local Orekit pass prediction with interpolated AOS and LOS boundaries.
- AMSAT and CelesTrak source fallback, 12-hour cache, offline cache use, and WorkManager refresh.
- GPS, decimal-coordinate entry, and OpenStreetMap pin selection with GPS reset.
- Modified-location indicator and four- and six-character Maidenhead locators.
- Upcoming-pass list with live countdowns and 24-hour, 2-day, 4-day, and 7-day windows.
- Cached pass list on launch, pull-to-refresh, and live Upcoming, Active, and Passed statuses.
- GPS refresh when the last device fix is older than one hour.
- Minimum-elevation and operating-mode filters.
- Compass-oriented pass path, device heading, live satellite marker, and azimuth/elevation timeline.
- Pass-detail compass rim aligned with its tick marks.
- Doppler-corrected downlink tuning timeline with duration-based steps, a radar tune prompt, color-coded AOS/LOS frequencies, and a programmable time/frequency table.
- Compact frequency, tone, mode, pass timing, and satellite information panels.
- Screen-awake behavior while the app is open.
- Signed release builds with R8 and resource shrinking.

## Verification

- `.\gradlew.bat :app:testDebugUnitTest`
- `.\gradlew.bat :app:assembleDebug`
- `.\gradlew.bat :app:assembleRelease`
- Installed and launched the release APK on a connected Pixel device.
- Checked launch, GPS display, Maidenhead locators, filters, and pass details on-device.
- Installed the debug APK and visually checked the pass-detail compass on a connected Pixel device.
- Matched the compact pass-card layout in the home-screen reference; pass times use 24-hour formatting and countdowns use hours/minutes or seconds when under one minute.
- Right-aligned compact `ELEV.` and `DIR.` pass metrics, using degree symbols and compact direction arrows to keep the card values fully visible.
- Fixed Doppler-chart timestamp precision so tuning points retain their distinct horizontal positions.
- Fixed pass-timeline timestamp precision and rendered elevation/azimuth tracks with smooth interpolation.
- Updated the pass timeline and downlink-tuning progress markers at display cadence for continuous real-time movement.
- Audited the downlink Doppler model: it uses one-way range rate with the correct approaching/receding sign convention and numerical regression coverage.
- Locked the app interface to portrait orientation.
- Added CHIRP pass export: numbered Doppler channels, receive/downlink frequencies, split uplink frequency, and transmit PL tone are shared through Android's app chooser.
- Changed CHIRP export to Android's attachment share sheet for Google Drive, email, and other destination apps; added an open-in-app icon to the export button.
- Corrected the CHIRP `CrossMode` export field to the valid `Tone->Tone` value.
- Added the Baofeng UV-5R high-power `4.0W` value required by CHIRP generic CSV imports.
- Rounded CHIRP channel frequencies to the UV-5R-compatible 5 kHz tuning grid and display the same values in the export table.
- Installed the updated debug APK on a connected Pixel 10 Pro XL for visual verification.

Manual coordinate entry, complete network failure, cached pass loading, stale GPS refresh, and Orekit resource loading still need isolated runtime checks.

## Next

- Verify satellite frequencies, tones, modes, and operating notes against current AMSAT and ARISS sources.
- Add geographic ground-track visualization.
- Add Doppler-corrected uplink and downlink frequencies.
- Add preferences for location source, satellites, time format, bands, and modes.
