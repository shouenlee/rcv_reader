package com.rcvreader.ui.reading

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rcvreader.data.model.Footnote
import com.rcvreader.data.model.Verse
import com.rcvreader.ui.theme.FootnoteHighlight
import com.rcvreader.ui.theme.GoldAccent
import com.rcvreader.ui.theme.VerseDotColor

@Composable
fun VerseItem(
    verse: Verse,
    isExpanded: Boolean,
    footnotes: List<Footnote>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isClickable = verse.has_footnotes == 1
    val bgColor = if (isExpanded) FootnoteHighlight else MaterialTheme.colorScheme.background
    val borderColor = GoldAccent

    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (isExpanded) {
                    Modifier.drawBehind {
                        drawLine(
                            color = borderColor,
                            start = Offset(0f, 0f),
                            end = Offset(0f, size.height),
                            strokeWidth = 3.dp.toPx()
                        )
                    }
                } else Modifier
            )
            .background(bgColor)
            .then(
                if (isClickable) Modifier.clickable { onClick() }
                else Modifier
            )
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        val verseText = buildAnnotatedString {
            withStyle(
                SpanStyle(
                    color = MaterialTheme.colorScheme.secondary,
                    fontSize = 10.sp
                )
            ) {
                append("${verse.verse_number} ")
            }
            append(verse.text)
            if (verse.has_footnotes == 1) {
                append(" ")
                withStyle(SpanStyle(color = VerseDotColor, fontSize = 8.sp)) {
                    append("\u25CF")
                }
            }
        }

        Text(
            text = verseText,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground
        )

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            FootnoteSection(footnotes = footnotes)
        }
    }
}
