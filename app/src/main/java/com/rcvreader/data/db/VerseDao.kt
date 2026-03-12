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
