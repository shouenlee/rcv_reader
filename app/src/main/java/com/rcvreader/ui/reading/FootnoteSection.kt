package com.rcvreader.ui.reading

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.rcvreader.data.model.Footnote
import com.rcvreader.ui.theme.GoldAccent

@Composable
fun FootnoteSection(
    footnotes: List<Footnote>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(top = 8.dp)) {
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp),
            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)
        )

        footnotes.forEach { footnote ->
            val annotatedText = buildAnnotatedString {
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
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
    }
}
