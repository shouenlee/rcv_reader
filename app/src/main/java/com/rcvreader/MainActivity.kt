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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rcvreader.ui.reading.ReadingScreen
import com.rcvreader.ui.reading.ReadingViewModel
import com.rcvreader.ui.search.SearchScreen
import com.rcvreader.ui.search.SearchViewModel
import com.rcvreader.ui.settings.SettingsViewModel
import com.rcvreader.ui.settings.ThemeMode
import com.rcvreader.ui.theme.RCVReaderTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settingsViewModel: SettingsViewModel = viewModel()
            val readingViewModel: ReadingViewModel = viewModel()
            val searchViewModel: SearchViewModel = viewModel()

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

                var searchVisible by remember { mutableStateOf(false) }
                val offsetX by animateDpAsState(
                    targetValue = if (searchVisible) -screenWidth else 0.dp,
                    animationSpec = tween(durationMillis = 280),
                    label = "pane-offset"
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(searchVisible) {
                            var dragTotal = 0f
                            detectHorizontalDragGestures(
                                onDragStart = { dragTotal = 0f },
                                onDragEnd = {
                                    if (dragTotal < -60.dp.toPx()) searchVisible = true
                                    else if (dragTotal > 60.dp.toPx()) searchVisible = false
                                },
                                onHorizontalDrag = { change, amount ->
                                    change.consume()
                                    dragTotal += amount
                                }
                            )
                        }
                ) {
                    // Reading screen
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

                    // Search screen
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .offset(x = offsetX + screenWidth)
                    ) {
                        SearchScreen(
                            viewModel = searchViewModel,
                            searchVisible = searchVisible,
                            onNavigate = { bookId, chapter ->
                                readingViewModel.navigateTo(bookId, chapter)
                                searchVisible = false
                            },
                            onCancel = { searchVisible = false },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}
