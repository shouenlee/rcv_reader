package com.rcvreader.ui.settings

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("rcv_reader_settings", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<UserSettings> = _settings.asStateFlow()

    private fun loadSettings(): UserSettings {
        val themeName = prefs.getString("theme_mode", ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name
        val sizeName = prefs.getString("text_size", TextSize.MEDIUM.name) ?: TextSize.MEDIUM.name
        return UserSettings(
            themeMode = ThemeMode.valueOf(themeName),
            textSize = TextSize.valueOf(sizeName)
        )
    }

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString("theme_mode", mode.name).apply()
        _settings.update { it.copy(themeMode = mode) }
    }

    fun setTextSize(size: TextSize) {
        prefs.edit().putString("text_size", size.name).apply()
        _settings.update { it.copy(textSize = size) }
    }
}
