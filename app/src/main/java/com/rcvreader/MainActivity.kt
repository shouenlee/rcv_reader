package com.rcvreader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rcvreader.ui.bookmarks.BookmarkScreen
import com.rcvreader.ui.bookmarks.BookmarkViewModel
import com.rcvreader.ui.reading.ReadingScreen
import com.rcvreader.ui.reading.ReadingViewModel
import com.rcvreader.ui.search.SearchScreen
import com.rcvreader.ui.search.SearchViewModel
import com.rcvreader.ui.settings.SettingsViewModel
import com.rcvreader.ui.settings.ThemeMode
import com.rcvreader.ui.theme.RCVReaderTheme

private enum class ActivePane { BOOKMARKS, READING, SEARCH }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settingsViewModel: SettingsViewModel = viewModel()
            val readingViewModel: ReadingViewModel = viewModel()
            val searchViewModel: SearchViewModel = viewModel()
            val bookmarkViewModel: BookmarkViewModel = viewModel()

            val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
            val readingUiState by readingViewModel.uiState.collectAsStateWithLifecycle()

            // Keep SearchViewModel in sync with the currently open book
            val currentBookId = readingUiState.currentBook?.id
            LaunchedEffect(currentBookId) {
                searchViewModel.setCurrentBookId(currentBookId)
            }

            val darkTheme = when (settings.themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            RCVReaderTheme(darkTheme = darkTheme) {
                val screenWidth = LocalConfiguration.current.screenWidthDp.dp

                val keyboardController = LocalSoftwareKeyboardController.current
                var activePane by remember { mutableStateOf(ActivePane.READING) }

                // Hide keyboard whenever leaving search pane
                LaunchedEffect(activePane) {
                    if (activePane != ActivePane.SEARCH) keyboardController?.hide()
                }

                val targetOffset = when (activePane) {
                    ActivePane.BOOKMARKS -> screenWidth
                    ActivePane.READING -> 0.dp
                    ActivePane.SEARCH -> -screenWidth
                }

                val offsetX by animateDpAsState(
                    targetValue = targetOffset,
                    animationSpec = tween(durationMillis = 280),
                    label = "pane-offset"
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(activePane) {
                            var dragTotal = 0f
                            detectHorizontalDragGestures(
                                onDragStart = { dragTotal = 0f },
                                onDragEnd = {
                                    val threshold = 60.dp.toPx()
                                    if (dragTotal < -threshold) {
                                        // Swipe left: Reading -> Search, Bookmarks -> Reading
                                        activePane = when (activePane) {
                                            ActivePane.READING -> ActivePane.SEARCH
                                            ActivePane.BOOKMARKS -> ActivePane.READING
                                            ActivePane.SEARCH -> ActivePane.SEARCH
                                        }
                                    } else if (dragTotal > threshold) {
                                        // Swipe right: Reading -> Bookmarks, Search -> Reading
                                        activePane = when (activePane) {
                                            ActivePane.READING -> ActivePane.BOOKMARKS
                                            ActivePane.SEARCH -> ActivePane.READING
                                            ActivePane.BOOKMARKS -> ActivePane.BOOKMARKS
                                        }
                                    }
                                },
                                onHorizontalDrag = { change, amount ->
                                    change.consume()
                                    dragTotal += amount
                                }
                            )
                        }
                ) {
                    // Bookmarks screen (left of reading)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .offset(x = offsetX - screenWidth)
                    ) {
                        BookmarkScreen(
                            viewModel = bookmarkViewModel,
                            onNavigate = { bookId, chapter, verseNumber, isFootnote ->
                                readingViewModel.navigateToVerse(
                                    bookId, chapter, verseNumber, isFootnote
                                )
                                activePane = ActivePane.READING
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Reading screen (center)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .offset(x = offsetX)
                    ) {
                        ReadingScreen(
                            viewModel = readingViewModel,
                            settingsViewModel = settingsViewModel,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Search screen (right of reading)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .offset(x = offsetX + screenWidth)
                    ) {
                        SearchScreen(
                            viewModel = searchViewModel,
                            searchVisible = activePane == ActivePane.SEARCH,
                            onNavigate = { bookId, chapter ->
                                readingViewModel.navigateTo(bookId, chapter)
                                activePane = ActivePane.READING
                            },
                            onCancel = { activePane = ActivePane.READING },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}
