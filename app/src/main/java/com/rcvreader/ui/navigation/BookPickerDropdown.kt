package com.rcvreader.ui.navigation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.rcvreader.data.model.Book

@Composable
fun BookPickerDropdown(
    currentBook: Book?,
    books: List<Book>,
    expanded: Boolean,
    onToggle: () -> Unit,
    onBookSelected: (Book) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        Surface(
            onClick = onToggle,
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = currentBook?.name ?: "",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(6.dp))
                Text("\u25BE", color = MaterialTheme.colorScheme.secondary)
            }
        }

        if (expanded) {
            Popup(
                onDismissRequest = onToggle,
                properties = PopupProperties(focusable = true)
            ) {
                Surface(
                    modifier = Modifier
                        .width(300.dp)
                        .heightIn(max = 400.dp),
                    shape = RoundedCornerShape(12.dp),
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(
                        modifier = Modifier
                            .padding(12.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        val otBooks = books.filter { it.testament == "OT" }
                        val ntBooks = books.filter { it.testament == "NT" }

                        Text(
                            "OLD TESTAMENT",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        BookGrid(otBooks, currentBook, onBookSelected)

                        Spacer(Modifier.height(12.dp))

                        Text(
                            "NEW TESTAMENT",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        BookGrid(ntBooks, currentBook, onBookSelected)
                    }
                }
            }
        }
    }
}

@Composable
private fun BookGrid(
    books: List<Book>,
    currentBook: Book?,
    onBookSelected: (Book) -> Unit
) {
    val rows = books.chunked(3)
    Column {
        rows.forEach { rowBooks ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                rowBooks.forEach { book ->
                    val isSelected = book.id == currentBook?.id
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 3.dp),
                        onClick = { onBookSelected(book) },
                        shape = RoundedCornerShape(6.dp),
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        } else {
                            MaterialTheme.colorScheme.background
                        },
                        border = if (isSelected) {
                            BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                        } else null
                    ) {
                        Text(
                            text = book.abbreviation,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onBackground
                            },
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
                            maxLines = 1
                        )
                    }
                }
                repeat(3 - rowBooks.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}
