# DoseTrack

A privacy-first Android app for logging self-administered injections: when each dose was taken, where it was injected, and how your body reacted.

All data stays on the device. The app has no internet permission, no account, no analytics and no ads.

## Screenshots

<p>
  <a href="screenshots/1_log_drug.png"><img src="screenshots/1_log_drug.png" width="160" alt="Log an injection on the body diagram"></a>
  <a href="screenshots/2_history.png"><img src="screenshots/2_history.png" width="160" alt="History of doses and notes"></a>
  <a href="screenshots/3_reminders.png"><img src="screenshots/3_reminders.png" width="160" alt="A medication's reminder, ending after 12 doses"></a>
  <a href="screenshots/4_medications.png"><img src="screenshots/4_medications.png" width="160" alt="Medication list with routes and reminders"></a>
  <a href="screenshots/5_log_details.png"><img src="screenshots/5_log_details.png" width="160" alt="Logging a dose of an oral medication"></a>
  <a href="screenshots/6_guide.png"><img src="screenshots/6_guide.png" width="160" alt="Built-in user guide"></a>
  <a href="screenshots/7_home.png"><img src="screenshots/7_home.png" width="160" alt="Home screen with quick actions and the app version"></a>
</p>

Screenshots use fictional sample data.

## Features

- **Home screen.** One tap to log a dose, add a note or add a new medication. The installed version is shown in the top-right corner.
- **Injection log.** Tap the site on a body diagram, pick a medication and dose, and record reactions and notes. Note-only entries are supported for days without an injection.
- **Site rotation.** A grid splits the body diagram into injection zones, and previous injection sites are shaded in light blue on it (the count is configurable), so the newly picked site in solid blue stands out.
- **Medications.** Add medications from the Meds tab or straight from Home. Save medications with a dose in mg, mL, mcg, IU or units, and how each is taken (injection, oral, topical, inhaled or other). Doses of medications that aren't injected are logged without a body site.
- **History.** Browse, edit and delete every entry.
- **Save confirmations.** A short message confirms every saved dose, note and medication, whether it was just added or edited.
- **Reminders.** Each medication can have its own reminder: on chosen days of the week, or every N hours, days, weeks or months. A reminder can run indefinitely, end on a date, or end after a number of logged doses. Tapping the notification opens the log with that medication selected. Reminders are re-armed after a reboot, time change or app update. Lock-screen notifications are discreet.
- **Bottom navigation.** Home, History, Meds, Settings and Guide tabs. Labels stay on one line, even with a wide system font or a larger font size.
- **Built-in guide.** A Guide tab explains every feature step by step, and opens automatically the first time a new user starts the app.
- **Encrypted backups.** Export to a file or share it, then restore on any device.

## Privacy and security

| Area | How it works |
| --- | --- |
| Network | No `INTERNET` permission, so the app cannot send data anywhere |
| Storage | Room database and DataStore in app-private storage |
| Android backup | Cloud backup is fully excluded. Only direct device-to-device transfer is allowed (`res/xml/data_extraction_rules.xml`, `backup_rules.xml`) |
| Backup files | AES-256-GCM, with the key derived via PBKDF2-HMAC-SHA256 from a user password. The header is bound to the ciphertext as associated data (`export/BackupCrypto.kt`) |
| Notifications | `VISIBILITY_PRIVATE` with a generic public version for the lock screen |

See the full [privacy policy](PRIVACY.md).

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
| minSdk | 29 (Android 10) |

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

Before each Play upload, raise `versionCode` and `versionName` in `app/build.gradle.kts`. Play rejects a `versionCode` it has seen before. The Home screen reads `versionName` through `BuildConfig.VERSION_NAME`, so it always shows the version you built.

Never commit keystores. `*.jks` and `*.keystore` are already in `.gitignore`.

### Store assets

- `screenshots/`: Play Store and README screenshots, 1280×2560, taken on a Pixel 9 Pro emulator with the status and gesture bars cropped off.
- `play-store/feature_graphic_1024x500.png`: the Play feature graphic, built from the Log Drug screenshot. Rebuild it whenever that screenshot changes:

```bash
python3 play-store/feature_graphic.py play-store screenshots/1_log_drug.png
```

## Project structure

```
app/src/main/java/com/dosetrack/
├── data/        Room database, DAOs, entities, domain models, repositories, settings
├── di/          AppContainer: manual dependency wiring
├── export/      Backup export/import, encryption, CSV format, password policy
├── reminders/   Alarm scheduling, notification, boot receiver, next-fire-time calculation
├── ui/          Compose screens and ViewModels (home, log entry, history, medications, settings, guide)
└── util/        Date formatting and permission helpers
```

The Room schema is exported to `app/schemas/` for migration tracking.

## Tests

Unit tests in `app/src/test` cover data validation, backup encryption and format, CSV parsing, backup restore, password policy, reminder time calculation, input validation and injection-site grids.

## License

DoseTrack is free software, licensed under the [GNU General Public License v3.0](LICENSE). You can use, study, modify and share the code. If you distribute a modified version, it must also be released under GPL-3.0, with its source code.

**Name and logo:** under the [additional terms in NOTICE](NOTICE), a modified version must use its own name, its own icon and a different application ID. It must not present itself as the official DoseTrack. Saying "based on DoseTrack" is fine.

The official DoseTrack is, and will stay, free, ad-free and without any data collection.

## Disclaimer

DoseTrack is a personal logging tool, not a medical device. It does not give medical advice or recommend doses. Always follow the instructions of your doctor or pharmacist.
