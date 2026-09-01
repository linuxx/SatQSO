# SatQSO Progress

Release: `1.2.1` (version code `4`)

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
- Compact frequency, tone, mode, pass timing, and satellite information panels.
- Screen-awake behavior while the app is open.
- Signed release builds with R8 and resource shrinking.

## Verification

- `.\gradlew.bat :app:testDebugUnitTest`
- `.\gradlew.bat :app:assembleDebug`
- `.\gradlew.bat :app:assembleRelease`
- Installed and launched the release APK on a connected Pixel device.
- Checked launch, GPS display, Maidenhead locators, filters, and pass details on-device.

Manual coordinate entry, complete network failure, cached pass loading, stale GPS refresh, and Orekit resource loading still need isolated runtime checks.

## Next

- Verify satellite frequencies, tones, modes, and operating notes against current AMSAT and ARISS sources.
- Add geographic ground-track visualization.
- Add Doppler-corrected uplink and downlink frequencies.
- Add preferences for location source, satellites, time format, bands, and modes.
