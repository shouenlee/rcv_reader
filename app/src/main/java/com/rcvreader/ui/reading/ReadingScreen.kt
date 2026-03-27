package com.rcvreader.ui.reading

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rcvreader.data.model.Book
import com.rcvreader.ui.navigation.NavigationBottomSheet
import com.rcvreader.ui.settings.SettingsDialog
import com.rcvreader.ui.settings.SettingsViewModel
import com.rcvreader.ui.theme.GoldAccent

// Nav height constants.
// ROW_HEIGHT:     height of each row (prev/next and book/chapter)
// ROW_GAP:        gap between the two rows in expanded state (closes to 0 when compact)
// BOTTOM_PADDING: space below book/chapter row, above the divider — constant in both states
private val NAV_ROW_HEIGHT = 44.dp
private val NAV_ROW_GAP = 6.dp
private val NAV_BOTTOM_PADDING = 14.dp

// Derived heights used for layout and LazyColumn contentPadding
private val NAV_EXPANDED_HEIGHT = NAV_ROW_HEIGHT + NAV_ROW_GAP + NAV_ROW_HEIGHT + NAV_BOTTOM_PADDING
private val NAV_COMPACT_HEIGHT = NAV_ROW_HEIGHT + NAV_BOTTOM_PADDING

// Row 2 y-offset: lerp from (ROW_HEIGHT + GAP) → 0 as compactProgress goes 0 → 1
private val NAV_ROW2_OFFSET_EXPANDED = NAV_ROW_HEIGHT + NAV_ROW_GAP

@Composable
fun ReadingScreen(
    viewModel: ReadingViewModel = viewModel(),
    settingsViewModel: SettingsViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    var sheetOpen by remember { mutableStateOf(false) }
    var sheetInitialTab by remember { mutableIntStateOf(0) }
    var settingsOpen by remember { mutableStateOf(false) }

    // 0f = fully expanded (at top), 1f = fully compact (scrolled)
    // Only returns to 0f when user scrolls all the way back to the very top
    val compactProgress by remember {
        derivedStateOf {
            if (listState.firstVisibleItemIndex > 0) {
                1f
            } else {
                (listState.firstVisibleItemScrollOffset / 120f).coerceIn(0f, 1f)
            }
        }
    }

    val currentBook = uiState.currentBook
    val currentChapter = uiState.currentChapter
    LaunchedEffect(currentBook?.id, currentChapter) {
        listState.scrollToItem(0)
    }

    // Scroll to a specific verse when navigating from bookmarks
    val scrollTarget = uiState.scrollToVerseNumber
    val autoExpandTarget = uiState.autoExpandFootnoteVerseNumber
    LaunchedEffect(scrollTarget, uiState.verses.size) {
        if (scrollTarget != null && uiState.verses.isNotEmpty()) {
            val index = uiState.verses.indexOfFirst { it.verseNumber == scrollTarget }
            if (index >= 0) {
                listState.animateScrollToItem(index)
                // Auto-expand footnotes if requested
                if (autoExpandTarget != null) {
                    val verse = uiState.verses[index]
                    if (verse.hasFootnotes) {
                        viewModel.toggleVerse(verse)
                    }
                }
                viewModel.clearScrollTarget()
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Verse list — fixed top padding matches expanded nav height
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = NAV_EXPANDED_HEIGHT)
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
                            textSizeSp = settings.textSize.sp,
                            onClick = { viewModel.toggleVerse(verse) },
                            isBookmarked = verse.id in uiState.bookmarkedVerseIds,
                            bookmarkedFootnoteIds = uiState.bookmarkedFootnoteIds,
                            onLongPress = { viewModel.toggleVerseBookmark(verse) },
                            onFootnoteLongPress = { footnote ->
                                viewModel.toggleFootnoteBookmark(footnote)
                            }
                        )
                    }
                    item {
                        uiState.nextChapter?.let { (book, chapter) ->
                            TextButton(
                                onClick = { viewModel.navigateTo(book.id, chapter) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                            ) {
                                Text(
                                    "Next: ${book.abbreviation} $chapter \u2192",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                // Animated collapsing top nav
                AnimatedTopNav(
                    uiState = uiState,
                    compactProgress = compactProgress,
                    onPrevChapter = { book, chapter -> viewModel.navigateTo(book.id, chapter) },
                    onNextChapter = { book, chapter -> viewModel.navigateTo(book.id, chapter) },
                    onBookClick = { sheetInitialTab = 0; sheetOpen = true },
                    onChapterClick = { sheetInitialTab = 1; sheetOpen = true },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Settings icon — bottom-right, overlaid above Scaffold content
        IconButton(
            onClick = { settingsOpen = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings",
                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
            )
        }
    }

    // Settings dialog
    if (settingsOpen) {
        SettingsDialog(
            settings = settings,
            onThemeChange = { settingsViewModel.setThemeMode(it) },
            onTextSizeChange = { settingsViewModel.setTextSize(it) },
            onDismiss = { settingsOpen = false }
        )
    }

    if (sheetOpen) {
        NavigationBottomSheet(
            books = uiState.books,
            currentBook = uiState.currentBook,
            selectedBook = uiState.pendingBook,
            currentChapter = uiState.currentChapter,
            initialTab = sheetInitialTab,
            onBookSelected = { book -> viewModel.selectBook(book) },
            onChapterSelected = { chapter ->
                sheetOpen = false
                val targetBook = uiState.pendingBook ?: uiState.currentBook
                targetBook?.let { book -> viewModel.navigateTo(book.id, chapter) }
            },
            onDismiss = { sheetOpen = false }
        )
    }
}

@Composable
private fun AnimatedTopNav(
    uiState: ReadingUiState,
    compactProgress: Float,
    onPrevChapter: (Book, Int) -> Unit,
    onNextChapter: (Book, Int) -> Unit,
    onBookClick: () -> Unit,
    onChapterClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = MaterialTheme.colorScheme.background
    val navHeight = lerp(NAV_EXPANDED_HEIGHT, NAV_COMPACT_HEIGHT, compactProgress)
    val row2OffsetY: Dp = lerp(NAV_ROW2_OFFSET_EXPANDED, 0.dp, compactProgress)

    Box(
        modifier = modifier
            .height(navHeight)
            .background(bgColor)
            .clipToBounds()
    ) {
        // Row 1: Prev / Next — stays fixed at top
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(NAV_ROW_HEIGHT)
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            uiState.previousChapter?.let { (book, chapter) ->
                TextButton(onClick = { onPrevChapter(book, chapter) }) {
                    Text(
                        text = "\u2190 ${book.abbreviation} $chapter",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            } ?: Spacer(Modifier.width(1.dp))

            uiState.nextChapter?.let { (book, chapter) ->
                TextButton(onClick = { onNextChapter(book, chapter) }) {
                    Text(
                        text = "${book.abbreviation} $chapter \u2192",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            } ?: Spacer(Modifier.width(1.dp))
        }

        // Row 2: Book + Chapter — slides up into Row 1 as compactProgress increases
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(NAV_ROW_HEIGHT)
                .offset(y = row2OffsetY),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Book name — font shrinks from 22sp → 15sp
            val bookFontSize = (22f - 7f * compactProgress).sp
            Text(
                text = uiState.currentBook?.name ?: "",
                fontSize = bookFontSize,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clickable(onClick = onBookClick)
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            )

            Spacer(Modifier.width(6.dp))

            // Chapter pill — subtle, font shrinks 14sp → 11sp
            val chapterFontSize = (14f - 3f * compactProgress).sp
            Surface(
                onClick = onChapterClick,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f),
                shape = RoundedCornerShape(20.dp) // pill shape
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Ch. ${uiState.currentChapter}",
                        fontSize = chapterFontSize,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                    Spacer(Modifier.width(3.dp))
                    Text(
                        text = "\u25BE",
                        fontSize = 10.sp,
                        color = GoldAccent.copy(alpha = 0.7f)
                    )
                }
            }
        }

        // Divider — NAV_BOTTOM_PADDING above it gives consistent breathing room
        HorizontalDivider(
            modifier = Modifier.align(Alignment.BottomCenter),
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f)
        )
    }
}
