package com.rcvreader.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.rcvreader.data.model.Bookmark
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkDao {

    @Query("SELECT * FROM bookmarks ORDER BY created_at DESC")
    fun getAllBookmarks(): Flow<List<Bookmark>>

    @Query("SELECT verse_id FROM bookmarks WHERE book_id = :bookId AND chapter = :chapter AND type = 'verse'")
    fun getBookmarkedVerseIds(bookId: Int, chapter: Int): Flow<List<Int>>

    @Query("SELECT footnote_id FROM bookmarks WHERE book_id = :bookId AND chapter = :chapter AND type = 'footnote'")
    fun getBookmarkedFootnoteIds(bookId: Int, chapter: Int): Flow<List<Int>>

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarks WHERE verse_id = :verseId AND type = 'verse')")
    suspend fun isVerseBookmarked(verseId: Int): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarks WHERE footnote_id = :footnoteId AND type = 'footnote')")
    suspend fun isFootnoteBookmarked(footnoteId: Int): Boolean

    @Insert
    suspend fun insert(bookmark: Bookmark): Long

    @Query("DELETE FROM bookmarks WHERE verse_id = :verseId AND type = 'verse'")
    suspend fun deleteByVerseId(verseId: Int)

    @Query("DELETE FROM bookmarks WHERE footnote_id = :footnoteId AND type = 'footnote'")
    suspend fun deleteByFootnoteId(footnoteId: Int)

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun deleteById(id: Long)
}
