package com.rcvreader.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rcvreader.data.model.Book
import com.rcvreader.ui.theme.GoldAccent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavigationBottomSheet(
    books: List<Book>,
    currentBook: Book?,
    selectedBook: Book?,
    currentChapter: Int,
    initialTab: Int = 0,
    onBookSelected: (Book) -> Unit,
    onChapterSelected: (Int) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedTab by remember { mutableIntStateOf(initialTab) }

    val activeBook = selectedBook ?: currentBook

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(12.dp))
                Surface(
                    modifier = Modifier
                        .padding(bottom = 4.dp),
                    shape = RoundedCornerShape(2.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                ) {
                    Spacer(
                        Modifier
                            .height(4.dp)
                            .padding(horizontal = 0.dp)
                            .fillMaxWidth(0.1f)
                    )
                }
            }
        },
        modifier = modifier
    ) {
        Column {
            // Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = GoldAccent
                    )
                },
                divider = {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(0.5.dp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                    ) {}
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            "Books",
                            fontWeight = if (selectedTab == 0) FontWeight.SemiBold else FontWeight.Normal
                        )
                    },
                    selectedContentColor = MaterialTheme.colorScheme.primary,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            "Chapters",
                            fontWeight = if (selectedTab == 1) FontWeight.SemiBold else FontWeight.Normal
                        )
                    },
                    selectedContentColor = MaterialTheme.colorScheme.primary,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }

            // Content
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                if (selectedTab == 0) {
                    BooksPanel(
                        books = books,
                        currentBook = activeBook,
                        onBookSelected = { book ->
                            onBookSelected(book)
                            selectedTab = 1
                        }
                    )
                } else {
                    ChaptersPanel(
                        chapterCount = activeBook?.chapterCount ?: 1,
                        currentChapter = if (selectedBook != null) -1 else currentChapter,
                        onChapterSelected = onChapterSelected
                    )
                }
            }
        }
    }
}

@Composable
private fun BooksPanel(
    books: List<Book>,
    currentBook: Book?,
    onBookSelected: (Book) -> Unit
) {
    val otBooks = books.filter { it.testament == "OT" }
    val ntBooks = books.filter { it.testament == "NT" }

    SectionLabel("Old Testament")
    Spacer(Modifier.height(8.dp))
    BookGrid(books = otBooks, currentBook = currentBook, onBookSelected = onBookSelected)

    Spacer(Modifier.height(20.dp))

    SectionLabel("New Testament")
    Spacer(Modifier.height(8.dp))
    BookGrid(books = ntBooks, currentBook = currentBook, onBookSelected = onBookSelected)

    Spacer(Modifier.height(16.dp))
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(
            letterSpacing = 1.5.sp,
            fontWeight = FontWeight.SemiBold
        ),
        color = GoldAccent
    )
}

@Composable
private fun BookGrid(
    books: List<Book>,
    currentBook: Book?,
    onBookSelected: (Book) -> Unit
) {
    val rows = books.chunked(3)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        rows.forEach { rowBooks ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                rowBooks.forEach { book ->
                    val isSelected = book.id == currentBook?.id
                    val bgColor by animateColorAsState(
                        targetValue = if (isSelected) {
                            GoldAccent.copy(alpha = 0.2f)
                        } else {
                            MaterialTheme.colorScheme.background
                        },
                        label = "bookBg"
                    )

                    Surface(
                        modifier = Modifier
                            .weight(1f),
                        onClick = { onBookSelected(book) },
                        shape = RoundedCornerShape(10.dp),
                        color = bgColor,
                        border = if (isSelected) {
                            BorderStroke(1.5.dp, GoldAccent)
                        } else null
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp)
                        ) {
                            Text(
                                text = book.abbreviation,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onBackground
                                },
                                maxLines = 1
                            )
                        }
                    }
                }
                repeat(3 - rowBooks.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ChaptersPanel(
    chapterCount: Int,
    currentChapter: Int,
    onChapterSelected: (Int) -> Unit
) {
    SectionLabel("Select Chapter")
    Spacer(Modifier.height(12.dp))

    val rows = (1..chapterCount).chunked(6)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        rows.forEach { rowChapters ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                rowChapters.forEach { chapter ->
                    val isSelected = chapter == currentChapter
                    val bgColor by animateColorAsState(
                        targetValue = if (isSelected) {
                            GoldAccent
                        } else {
                            MaterialTheme.colorScheme.background
                        },
                        label = "chBg"
                    )

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f),
                        onClick = { onChapterSelected(chapter) },
                        shape = RoundedCornerShape(12.dp),
                        color = bgColor,
                        border = if (isSelected) {
                            BorderStroke(1.5.dp, GoldAccent)
                        } else null
                    ) {
                        Column(
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(4.dp)
                        ) {
                            Text(
                                text = "$chapter",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    lineHeight = 20.sp
                                ),
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.surface
                                } else {
                                    MaterialTheme.colorScheme.onBackground
                                }
                            )
                        }
                    }
                }
                repeat(6 - rowChapters.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }

    Spacer(Modifier.height(16.dp))
}
