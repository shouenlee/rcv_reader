#!/usr/bin/env python3
"""Import Bible data from raw text files into a SQLite database."""

import html
import os
import re
import sqlite3
import sys
from pathlib import Path

# Canonical order: folder_name -> (id, full_name, testament)
BOOK_MAP = {
    # Old Testament (1-39)
    "Gen":      {"id": 1,  "name": "Genesis",         "testament": "OT"},
    "Exo":      {"id": 2,  "name": "Exodus",          "testament": "OT"},
    "Lev":      {"id": 3,  "name": "Leviticus",       "testament": "OT"},
    "Num":      {"id": 4,  "name": "Numbers",         "testament": "OT"},
    "Deut":     {"id": 5,  "name": "Deuteronomy",     "testament": "OT"},
    "Josh":     {"id": 6,  "name": "Joshua",          "testament": "OT"},
    "Judg":     {"id": 7,  "name": "Judges",          "testament": "OT"},
    "Ruth":     {"id": 8,  "name": "Ruth",            "testament": "OT"},
    "1Sam":     {"id": 9,  "name": "1 Samuel",        "testament": "OT"},
    "2Sam":     {"id": 10, "name": "2 Samuel",        "testament": "OT"},
    "1Kings":   {"id": 11, "name": "1 Kings",         "testament": "OT"},
    "2Kings":   {"id": 12, "name": "2 Kings",         "testament": "OT"},
    "1Chron":   {"id": 13, "name": "1 Chronicles",    "testament": "OT"},
    "2Chron":   {"id": 14, "name": "2 Chronicles",    "testament": "OT"},
    "Ezra":     {"id": 15, "name": "Ezra",            "testament": "OT"},
    "Neh":      {"id": 16, "name": "Nehemiah",        "testament": "OT"},
    "Esther":   {"id": 17, "name": "Esther",          "testament": "OT"},
    "Job":      {"id": 18, "name": "Job",             "testament": "OT"},
    "Psa":      {"id": 19, "name": "Psalms",          "testament": "OT"},
    "Prov":     {"id": 20, "name": "Proverbs",        "testament": "OT"},
    "Eccl":     {"id": 21, "name": "Ecclesiastes",    "testament": "OT"},
    "SOS":      {"id": 22, "name": "Song of Songs",   "testament": "OT"},
    "Isa":      {"id": 23, "name": "Isaiah",          "testament": "OT"},
    "Jer":      {"id": 24, "name": "Jeremiah",        "testament": "OT"},
    "Lam":      {"id": 25, "name": "Lamentations",    "testament": "OT"},
    "Ezek":     {"id": 26, "name": "Ezekiel",        "testament": "OT"},
    "Dan":      {"id": 27, "name": "Daniel",          "testament": "OT"},
    "Hos":      {"id": 28, "name": "Hosea",           "testament": "OT"},
    "Joel":     {"id": 29, "name": "Joel",            "testament": "OT"},
    "Amos":     {"id": 30, "name": "Amos",            "testament": "OT"},
    "Obad":     {"id": 31, "name": "Obadiah",         "testament": "OT"},
    "Jonah":    {"id": 32, "name": "Jonah",           "testament": "OT"},
    "Micah":    {"id": 33, "name": "Micah",           "testament": "OT"},
    "Nahum":    {"id": 34, "name": "Nahum",           "testament": "OT"},
    "Hab":      {"id": 35, "name": "Habakkuk",        "testament": "OT"},
    "Zeph":     {"id": 36, "name": "Zephaniah",       "testament": "OT"},
    "Hag":      {"id": 37, "name": "Haggai",          "testament": "OT"},
    "Zech":     {"id": 38, "name": "Zechariah",       "testament": "OT"},
    "Mal":      {"id": 39, "name": "Malachi",         "testament": "OT"},
    # New Testament (40-66)
    "Matt":     {"id": 40, "name": "Matthew",         "testament": "NT"},
    "Mark":     {"id": 41, "name": "Mark",            "testament": "NT"},
    "Luke":     {"id": 42, "name": "Luke",            "testament": "NT"},
    "John":     {"id": 43, "name": "John",            "testament": "NT"},
    "Acts":     {"id": 44, "name": "Acts",            "testament": "NT"},
    "Rom":      {"id": 45, "name": "Romans",          "testament": "NT"},
    "1Cor":     {"id": 46, "name": "1 Corinthians",   "testament": "NT"},
    "2Cor":     {"id": 47, "name": "2 Corinthians",   "testament": "NT"},
    "Gal":      {"id": 48, "name": "Galatians",       "testament": "NT"},
    "Eph":      {"id": 49, "name": "Ephesians",       "testament": "NT"},
    "Phil":     {"id": 50, "name": "Philippians",     "testament": "NT"},
    "Col":      {"id": 51, "name": "Colossians",      "testament": "NT"},
    "1Thes":    {"id": 52, "name": "1 Thessalonians", "testament": "NT"},
    "2Thes":    {"id": 53, "name": "2 Thessalonians", "testament": "NT"},
    "1Tim":     {"id": 54, "name": "1 Timothy",       "testament": "NT"},
    "2Tim":     {"id": 55, "name": "2 Timothy",       "testament": "NT"},
    "Titus":    {"id": 56, "name": "Titus",           "testament": "NT"},
    "Philemon": {"id": 57, "name": "Philemon",        "testament": "NT"},
    "Heb":      {"id": 58, "name": "Hebrews",         "testament": "NT"},
    "James":    {"id": 59, "name": "James",           "testament": "NT"},
    "1Pet":     {"id": 60, "name": "1 Peter",         "testament": "NT"},
    "2Pet":     {"id": 61, "name": "2 Peter",         "testament": "NT"},
    "1John":    {"id": 62, "name": "1 John",          "testament": "NT"},
    "2John":    {"id": 63, "name": "2 John",          "testament": "NT"},
    "3John":    {"id": 64, "name": "3 John",          "testament": "NT"},
    "Jude":     {"id": 65, "name": "Jude",            "testament": "NT"},
    "Rev":      {"id": 66, "name": "Revelation",      "testament": "NT"},
}


def get_book_info(folder_name):
    """Return dict with id, name, testament for a given folder name, or None."""
    return BOOK_MAP.get(folder_name)


def format_abbreviation(folder_name):
    """Insert a space between a leading digit and the book letters.

    e.g. '1Sam' -> '1 Sam', '2John' -> '2 John', 'Gen' -> 'Gen'
    """
    if folder_name and folder_name[0].isdigit():
        return folder_name[0] + " " + folder_name[1:]
    return folder_name


def extract_verse_text(line):
    """Strip the reference prefix from a verse line and return just the text.

    Handles four format variants:
    - Standard: "Gen. 1:1 text"
    - Numbered book: "1 Chron. 1:1 text"
    - No space before chapter: "Neh.1:1 text"
    - Single-chapter: "Jude 1 text" / "2 John 1 text"
    """
    # Try Ch:V format first — find ":digit(s) " pattern
    m = re.search(r':\d+\s', line)
    if m:
        return html.unescape(line[m.end():])

    # Single-chapter format: find the LAST bare number followed by space+non-digit
    # Use finditer to get the last match (avoids matching "2" in "2 John")
    matches = list(re.finditer(r'(?:^|\s)(\d+)\s(?=\D)', line))
    if matches:
        last = matches[-1]
        # The text starts after the digit and space
        # Find position after the digit
        digit_end = last.end()
        return html.unescape(line[digit_end:])

    # Fallback: return the whole line
    return html.unescape(line)


def parse_footnote_content(text):
    """Parse footnote content into (keyword, content).

    Split on ' - ' to separate reference from body, then split body on first ':'.
    If no ' - ' separator: keyword=None, entire text stored as content.
    """
    parts = text.split(" - ", 1)
    if len(parts) < 2:
        return (None, text)

    body = parts[1]
    colon_pos = body.find(":")
    if colon_pos == -1:
        return (None, body)

    keyword = body[:colon_pos].strip()
    content = body[colon_pos + 1:].strip()
    return (keyword, content)


CREATE_TABLES_SQL = """
CREATE TABLE IF NOT EXISTS `books` (`id` INTEGER NOT NULL, `abbreviation` TEXT NOT NULL, `name` TEXT NOT NULL, `testament` TEXT NOT NULL, `chapter_count` INTEGER NOT NULL, PRIMARY KEY(`id`));

CREATE TABLE IF NOT EXISTS `verses` (`id` INTEGER NOT NULL, `book_id` INTEGER NOT NULL, `chapter` INTEGER NOT NULL, `verse_number` INTEGER NOT NULL, `text` TEXT NOT NULL, `has_footnotes` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`book_id`) REFERENCES `books`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION );

CREATE INDEX IF NOT EXISTS `idx_verses_lookup` ON `verses` (`book_id`, `chapter`);

CREATE TABLE IF NOT EXISTS `footnotes` (`id` INTEGER NOT NULL, `book_id` INTEGER NOT NULL, `chapter` INTEGER NOT NULL, `verse_number` INTEGER NOT NULL, `footnote_number` INTEGER NOT NULL, `keyword` TEXT, `content` TEXT NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`book_id`) REFERENCES `books`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION );

CREATE INDEX IF NOT EXISTS `idx_footnotes_lookup` ON `footnotes` (`book_id`, `chapter`, `verse_number`);

CREATE VIRTUAL TABLE IF NOT EXISTS `verses_fts` USING fts5(`text`, content=`verses`, content_rowid=`id`);

CREATE VIRTUAL TABLE IF NOT EXISTS `footnotes_fts` USING fts5(`content`, content=`footnotes`, content_rowid=`id`);
"""


def build_database(verses_dir, footnotes_dir, db_path):
    """Build the SQLite database from raw verse and footnote files."""
    verses_path = Path(verses_dir)
    footnotes_path = Path(footnotes_dir)

    # Ensure output directory exists
    os.makedirs(os.path.dirname(db_path), exist_ok=True)

    # Remove existing db if present
    if os.path.exists(db_path):
        os.remove(db_path)

    conn = sqlite3.connect(db_path)
    cursor = conn.cursor()
    cursor.executescript(CREATE_TABLES_SQL)

    # Track which verses have footnotes: (book_id, chapter, verse_number)
    footnote_verses = set()

    # Import books and verses
    for folder_name, info in sorted(BOOK_MAP.items(), key=lambda x: x[1]["id"]):
        book_id = info["id"]
        book_dir = verses_path / folder_name

        if not book_dir.is_dir():
            print(f"WARNING: Missing verse directory for {folder_name}")
            continue

        # Count chapters
        chapter_files = [f for f in book_dir.iterdir() if f.is_file() and f.name.isdigit()]
        chapter_count = len(chapter_files)

        cursor.execute(
            "INSERT INTO books (id, name, abbreviation, testament, chapter_count) VALUES (?, ?, ?, ?, ?)",
            (book_id, info["name"], format_abbreviation(folder_name), info["testament"], chapter_count),
        )

        # Import verses for each chapter
        for chapter_file in sorted(chapter_files, key=lambda f: int(f.name)):
            chapter_num = int(chapter_file.name)
            with open(chapter_file, "r", encoding="utf-8") as f:
                for line_num, line in enumerate(f, start=1):
                    line = line.rstrip("\n\r")
                    if not line.strip():
                        continue
                    text = extract_verse_text(line)
                    cursor.execute(
                        "INSERT INTO verses (book_id, chapter, verse_number, text, has_footnotes) VALUES (?, ?, ?, ?, 0)",
                        (book_id, chapter_num, line_num, text),
                    )

    # Import footnotes
    for folder_name, info in sorted(BOOK_MAP.items(), key=lambda x: x[1]["id"]):
        book_id = info["id"]
        book_fn_dir = footnotes_path / folder_name

        if not book_fn_dir.is_dir():
            continue

        for chapter_dir in book_fn_dir.iterdir():
            if not chapter_dir.is_dir():
                continue
            try:
                chapter_num = int(chapter_dir.name)
            except ValueError:
                continue

            for verse_dir in chapter_dir.iterdir():
                if not verse_dir.is_dir():
                    continue
                try:
                    verse_num = int(verse_dir.name)
                except ValueError:
                    continue

                for note_file in sorted(verse_dir.iterdir()):
                    if not note_file.is_file():
                        continue

                    # Determine footnote number
                    note_name = note_file.name
                    if verse_num == 0:
                        # Psalms superscription: footnote_number=0
                        fn_number = 0
                    else:
                        try:
                            fn_number = int(note_name)
                        except ValueError:
                            continue

                    with open(note_file, "r", encoding="utf-8") as f:
                        content = f.read().strip()

                    if not content:
                        continue

                    keyword, body = parse_footnote_content(content)

                    cursor.execute(
                        "INSERT INTO footnotes (book_id, chapter, verse_number, footnote_number, keyword, content) VALUES (?, ?, ?, ?, ?, ?)",
                        (book_id, chapter_num, verse_num, fn_number, keyword, body),
                    )
                    footnote_verses.add((book_id, chapter_num, verse_num))

    # Update has_footnotes flag
    for book_id, chapter, verse_num in footnote_verses:
        cursor.execute(
            "UPDATE verses SET has_footnotes = 1 WHERE book_id = ? AND chapter = ? AND verse_number = ?",
            (book_id, chapter, verse_num),
        )

    # Rebuild FTS indexes
    cursor.execute("INSERT INTO verses_fts(verses_fts) VALUES('rebuild')")
    cursor.execute("INSERT INTO footnotes_fts(footnotes_fts) VALUES('rebuild')")

    conn.commit()
    conn.close()


def main():
    """CLI entry point."""
    project_root = Path(__file__).parent.parent
    verses_dir = project_root / "Verses"
    footnotes_dir = project_root / "Footnotes"
    db_path = project_root / "app" / "src" / "main" / "assets" / "bible.db"

    print(f"Importing Bible data...")
    print(f"  Verses:    {verses_dir}")
    print(f"  Footnotes: {footnotes_dir}")
    print(f"  Output:    {db_path}")

    build_database(str(verses_dir), str(footnotes_dir), str(db_path))

    # Print stats
    conn = sqlite3.connect(str(db_path))
    cursor = conn.cursor()
    cursor.execute("SELECT COUNT(*) FROM books")
    book_count = cursor.fetchone()[0]
    cursor.execute("SELECT COUNT(*) FROM verses")
    verse_count = cursor.fetchone()[0]
    cursor.execute("SELECT COUNT(*) FROM footnotes")
    footnote_count = cursor.fetchone()[0]
    conn.close()

    file_size = os.path.getsize(str(db_path))
    print(f"\nDone!")
    print(f"  Books:     {book_count}")
    print(f"  Verses:    {verse_count}")
    print(f"  Footnotes: {footnote_count}")
    print(f"  DB size:   {file_size / 1024 / 1024:.1f} MB")


if __name__ == "__main__":
    main()
