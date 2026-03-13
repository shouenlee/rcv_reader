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
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReadingUiState(
    val books: List<Book> = emptyList(),
    val currentBook: Book? = null,
    val currentChapter: Int = 1,
    val verses: List<Verse> = emptyList(),
    val expandedVerseId: Int? = null,
    val expandedFootnotes: List<Footnote> = emptyList(),
    val previousChapter: Pair<Book, Int>? = null,
    val nextChapter: Pair<Book, Int>? = null,
    val pendingBook: Book? = null,
)

class ReadingViewModel(application: Application) : AndroidViewModel(application) {
    private val db = BibleDatabase.getInstance(application)
    private val repository = BibleRepository(
        db.bookDao(), db.verseDao(), db.footnoteDao(), db.searchDao()
    )

    private val prefs = application.getSharedPreferences("rcv_reader", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(ReadingUiState())
    val uiState: StateFlow<ReadingUiState> = _uiState.asStateFlow()

    private var allBooks: List<Book> = emptyList()
    private var versesJob: Job? = null

    init {
        viewModelScope.launch {
            val books = repository.getAllBooks().first()
            allBooks = books
            _uiState.update { it.copy(books = books) }

            val savedBookId = prefs.getInt("last_book_id", 1)
            val savedChapter = prefs.getInt("last_chapter", 1)
            val book = books.find { it.id == savedBookId } ?: books.firstOrNull()
            if (book != null) {
                navigateTo(book.id, savedChapter)
            }
        }
    }

    fun selectBook(book: Book) {
        _uiState.update { it.copy(pendingBook = book) }
    }

    fun navigateTo(bookId: Int, chapter: Int) {
        _uiState.update { it.copy(pendingBook = null) }
        versesJob?.cancel()
        versesJob = viewModelScope.launch {
            val book = repository.getBookById(bookId) ?: return@launch

            prefs.edit()
                .putInt("last_book_id", bookId)
                .putInt("last_chapter", chapter)
                .apply()

            val prev = computeAdjacentChapter(book, chapter, -1)
            val next = computeAdjacentChapter(book, chapter, 1)

            _uiState.update {
                it.copy(
                    currentBook = book,
                    currentChapter = chapter,
                    expandedVerseId = null,
                    expandedFootnotes = emptyList(),
                    previousChapter = prev,
                    nextChapter = next
                )
            }

            repository.getVersesForChapter(bookId, chapter).collect { verses ->
                _uiState.update { it.copy(verses = verses) }
            }
        }
    }

    fun toggleVerse(verse: Verse) {
        if (!verse.hasFootnotes) return

        viewModelScope.launch {
            val currentExpanded = _uiState.value.expandedVerseId
            if (currentExpanded == verse.id) {
                _uiState.update {
                    it.copy(expandedVerseId = null, expandedFootnotes = emptyList())
                }
            } else {
                val footnotes = repository.getFootnotesForVerse(
                    verse.bookId, verse.chapter, verse.verseNumber
                )
                _uiState.update {
                    it.copy(
                        expandedVerseId = verse.id,
                        expandedFootnotes = footnotes
                    )
                }
            }
        }
    }

    private fun computeAdjacentChapter(
        currentBook: Book,
        currentChapter: Int,
        direction: Int
    ): Pair<Book, Int>? {
        val targetChapter = currentChapter + direction
        if (targetChapter in 1..currentBook.chapterCount) {
            return currentBook to targetChapter
        }
        val targetBookIndex = allBooks.indexOfFirst { it.id == currentBook.id } + direction
        if (targetBookIndex !in allBooks.indices) return null
        val targetBook = allBooks[targetBookIndex]
        val chapter = if (direction > 0) 1 else targetBook.chapterCount
        return targetBook to chapter
    }
}
