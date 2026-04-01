package com.rcvreader.ui.reading

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rcvreader.data.db.BibleDatabase
import com.rcvreader.data.db.BookmarkDatabase
import com.rcvreader.data.model.Book
import com.rcvreader.data.model.Footnote
import com.rcvreader.data.model.Verse
import com.rcvreader.data.repository.BibleRepository
import com.rcvreader.data.repository.BookmarkRepository
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
    val bookmarkedVerseIds: Set<Int> = emptySet(),
    val bookmarkedFootnoteIds: Set<Int> = emptySet(),
    val scrollToVerseNumber: Int? = null,
    val autoExpandFootnoteVerseNumber: Int? = null,
    val backStack: List<Pair<Book, Int>> = emptyList(),
    val forwardStack: List<Pair<Book, Int>> = emptyList(),
)

private const val MAX_HISTORY = 50

class ReadingViewModel(application: Application) : AndroidViewModel(application) {
    private val db = BibleDatabase.getInstance(application)
    private val repository = BibleRepository(
        db.bookDao(), db.verseDao(), db.footnoteDao(), db.searchDao()
    )
    private val bookmarkRepository = BookmarkRepository(
        BookmarkDatabase.getInstance(application).bookmarkDao()
    )

    private val prefs = application.getSharedPreferences("rcv_reader", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(ReadingUiState())
    val uiState: StateFlow<ReadingUiState> = _uiState.asStateFlow()

    private var allBooks: List<Book> = emptyList()
    private var versesJob: Job? = null
    private var bookmarkJob: Job? = null

    init {
        viewModelScope.launch {
            val books = repository.getAllBooks().first()
            allBooks = books

            val back = deserializeStack(prefs.getString("nav_back_stack", "") ?: "", books)
            val fwd = deserializeStack(prefs.getString("nav_forward_stack", "") ?: "", books)
            _uiState.update { it.copy(books = books, backStack = back, forwardStack = fwd) }

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
        val fromBook = _uiState.value.currentBook
        val fromChapter = _uiState.value.currentChapter
        if (fromBook != null && (fromBook.id != bookId || fromChapter != chapter)) {
            val newBack = (_uiState.value.backStack + (fromBook to fromChapter)).takeLast(MAX_HISTORY)
            _uiState.update { it.copy(backStack = newBack, forwardStack = emptyList()) }
            persistStacks(newBack, emptyList())
        }
        loadChapter(bookId, chapter)
    }

    fun navigateBack() {
        val backStack = _uiState.value.backStack
        if (backStack.isEmpty()) return
        val fromBook = _uiState.value.currentBook ?: return
        val fromChapter = _uiState.value.currentChapter
        val destination = backStack.last()
        val newBack = backStack.dropLast(1)
        val newFwd = listOf(fromBook to fromChapter) + _uiState.value.forwardStack
        _uiState.update { it.copy(backStack = newBack, forwardStack = newFwd) }
        persistStacks(newBack, newFwd)
        loadChapter(destination.first.id, destination.second)
    }

    fun navigateForward() {
        val forwardStack = _uiState.value.forwardStack
        if (forwardStack.isEmpty()) return
        val fromBook = _uiState.value.currentBook ?: return
        val fromChapter = _uiState.value.currentChapter
        val destination = forwardStack.first()
        val newBack = (_uiState.value.backStack + (fromBook to fromChapter)).takeLast(MAX_HISTORY)
        val newFwd = forwardStack.drop(1)
        _uiState.update { it.copy(backStack = newBack, forwardStack = newFwd) }
        persistStacks(newBack, newFwd)
        loadChapter(destination.first.id, destination.second)
    }

    private fun loadChapter(bookId: Int, chapter: Int) {
        _uiState.update { it.copy(pendingBook = null) }
        versesJob?.cancel()
        bookmarkJob?.cancel()
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
        bookmarkJob = viewModelScope.launch {
            launch {
                bookmarkRepository.getBookmarkedVerseIds(bookId, chapter).collect { ids ->
                    _uiState.update { it.copy(bookmarkedVerseIds = ids) }
                }
            }
            launch {
                bookmarkRepository.getBookmarkedFootnoteIds(bookId, chapter).collect { ids ->
                    _uiState.update { it.copy(bookmarkedFootnoteIds = ids) }
                }
            }
        }
    }

    private fun persistStacks(back: List<Pair<Book, Int>>, fwd: List<Pair<Book, Int>>) {
        prefs.edit()
            .putString("nav_back_stack", serializeStack(back))
            .putString("nav_forward_stack", serializeStack(fwd))
            .apply()
    }

    private fun serializeStack(stack: List<Pair<Book, Int>>): String =
        stack.joinToString("|") { (book, chapter) -> "${book.id}:$chapter" }

    private fun deserializeStack(encoded: String, books: List<Book>): List<Pair<Book, Int>> {
        if (encoded.isBlank()) return emptyList()
        return encoded.split("|").mapNotNull { entry ->
            val parts = entry.split(":")
            if (parts.size != 2) return@mapNotNull null
            val bookId = parts[0].toIntOrNull() ?: return@mapNotNull null
            val chapter = parts[1].toIntOrNull() ?: return@mapNotNull null
            val book = books.find { it.id == bookId } ?: return@mapNotNull null
            book to chapter
        }
    }

    fun navigateToVerse(bookId: Int, chapter: Int, verseNumber: Int, expandFootnotes: Boolean) {
        _uiState.update {
            it.copy(
                scrollToVerseNumber = verseNumber,
                autoExpandFootnoteVerseNumber = if (expandFootnotes) verseNumber else null
            )
        }
        navigateTo(bookId, chapter)
    }

    fun clearScrollTarget() {
        _uiState.update {
            it.copy(scrollToVerseNumber = null, autoExpandFootnoteVerseNumber = null)
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

    fun toggleVerseBookmark(verse: Verse) {
        val book = _uiState.value.currentBook ?: return
        viewModelScope.launch {
            bookmarkRepository.toggleVerseBookmark(verse, book.name, book.abbreviation)
        }
    }

    fun toggleFootnoteBookmark(footnote: Footnote) {
        val book = _uiState.value.currentBook ?: return
        viewModelScope.launch {
            bookmarkRepository.toggleFootnoteBookmark(footnote, book.name, book.abbreviation)
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
