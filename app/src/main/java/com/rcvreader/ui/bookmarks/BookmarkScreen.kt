package com.rcvreader.ui.bookmarks

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rcvreader.data.model.Bookmark

@Composable
fun BookmarkScreen(
    onNavigate: (bookId: Int, chapter: Int, verseNumber: Int, isFootnote: Boolean) -> Unit,
    viewModel: BookmarkViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val bookmarks by viewModel.bookmarks.collectAsStateWithLifecycle()
    val expandedBookmarkId by viewModel.expandedBookmarkId.collectAsStateWithLifecycle()
    var jumpTarget by remember { mutableStateOf<Bookmark?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        Text(
            text = "Bookmarks",
            style = MaterialTheme.typography.titleLarge.copy(
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold
            ),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 18.dp, top = 52.dp, bottom = 8.dp)
        )

        // Count
        if (bookmarks.isNotEmpty()) {
            Text(
                text = "${bookmarks.size} bookmark${if (bookmarks.size == 1) "" else "s"}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f),
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp)
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f))

        if (bookmarks.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.BookmarkBorder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f),
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "No bookmarks yet",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.35f)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Long-press a verse or footnote to bookmark it",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.25f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(items = bookmarks, key = { it.id }) { bookmark ->
                    BookmarkItem(
                        bookmark = bookmark,
                        isExpanded = expandedBookmarkId == bookmark.id,
                        onToggle = { viewModel.toggleExpanded(bookmark.id) },
                        onLongPress = { jumpTarget = bookmark },
                        onDelete = { viewModel.deleteBookmark(bookmark.id) }
                    )
                }
            }
        }
    }

    // Jump confirmation dialog
    jumpTarget?.let { bookmark ->
        val verseLabel = if (bookmark.verseNumber == 0) "sup" else "${bookmark.verseNumber}"
        AlertDialog(
            onDismissRequest = { jumpTarget = null },
            title = {
                Text(
                    "Jump to ${bookmark.abbreviation} ${bookmark.chapter}:$verseLabel?",
                    style = MaterialTheme.typography.titleMedium
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    jumpTarget = null
                    onNavigate(
                        bookmark.bookId,
                        bookmark.chapter,
                        bookmark.verseNumber,
                        bookmark.type == "footnote"
                    )
                }) {
                    Text("Go", color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { jumpTarget = null }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.secondary)
                }
            }
        )
    }
}
