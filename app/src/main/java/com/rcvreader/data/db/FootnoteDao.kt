package com.rcvreader.data.db

import androidx.room.Dao
import androidx.room.Query
import com.rcvreader.data.model.Footnote

@Dao
interface FootnoteDao {
    @Query("SELECT * FROM footnotes WHERE book_id = :bookId AND chapter = :chapter AND verse_number = :verseNumber ORDER BY footnote_number")
    suspend fun getFootnotesForVerse(bookId: Int, chapter: Int, verseNumber: Int): List<Footnote>
}
