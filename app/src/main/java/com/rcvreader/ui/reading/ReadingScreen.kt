package com.rcvreader.ui.reading

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
private fun GradientChip(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val bg = MaterialTheme.colorScheme.surface
    Box(
        modifier = modifier
            .drawBehind {
                // Draw an oval larger than the chip bounds so the gradient
                // fades to transparent in all directions with no hard edge.
                val rx = size.width * 0.25f
                val ry = size.height * 0.6f
                drawOval(
                    brush = Brush.radialGradient(
                        colors = listOf(bg, Color.Transparent),
                        center = Offset(size.width / 2f, size.height / 2f),
                        radius = maxOf(size.width + rx * 2f, size.height + ry * 2f) / 2f
                    ),
                    topLeft = Offset(-rx, -ry),
                    size = Size(size.width + rx * 2f, size.height + ry * 2f)
                )
            }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
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
    androidx.compose.foundation.layout.Column(
        modifier = modifier.fillMaxWidth()
    ) {
        // Prev / Next row — fully transparent background
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            uiState.previousChapter?.let { (book, chapter) ->
                GradientChip(
                    onClick = { onPrevChapter(book, chapter) }
                ) {
                    Text(
                        text = "\u2190 ${book.name} $chapter",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            } ?: Spacer(Modifier.width(1.dp))

            uiState.nextChapter?.let { (book, chapter) ->
                GradientChip(
                    onClick = { onNextChapter(book, chapter) }
                ) {
                    Text(
                        text = "${book.name} $chapter \u2192",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            } ?: Spacer(Modifier.width(1.dp))
        }

        // Book / Chapter row — fully transparent background
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            GradientChip(onClick = onBookClick) {
                Text(
                    text = uiState.currentBook?.name ?: "",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 22.sp
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            Spacer(Modifier.width(8.dp))

            GradientChip(onClick = onChapterClick) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
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
    }
}
