# SatQSO Progress

## 2026-08-25

### Completed

- Inspected the initial Android project.
- Confirmed the app is currently a stock Jetpack Compose scaffold.
- Verified `.\gradlew.bat :app:assembleDebug` succeeds.
- Added project memory files to guide future AI sessions.
- Initialized Git and committed the baseline project/docs.
- Added first-slice dependencies: lifecycle ViewModel/Compose state, coroutines, Google Play Services location, OkHttp, and Orekit.
- Added foreground location permission and internet permission.
- Added domain models for operating modes, satellites, TLEs, observer location, and pass summaries.
- Added a curated starter satellite catalog.
- Added CelesTrak TLE fetching/parsing for `amateur` and `stations` groups.
- Added Orekit-backed local pass prediction behind the `PassPredictor` interface.
- Added minimal Orekit data initialization with packaged `orekit-data/tai-utc.dat`.
- Replaced the starter screen with a Material 3 Compose pass list with permission, loading, error, empty, and populated states.
- Added unit tests for TLE parsing and Orekit pass prediction smoke coverage.

### Build Notes

- First build attempt timed out after 2 minutes while Gradle/java processes continued resolving and building.
- A second `.\gradlew.bat :app:assembleDebug` run completed successfully in 15 seconds using cached work.
- Dependency build after adding Orekit and Android support libraries completed successfully.
- `.\gradlew.bat :app:testDebugUnitTest` passes.
- `.\gradlew.bat :app:assembleDebug` passes.
- CelesTrak was observed timing out on 2026-08-25 at `104.168.149.178:443`; AMSAT `nasabare.txt` was reachable and added as a fallback source.

### Current State

- The app can request location, fetch TLEs, calculate today's visible passes locally, and display a pass list.
- TLE fetch now tries AMSAT and CelesTrak independently, using whichever configured source responds.
- Pass prediction currently samples at 60-second intervals. AOS/LOS times are useful for the first slice but should be refined before relying on them for operating-critical timing.
- Runtime behavior on an Android device/emulator still needs manual or instrumentation verification.

### Next Step

Commit the Phase 1 slice, then harden runtime behavior and pass accuracy.
