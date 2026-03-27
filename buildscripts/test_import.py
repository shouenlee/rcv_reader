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


from import_bible_data import build_database


@pytest.fixture(scope="session")
def bible_db(tmp_path_factory):
    db_path = tmp_path_factory.mktemp("data") / "bible.db"
    verses_dir = Path(__file__).parent.parent / "Verses"
    footnotes_dir = Path(__file__).parent.parent / "Footnotes"
    build_database(str(verses_dir), str(footnotes_dir), str(db_path))
    return str(db_path)


def test_build_database_creates_all_tables(bible_db):
    conn = sqlite3.connect(bible_db)
    cursor = conn.cursor()
    cursor.execute("SELECT COUNT(*) FROM books")
    assert cursor.fetchone()[0] == 66
    cursor.execute("SELECT chapter_count FROM books WHERE abbreviation = 'Gen'")
    assert cursor.fetchone()[0] == 50
    cursor.execute("SELECT COUNT(*) FROM verses WHERE book_id = 1 AND chapter = 1")
    count = cursor.fetchone()[0]
    assert count >= 31
    cursor.execute("SELECT text FROM verses WHERE book_id = 1 AND chapter = 1 AND verse_number = 1")
    text = cursor.fetchone()[0]
    assert text.startswith("In the beginning")
    assert not text.startswith("Gen")
    cursor.execute("SELECT has_footnotes FROM verses WHERE book_id = 1 AND chapter = 1 AND verse_number = 1")
    assert cursor.fetchone()[0] == 1
    cursor.execute("SELECT COUNT(*) FROM footnotes WHERE book_id = 1 AND chapter = 1 AND verse_number = 1")
    assert cursor.fetchone()[0] >= 5
    cursor.execute("SELECT keyword, content FROM footnotes WHERE book_id = 1 AND chapter = 1 AND verse_number = 1 AND footnote_number = 1")
    row = cursor.fetchone()
    assert row[0] == "In"
    assert "Bible" in row[1]
    cursor.execute("SELECT text FROM verses WHERE book_id = 65 AND chapter = 1 AND verse_number = 1")
    text = cursor.fetchone()[0]
    assert text.startswith("Jude, a slave")
    cursor.execute("SELECT COUNT(*) FROM verses")
    total = cursor.fetchone()[0]
    assert total > 30000
    conn.close()


def test_psalms_superscription_footnotes(bible_db):
    conn = sqlite3.connect(bible_db)
    cursor = conn.cursor()
    cursor.execute("SELECT COUNT(*) FROM footnotes WHERE book_id = 19 AND verse_number = 0")
    count = cursor.fetchone()[0]
    assert count > 0
    conn.close()


@pytest.fixture
def built_db(tmp_path):
    """Build a real bible.db from the Verses/Footnotes directories."""
    project_root = Path(__file__).parent.parent
    verses_dir = project_root / "Verses"
    footnotes_dir = project_root / "Footnotes"
    db_path = tmp_path / "bible.db"
    build_database(str(verses_dir), str(footnotes_dir), str(db_path))
    return str(db_path)


def test_fts_verses_table_exists(built_db):
    conn = sqlite3.connect(built_db)
    cursor = conn.cursor()
    cursor.execute("SELECT name FROM sqlite_master WHERE type='table' AND name='verses_fts'")
    assert cursor.fetchone() is not None
    conn.close()


def test_fts_footnotes_table_exists(built_db):
    conn = sqlite3.connect(built_db)
    cursor = conn.cursor()
    cursor.execute("SELECT name FROM sqlite_master WHERE type='table' AND name='footnotes_fts'")
    assert cursor.fetchone() is not None
    conn.close()


def test_fts_verses_search_returns_results(built_db):
    conn = sqlite3.connect(built_db)
    cursor = conn.cursor()
    cursor.execute("SELECT rowid FROM verses_fts WHERE verses_fts MATCH 'grace' LIMIT 5")
    rows = cursor.fetchall()
    assert len(rows) > 0
    conn.close()


def test_fts_footnotes_search_returns_results(built_db):
    conn = sqlite3.connect(built_db)
    cursor = conn.cursor()
    cursor.execute("SELECT rowid FROM footnotes_fts WHERE footnotes_fts MATCH 'grace' LIMIT 5")
    rows = cursor.fetchall()
    assert len(rows) > 0
    conn.close()
