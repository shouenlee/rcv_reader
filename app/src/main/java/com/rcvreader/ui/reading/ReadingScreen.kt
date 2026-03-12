package com.rcvreader.ui.reading

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rcvreader.ui.navigation.NavigationBottomSheet
import com.rcvreader.ui.settings.SettingsPanel
import com.rcvreader.ui.settings.SettingsViewModel
import com.rcvreader.ui.theme.GoldAccent

@Composable
fun ReadingScreen(
    viewModel: ReadingViewModel = viewModel(),
    settingsViewModel: SettingsViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val density = LocalDensity.current

    var sheetOpen by remember { mutableStateOf(false) }
    var sheetInitialTab by remember { mutableIntStateOf(0) }
    var settingsPanelOpen by remember { mutableStateOf(false) }
    var navHeightPx by remember { mutableIntStateOf(0) }
    val navHeightDp = with(density) { navHeightPx.toDp() }

    val currentBook = uiState.currentBook
    val currentChapter = uiState.currentChapter
    LaunchedEffect(currentBook?.id, currentChapter) {
        listState.scrollToItem(0)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(settingsPanelOpen) {
                var dragTotal = 0f
                detectHorizontalDragGestures(
                    onDragStart = { dragTotal = 0f },
                    onDragEnd = {
                        if (dragTotal < -60.dp.toPx()) settingsPanelOpen = true
                        else if (dragTotal > 60.dp.toPx()) settingsPanelOpen = false
                    },
                    onHorizontalDrag = { change, amount ->
                        change.consume()
                        dragTotal += amount
                    }
                )
            }
    ) {
        Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Verse list — scrolls under the floating nav
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = navHeightDp)
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
                            onClick = { viewModel.toggleVerse(verse) }
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

                // Floating nav overlay — transparent background, opaque buttons only
                FloatingNav(
                    uiState = uiState,
                    onPrevChapter = { book, chapter -> viewModel.navigateTo(book.id, chapter) },
                    onNextChapter = { book, chapter -> viewModel.navigateTo(book.id, chapter) },
                    onBookClick = { sheetInitialTab = 0; sheetOpen = true },
                    onChapterClick = { sheetInitialTab = 1; sheetOpen = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onSizeChanged { navHeightPx = it.height }
                )
            }
        }

        // Settings panel
        SettingsPanel(
            settings = settings,
            onThemeChange = { settingsViewModel.setThemeMode(it) },
            onTextSizeChange = { settingsViewModel.setTextSize(it) },
            onDismiss = { settingsPanelOpen = false },
            visible = settingsPanelOpen
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
private fun NavChip(text: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(7.dp),
        color = MaterialTheme.colorScheme.background,
        border = androidx.compose.foundation.BorderStroke(
            0.5.dp,
            MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f)
        )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun FloatingNav(
    uiState: ReadingUiState,
    onPrevChapter: (com.rcvreader.data.model.Book, Int) -> Unit,
    onNextChapter: (com.rcvreader.data.model.Book, Int) -> Unit,
    onBookClick: () -> Unit,
    onChapterClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = MaterialTheme.colorScheme.background

    Box(modifier = modifier) {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Solid background for the chips area
            androidx.compose.foundation.layout.Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(bgColor)
            ) {
                Spacer(Modifier.height(4.dp))

                // Prev / Next row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    uiState.previousChapter?.let { (book, chapter) ->
                        NavChip(
                            text = "\u2190 ${book.name} $chapter",
                            onClick = { onPrevChapter(book, chapter) }
                        )
                    } ?: Spacer(Modifier.width(1.dp))

                    uiState.nextChapter?.let { (book, chapter) ->
                        NavChip(
                            text = "${book.name} $chapter \u2192",
                            onClick = { onNextChapter(book, chapter) }
                        )
                    } ?: Spacer(Modifier.width(1.dp))
                }

                Spacer(Modifier.height(6.dp))

                // Book / Chapter row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        onClick = onBookClick,
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(
                            0.5.dp,
                            MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f)
                        ),
                        shadowElevation = 1.dp
                    ) {
                        Text(
                            text = uiState.currentBook?.name ?: "",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 22.sp
                            ),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }

                    Spacer(Modifier.width(8.dp))

                    Surface(
                        onClick = onChapterClick,
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(
                            0.5.dp,
                            MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f)
                        ),
                        shadowElevation = 1.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Ch. ${uiState.currentChapter}",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Medium
                                ),
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "\u25BE",
                                fontSize = 11.sp,
                                color = GoldAccent.copy(alpha = 0.8f)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
            }

            // Gradient fade — background color to transparent
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(bgColor, bgColor.copy(alpha = 0f))
                        )
                    )
            )
        }
    }
}
