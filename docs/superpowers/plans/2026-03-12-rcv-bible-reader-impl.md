# RCV Bible Reader Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build an offline Android Bible reading app that displays Recovery Version verses with tap-to-expand footnotes, navigable by book and chapter.

**Architecture:** Single-activity Jetpack Compose app backed by a pre-built SQLite database (via Room). A Python build script converts raw text files into the bundled `bible.db`. The UI is one screen with toolbar dropdowns for navigation and a LazyColumn of expandable verse items.

**Tech Stack:** Kotlin, Jetpack Compose (Material 3), Room, Coroutines/Flow, SharedPreferences, Python 3 (build script)

**Spec:** `docs/superpowers/specs/2026-03-12-rcv-bible-reader-design.md`

---

## Chunk 1: Project Scaffolding + Build Script + Database

### Task 1: Create Android Project Skeleton

**Files:**
- Create: `build.gradle.kts` (project-level)
- Create: `settings.gradle.kts`
- Create: `gradle.properties`
- Create: `app/build.gradle.kts`
- Create: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/java/com/rcvreader/MainActivity.kt`
- Create: `app/src/main/res/values/strings.xml`
- Create: `app/src/main/res/values/themes.xml`

- [ ] **Step 1: Create project-level build.gradle.kts**

```kotlin
// build.gradle.kts (project root)
plugins {
    id("com.android.application") version "8.7.3" apply false
    id("org.jetbrains.kotlin.android") version "2.1.0" apply false
    id("com.google.devtools.ksp") version "2.1.0-1.0.29" apply false
}
```

- [ ] **Step 2: Create settings.gradle.kts**

```kotlin
// settings.gradle.kts
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "RCVReader"
include(":app")
```

- [ ] **Step 3: Create gradle.properties**

```properties
# gradle.properties
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
android.useAndroidX=true
kotlin.code.style=official
android.nonTransitiveRClass=true
```

- [ ] **Step 4: Create app/build.gradle.kts**

```kotlin
// app/build.gradle.kts
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.0"
}

android {
    namespace = "com.rcvreader"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.rcvreader"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    // Compose
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Activity
    implementation("androidx.activity:activity-compose:1.9.3")

    // Lifecycle + ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
}
```

- [ ] **Step 5: Create AndroidManifest.xml**

```xml
<!-- app/src/main/AndroidManifest.xml -->
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <application
        android:allowBackup="true"
        android:label="@string/app_name"
        android:supportsRtl="true"
        android:theme="@style/Theme.RCVReader">
        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

- [ ] **Step 6: Create minimal MainActivity.kt**

```kotlin
// app/src/main/java/com/rcvreader/MainActivity.kt
package com.rcvreader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Text

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Text("RCV Reader")
        }
    }
}
```

- [ ] **Step 7: Create res/values/strings.xml and themes.xml**

```xml
<!-- app/src/main/res/values/strings.xml -->
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">RCV Reader</string>
</resources>
```

```xml
<!-- app/src/main/res/values/themes.xml -->
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.RCVReader" parent="android:Theme.Material.Light.NoActionBar" />
</resources>
```

- [ ] **Step 8: Add Gradle wrapper**

Run:
```bash
cd /Users/work/Documents/repos/rcv_reader
gradle wrapper --gradle-version 8.11.1
```

- [ ] **Step 9: Verify project compiles**

Run:
```bash
./gradlew assembleDebug
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 10: Commit**

```bash
git add -A
git commit -m "feat: scaffold Android project with Compose + Room dependencies"
```

---

### Task 2: Python Build Script — Import Bible Data into SQLite

**Files:**
- Create: `buildscripts/import_bible_data.py`
- Create: `buildscripts/test_import.py`

This script reads the raw `Verses/` and `Footnotes/` directories and produces `app/src/main/assets/bible.db`.

- [ ] **Step 1: Write test for book mapping**

```python
# buildscripts/test_import.py
import sqlite3
from pathlib import Path

import pytest
from import_bible_data import BOOK_MAP, get_book_info

def test_book_map_has_66_entries():
    assert len(BOOK_MAP) == 66

def test_genesis_is_first():
    info = get_book_info("Gen")
    assert info["id"] == 1
    assert info["name"] == "Genesis"
    assert info["testament"] == "OT"

def test_revelation_is_last():
    info = get_book_info("Rev")
    assert info["id"] == 66
    assert info["name"] == "Revelation"
    assert info["testament"] == "NT"

def test_single_chapter_books():
    for abbr in ["2John", "3John", "Jude", "Obad", "Philemon"]:
        info = get_book_info(abbr)
        assert info is not None, f"{abbr} not in BOOK_MAP"
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd buildscripts && python -m pytest test_import.py -v`
Expected: FAIL — `import_bible_data` not found

- [ ] **Step 3: Write import_bible_data.py — book mapping**

```python
#!/usr/bin/env python3
"""Import Bible verse and footnote data from raw text files into SQLite."""

import os
import re
import sqlite3
import sys
from pathlib import Path

# Folder name → (canonical_id, full_name, testament)
BOOK_MAP = {
    "Gen": (1, "Genesis", "OT"),
    "Exo": (2, "Exodus", "OT"),
    "Lev": (3, "Leviticus", "OT"),
    "Num": (4, "Numbers", "OT"),
    "Deut": (5, "Deuteronomy", "OT"),
    "Josh": (6, "Joshua", "OT"),
    "Judg": (7, "Judges", "OT"),
    "Ruth": (8, "Ruth", "OT"),
    "1Sam": (9, "1 Samuel", "OT"),
    "2Sam": (10, "2 Samuel", "OT"),
    "1Kings": (11, "1 Kings", "OT"),
    "2Kings": (12, "2 Kings", "OT"),
    "1Chron": (13, "1 Chronicles", "OT"),
    "2Chron": (14, "2 Chronicles", "OT"),
    "Ezra": (15, "Ezra", "OT"),
    "Neh": (16, "Nehemiah", "OT"),
    "Esther": (17, "Esther", "OT"),
    "Job": (18, "Job", "OT"),
    "Psa": (19, "Psalms", "OT"),
    "Prov": (20, "Proverbs", "OT"),
    "Eccl": (21, "Ecclesiastes", "OT"),
    "SOS": (22, "Song of Songs", "OT"),
    "Isa": (23, "Isaiah", "OT"),
    "Jer": (24, "Jeremiah", "OT"),
    "Lam": (25, "Lamentations", "OT"),
    "Ezek": (26, "Ezekiel", "OT"),
    "Dan": (27, "Daniel", "OT"),
    "Hos": (28, "Hosea", "OT"),
    "Joel": (29, "Joel", "OT"),
    "Amos": (30, "Amos", "OT"),
    "Obad": (31, "Obadiah", "OT"),
    "Jonah": (32, "Jonah", "OT"),
    "Micah": (33, "Micah", "OT"),
    "Nahum": (34, "Nahum", "OT"),
    "Hab": (35, "Habakkuk", "OT"),
    "Zeph": (36, "Zephaniah", "OT"),
    "Hag": (37, "Haggai", "OT"),
    "Zech": (38, "Zechariah", "OT"),
    "Mal": (39, "Malachi", "OT"),
    "Matt": (40, "Matthew", "NT"),
    "Mark": (41, "Mark", "NT"),
    "Luke": (42, "Luke", "NT"),
    "John": (43, "John", "NT"),
    "Acts": (44, "Acts", "NT"),
    "Rom": (45, "Romans", "NT"),
    "1Cor": (46, "1 Corinthians", "NT"),
    "2Cor": (47, "2 Corinthians", "NT"),
    "Gal": (48, "Galatians", "NT"),
    "Eph": (49, "Ephesians", "NT"),
    "Phil": (50, "Philippians", "NT"),
    "Col": (51, "Colossians", "NT"),
    "1Thes": (52, "1 Thessalonians", "NT"),
    "2Thes": (53, "2 Thessalonians", "NT"),
    "1Tim": (54, "1 Timothy", "NT"),
    "2Tim": (55, "2 Timothy", "NT"),
    "Titus": (56, "Titus", "NT"),
    "Philemon": (57, "Philemon", "NT"),
    "Heb": (58, "Hebrews", "NT"),
    "James": (59, "James", "NT"),
    "1Pet": (60, "1 Peter", "NT"),
    "2Pet": (61, "2 Peter", "NT"),
    "1John": (62, "1 John", "NT"),
    "2John": (63, "2 John", "NT"),
    "3John": (64, "3 John", "NT"),
    "Jude": (65, "Jude", "NT"),
    "Rev": (66, "Revelation", "NT"),
}


def get_book_info(folder_name: str) -> dict | None:
    """Look up book info by folder name. Returns dict with id, name, testament or None."""
    entry = BOOK_MAP.get(folder_name)
    if entry is None:
        return None
    return {"id": entry[0], "name": entry[1], "testament": entry[2]}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd buildscripts && python -m pytest test_import.py::test_book_map_has_66_entries test_import.py::test_genesis_is_first test_import.py::test_revelation_is_last test_import.py::test_single_chapter_books -v`
Expected: 4 PASSED

- [ ] **Step 5: Write test for verse text extraction**

Add to `buildscripts/test_import.py`:

```python
from import_bible_data import extract_verse_text

def test_extract_standard_format():
    line = "Gen. 1:1 In the beginning God created the heavens and the earth."
    assert extract_verse_text(line) == "In the beginning God created the heavens and the earth."

def test_extract_no_period_format():
    line = "1 Chron. 1:1 Adam, Seth, Enosh,"
    assert extract_verse_text(line) == "Adam, Seth, Enosh,"

def test_extract_no_space_format():
    line = "Neh.1:1 The words of Nehemiah the son of Hacaliah."
    assert extract_verse_text(line) == "The words of Nehemiah the son of Hacaliah."

def test_extract_single_chapter_format():
    line = "Jude 1 Jude, a slave of Jesus Christ and a brother of James."
    assert extract_verse_text(line) == "Jude, a slave of Jesus Christ and a brother of James."

def test_extract_obadiah():
    line = "Obad. 1 The vision of Obadiah."
    assert extract_verse_text(line) == "The vision of Obadiah."

def test_extract_2john():
    line = "2 John 1 The elder to the chosen lady."
    assert extract_verse_text(line) == "The elder to the chosen lady."
```

- [ ] **Step 6: Run test to verify it fails**

Run: `cd buildscripts && python -m pytest test_import.py::test_extract_standard_format -v`
Expected: FAIL — `extract_verse_text` not found

- [ ] **Step 7: Implement extract_verse_text**

Add to `buildscripts/import_bible_data.py`:

```python
# Regex to match the reference prefix in all four format variants.
# Captures everything up to and including the verse number + space.
# Group 1: the verse text after the prefix.
#
# Variants matched:
#   "Gen. 1:1 text"        — standard (Book. Ch:V)
#   "1 Chron. 1:1 text"    — numbered book with space (Book Ch:V)
#   "Neh.1:1 text"         — no space before chapter (Book.Ch:V)
#   "Jude 1 text"          — single-chapter (Book V)
#   "2 John 1 text"        — numbered single-chapter
#   "Obad. 1 text"         — single-chapter with period
#
# Strategy: match the reference pattern, then take everything after.
_VERSE_PREFIX_RE = re.compile(
    r'^.+?\d+[:\s](\d+)\s'  # lazy match up to chapter:verse or book verse
)


def extract_verse_text(line: str) -> str:
    """Strip the reference prefix from a verse line, returning just the verse body.

    Handles four format variants:
      - "Gen. 1:1 text"       — standard (Book. Ch:V)
      - "1 Chron. 1:1 text"   — numbered book (Book Ch:V)
      - "Neh.1:1 text"        — no space (Book.Ch:V)
      - "Jude 1 text"         — single-chapter (Book V)
      - "2 John 1 text"       — numbered single-chapter
      - "Obad. 1 text"        — single-chapter with period
    """
    # Try Ch:V pattern first (most common — matches "1:1 ", "28:19 ", etc.)
    m = re.search(r':\d+\s', line)
    if m:
        return line[m.end():]

    # Single-chapter format: find the LAST standalone number followed by a space and non-digit.
    # This avoids matching the leading "2" or "3" in "2 John" / "3 John".
    last_match = None
    for last_match in re.finditer(r'(?:^|\s)(\d+)\s(?=\D)', line):
        pass
    if last_match:
        return line[last_match.end():]

    return line  # fallback: return whole line
```

- [ ] **Step 8: Run all extract tests to verify they pass**

Run: `cd buildscripts && python -m pytest test_import.py -k "extract" -v`
Expected: 6 PASSED

- [ ] **Step 9: Write test for footnote parsing**

Add to `buildscripts/test_import.py`:

```python
from import_bible_data import parse_footnote_content

def test_parse_standard_footnote():
    text = "Gen. 1:1.1 - In: The Bible, composed of two testaments."
    keyword, content = parse_footnote_content(text)
    assert keyword == "In"
    assert content == "The Bible, composed of two testaments."

def test_parse_footnote_no_separator():
    text = "Maschil: The meaning of this term is uncertain."
    keyword, content = parse_footnote_content(text)
    assert keyword is None
    assert content == "Maschil: The meaning of this term is uncertain."

def test_parse_footnote_keyword_with_colon_in_content():
    text = "Gen. 1:2.1 - But: God created the earth in a good order (Job 38:4-7; Isa. 45:18)."
    keyword, content = parse_footnote_content(text)
    assert keyword == "But"
    assert "God created" in content
    assert "Job 38:4-7" in content
```

- [ ] **Step 10: Run test to verify it fails**

Run: `cd buildscripts && python -m pytest test_import.py::test_parse_standard_footnote -v`
Expected: FAIL

- [ ] **Step 11: Implement parse_footnote_content**

Add to `buildscripts/import_bible_data.py`:

```python
def parse_footnote_content(text: str) -> tuple[str | None, str]:
    """Parse footnote text into (keyword, content).

    Format: "{reference} - {keyword}: {content}"
    If no " - " separator, returns (None, full_text).
    """
    parts = text.split(" - ", 1)
    if len(parts) < 2:
        return None, text

    body = parts[1]
    colon_idx = body.find(":")
    if colon_idx == -1:
        return None, body

    keyword = body[:colon_idx].strip()
    content = body[colon_idx + 1:].strip()
    return keyword, content
```

- [ ] **Step 12: Run all footnote parse tests**

Run: `cd buildscripts && python -m pytest test_import.py -k "parse" -v`
Expected: 3 PASSED

- [ ] **Step 13: Write test for full database generation**

Add to `buildscripts/test_import.py`:

```python
from import_bible_data import build_database

@pytest.fixture(scope="session")
def bible_db(tmp_path_factory):
    """Build bible.db once per test session, shared across all integration tests."""
    db_path = tmp_path_factory.mktemp("data") / "bible.db"
    verses_dir = Path(__file__).parent.parent / "Verses"
    footnotes_dir = Path(__file__).parent.parent / "Footnotes"
    build_database(str(verses_dir), str(footnotes_dir), str(db_path))
    return str(db_path)

def test_build_database_creates_all_tables(bible_db):
    """Run build_database against actual data and verify structure."""
    db_path = bible_db

    conn = sqlite3.connect(db_path)
    cursor = conn.cursor()

    # Check books
    cursor.execute("SELECT COUNT(*) FROM books")
    assert cursor.fetchone()[0] == 66

    # Check Genesis has 50 chapters
    cursor.execute("SELECT chapter_count FROM books WHERE abbreviation = 'Gen'")
    assert cursor.fetchone()[0] == 50

    # Check verse count for Genesis 1
    cursor.execute("""
        SELECT COUNT(*) FROM verses
        WHERE book_id = 1 AND chapter = 1
    """)
    count = cursor.fetchone()[0]
    assert count >= 31  # Genesis 1 has 31 verses

    # Check verse text doesn't contain reference prefix
    cursor.execute("""
        SELECT text FROM verses
        WHERE book_id = 1 AND chapter = 1 AND verse_number = 1
    """)
    text = cursor.fetchone()[0]
    assert text.startswith("In the beginning")
    assert not text.startswith("Gen")

    # Check has_footnotes flag
    cursor.execute("""
        SELECT has_footnotes FROM verses
        WHERE book_id = 1 AND chapter = 1 AND verse_number = 1
    """)
    assert cursor.fetchone()[0] == 1

    # Check footnotes exist
    cursor.execute("""
        SELECT COUNT(*) FROM footnotes
        WHERE book_id = 1 AND chapter = 1 AND verse_number = 1
    """)
    assert cursor.fetchone()[0] >= 5  # Gen 1:1 has 5 footnotes

    # Check footnote keyword extraction
    cursor.execute("""
        SELECT keyword, content FROM footnotes
        WHERE book_id = 1 AND chapter = 1 AND verse_number = 1 AND footnote_number = 1
    """)
    row = cursor.fetchone()
    assert row[0] == "In"
    assert "Bible" in row[1]

    # Check single-chapter book (Jude)
    cursor.execute("""
        SELECT text FROM verses
        WHERE book_id = 65 AND chapter = 1 AND verse_number = 1
    """)
    text = cursor.fetchone()[0]
    assert text.startswith("Jude, a slave")

    # Check total verse count is reasonable (31,102 in standard Bible)
    cursor.execute("SELECT COUNT(*) FROM verses")
    total = cursor.fetchone()[0]
    assert total > 30000

    conn.close()


def test_psalms_superscription_footnotes(bible_db):
    """Verify Psalms superscription footnotes (verse=0) are imported."""
    conn = sqlite3.connect(bible_db)
    cursor = conn.cursor()

    # Psalms = book_id 19, check verse_number=0 footnotes exist
    cursor.execute("""
        SELECT COUNT(*) FROM footnotes
        WHERE book_id = 19 AND verse_number = 0
    """)
    count = cursor.fetchone()[0]
    assert count > 0, "Psalms superscription footnotes should be imported"

    conn.close()
```

- [ ] **Step 14: Run test to verify it fails**

Run: `cd buildscripts && python -m pytest test_import.py::test_build_database_creates_all_tables -v`
Expected: FAIL — `build_database` not found

- [ ] **Step 15: Implement build_database**

Add to `buildscripts/import_bible_data.py`:

```python
def build_database(verses_dir: str, footnotes_dir: str, db_path: str) -> None:
    """Build the bible.db SQLite database from raw text files."""
    conn = sqlite3.connect(db_path)
    cursor = conn.cursor()

    # Create tables
    cursor.executescript("""
        CREATE TABLE books (
            id INTEGER PRIMARY KEY,
            abbreviation TEXT NOT NULL,
            name TEXT NOT NULL,
            testament TEXT NOT NULL,
            chapter_count INTEGER NOT NULL
        );

        CREATE TABLE verses (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            book_id INTEGER NOT NULL REFERENCES books(id),
            chapter INTEGER NOT NULL,
            verse_number INTEGER NOT NULL,
            text TEXT NOT NULL,
            has_footnotes INTEGER NOT NULL DEFAULT 0
        );

        CREATE TABLE footnotes (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            book_id INTEGER NOT NULL REFERENCES books(id),
            chapter INTEGER NOT NULL,
            verse_number INTEGER NOT NULL,
            footnote_number INTEGER NOT NULL,
            keyword TEXT,
            content TEXT NOT NULL
        );

        CREATE INDEX idx_verses_lookup ON verses(book_id, chapter);
        CREATE INDEX idx_footnotes_lookup ON footnotes(book_id, chapter, verse_number);
    """)

    verses_path = Path(verses_dir)
    footnotes_path = Path(footnotes_dir)

    # Import books and verses
    for entry in sorted(verses_path.iterdir()):
        if not entry.is_dir():
            continue  # skip Makefile etc.

        folder_name = entry.name
        book_info = get_book_info(folder_name)
        if book_info is None:
            print(f"WARNING: Unknown book folder '{folder_name}', skipping")
            continue

        book_id = book_info["id"]

        # Count chapters
        chapter_files = sorted(
            [f for f in entry.iterdir() if f.is_file() and f.name.isdigit()],
            key=lambda f: int(f.name)
        )
        chapter_count = len(chapter_files)

        # Insert book
        cursor.execute(
            "INSERT INTO books (id, abbreviation, name, testament, chapter_count) VALUES (?, ?, ?, ?, ?)",
            (book_id, folder_name, book_info["name"], book_info["testament"], chapter_count)
        )

        # Import verses for each chapter
        for chapter_file in chapter_files:
            chapter_num = int(chapter_file.name)

            with open(chapter_file, "r", encoding="utf-8") as f:
                lines = f.readlines()

            for verse_num, line in enumerate(lines, start=1):
                line = line.strip()
                if not line:
                    continue

                verse_text = extract_verse_text(line)

                # Check if footnotes exist for this verse
                fn_verse_dir = footnotes_path / folder_name / str(chapter_num) / str(verse_num)
                has_footnotes = 1 if fn_verse_dir.is_dir() else 0

                cursor.execute(
                    "INSERT INTO verses (book_id, chapter, verse_number, text, has_footnotes) VALUES (?, ?, ?, ?, ?)",
                    (book_id, chapter_num, verse_num, verse_text, has_footnotes)
                )

    # Import footnotes
    for entry in sorted(footnotes_path.iterdir()):
        if not entry.is_dir():
            continue

        folder_name = entry.name
        book_info = get_book_info(folder_name)
        if book_info is None:
            continue

        book_id = book_info["id"]

        for chapter_dir in sorted(entry.iterdir(), key=lambda d: int(d.name) if d.name.isdigit() else 0):
            if not chapter_dir.is_dir():
                continue
            chapter_num = int(chapter_dir.name)

            for verse_dir in sorted(chapter_dir.iterdir(), key=lambda d: int(d.name) if d.name.isdigit() else 0):
                if not verse_dir.is_dir():
                    continue
                verse_num = int(verse_dir.name)

                # Sort footnote files; handle * and ** for Psalms superscriptions
                fn_files = sorted(verse_dir.iterdir(), key=lambda f: (
                    int(f.name) if f.name.isdigit() else 0
                ))

                for fn_idx, fn_file in enumerate(fn_files):
                    if not fn_file.is_file():
                        continue

                    fn_number = int(fn_file.name) if fn_file.name.isdigit() else 0

                    with open(fn_file, "r", encoding="utf-8") as f:
                        text = f.read().strip()

                    if not text:
                        continue

                    keyword, content = parse_footnote_content(text)

                    cursor.execute(
                        "INSERT INTO footnotes (book_id, chapter, verse_number, footnote_number, keyword, content) VALUES (?, ?, ?, ?, ?, ?)",
                        (book_id, chapter_num, verse_num, fn_number, keyword, content)
                    )

    # Create FTS5 tables (empty, for future search)
    cursor.executescript("""
        CREATE VIRTUAL TABLE verses_fts USING fts5(text, content=verses, content_rowid=id);
        CREATE VIRTUAL TABLE footnotes_fts USING fts5(keyword, content, content=footnotes, content_rowid=id);
    """)

    conn.commit()
    conn.close()


def main():
    """CLI entry point."""
    script_dir = Path(__file__).parent
    project_root = script_dir.parent

    verses_dir = project_root / "Verses"
    footnotes_dir = project_root / "Footnotes"
    db_path = project_root / "app" / "src" / "main" / "assets" / "bible.db"

    # Ensure assets directory exists
    db_path.parent.mkdir(parents=True, exist_ok=True)

    # Remove old db if exists
    if db_path.exists():
        db_path.unlink()

    print(f"Building bible.db...")
    print(f"  Verses: {verses_dir}")
    print(f"  Footnotes: {footnotes_dir}")
    print(f"  Output: {db_path}")

    build_database(str(verses_dir), str(footnotes_dir), str(db_path))

    # Print stats
    conn = sqlite3.connect(str(db_path))
    cursor = conn.cursor()
    cursor.execute("SELECT COUNT(*) FROM books")
    print(f"  Books: {cursor.fetchone()[0]}")
    cursor.execute("SELECT COUNT(*) FROM verses")
    print(f"  Verses: {cursor.fetchone()[0]}")
    cursor.execute("SELECT COUNT(*) FROM footnotes")
    print(f"  Footnotes: {cursor.fetchone()[0]}")
    file_size_mb = db_path.stat().st_size / (1024 * 1024)
    print(f"  Database size: {file_size_mb:.1f} MB")
    conn.close()

    print("Done!")


if __name__ == "__main__":
    main()
```

- [ ] **Step 16: Run all tests**

Run: `cd buildscripts && python -m pytest test_import.py -v`
Expected: ALL PASSED

- [ ] **Step 17: Run the import script to generate bible.db**

Run:
```bash
cd /Users/work/Documents/repos/rcv_reader
python3 buildscripts/import_bible_data.py
```
Expected: Prints stats — 66 books, ~31,000 verses, ~15,000 footnotes

- [ ] **Step 18: Commit**

```bash
git add buildscripts/
git add app/src/main/assets/bible.db
git commit -m "feat: add Python import script and generate bible.db from raw data"
```

---

### Task 3: Room Entities and DAOs

**Files:**
- Create: `app/src/main/java/com/rcvreader/data/model/Book.kt`
- Create: `app/src/main/java/com/rcvreader/data/model/Verse.kt`
- Create: `app/src/main/java/com/rcvreader/data/model/Footnote.kt`
- Create: `app/src/main/java/com/rcvreader/data/db/BookDao.kt`
- Create: `app/src/main/java/com/rcvreader/data/db/VerseDao.kt`
- Create: `app/src/main/java/com/rcvreader/data/db/FootnoteDao.kt`
- Create: `app/src/main/java/com/rcvreader/data/db/BibleDatabase.kt`

- [ ] **Step 1: Create Room entities**

```kotlin
// app/src/main/java/com/rcvreader/data/model/Book.kt
package com.rcvreader.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "books")
data class Book(
    @PrimaryKey val id: Int,
    val abbreviation: String,
    val name: String,
    val testament: String,
    val chapter_count: Int
)
```

```kotlin
// app/src/main/java/com/rcvreader/data/model/Verse.kt
package com.rcvreader.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "verses")
data class Verse(
    @PrimaryKey val id: Int,
    val book_id: Int,
    val chapter: Int,
    val verse_number: Int,
    val text: String,
    val has_footnotes: Int
)
```

```kotlin
// app/src/main/java/com/rcvreader/data/model/Footnote.kt
package com.rcvreader.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "footnotes")
data class Footnote(
    @PrimaryKey val id: Int,
    val book_id: Int,
    val chapter: Int,
    val verse_number: Int,
    val footnote_number: Int,
    val keyword: String?,
    val content: String
)
```

- [ ] **Step 2: Create DAOs**

```kotlin
// app/src/main/java/com/rcvreader/data/db/BookDao.kt
package com.rcvreader.data.db

import androidx.room.Dao
import androidx.room.Query
import com.rcvreader.data.model.Book
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {
    @Query("SELECT * FROM books ORDER BY id")
    fun getAllBooks(): Flow<List<Book>>

    @Query("SELECT * FROM books WHERE id = :bookId")
    suspend fun getBookById(bookId: Int): Book?
}
```

```kotlin
// app/src/main/java/com/rcvreader/data/db/VerseDao.kt
package com.rcvreader.data.db

import androidx.room.Dao
import androidx.room.Query
import com.rcvreader.data.model.Verse
import kotlinx.coroutines.flow.Flow

@Dao
interface VerseDao {
    @Query("SELECT * FROM verses WHERE book_id = :bookId AND chapter = :chapter ORDER BY verse_number")
    fun getVersesForChapter(bookId: Int, chapter: Int): Flow<List<Verse>>
}
```

```kotlin
// app/src/main/java/com/rcvreader/data/db/FootnoteDao.kt
package com.rcvreader.data.db

import androidx.room.Dao
import androidx.room.Query
import com.rcvreader.data.model.Footnote

@Dao
interface FootnoteDao {
    @Query("SELECT * FROM footnotes WHERE book_id = :bookId AND chapter = :chapter AND verse_number = :verseNumber ORDER BY footnote_number")
    suspend fun getFootnotesForVerse(bookId: Int, chapter: Int, verseNumber: Int): List<Footnote>
}
```

- [ ] **Step 3: Create BibleDatabase**

```kotlin
// app/src/main/java/com/rcvreader/data/db/BibleDatabase.kt
package com.rcvreader.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.rcvreader.data.model.Book
import com.rcvreader.data.model.Footnote
import com.rcvreader.data.model.Verse

@Database(
    entities = [Book::class, Verse::class, Footnote::class],
    version = 1,
    exportSchema = false
)
abstract class BibleDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao
    abstract fun verseDao(): VerseDao
    abstract fun footnoteDao(): FootnoteDao

    companion object {
        @Volatile
        private var INSTANCE: BibleDatabase? = null

        fun getInstance(context: Context): BibleDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    BibleDatabase::class.java,
                    "bible.db"
                )
                    .createFromAsset("bible.db")
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
```

- [ ] **Step 4: Verify project compiles**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/rcvreader/data/
git commit -m "feat: add Room entities, DAOs, and database for Bible data"
```

---

### Task 4: Repository Layer

**Files:**
- Create: `app/src/main/java/com/rcvreader/data/repository/BibleRepository.kt`

- [ ] **Step 1: Create BibleRepository**

```kotlin
// app/src/main/java/com/rcvreader/data/repository/BibleRepository.kt
package com.rcvreader.data.repository

import com.rcvreader.data.db.BookDao
import com.rcvreader.data.db.FootnoteDao
import com.rcvreader.data.db.VerseDao
import com.rcvreader.data.model.Book
import com.rcvreader.data.model.Footnote
import com.rcvreader.data.model.Verse
import kotlinx.coroutines.flow.Flow

class BibleRepository(
    private val bookDao: BookDao,
    private val verseDao: VerseDao,
    private val footnoteDao: FootnoteDao
) {
    fun getAllBooks(): Flow<List<Book>> = bookDao.getAllBooks()

    suspend fun getBookById(bookId: Int): Book? = bookDao.getBookById(bookId)

    fun getVersesForChapter(bookId: Int, chapter: Int): Flow<List<Verse>> =
        verseDao.getVersesForChapter(bookId, chapter)

    suspend fun getFootnotesForVerse(bookId: Int, chapter: Int, verseNumber: Int): List<Footnote> =
        footnoteDao.getFootnotesForVerse(bookId, chapter, verseNumber)
}
```

- [ ] **Step 2: Verify project compiles**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/rcvreader/data/repository/
git commit -m "feat: add BibleRepository as single data access layer"
```

---

## Chunk 2: UI — Theme, Reading Screen, Navigation

### Task 5: Compose Theme (Light + Dark)

**Files:**
- Create: `app/src/main/java/com/rcvreader/ui/theme/Color.kt`
- Create: `app/src/main/java/com/rcvreader/ui/theme/Type.kt`
- Create: `app/src/main/java/com/rcvreader/ui/theme/Theme.kt`

- [ ] **Step 1: Create Color.kt**

```kotlin
// app/src/main/java/com/rcvreader/ui/theme/Color.kt
package com.rcvreader.ui.theme

import androidx.compose.ui.graphics.Color

// Light theme
val LightBackground = Color(0xFFFAF8F4)
val LightSurface = Color(0xFFEEE8DC)
val LightOnBackground = Color(0xFF2C2C2C)
val LightOnSurface = Color(0xFF2C2C2C)
val LightPrimary = Color(0xFF8B6914)
val LightSecondary = Color(0xFF998866)

// Dark theme
val DarkBackground = Color(0xFF1A1715)
val DarkSurface = Color(0xFF2C2520)
val DarkOnBackground = Color(0xFFD4C5A9)
val DarkOnSurface = Color(0xFFD4C5A9)
val DarkPrimary = Color(0xFFC49B5E)
val DarkSecondary = Color(0xFF8B7355)

// Shared
val GoldAccent = Color(0xFFC49B5E)
val VerseDotColor = Color(0xFFC49B5E)
val FootnoteHighlight = Color(0x1FC49B5E)  // 12% alpha gold
```

- [ ] **Step 2: Create Type.kt**

```kotlin
// app/src/main/java/com/rcvreader/ui/theme/Type.kt
package com.rcvreader.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val SerifFontFamily = FontFamily.Serif

val AppTypography = Typography(
    bodyLarge = TextStyle(
        fontFamily = SerifFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 30.sp  // ~1.9x
    ),
    bodyMedium = TextStyle(
        fontFamily = SerifFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 24.sp
    ),
    bodySmall = TextStyle(
        fontFamily = SerifFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 20.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        letterSpacing = 1.sp
    )
)
```

- [ ] **Step 3: Create Theme.kt**

```kotlin
// app/src/main/java/com/rcvreader/ui/theme/Theme.kt
package com.rcvreader.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    secondary = LightSecondary,
    background = LightBackground,
    surface = LightSurface,
    onBackground = LightOnBackground,
    onSurface = LightOnSurface,
    onPrimary = LightBackground,
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    secondary = DarkSecondary,
    background = DarkBackground,
    surface = DarkSurface,
    onBackground = DarkOnBackground,
    onSurface = DarkOnSurface,
    onPrimary = DarkBackground,
)

@Composable
fun RCVReaderTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
```

- [ ] **Step 4: Update MainActivity to use theme**

```kotlin
// app/src/main/java/com/rcvreader/MainActivity.kt
package com.rcvreader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Text
import com.rcvreader.ui.theme.RCVReaderTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RCVReaderTheme {
                Text("RCV Reader")
            }
        }
    }
}
```

- [ ] **Step 5: Verify project compiles**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/rcvreader/ui/theme/ app/src/main/java/com/rcvreader/MainActivity.kt
git commit -m "feat: add warm light/dark theme with serif typography"
```

---

### Task 6: ReadingViewModel

**Files:**
- Create: `app/src/main/java/com/rcvreader/ui/reading/ReadingViewModel.kt`

- [ ] **Step 1: Create ReadingViewModel**

```kotlin
// app/src/main/java/com/rcvreader/ui/reading/ReadingViewModel.kt
package com.rcvreader.ui.reading

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rcvreader.data.db.BibleDatabase
import com.rcvreader.data.model.Book
import com.rcvreader.data.model.Footnote
import com.rcvreader.data.model.Verse
import com.rcvreader.data.repository.BibleRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ReadingUiState(
    val books: List<Book> = emptyList(),
    val currentBook: Book? = null,
    val currentChapter: Int = 1,
    val verses: List<Verse> = emptyList(),
    val expandedVerseNumber: Int? = null,
    val expandedFootnotes: List<Footnote> = emptyList(),
    val previousChapter: Pair<Book, Int>? = null,  // book, chapter
    val nextChapter: Pair<Book, Int>? = null,       // book, chapter
)

class ReadingViewModel(application: Application) : AndroidViewModel(application) {
    private val db = BibleDatabase.getInstance(application)
    private val repository = BibleRepository(db.bookDao(), db.verseDao(), db.footnoteDao())

    private val prefs = application.getSharedPreferences("rcv_reader", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(ReadingUiState())
    val uiState: StateFlow<ReadingUiState> = _uiState.asStateFlow()

    private var allBooks: List<Book> = emptyList()
    private var versesJob: kotlinx.coroutines.Job? = null

    init {
        viewModelScope.launch {
            repository.getAllBooks().collect { books ->
                allBooks = books
                _uiState.update { it.copy(books = books) }

                // Load last-read position or default to Genesis 1
                val savedBookId = prefs.getInt("last_book_id", 1)
                val savedChapter = prefs.getInt("last_chapter", 1)
                val book = books.find { it.id == savedBookId } ?: books.firstOrNull()
                if (book != null) {
                    navigateTo(book.id, savedChapter)
                }
            }
        }
    }

    fun navigateTo(bookId: Int, chapter: Int) {
        versesJob?.cancel()
        versesJob = viewModelScope.launch {
            val book = repository.getBookById(bookId) ?: return@launch

            // Save position
            prefs.edit()
                .putInt("last_book_id", bookId)
                .putInt("last_chapter", chapter)
                .apply()

            // Compute prev/next chapter
            val prev = computeAdjacentChapter(book, chapter, -1)
            val next = computeAdjacentChapter(book, chapter, 1)

            _uiState.update {
                it.copy(
                    currentBook = book,
                    currentChapter = chapter,
                    expandedVerseNumber = null,
                    expandedFootnotes = emptyList(),
                    previousChapter = prev,
                    nextChapter = next
                )
            }

            // Load verses
            repository.getVersesForChapter(bookId, chapter).collect { verses ->
                _uiState.update { it.copy(verses = verses) }
            }
        }
    }

    fun toggleVerse(verse: Verse) {
        if (verse.has_footnotes == 0) return

        viewModelScope.launch {
            val currentExpanded = _uiState.value.expandedVerseNumber
            if (currentExpanded == verse.verse_number) {
                // Collapse
                _uiState.update {
                    it.copy(expandedVerseNumber = null, expandedFootnotes = emptyList())
                }
            } else {
                // Expand — load footnotes
                val footnotes = repository.getFootnotesForVerse(
                    verse.book_id, verse.chapter, verse.verse_number
                )
                _uiState.update {
                    it.copy(
                        expandedVerseNumber = verse.verse_number,
                        expandedFootnotes = footnotes
                    )
                }
            }
        }
    }

    private fun computeAdjacentChapter(
        currentBook: Book,
        currentChapter: Int,
        direction: Int  // -1 for prev, +1 for next
    ): Pair<Book, Int>? {
        val targetChapter = currentChapter + direction
        if (targetChapter in 1..currentBook.chapter_count) {
            return currentBook to targetChapter
        }
        // Cross book boundary
        val targetBookIndex = allBooks.indexOfFirst { it.id == currentBook.id } + direction
        if (targetBookIndex !in allBooks.indices) return null
        val targetBook = allBooks[targetBookIndex]
        val chapter = if (direction > 0) 1 else targetBook.chapter_count
        return targetBook to chapter
    }
}
```

- [ ] **Step 2: Verify project compiles**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/rcvreader/ui/reading/ReadingViewModel.kt
git commit -m "feat: add ReadingViewModel with navigation, expansion, and persistence"
```

---

### Task 7: Verse Item and Footnote Section Composables

**Files:**
- Create: `app/src/main/java/com/rcvreader/ui/reading/FootnoteSection.kt`
- Create: `app/src/main/java/com/rcvreader/ui/reading/VerseItem.kt`

- [ ] **Step 1: Create FootnoteSection.kt**

```kotlin
// app/src/main/java/com/rcvreader/ui/reading/FootnoteSection.kt
package com.rcvreader.ui.reading

import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.rcvreader.data.model.Footnote
import com.rcvreader.ui.theme.GoldAccent

@Composable
fun FootnoteSection(
    footnotes: List<Footnote>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(top = 8.dp)) {
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp),
            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)
        )

        footnotes.forEach { footnote ->
            val annotatedText = buildAnnotatedString {
                if (footnote.keyword != null) {
                    withStyle(SpanStyle(color = GoldAccent, fontWeight = FontWeight.Bold)) {
                        append(footnote.keyword)
                        append(": ")
                    }
                }
                append(footnote.content)
            }

            Text(
                text = annotatedText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
    }
}
```

- [ ] **Step 2: Create VerseItem.kt**

```kotlin
// app/src/main/java/com/rcvreader/ui/reading/VerseItem.kt
package com.rcvreader.ui.reading

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rcvreader.data.model.Footnote
import com.rcvreader.data.model.Verse
import com.rcvreader.ui.theme.FootnoteHighlight
import com.rcvreader.ui.theme.GoldAccent
import com.rcvreader.ui.theme.VerseDotColor

@Composable
fun VerseItem(
    verse: Verse,
    isExpanded: Boolean,
    footnotes: List<Footnote>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isClickable = verse.has_footnotes == 1
    val bgColor = if (isExpanded) FootnoteHighlight else MaterialTheme.colorScheme.background
    val borderColor = GoldAccent

    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (isExpanded) {
                    Modifier.drawBehind {
                        drawLine(
                            color = borderColor,
                            start = Offset(0f, 0f),
                            end = Offset(0f, size.height),
                            strokeWidth = 3.dp.toPx()
                        )
                    }
                } else Modifier
            )
            .background(bgColor)
            .then(
                if (isClickable) Modifier.clickable { onClick() }
                else Modifier
            )
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        // Verse text with superscript number and optional dot
        val verseText = buildAnnotatedString {
            withStyle(SpanStyle(
                color = MaterialTheme.colorScheme.secondary,
                fontSize = 10.sp
            )) {
                append("${verse.verse_number} ")
            }
            append(verse.text)
            if (verse.has_footnotes == 1) {
                append(" ")
                withStyle(SpanStyle(color = VerseDotColor, fontSize = 8.sp)) {
                    append("\u25CF")  // ● dot
                }
            }
        }

        Text(
            text = verseText,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground
        )

        // Expandable footnotes
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            FootnoteSection(footnotes = footnotes)
        }
    }
}
```

- [ ] **Step 3: Verify project compiles**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/rcvreader/ui/reading/VerseItem.kt app/src/main/java/com/rcvreader/ui/reading/FootnoteSection.kt
git commit -m "feat: add VerseItem with animated footnote expansion and FootnoteSection"
```

---

### Task 8: Book and Chapter Picker Dropdowns

**Files:**
- Create: `app/src/main/java/com/rcvreader/ui/navigation/BookPickerDropdown.kt`
- Create: `app/src/main/java/com/rcvreader/ui/navigation/ChapterPickerDropdown.kt`

- [ ] **Step 1: Create BookPickerDropdown.kt**

```kotlin
// app/src/main/java/com/rcvreader/ui/navigation/BookPickerDropdown.kt
package com.rcvreader.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.rcvreader.data.model.Book

@Composable
fun BookPickerDropdown(
    currentBook: Book?,
    books: List<Book>,
    expanded: Boolean,
    onToggle: () -> Unit,
    onBookSelected: (Book) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        // Trigger button
        Surface(
            onClick = onToggle,
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = currentBook?.name ?: "",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(6.dp))
                Text("▾", color = MaterialTheme.colorScheme.secondary)
            }
        }

        // Dropdown popup
        if (expanded) {
            Popup(
                onDismissRequest = onToggle,
                properties = PopupProperties(focusable = true)
            ) {
                Surface(
                    modifier = Modifier
                        .width(300.dp)
                        .heightIn(max = 400.dp),
                    shape = RoundedCornerShape(12.dp),
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        val otBooks = books.filter { it.testament == "OT" }
                        val ntBooks = books.filter { it.testament == "NT" }

                        Text(
                            "OLD TESTAMENT",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        BookGrid(
                            books = otBooks,
                            currentBook = currentBook,
                            onBookSelected = onBookSelected
                        )

                        Spacer(Modifier.height(12.dp))

                        Text(
                            "NEW TESTAMENT",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        BookGrid(
                            books = ntBooks,
                            currentBook = currentBook,
                            onBookSelected = onBookSelected
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BookGrid(
    books: List<Book>,
    currentBook: Book?,
    onBookSelected: (Book) -> Unit
) {
    // Use a simple FlowRow-style grid with 3 columns
    val rows = books.chunked(3)
    Column {
        rows.forEach { rowBooks ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                rowBooks.forEach { book ->
                    val isSelected = book.id == currentBook?.id
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 3.dp),
                        onClick = { onBookSelected(book) },
                        shape = RoundedCornerShape(6.dp),
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        } else {
                            MaterialTheme.colorScheme.background
                        },
                        border = if (isSelected) {
                            androidx.compose.foundation.BorderStroke(
                                1.dp, MaterialTheme.colorScheme.primary
                            )
                        } else null
                    ) {
                        Text(
                            text = book.abbreviation,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onBackground
                            },
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
                            maxLines = 1
                        )
                    }
                }
                // Fill remaining space if row has fewer than 3 items
                repeat(3 - rowBooks.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}
```

- [ ] **Step 2: Create ChapterPickerDropdown.kt**

```kotlin
// app/src/main/java/com/rcvreader/ui/navigation/ChapterPickerDropdown.kt
package com.rcvreader.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties

@Composable
fun ChapterPickerDropdown(
    currentChapter: Int,
    chapterCount: Int,
    expanded: Boolean,
    onToggle: () -> Unit,
    onChapterSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        // Trigger button
        Surface(
            onClick = onToggle,
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$currentChapter",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(6.dp))
                Text("▾", color = MaterialTheme.colorScheme.secondary)
            }
        }

        // Dropdown popup
        if (expanded) {
            Popup(
                onDismissRequest = onToggle,
                properties = PopupProperties(focusable = true)
            ) {
                Surface(
                    modifier = Modifier
                        .width(250.dp)
                        .heightIn(max = 300.dp),
                    shape = RoundedCornerShape(12.dp),
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        val rows = (1..chapterCount).chunked(5)
                        rows.forEach { rowChapters ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                rowChapters.forEach { chapter ->
                                    val isSelected = chapter == currentChapter
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(vertical = 3.dp),
                                        onClick = { onChapterSelected(chapter) },
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (isSelected) {
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                        } else {
                                            MaterialTheme.colorScheme.background
                                        },
                                        border = if (isSelected) {
                                            androidx.compose.foundation.BorderStroke(
                                                1.dp, MaterialTheme.colorScheme.primary
                                            )
                                        } else null
                                    ) {
                                        Text(
                                            text = "$chapter",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            ),
                                            color = if (isSelected) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.onBackground
                                            },
                                            modifier = Modifier.padding(8.dp)
                                        )
                                    }
                                }
                                repeat(5 - rowChapters.size) {
                                    Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
```

- [ ] **Step 3: Verify project compiles**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/rcvreader/ui/navigation/
git commit -m "feat: add book and chapter picker dropdown composables"
```

---

### Task 9: ReadingScreen — Main Screen Assembly

**Files:**
- Create: `app/src/main/java/com/rcvreader/ui/reading/ReadingScreen.kt`
- Modify: `app/src/main/java/com/rcvreader/MainActivity.kt`

- [ ] **Step 1: Create ReadingScreen.kt**

```kotlin
// app/src/main/java/com/rcvreader/ui/reading/ReadingScreen.kt
package com.rcvreader.ui.reading

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rcvreader.ui.navigation.BookPickerDropdown
import com.rcvreader.ui.navigation.ChapterPickerDropdown

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingScreen(
    viewModel: ReadingViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    var bookPickerExpanded by remember { mutableStateOf(false) }
    var chapterPickerExpanded by remember { mutableStateOf(false) }

    // Reset scroll when chapter changes
    val currentBook = uiState.currentBook
    val currentChapter = uiState.currentChapter
    LaunchedEffect(currentBook?.id, currentChapter) {
        listState.scrollToItem(0)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Previous chapter link
            uiState.previousChapter?.let { (book, chapter) ->
                TextButton(
                    onClick = { viewModel.navigateTo(book.id, chapter) },
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        "← ${book.name} $chapter",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            // Toolbar with dropdowns
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BookPickerDropdown(
                    currentBook = uiState.currentBook,
                    books = uiState.books,
                    expanded = bookPickerExpanded,
                    onToggle = {
                        bookPickerExpanded = !bookPickerExpanded
                        chapterPickerExpanded = false
                    },
                    onBookSelected = { book ->
                        bookPickerExpanded = false
                        chapterPickerExpanded = true  // auto-open chapter picker
                        viewModel.navigateTo(book.id, 1)
                    }
                )

                Spacer(Modifier.width(8.dp))

                ChapterPickerDropdown(
                    currentChapter = uiState.currentChapter,
                    chapterCount = uiState.currentBook?.chapter_count ?: 1,
                    expanded = chapterPickerExpanded,
                    onToggle = {
                        chapterPickerExpanded = !chapterPickerExpanded
                        bookPickerExpanded = false
                    },
                    onChapterSelected = { chapter ->
                        chapterPickerExpanded = false
                        uiState.currentBook?.let { book ->
                            viewModel.navigateTo(book.id, chapter)
                        }
                    }
                )
            }

            // Verse list
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                items(
                    items = uiState.verses,
                    key = { it.id }
                ) { verse ->
                    VerseItem(
                        verse = verse,
                        isExpanded = uiState.expandedVerseNumber == verse.verse_number,
                        footnotes = if (uiState.expandedVerseNumber == verse.verse_number) {
                            uiState.expandedFootnotes
                        } else {
                            emptyList()
                        },
                        onClick = { viewModel.toggleVerse(verse) }
                    )
                }

                // Next chapter button at bottom
                item {
                    uiState.nextChapter?.let { (book, chapter) ->
                        TextButton(
                            onClick = { viewModel.navigateTo(book.id, chapter) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                        ) {
                            Text(
                                "Next: ${book.name} $chapter →",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}
```

- [ ] **Step 2: Update MainActivity.kt to use ReadingScreen**

```kotlin
// app/src/main/java/com/rcvreader/MainActivity.kt
package com.rcvreader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.rcvreader.ui.reading.ReadingScreen
import com.rcvreader.ui.theme.RCVReaderTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RCVReaderTheme {
                ReadingScreen()
            }
        }
    }
}
```

- [ ] **Step 3: Verify project compiles**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/rcvreader/ui/reading/ReadingScreen.kt app/src/main/java/com/rcvreader/MainActivity.kt
git commit -m "feat: add ReadingScreen with verse list, toolbar navigation, and chapter links"
```

---

### Task 10: Add .gitignore and Final Cleanup

**Files:**
- Create: `.gitignore`
- Modify: `.superpowers/` entry in `.gitignore`

- [ ] **Step 1: Create .gitignore**

```
# Android / Gradle
.gradle/
build/
local.properties
*.iml

# IDE
.idea/caches/
.idea/libraries/
.idea/workspace.xml

# OS
.DS_Store
Thumbs.db

# Superpowers
.superpowers/

# Database (generated, can rebuild)
# app/src/main/assets/bible.db
```

- [ ] **Step 2: Verify the full app compiles**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add .gitignore
git commit -m "chore: add .gitignore for Android project"
```

---

## Summary

| Task | Component | Key Files |
|------|-----------|-----------|
| 1 | Android project skeleton | `build.gradle.kts`, `app/build.gradle.kts`, `AndroidManifest.xml`, `MainActivity.kt` |
| 2 | Python import script + bible.db | `buildscripts/import_bible_data.py`, `buildscripts/test_import.py`, `app/src/main/assets/bible.db` |
| 3 | Room entities + DAOs | `data/model/*.kt`, `data/db/*.kt` |
| 4 | Repository layer | `data/repository/BibleRepository.kt` |
| 5 | Compose theme | `ui/theme/Color.kt`, `Type.kt`, `Theme.kt` |
| 6 | ReadingViewModel | `ui/reading/ReadingViewModel.kt` |
| 7 | VerseItem + FootnoteSection | `ui/reading/VerseItem.kt`, `FootnoteSection.kt` |
| 8 | Book/Chapter pickers | `ui/navigation/BookPickerDropdown.kt`, `ChapterPickerDropdown.kt` |
| 9 | ReadingScreen + MainActivity | `ui/reading/ReadingScreen.kt`, `MainActivity.kt` |
| 10 | .gitignore + cleanup | `.gitignore` |
