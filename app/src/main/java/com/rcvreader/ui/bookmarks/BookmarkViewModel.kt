package com.rcvreader.ui.bookmarks

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rcvreader.data.db.BookmarkDatabase
import com.rcvreader.data.model.Bookmark
import com.rcvreader.data.repository.BookmarkRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BookmarkViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = BookmarkRepository(
        BookmarkDatabase.getInstance(application).bookmarkDao()
    )

    val bookmarks: StateFlow<List<Bookmark>> = repository.getAllBookmarks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private val _expandedBookmarkId = MutableStateFlow<Long?>(null)
    val expandedBookmarkId: StateFlow<Long?> = _expandedBookmarkId.asStateFlow()

    fun toggleExpanded(id: Long) {
        _expandedBookmarkId.value = if (_expandedBookmarkId.value == id) null else id
    }

    fun deleteBookmark(id: Long) {
        viewModelScope.launch {
            repository.deleteById(id)
        }
    }
}
