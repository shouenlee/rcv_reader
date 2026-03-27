package com.rcvreader.data.repository

import com.rcvreader.data.db.BookmarkDao
import com.rcvreader.data.model.Bookmark
import com.rcvreader.data.model.Footnote
import com.rcvreader.data.model.Verse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class BookmarkRepository(private val bookmarkDao: BookmarkDao) {

    fun getAllBookmarks(): Flow<List<Bookmark>> = bookmarkDao.getAllBookmarks()

    fun getBookmarkedVerseIds(bookId: Int, chapter: Int): Flow<Set<Int>> =
        bookmarkDao.getBookmarkedVerseIds(bookId, chapter).map { it.filterNotNull().toSet() }

    fun getBookmarkedFootnoteIds(bookId: Int, chapter: Int): Flow<Set<Int>> =
        bookmarkDao.getBookmarkedFootnoteIds(bookId, chapter).map { it.filterNotNull().toSet() }

    suspend fun toggleVerseBookmark(
        verse: Verse,
        bookName: String,
        abbreviation: String
    ): Boolean {
        return if (bookmarkDao.isVerseBookmarked(verse.id)) {
            bookmarkDao.deleteByVerseId(verse.id)
            false
        } else {
            bookmarkDao.insert(
                Bookmark(
                    type = "verse",
                    verseId = verse.id,
                    bookId = verse.bookId,
                    bookName = bookName,
                    abbreviation = abbreviation,
                    chapter = verse.chapter,
                    verseNumber = verse.verseNumber,
                    previewText = verse.text
                )
            )
            true
        }
    }

    suspend fun toggleFootnoteBookmark(
        footnote: Footnote,
        bookName: String,
        abbreviation: String
    ): Boolean {
        return if (bookmarkDao.isFootnoteBookmarked(footnote.id)) {
            bookmarkDao.deleteByFootnoteId(footnote.id)
            false
        } else {
            bookmarkDao.insert(
                Bookmark(
                    type = "footnote",
                    footnoteId = footnote.id,
                    bookId = footnote.bookId,
                    bookName = bookName,
                    abbreviation = abbreviation,
                    chapter = footnote.chapter,
                    verseNumber = footnote.verseNumber,
                    previewText = footnote.content,
                    keyword = footnote.keyword
                )
            )
            true
        }
    }

    suspend fun deleteById(id: Long) = bookmarkDao.deleteById(id)
}
