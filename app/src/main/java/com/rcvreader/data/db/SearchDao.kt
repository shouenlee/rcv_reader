package com.rcvreader.data.db

import androidx.room.Dao
import androidx.room.RawQuery
import androidx.sqlite.db.SupportSQLiteQuery
import com.rcvreader.data.model.Book
import com.rcvreader.data.model.Footnote
import com.rcvreader.data.model.SearchResult
import com.rcvreader.data.model.Verse

@Dao
interface SearchDao {

    /**
     * Search verses via FTS5. The caller supplies the full SQL including the MATCH expression.
     * observedEntities tells Room which tables to watch for invalidation.
     */
    @RawQuery(observedEntities = [Verse::class, Book::class])
    suspend fun queryVerses(query: SupportSQLiteQuery): List<SearchResult>

    /**
     * Search footnotes via FTS5.
     */
    @RawQuery(observedEntities = [Footnote::class, Book::class])
    suspend fun queryFootnotes(query: SupportSQLiteQuery): List<SearchResult>
}
