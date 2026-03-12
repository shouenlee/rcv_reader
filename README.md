# RCV Bible Reader

An offline Android app for reading the Recovery Version of the Bible with tap-to-expand footnotes. Built with Kotlin, Jetpack Compose, and Room.

## Prerequisites

- **Android Studio** Ladybug (2024.2) or newer
- **JDK 17** (Android Studio bundles one, or install via `brew install openjdk@17`)
- **Android SDK 35** (install via Android Studio SDK Manager)
- **Python 3.10+** (only needed if regenerating the database)

## Getting Started

### 1. Clone and open

```bash
git clone <repo-url>
cd rcv_reader
```

Open in Android Studio: **File > Open** and select the `rcv_reader` directory.

Android Studio will create `local.properties` automatically with your SDK path. If it doesn't, create it manually:

```properties
sdk.dir=/Users/<you>/Library/Android/sdk
```

### 2. Build

Android Studio will sync Gradle automatically. To build from the command line:

```bash
# If your default JDK is not 17, set JAVA_HOME explicitly
export JAVA_HOME=/opt/homebrew/opt/openjdk@17
./gradlew assembleDebug
```

The debug APK is output to `app/build/outputs/apk/debug/app-debug.apk`.

### 3. Run

- **Emulator**: In Android Studio, create an AVD targeting API 26+ and click Run.
- **Physical device**: Enable Developer Options and USB Debugging on your device, connect via USB, and click Run.

## Project Structure

```
rcv_reader/
├── app/                            # Android application module
│   ├── build.gradle.kts            # Dependencies and SDK config
│   └── src/main/
│       ├── assets/bible.db         # Pre-built SQLite database (10 MB)
│       ├── AndroidManifest.xml
│       └── java/com/rcvreader/
│           ├── MainActivity.kt     # Single-activity entry point
│           ├── data/
│           │   ├── model/          # Room entities: Book, Verse, Footnote
│           │   ├── db/             # Room DAOs and database singleton
│           │   └── repository/     # BibleRepository (data access facade)
│           └── ui/
│               ├── theme/          # Colors, typography, Material 3 theme
│               ├── reading/        # ReadingScreen, VerseItem, FootnoteSection, ViewModel
│               └── navigation/     # NavigationBottomSheet (book/chapter picker)
├── buildscripts/
│   ├── import_bible_data.py        # Generates bible.db from raw text files
│   └── test_import.py             # Pytest suite for the import script
├── Verses/                         # Raw verse text files (66 books)
├── Footnotes/                      # Raw footnote files (nested directories)
├── docs/superpowers/
│   ├── specs/                      # Design specification
│   └── plans/                      # Implementation plan
├── build.gradle.kts                # Project-level Gradle config
├── settings.gradle.kts
└── gradle.properties
```

## Architecture

### Overview

Single-activity, single-screen Compose app. All Bible data is bundled in a pre-built SQLite database shipped inside the APK. No network access required.

```
MainActivity
  └── RCVReaderTheme
        └── ReadingScreen
              ├── ReadingViewModel (state + navigation logic)
              ├── BibleRepository
              │     ├── BookDao      ← Room (Flow)
              │     ├── VerseDao     ← Room (Flow)
              │     └── FootnoteDao  ← Room (suspend)
              └── BibleDatabase.createFromAsset("bible.db")
```

### Data Layer

**Room entities** map directly to SQLite tables:

| Entity | Table | Key Fields |
|--------|-------|------------|
| `Book` | `books` | id, abbreviation, name, testament, chapterCount |
| `Verse` | `verses` | id, bookId, chapter, verseNumber, text, hasFootnotes |
| `Footnote` | `footnotes` | id, bookId, chapter, verseNumber, footnoteNumber, keyword, content |

**DAOs** return `Flow<List<T>>` for reactive UI updates (books, verses) or `suspend` functions for one-shot reads (footnotes, book by ID).

**BibleDatabase** is a Room singleton that loads from `assets/bible.db` on first launch using `createFromAsset()`.

### UI Layer

| Component | Purpose |
|-----------|---------|
| `ReadingScreen` | Main screen: navigation bar, verse list, bottom sheet trigger |
| `ReadingViewModel` | Holds `ReadingUiState`, manages navigation, footnote expansion, SharedPreferences persistence |
| `VerseItem` | Single verse row with superscript number, text, gold footnote dot indicator |
| `FootnoteSection` | Expandable footnote list (AnimatedVisibility) with gold keyword styling |
| `NavigationBottomSheet` | Modal bottom sheet with tabbed Books (3-col grid) and Chapters (6-col grid) panels |

### State Management

`ReadingViewModel` exposes a single `StateFlow<ReadingUiState>` consumed by `ReadingScreen` via `collectAsStateWithLifecycle()`. Key behaviors:

- **Navigation**: `navigateTo(bookId, chapter)` cancels the previous verses Flow, loads new verses, computes adjacent chapters
- **Footnote expansion**: Single-expansion — tapping a verse collapses any previously expanded verse
- **Persistence**: Last-read book/chapter saved to SharedPreferences, restored on launch
- **Book selection**: Two-step flow — `selectBook()` sets a pending book, `navigateTo()` is called when chapter is chosen

### Theme

Warm gold/brown palette following system light/dark mode. Serif font for verse text, sans-serif for UI chrome. Key colors:

- Light background: `#FAF8F4` (warm off-white)
- Dark background: `#1A1715` (warm dark brown)
- Primary/gold: `#8B6914` (light) / `#C49B5E` (dark)

## Database

### Schema

```sql
-- 66 rows, one per Bible book in canonical order
CREATE TABLE books (
    id INTEGER PRIMARY KEY,
    abbreviation TEXT NOT NULL,
    name TEXT NOT NULL,
    testament TEXT NOT NULL,        -- "OT" or "NT"
    chapter_count INTEGER NOT NULL
);

-- 31,103 rows
CREATE TABLE verses (
    id INTEGER PRIMARY KEY,
    book_id INTEGER NOT NULL REFERENCES books(id),
    chapter INTEGER NOT NULL,
    verse_number INTEGER NOT NULL,
    text TEXT NOT NULL,
    has_footnotes INTEGER NOT NULL  -- 0 or 1
);
CREATE INDEX idx_verses_lookup ON verses(book_id, chapter);

-- 14,876 rows
CREATE TABLE footnotes (
    id INTEGER PRIMARY KEY,
    book_id INTEGER NOT NULL REFERENCES books(id),
    chapter INTEGER NOT NULL,
    verse_number INTEGER NOT NULL,  -- 0 for Psalms superscriptions
    footnote_number INTEGER NOT NULL,
    keyword TEXT,                   -- nullable; extracted from footnote text
    content TEXT NOT NULL
);
CREATE INDEX idx_footnotes_lookup ON footnotes(book_id, chapter, verse_number);
```

### Regenerating the Database

The database is pre-built and committed at `app/src/main/assets/bible.db`. To regenerate it from the raw `Verses/` and `Footnotes/` directories:

```bash
python3 buildscripts/import_bible_data.py
```

This reads all raw text files and outputs `bible.db` (~10 MB). The script handles four verse format variants across different books and extracts footnote keywords automatically.

Run the tests to validate:

```bash
python3 -m pytest buildscripts/test_import.py -v
```

**Important**: The database schema must exactly match what Room expects (column names, types, foreign key clauses, index names). The Python script generates SQL that mirrors Room's generated `BibleDatabase_Impl.java`. If you change the Room entities, you must update the Python script's `CREATE_TABLES_SQL` to match.

## Packaging for Installation

### Debug APK (development)

```bash
./gradlew assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`

Install directly to a connected device:

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

Or from Android Studio: **Run > Run 'app'** with a device/emulator selected.

### Release APK (distribution)

#### 1. Create a signing key (one-time)

```bash
keytool -genkey -v -keystore rcv-reader-release.jks \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias rcvreader
```

Keep the keystore file and passwords safe. Do not commit to git.

#### 2. Configure signing in `app/build.gradle.kts`

Add a `signingConfigs` block inside `android {}`:

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
        proguardFiles(
            getDefaultProguardFile("proguard-android-optimize.txt"),
            "proguard-rules.pro"
        )
        signingConfig = signingConfigs.getByName("release")
    }
}
```

#### 3. Build the release APK

```bash
KEYSTORE_PASSWORD=<your-pw> KEY_PASSWORD=<your-pw> ./gradlew assembleRelease
```

Output: `app/build/outputs/apk/release/app-release.apk`

#### 4. Install

```bash
adb install app/build/outputs/apk/release/app-release.apk
```

Or distribute the APK file directly — recipients can install it by opening the file on their Android device (requires "Install from unknown sources" enabled).

### Android App Bundle (Google Play)

To publish on Google Play, build an AAB instead:

```bash
./gradlew bundleRelease
```

Output: `app/build/outputs/bundle/release/app-release.aab`

Upload the `.aab` file to the Google Play Console.

## Tech Stack

| Component | Version |
|-----------|---------|
| Kotlin | 2.1.0 |
| Android Gradle Plugin | 8.7.3 |
| Compile SDK | 35 (Android 15) |
| Min SDK | 26 (Android 8.0) |
| Jetpack Compose BOM | 2024.12.01 |
| Room | 2.6.1 |
| Lifecycle | 2.8.7 |
| KSP | 2.1.0-1.0.29 |
| JDK | 17 |

## Future Scope

These features are not in v1 but the architecture supports them:

- **Full-text search** — FTS5 virtual tables are pre-created (empty) in the database
- **Bookmarks and highlights** — Add new Room entities
- **Font size settings** — Theme typography is centralized in `Type.kt`
- **Cross-reference navigation** — Footnote content often references other verses
- **Reading plans** — Track progress via new table + SharedPreferences
