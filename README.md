# RCV Bible Reader

Offline Android app for reading the Recovery Version of the Bible with tap-to-expand footnotes. Kotlin, Jetpack Compose, Room, Material 3.

## Quick Start

1. **Open** in Android Studio (Ladybug 2024.2+): File > Open > select `rcv_reader/`
2. **Build**: Android Studio syncs automatically. Or from CLI:
   ```bash
   export JAVA_HOME=/opt/homebrew/opt/openjdk@17  # if default JDK isn't 17
   ./gradlew assembleDebug
   ```
3. **Run**: Select a device/emulator (API 26+) and click Run

Android Studio creates `local.properties` automatically. If not:
```properties
sdk.dir=/Users/<you>/Library/Android/sdk
```

## Project Structure

```
app/src/main/
├── assets/bible.db              # Pre-built SQLite (10 MB, 66 books, 31K verses, 15K footnotes)
└── java/com/rcvreader/
    ├── MainActivity.kt          # Entry point
    ├── data/
    │   ├── model/               # Room entities: Book, Verse, Footnote
    │   ├── db/                  # DAOs + BibleDatabase singleton
    │   └── repository/          # BibleRepository facade
    └── ui/
        ├── theme/               # Colors, typography, light/dark theme
        ├── reading/             # ReadingScreen, ViewModel, VerseItem, FootnoteSection
        └── navigation/          # NavigationBottomSheet (book/chapter picker)

buildscripts/
├── import_bible_data.py         # Generates bible.db from raw Verses/ and Footnotes/ dirs
└── test_import.py               # Pytest validation suite
```

## Architecture

Single-activity Compose app. All data bundled offline in SQLite via Room's `createFromAsset()`.

```
Room DB (assets/bible.db) → DAOs (Flow) → Repository → ViewModel (StateFlow) → Compose UI
```

- **ReadingViewModel**: Manages navigation, verse expansion, last-read persistence (SharedPreferences)
- **NavigationBottomSheet**: Tabbed book/chapter picker. Book tap → auto-switch to chapters tab
- **VerseItem**: Tap to expand footnotes (single-expansion). Gold dot indicates footnotes exist

## Database

The database is pre-built and committed. To regenerate from raw data:

```bash
python3 buildscripts/import_bible_data.py        # outputs app/src/main/assets/bible.db
python3 -m pytest buildscripts/test_import.py -v  # validate
```

**If you change Room entities**, you must update `CREATE_TABLES_SQL` in the Python script to match Room's exact generated schema (backtick-quoted identifiers, explicit ON UPDATE/DELETE NO ACTION, exact index names). See `CLAUDE.md` for details.

## Packaging

### Debug APK
```bash
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Release APK

1. Create signing key:
   ```bash
   keytool -genkey -v -keystore rcv-reader-release.jks \
     -keyalg RSA -keysize 2048 -validity 10000 -alias rcvreader
   ```

2. Add to `app/build.gradle.kts` inside `android {}`:
   ```kotlin
   signingConfigs {
       create("release") {
           storeFile = file("../rcv-reader-release.jks")
           storePassword = System.getenv("KEYSTORE_PASSWORD") ?: ""
           keyAlias = "rcvreader"
           keyPassword = System.getenv("KEY_PASSWORD") ?: ""
       }
   }
   buildTypes {
       release {
           isMinifyEnabled = true
           proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
           signingConfig = signingConfigs.getByName("release")
       }
   }
   ```

3. Build and install:
   ```bash
   KEYSTORE_PASSWORD=<pw> KEY_PASSWORD=<pw> ./gradlew assembleRelease
   adb install app/build/outputs/apk/release/app-release.apk
   ```

### Google Play (AAB)
```bash
./gradlew bundleRelease
# Upload app/build/outputs/bundle/release/app-release.aab to Play Console
```

## Tech Stack

Kotlin 2.1.0 · AGP 8.7.3 · Compose BOM 2024.12.01 · Room 2.6.1 · Lifecycle 2.8.7 · KSP 2.1.0-1.0.29 · compileSdk 35 · minSdk 26 · JDK 17
