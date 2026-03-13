package com.rcvreader.ui.search

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rcvreader.data.db.BibleDatabase
import com.rcvreader.data.model.SearchResult
import com.rcvreader.data.model.SearchScope
import com.rcvreader.data.repository.BibleRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val scope: SearchScope = SearchScope.ALL,
    val includeFootnotes: Boolean = false,
    val results: List<SearchResult> = emptyList(),
    val isSearching: Boolean = false
)

private data class SearchParams(
    val query: String,
    val scope: SearchScope,
    val includeFootnotes: Boolean,
    val currentBookId: Int?
)

@OptIn(FlowPreview::class)
class SearchViewModel(application: Application) : AndroidViewModel(application) {

    private val db = BibleDatabase.getInstance(application)
    private val repository = BibleRepository(
        db.bookDao(), db.verseDao(), db.footnoteDao(), db.searchDao()
    )

    private val _query = MutableStateFlow("")
    private val _scope = MutableStateFlow(SearchScope.ALL)
    private val _includeFootnotes = MutableStateFlow(false)
    private val _currentBookId = MutableStateFlow<Int?>(null)
    private val _results = MutableStateFlow<List<SearchResult>>(emptyList())
    private val _isSearching = MutableStateFlow(false)

    val uiState: StateFlow<SearchUiState> = combine(
        combine(_query, _scope, _includeFootnotes) { q, s, f -> Triple(q, s, f) },
        combine(_results, _isSearching) { r, s -> Pair(r, s) }
    ) { (query, scope, footnotes), (results, searching) ->
        SearchUiState(query, scope, footnotes, results, searching)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), SearchUiState())

    init {
        viewModelScope.launch {
            combine(_query, _scope, _includeFootnotes, _currentBookId) { q, s, f, id ->
                SearchParams(q, s, f, id)
            }
                .debounce(300L)
                .collect { params ->
                    if (params.query.isBlank()) {
                        _results.value = emptyList()
                        _isSearching.value = false
                        return@collect
                    }
                    _isSearching.value = true
                    try {
                        _results.value = repository.search(
                            params.query,
                            params.scope,
                            params.includeFootnotes,
                            params.currentBookId
                        )
                    } catch (e: Exception) {
                        Log.e("SearchViewModel", "Search failed", e)
                        _results.value = emptyList()
                    } finally {
                        _isSearching.value = false
                    }
                }
        }
    }

    fun setQuery(query: String) { _query.value = query }
    fun setScope(scope: SearchScope) { _scope.value = scope }
    fun setIncludeFootnotes(include: Boolean) { _includeFootnotes.value = include }
    fun setCurrentBookId(id: Int?) { _currentBookId.value = id }
    fun hasCurrentBook(): Boolean = _currentBookId.value != null
}
