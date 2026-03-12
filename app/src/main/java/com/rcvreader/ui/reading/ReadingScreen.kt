package com.rcvreader.ui.reading

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
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

    var sheetOpen by remember { mutableStateOf(false) }
    var sheetInitialTab by remember { mutableIntStateOf(0) }
    var settingsPanelOpen by remember { mutableStateOf(false) }

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
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Previous / Next chapter links
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    uiState.previousChapter?.let { (book, chapter) ->
                        TextButton(
                            onClick = { viewModel.navigateTo(book.id, chapter) }
                        ) {
                            Text(
                                "\u2190 ${book.name} $chapter",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    } ?: Spacer(Modifier.width(1.dp))

                    uiState.nextChapter?.let { (book, chapter) ->
                        TextButton(
                            onClick = { viewModel.navigateTo(book.id, chapter) }
                        ) {
                            Text(
                                "${book.name} $chapter \u2192",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    } ?: Spacer(Modifier.width(1.dp))
                }

                // Navigation trigger bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = currentBook?.name ?: "",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 22.sp
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable {
                            sheetInitialTab = 0
                            sheetOpen = true
                        }
                    )

                    Spacer(Modifier.width(10.dp))

                    Surface(
                        onClick = {
                            sheetInitialTab = 1
                            sheetOpen = true
                        },
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Ch. $currentChapter",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Medium
                                ),
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f)
                            )
                        }
                    }

                    Spacer(Modifier.width(4.dp))

                    Text(
                        text = "\u25BE",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f)
                    )
                }

                // Verse list
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(top = 8.dp)
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
            }
        }

        // Settings panel (overlays everything)
        SettingsPanel(
            settings = settings,
            onThemeChange = { settingsViewModel.setThemeMode(it) },
            onTextSizeChange = { settingsViewModel.setTextSize(it) },
            onDismiss = { settingsPanelOpen = false },
            visible = settingsPanelOpen
        )
    }

    // Navigation bottom sheet
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
