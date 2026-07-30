# ScoreX Cricket Scorer

ScoreX is an offline Android cricket-scoring app built with Kotlin and Jetpack Compose. Record ball-by-ball scoring, extras, wickets, partnerships, scorecards, analytics, match history, and Super Overs without an account or network connection.

## Features

- Ball-by-ball scoring for runs, wides, no-balls, byes, leg byes, and wickets
- Live batting, bowling, extras, fall-of-wicket, partnership, and run-rate statistics
- Match setup with custom teams, player rosters, toss, and T20/ODI/Test/custom-over formats
- Match history stored locally with Room
- Scorecard, match-recap, and analytics screens
- Undo and redo support for the current innings
- Super Over scoring for tied matches

## Requirements

- Android Studio with Android SDK Platform 36 installed
- JDK 17 or newer

## Build and test

The Gradle wrapper is included, so no system Gradle installation is required.

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat testDebugUnitTest
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

## Run

1. Open this folder in Android Studio.
2. Allow Gradle sync to finish.
3. Choose an Android emulator or connected device.
4. Run the `app` configuration.

## Privacy

ScoreX stores match data only on the device. It does not require an API key, account, or network permission.

## Release signing

Release builds are unsigned by default. Keep your keystore and signing credentials outside the repository, then sign the generated release artifact in Android Studio or your CI pipeline.

## License

Released under the [MIT License](LICENSE).
