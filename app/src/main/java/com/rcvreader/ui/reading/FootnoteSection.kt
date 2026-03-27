package com.rcvreader.ui.reading

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rcvreader.data.model.Footnote
import com.rcvreader.ui.theme.GoldAccent

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FootnoteSection(
    footnotes: List<Footnote>,
    bookmarkedFootnoteIds: Set<Int> = emptySet(),
    onFootnoteLongPress: (Footnote) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    Column(modifier = modifier.padding(top = 8.dp)) {
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp),
            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)
        )

        footnotes.forEach { footnote ->
            val isBookmarked = footnote.id in bookmarkedFootnoteIds
            val annotatedText = buildAnnotatedString {
                if (isBookmarked) {
                    withStyle(SpanStyle(color = GoldAccent, fontSize = 10.sp)) {
                        append("\uD83C\uDFF7\uFE0E ")
                    }
                }
                if (footnote.keyword != null) {
                    withStyle(SpanStyle(color = GoldAccent, fontWeight = FontWeight.Bold)) {
                        append(footnote.keyword)
                        append(": ")
                    }
                }
                append(footnote.content)
            }

            Text(
                text = annotatedText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                modifier = Modifier
                    .combinedClickable(
                        onClick = {},
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onFootnoteLongPress(footnote)
                        }
                    )
                    .padding(bottom = 8.dp)
            )
        }
    }
}
