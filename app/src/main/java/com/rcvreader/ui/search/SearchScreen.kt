package com.rcvreader.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rcvreader.data.model.SearchScope
import com.rcvreader.ui.theme.GoldAccent

@Composable
fun SearchScreen(
    onNavigate: (bookId: Int, chapter: Int) -> Unit,
    onCancel: () -> Unit,
    searchVisible: Boolean,
    viewModel: SearchViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(searchVisible) {
        if (searchVisible) {
            try { focusRequester.requestFocus() } catch (_: Exception) {}
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Search bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 52.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = GoldAccent,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    BasicTextField(
                        value = uiState.query,
                        onValueChange = { viewModel.setQuery(it) },
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 14.sp
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        singleLine = true,
                        decorationBox = { inner ->
                            if (uiState.query.isEmpty()) {
                                Text(
                                    "Search verses...",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.35f)
                                )
                            }
                            inner()
                        }
                    )
                }
            }
            if (uiState.query.isNotEmpty()) {
                TextButton(onClick = { viewModel.setQuery("") }, modifier = Modifier.padding(start = 4.dp)) {
                    Text(
                        "Clear",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            } else {
                TextButton(onClick = { keyboardController?.hide(); onCancel() }, modifier = Modifier.padding(start = 4.dp)) {
                    Text(
                        "Cancel",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Scope chips + footnotes chip — unified filter row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val scopes = listOf(
                SearchScope.ALL to "All",
                SearchScope.OT to "OT",
                SearchScope.NT to "NT",
                SearchScope.THIS_BOOK to "This Book"
            )
            scopes.forEach { (scope, label) ->
                val selected = uiState.scope == scope
                val enabled = scope != SearchScope.THIS_BOOK || viewModel.hasCurrentBook()
                Surface(
                    onClick = { if (enabled) viewModel.setScope(scope) },
                    shape = RoundedCornerShape(20.dp),
                    color = if (selected) GoldAccent.copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f),
                    border = if (selected)
                        androidx.compose.foundation.BorderStroke(1.5.dp, GoldAccent)
                    else null
                ) {
                    Text(
                        text = label,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (selected) MaterialTheme.colorScheme.primary
                                else if (enabled) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.25f)
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            // Footnotes toggle — abbreviate on narrow screens
            val screenWidth = LocalConfiguration.current.screenWidthDp
            val footnoteLabel = if (screenWidth < 360) "Fn" else "Footnotes"

            Surface(
                onClick = { viewModel.setIncludeFootnotes(!uiState.includeFootnotes) },
                shape = RoundedCornerShape(20.dp),
                color = if (uiState.includeFootnotes) GoldAccent.copy(alpha = 0.15f)
                        else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f),
                border = if (uiState.includeFootnotes)
                    androidx.compose.foundation.BorderStroke(1.5.dp, GoldAccent)
                else null
            ) {
                Text(
                    text = footnoteLabel,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (uiState.includeFootnotes) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }
        }

        // Status row
        if (uiState.query.isNotBlank()) {
            Row(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (uiState.isSearching) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    val count = uiState.results.size
                    Text(
                        text = if (count == 0) "No results" else "$count result${if (count == 1) "" else "s"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f)
                    )
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f))

        // Results / empty state
        if (uiState.query.isBlank()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f),
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Type to search across 31,103 verses",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.35f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(items = uiState.results, key = { "${it.id}-${it.tier}-${it.keyword}" }) { result ->
                    SearchResultItem(
                        result = result,
                        query = uiState.query,
                        onClick = { onNavigate(result.bookId, result.chapter) }
                    )
                }
            }
        }
    }
}
