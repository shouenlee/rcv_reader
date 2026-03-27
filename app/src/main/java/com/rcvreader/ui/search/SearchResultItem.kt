package com.rcvreader.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rcvreader.data.model.SearchResult
import com.rcvreader.ui.theme.GoldAccent

@Composable
fun SearchResultItem(
    result: SearchResult,
    query: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val matchWords = remember(query) {
        query.trim().split(Regex("\\s+"))
            .map { it.replace(Regex("""["*()\-]"""), "").trim().lowercase() }
            .filter { it.length >= 2 }
    }

    val displayText = remember(result, matchWords) {
        buildAnnotatedString {
            if (result.keyword != null) {
                withStyle(SpanStyle(color = GoldAccent, fontWeight = FontWeight.Bold)) {
                    append("${result.keyword}: ")
                }
            }

            val keywordOffset = result.keyword?.let { it.length + 2 } ?: 0
            append(result.text)

            val lowerText = result.text.lowercase()
            matchWords.forEach { word ->
                var idx = lowerText.indexOf(word)
                while (idx >= 0) {
                    addStyle(
                        SpanStyle(
                            background = GoldAccent.copy(alpha = 0.2f),
                            fontWeight = FontWeight.Bold
                        ),
                        start = keywordOffset + idx,
                        end = keywordOffset + idx + word.length
                    )
                    idx = lowerText.indexOf(word, idx + 1)
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 14.dp)
    ) {
        Text(
            text = "${result.abbreviation} ${result.chapter}:${result.verseNumber}",
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = GoldAccent,
            letterSpacing = 0.5.sp,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = displayText,
            style = MaterialTheme.typography.bodySmall.copy(
                lineHeight = 20.sp
            ),
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f)
        )
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f))
}
