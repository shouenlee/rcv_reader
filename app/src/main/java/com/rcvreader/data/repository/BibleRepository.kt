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
