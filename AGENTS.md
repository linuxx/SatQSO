# SatQSO Agent Guidance

## Project

SatQSO is a native Android app written in Kotlin with Jetpack Compose, Material 3, coroutines, and Orekit.

## Architecture

- Keep orbital calculations and parsing in `domain` or `data`, not in composables.
- Keep Android location and permission handling in `location`.
- Keep UI state and presentation in `ui`.
- Preserve the `PassPredictor` abstraction so orbital logic remains unit-testable.
- Prefer the existing cache, repository, and WorkManager patterns before adding new infrastructure.

## Verification

Run these commands from the repository root after code changes:

```powershell
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:assembleDebug
```

Use JDK 11 and Android SDK platform 37. Runtime changes should also be checked on an emulator or connected device when available.

## Documentation

- Update `TODO.md` when work is completed or priorities change.
- Update `PROGRESS.md` when shipped functionality or verification status changes.
- Keep `PROJECT.md` aligned with architectural decisions and current scope.
- Keep `README.md` focused on setup, features, and user-facing project information.

## Change Discipline

Make focused changes, add or update unit tests for domain behavior, and do not commit local IDE metadata or generated build output.
