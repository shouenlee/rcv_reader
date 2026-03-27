package com.rcvreader.ui.reading

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rcvreader.data.model.Footnote
import com.rcvreader.data.model.Verse
import com.rcvreader.ui.theme.FootnoteHighlight
import com.rcvreader.ui.theme.GoldAccent
import com.rcvreader.ui.theme.VerseDotColor

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VerseItem(
    verse: Verse,
    isExpanded: Boolean,
    footnotes: List<Footnote>,
    onClick: () -> Unit,
    isBookmarked: Boolean = false,
    bookmarkedFootnoteIds: Set<Int> = emptySet(),
    onLongPress: () -> Unit = {},
    onFootnoteLongPress: (Footnote) -> Unit = {},
    textSizeSp: Float = 17f,
    modifier: Modifier = Modifier
) {
    val bgColor = if (isExpanded) FootnoteHighlight else MaterialTheme.colorScheme.background
    val borderColor = GoldAccent
    val haptic = LocalHapticFeedback.current

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
            .combinedClickable(
                onClick = { if (verse.hasFootnotes) onClick() },
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongPress()
                }
            )
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        val verseText = buildAnnotatedString {
            withStyle(
                SpanStyle(
                    color = MaterialTheme.colorScheme.secondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal,
                    baselineShift = BaselineShift.Superscript
                )
            ) {
                append("${verse.verseNumber}")
            }
            append("  ")
            append(verse.text)
            if (verse.hasFootnotes || isBookmarked) {
                append("  ")
            }
            if (verse.hasFootnotes) {
                withStyle(SpanStyle(color = VerseDotColor, fontSize = 12.sp)) {
                    append("\u2022")
                }
            }
            if (isBookmarked) {
                if (verse.hasFootnotes) append(" ")
                withStyle(SpanStyle(color = GoldAccent, fontSize = 11.sp)) {
                    append("\uD83C\uDFF7\uFE0E") // bookmark emoji
                }
            }
        }

        Text(
            text = verseText,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = textSizeSp.sp,
                lineHeight = (textSizeSp * 1.75f).sp
            ),
            color = MaterialTheme.colorScheme.onBackground
        )

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            FootnoteSection(
                footnotes = footnotes,
                bookmarkedFootnoteIds = bookmarkedFootnoteIds,
                onFootnoteLongPress = onFootnoteLongPress
            )
        }
    }
}
