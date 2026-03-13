package com.rcvreader.data.repository

import androidx.sqlite.db.SimpleSQLiteQuery
import com.rcvreader.data.db.BookDao
import com.rcvreader.data.db.FootnoteDao
import com.rcvreader.data.db.SearchDao
import com.rcvreader.data.db.VerseDao
import com.rcvreader.data.model.Book
import com.rcvreader.data.model.Footnote
import com.rcvreader.data.model.SearchResult
import com.rcvreader.data.model.SearchScope
import com.rcvreader.data.model.Verse
import kotlinx.coroutines.flow.Flow

/**
 * Strips tokens that are too short or contain FTS5 special characters.
 * Package-internal so unit tests can call it directly.
 */
internal fun sanitizeQuery(raw: String): List<String> {
    val specialChars = Regex("""["*()\-]""")
    return raw.trim()
        .split(Regex("\\s+"))
        .map { it.replace(specialChars, "").trim() }
        .filter { it.length >= 2 }
}

class BibleRepository(
    private val bookDao: BookDao,
    private val verseDao: VerseDao,
    private val footnoteDao: FootnoteDao,
    private val searchDao: SearchDao
) {
    fun getAllBooks(): Flow<List<Book>> = bookDao.getAllBooks()

    suspend fun getBookById(bookId: Int): Book? = bookDao.getBookById(bookId)

    fun getVersesForChapter(bookId: Int, chapter: Int): Flow<List<Verse>> =
        verseDao.getVersesForChapter(bookId, chapter)

    suspend fun getFootnotesForVerse(bookId: Int, chapter: Int, verseNumber: Int): List<Footnote> =
        footnoteDao.getFootnotesForVerse(bookId, chapter, verseNumber)

    suspend fun search(
        query: String,
        scope: SearchScope,
        includeFootnotes: Boolean,
        currentBookId: Int?
    ): List<SearchResult> {
        val tokens = sanitizeQuery(query)
        if (tokens.isEmpty()) return emptyList()

        val phraseExpr = "\"${tokens.joinToString(" ")}\""
        val andExpr = tokens.joinToString(" AND ")
        val orExpr = tokens.joinToString(" OR ")

        // ── Verse results ────────────────────────────────────────────────────
        val verseScopeClause = when (scope) {
            SearchScope.ALL -> ""
            SearchScope.OT -> "AND b.testament = 'OT'"
            SearchScope.NT -> "AND b.testament = 'NT'"
            SearchScope.THIS_BOOK ->
                if (currentBookId != null) "AND v.book_id = $currentBookId" else ""
        }

        val vTier1 = searchDao.queryVerses(buildVerseQuery(phraseExpr, verseScopeClause))
            .map { it.copy(tier = 1) }
        val seenVerseIds = vTier1.map { it.id }.toHashSet()

        val vTier2 = searchDao.queryVerses(buildVerseQuery(andExpr, verseScopeClause))
            .filter { it.id !in seenVerseIds }.map { it.copy(tier = 2) }
        seenVerseIds += vTier2.map { it.id }

        val vTier3 = searchDao.queryVerses(buildVerseQuery(orExpr, verseScopeClause))
            .filter { it.id !in seenVerseIds }.map { it.copy(tier = 3) }

        val verseResults = vTier1 + vTier2 + vTier3

        // ── Footnote results ─────────────────────────────────────────────────
        val footnoteResults = if (includeFootnotes) {
            val fnScopeClause = when (scope) {
                SearchScope.ALL -> ""
                SearchScope.OT -> "AND b.testament = 'OT'"
                SearchScope.NT -> "AND b.testament = 'NT'"
                SearchScope.THIS_BOOK ->
                    if (currentBookId != null) "AND f.book_id = $currentBookId" else ""
            }

            val fTier1 = searchDao.queryFootnotes(buildFootnoteQuery(phraseExpr, fnScopeClause))
                .map { it.copy(tier = 1) }
            val seenFnIds = fTier1.map { it.id }.toHashSet()

            val fTier2 = searchDao.queryFootnotes(buildFootnoteQuery(andExpr, fnScopeClause))
                .filter { it.id !in seenFnIds }.map { it.copy(tier = 2) }
            seenFnIds += fTier2.map { it.id }

            val fTier3 = searchDao.queryFootnotes(buildFootnoteQuery(orExpr, fnScopeClause))
                .filter { it.id !in seenFnIds }.map { it.copy(tier = 3) }

            fTier1 + fTier2 + fTier3
        } else emptyList()

        return (verseResults + footnoteResults).take(200)
    }

    private fun buildVerseQuery(ftsExpr: String, scopeClause: String) = SimpleSQLiteQuery(
        """
        SELECT v.id, v.book_id AS bookId, v.chapter, v.verse_number AS verseNumber, v.text,
               CAST(NULL AS TEXT) AS keyword, b.name AS bookName, b.abbreviation, 0 AS tier
        FROM verses v
        JOIN books b ON v.book_id = b.id
        JOIN verses_fts ON verses_fts.rowid = v.id
        WHERE verses_fts MATCH ? $scopeClause
        ORDER BY bm25(verses_fts)
        """.trimIndent(),
        arrayOf(ftsExpr)
    )

    private fun buildFootnoteQuery(ftsExpr: String, scopeClause: String) = SimpleSQLiteQuery(
        """
        SELECT f.id, f.book_id AS bookId, f.chapter, f.verse_number AS verseNumber,
               f.content AS text, f.keyword, b.name AS bookName, b.abbreviation, 0 AS tier
        FROM footnotes f
        JOIN books b ON f.book_id = b.id
        JOIN footnotes_fts ON footnotes_fts.rowid = f.id
        WHERE footnotes_fts MATCH ? $scopeClause
        ORDER BY bm25(footnotes_fts)
        """.trimIndent(),
        arrayOf(ftsExpr)
    )
}
