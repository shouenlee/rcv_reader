package com.rcvreader.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rcvreader.ui.theme.GoldAccent

@Composable
fun SettingsPanel(
    settings: UserSettings,
    onThemeChange: (ThemeMode) -> Unit,
    onTextSizeChange: (TextSize) -> Unit,
    onDismiss: () -> Unit,
    visible: Boolean
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        // Scrim
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f))
                .clickable(onClick = onDismiss)
        )
    }

    AnimatedVisibility(
        visible = visible,
        enter = slideInHorizontally { it },
        exit = slideOutHorizontally { it }
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.CenterEnd) {
            Surface(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(300.dp),
                color = MaterialTheme.colorScheme.background,
                shadowElevation = 16.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .padding(24.dp)
                ) {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 22.sp
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(Modifier.height(32.dp))

                    // Theme section
                    SectionLabel("Theme")
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ThemeMode.entries.forEach { mode ->
                            SettingsChip(
                                label = mode.name.lowercase().replaceFirstChar { it.uppercase() },
                                selected = settings.themeMode == mode,
                                onClick = { onThemeChange(mode) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(Modifier.height(28.dp))

                    // Text size section
                    SectionLabel("Text Size")
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextSize.entries.forEach { size ->
                            val label = when (size) {
                                TextSize.SMALL -> "A"
                                TextSize.MEDIUM -> "A"
                                TextSize.LARGE -> "A"
                            }
                            SettingsChip(
                                label = label,
                                selected = settings.textSize == size,
                                onClick = { onTextSizeChange(size) },
                                labelSize = when (size) {
                                    TextSize.SMALL -> 13.sp
                                    TextSize.MEDIUM -> 16.sp
                                    TextSize.LARGE -> 20.sp
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(
            letterSpacing = 1.5.sp,
            fontWeight = FontWeight.SemiBold
        ),
        color = GoldAccent
    )
}

@Composable
private fun SettingsChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    labelSize: androidx.compose.ui.unit.TextUnit = 14.sp
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (selected) GoldAccent.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface,
        modifier = modifier,
        border = if (selected) androidx.compose.foundation.BorderStroke(1.5.dp, GoldAccent) else null
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 12.dp)) {
            Text(
                text = label,
                fontSize = labelSize,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
        }
    }
}
