# DoseTrack

A privacy-first Android app for logging self-administered injections: when each dose was taken, where it was injected, and how your body reacted.

All data stays on the device. The app has no internet permission, no account, no analytics and no ads.

## Features

- **Injection log.** Tap the site on a body diagram, pick a medication and dose, and record reactions and notes. Note-only entries are supported for days without an injection.
- **Site rotation.** Previous injection sites are shown on the diagram (the count is configurable). An optional guide grid splits the diagram into injection zones.
- **Medications.** Save medications with a dose in mg, mL, mcg, IU or units.
- **History.** Browse, edit and delete every entry.
- **Reminders.** Remind on chosen days of the week, or every N hours, days, weeks or months. Reminders are re-armed after a reboot, time change or app update. Lock-screen notifications are discreet.
- **Encrypted backups.** Export to a file or share it, then restore on any device.

## Privacy and security

| Area | How it works |
| --- | --- |
| Network | No `INTERNET` permission, so the app cannot send data anywhere |
| Storage | Room database and DataStore in app-private storage |
| Android backup | Cloud backup is fully excluded. Only direct device-to-device transfer is allowed (`res/xml/data_extraction_rules.xml`, `backup_rules.xml`) |
| Backup files | AES-256-GCM, with the key derived via PBKDF2-HMAC-SHA256 from a user password. The header is bound to the ciphertext as associated data (`export/BackupCrypto.kt`) |
| Notifications | `VISIBILITY_PRIVATE` with a generic public version for the lock screen |

## Tech stack

- Kotlin, Jetpack Compose (Material 3), Navigation Compose
- Room (with KSP) for the injection and medication database
- DataStore Preferences for settings
- AlarmManager exact alarms for reminders, with an inexact fallback when the permission is denied
- Manual dependency injection through `AppContainer`, with no DI framework

## Requirements

| Item | Version |
| --- | --- |
| JDK | 17 |
| Gradle | 9.9.2 (wrapper included) |
| Android Gradle Plugin | 9.5.2 |
| compileSdk | 37 (minor API level 2) |
| targetSdk | 36 |
| minSdk | 30 (Android 11) |

Use a recent Android Studio that supports AGP 9.5.

## Building

```bash
# Debug APK
./gradlew :app:assembleDebug

# Install on a connected device or emulator
./gradlew :app:installDebug

# Unit tests
./gradlew :app:testDebugUnitTest

# Lint
./gradlew :app:lintDebug
```

### Release builds

Release builds are signed with credentials read from `~/.gradle/gradle.properties`, which lives outside the repository:

```properties
dosetrack.storeFile=/absolute/path/to/upload.jks
dosetrack.storePassword=...
dosetrack.keyAlias=upload
dosetrack.keyPassword=...
```

Then build the Play Store bundle:

```bash
./gradlew :app:bundleRelease
# Output: app/build/outputs/bundle/release/app-release.aab
```

Never commit keystores. `*.jks` and `*.keystore` are already in `.gitignore`.

## Project structure

```
app/src/main/java/com/dosetrack/
├── data/        Room database, DAOs, entities, domain models, repositories, settings
├── di/          AppContainer: manual dependency wiring
├── export/      Backup export/import, encryption, CSV format, password policy
├── reminders/   Alarm scheduling, notification, boot receiver, next-fire-time calculation
├── ui/          Compose screens and ViewModels (home, log entry, history, medications, settings)
└── util/        Date formatting and permission helpers
```

The Room schema is exported to `app/schemas/` for migration tracking.

## Tests

Unit tests in `app/src/test` cover data validation, backup encryption and format, CSV parsing, backup restore, password policy, reminder time calculation, input validation and injection-site grids.

## Disclaimer

DoseTrack is a personal logging tool, not a medical device. It does not give medical advice or recommend doses. Always follow the instructions of your doctor or pharmacist.
