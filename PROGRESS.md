# SatQSO Progress

Release: `1.3.5` (version code `7`)

## Shipped

- GPS summary now includes an inline cyan “Tap to change” affordance beside the GPS/modified label while preserving the existing card size and tap behavior.
- GPS summary shows the exact “Tap To Change” affordance only for live GPS coordinates and hides it for modified locations.
- Pass-detail radar shows the satellite image only during Active status; Upcoming and Passed states show the pass arc with its green AOS and red LOS markers.
- Radar uses a clean background, quieter rings/ticks, finer pass track and markers, a separated north label, and dedicated status/countdown and timing/tuning rows within the existing card size.
- Compass plot is shifted up 6 dp and enlarged approximately 5%; the track, satellite position, and compass labels share the adjusted geometry.
- Larger responsive location-map dialog with explicitly clipped map bounds, persistent accessible +/− zoom buttons, pinch-to-zoom, fixed header/actions, and map cleanup on dismissal.
- GPS summary uses a clean background, left-aligned coordinates, a prominent right-aligned local clock/date, and a divided grid/subsquare footer. Manual-location highlighting and map access remain available.
- Refresh fades the list to dimmed grayscale over 180 ms, disables scrolling and pass/location taps, and shows a cyan spinner with a Refreshing label; normal appearance and interaction return when loading finishes.
- Local Orekit pass prediction with interpolated AOS and LOS boundaries.
- AMSAT and CelesTrak source fallback, 12-hour cache, offline cache use, and WorkManager refresh.
- GPS, decimal-coordinate entry, and OpenStreetMap pin selection with GPS reset.
- Modified-location indicator and four- and six-character Maidenhead locators.
- Upcoming-pass list with live countdowns and 24-hour, 2-day, 4-day, and 7-day windows.
- Pass cards show the date above the time and a separate countdown beneath a compact status pill, retaining the existing card size and satellite/mode/elevation/direction information.
- Maximum elevation and direction use centered columns, matching value typography, a subtle divider, and a proper direction arrow within the existing card height.
- Cached pass list on launch, pull-to-refresh, and live Upcoming, Active, and Passed statuses.
- GPS refresh when the last device fix is older than one hour.
- Minimum-elevation and operating-mode filters.
- Compass-oriented pass path, device heading, live satellite marker, and azimuth/elevation timeline.
- Pass-detail compass rim aligned with its tick marks.
- Combined frequencies and operating information with the Doppler-corrected downlink tuning timeline, duration-based steps, radar tune prompt, color-coded AOS/LOS frequencies, and programmable time/frequency table.
- Compact frequency, tone, mode, pass timing, and satellite information panels.
- Screen-awake behavior while the app is open.
- Signed release builds with R8 and resource shrinking.

## Verification

- GPS affordance: unit tests and debug assembly passed; debug APK installed on the connected Pixel 10 Pro XL. The hint is rendered inline with the GPS label and does not add card height.
- Satellite marker visibility: unit tests and debug assembly passed; the debug APK was installed on the connected Pixel 10 Pro XL. The composable condition is keyed to the existing `Active` pass status, while arc and endpoint rendering remains unconditional.
- Radar restyle: unit tests and debug assembly passed; installed and compared before/after screenshots of the SO-50 radar on the Pixel 10 Pro XL. Timing, countdown, channel/frequency, compass labels, and track are visible without overlapping panels.
- Map picker: reproduced overflow in a phone screenshot; unit tests and debug assembly passed. Installed on the Pixel 10 Pro XL and checked clipping after button zoom, panning, and pin selection. Test selection was canceled. Two-finger pinch remains enabled but needs a manual gesture check.
- GPS summary restyle: unit tests and debug assembly passed; installed and visually checked the complete coordinate, local time/date, grid, and subsquare display on the Pixel 10 Pro XL.
- Refresh feedback: unit tests and debug assembly passed; installed on the Pixel 10 Pro XL and captured loading/completed states. A card tap during refresh was ignored, and a card tap after completion opened pass details.
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
- Rounded CHIRP channel frequencies to the UV-5R-compatible 5 kHz tuning grid and display the same values in the export table and radar tune prompt, including the active green channel number.
- Added a reference-style bottom navigation bar on the pass list with disabled Recordings plus working Filter, Refresh, and Settings actions.
- Added a Settings page with persisted screen-awake, portrait-lock, and 24-hour/12-hour time-format controls; screen-awake, portrait lock, and 24-hour time are enabled by default.
- Installed the updated debug APK on a connected Pixel 10 Pro XL for visual verification.
- Verified the date/time/countdown card layout on the Pixel 10 Pro XL in 12-hour mode; before/after screenshots show unchanged card boundaries and fully visible countdowns. Unit tests and debug assembly passed with the JDK 11 launcher, the repository-configured Gradle daemon JDK 25, and Android SDK 37.
- Verified the refined maximum-elevation/direction layout on the connected Pixel 10 Pro XL, including two-letter compass endpoints; date/time and countdown remain fully visible and card height is unchanged. Unit tests and debug assembly passed, and the debug APK was installed.

Manual coordinate entry, complete network failure, cached pass loading, stale GPS refresh, and Orekit resource loading still need isolated runtime checks.

## Next

- Verify satellite frequencies, tones, modes, and operating notes against current AMSAT and ARISS sources.
- Add geographic ground-track visualization.
- Add Doppler-corrected uplink and downlink frequencies.
- Add preferences for location source, satellites, time format, bands, and modes.
