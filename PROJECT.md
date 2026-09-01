# SatQSO Architecture

SatQSO is a native Android application written in Kotlin with Jetpack Compose, Material 3, coroutines, WorkManager, and Orekit.

## Layers

- `data`: TLE and pass downloads, persistence caches, repositories, preferences, and WorkManager jobs.
- `domain`: satellite models, TLE parsing, filters, and pass prediction.
- `location`: Android location, permission, and orientation-sensor handling.
- `ui`: Compose screens, view models, UI state, formatting, and theme.

Composables do not perform orbital calculations or call Android location APIs. `PassPredictor` isolates orbital calculations from the UI and Android framework so prediction behavior can be unit-tested.

## Orbital Data

- Orekit provides TLE propagation and pass calculations.
- `orekit-data/tai-utc.dat` supplies Orekit time-scale data.
- AMSAT `nasabare.txt` and CelesTrak `amateur` and `stations` feeds provide TLE data.
- Feed failures are handled independently; one failed source does not discard successful responses from the others.
- Successful TLE data is cached for 12 hours. Stale cached data remains available when all network sources fail.
- Passes are calculated locally. No pass-prediction service is required.
- Pass tracks retain elevation, azimuth, and line-of-sight range rate so the domain layer can derive Doppler-corrected downlink tuning steps.
- The latest calculated pass set and observer location are persisted so the UI can render immediately on launch while a refresh runs.

## Location

The observer location can come from GPS, decimal coordinates, or a saved OpenStreetMap pin. Location and permission handling remain in the `location` layer; saved observer preferences remain in `data`. Device GPS fixes older than one hour are replaced with a current balanced-power request; manual locations are never replaced automatically.

## Build

- Package: `com.thenetworkings.satqso`
- Minimum SDK: 24
- Compile and target SDK: 37
- Release builds use R8, resource shrinking, and signing values from the ignored `keystore.properties` file.

Room is not currently used. Add structured persistence only if the file-backed cache and preferences no longer fit the data model.
