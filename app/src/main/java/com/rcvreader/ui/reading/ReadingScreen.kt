package com.rcvreader.ui.reading

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rcvreader.ui.navigation.BookPickerDropdown
import com.rcvreader.ui.navigation.ChapterPickerDropdown

@Composable
fun ReadingScreen(
    viewModel: ReadingViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    var bookPickerExpanded by remember { mutableStateOf(false) }
    var chapterPickerExpanded by remember { mutableStateOf(false) }

    val currentBook = uiState.currentBook
    val currentChapter = uiState.currentChapter
    LaunchedEffect(currentBook?.id, currentChapter) {
        listState.scrollToItem(0)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Previous chapter link
            uiState.previousChapter?.let { (book, chapter) ->
                TextButton(
                    onClick = { viewModel.navigateTo(book.id, chapter) },
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        "\u2190 ${book.name} $chapter",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            // Toolbar with dropdowns
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BookPickerDropdown(
                    currentBook = uiState.currentBook,
                    books = uiState.books,
                    expanded = bookPickerExpanded,
                    onToggle = {
                        bookPickerExpanded = !bookPickerExpanded
                        chapterPickerExpanded = false
                    },
                    onBookSelected = { book ->
                        bookPickerExpanded = false
                        chapterPickerExpanded = true
                        viewModel.selectBook(book)
                    }
                )

                Spacer(Modifier.width(8.dp))

                ChapterPickerDropdown(
                    currentChapter = uiState.currentChapter,
                    chapterCount = uiState.currentBook?.chapterCount ?: 1,
                    expanded = chapterPickerExpanded,
                    onToggle = {
                        chapterPickerExpanded = !chapterPickerExpanded
                        bookPickerExpanded = false
                    },
                    onChapterSelected = { chapter ->
                        chapterPickerExpanded = false
                        val targetBook = uiState.pendingBook ?: uiState.currentBook
                        targetBook?.let { book ->
                            viewModel.navigateTo(book.id, chapter)
                        }
                    }
                )
            }

            // Verse list
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                items(
                    items = uiState.verses,
                    key = { it.id }
                ) { verse ->
                    VerseItem(
                        verse = verse,
                        isExpanded = uiState.expandedVerseId == verse.id,
                        footnotes = if (uiState.expandedVerseId == verse.id) {
                            uiState.expandedFootnotes
                        } else {
                            emptyList()
                        },
                        onClick = { viewModel.toggleVerse(verse) }
                    )
                }

                // Next chapter button at bottom
                item {
                    uiState.nextChapter?.let { (book, chapter) ->
                        TextButton(
                            onClick = { viewModel.navigateTo(book.id, chapter) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                        ) {
                            Text(
                                "Next: ${book.name} $chapter \u2192",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}
