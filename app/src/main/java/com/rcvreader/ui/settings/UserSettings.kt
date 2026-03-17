package com.rcvreader.ui.settings

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class TextSize(val sp: Float) {
    SMALL(15f),
    MEDIUM(17f),
    LARGE(20f)
}

data class UserSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val textSize: TextSize = TextSize.MEDIUM
)
