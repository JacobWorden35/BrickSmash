# Tests

BrickSmash includes both unit tests and instrumentation tests.

## Unit Tests (JVM)

Located in `app/src/test/java/com/bricksmash/model/`. These run on the JVM
and cover the pure-Kotlin model classes:

- `LevelDataTest` — verifies brick counting logic
- `ScoreEntryTest` — verifies Firestore map round-trip
- `UserProfileTest` — verifies profile field mapping and defaults

Run from Android Studio: right-click the `test` folder and select **Run 'Tests in ...'**,
or from the command line:

```bash
./gradlew test
```

## Instrumentation Tests (Device/Emulator)

Located in `app/src/androidTest/java/com/bricksmash/game/`. These require
an Android runtime because they depend on `android.graphics`:

- `BrickTest` — verifies hit tracking and destruction logic for all brick types

Run from Android Studio: right-click the `androidTest` folder and select **Run**,
or from the command line with a device/emulator connected:

```bash
./gradlew connectedAndroidTest
```
