package com.music.revive.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.music.revive.presentation.theme.ThemeMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(name = "theme_preferences")

/**
 * Color source for the app theme
 */
enum class ColorSource {
    STATIC,         // Static Material 3 colors (default palette)
    DYNAMIC,        // Material You dynamic colors from wallpaper (Android 12+)
    ALBUM           // Extract colors from current album art
}

/**
 * Preferences for theme settings
 */
@Singleton
class ThemePreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val THEME_MODE = intPreferencesKey("theme_mode") // 0: System, 1: Light, 2: Dark
        private val COLOR_SOURCE = intPreferencesKey("color_source") // 0: Static, 1: Dynamic, 2: Album
        // Legacy key for migration
        private val DYNAMIC_COLORS = booleanPreferencesKey("dynamic_colors")
    }
    
    // Theme mode (System, Light, Dark)
    val themeMode: Flow<ThemeMode> = context.themeDataStore.data
        .map { preferences ->
            when (preferences[THEME_MODE] ?: 0) {
                1 -> ThemeMode.LIGHT
                2 -> ThemeMode.DARK
                else -> ThemeMode.SYSTEM
            }
        }
    
    // Color source
    val colorSource: Flow<ColorSource> = context.themeDataStore.data
        .map { preferences ->
            // Check new key first
            when (preferences[COLOR_SOURCE]) {
                0 -> ColorSource.STATIC
                1 -> ColorSource.DYNAMIC
                2 -> ColorSource.ALBUM
                else -> {
                    // Migration: check legacy dynamic_colors key
                    if (preferences[DYNAMIC_COLORS] == true) {
                        ColorSource.DYNAMIC
                    } else {
                        ColorSource.DYNAMIC // Default to dynamic
                    }
                }
            }
        }
    
    // Legacy support for dynamic colors enabled check
    val dynamicColorsEnabled: Flow<Boolean> = colorSource.map { it == ColorSource.DYNAMIC }
    
    // Check if album colors are enabled
    val albumColorsEnabled: Flow<Boolean> = colorSource.map { it == ColorSource.ALBUM }
    
    suspend fun setThemeMode(mode: ThemeMode) {
        context.themeDataStore.edit { preferences ->
            preferences[THEME_MODE] = when (mode) {
                ThemeMode.LIGHT -> 1
                ThemeMode.DARK -> 2
                ThemeMode.SYSTEM -> 0
            }
        }
    }
    
    suspend fun setColorSource(source: ColorSource) {
        context.themeDataStore.edit { preferences ->
            preferences[COLOR_SOURCE] = when (source) {
                ColorSource.STATIC -> 0
                ColorSource.DYNAMIC -> 1
                ColorSource.ALBUM -> 2
            }
        }
    }
    
    // Legacy support
    suspend fun setDynamicColorsEnabled(enabled: Boolean) {
        setColorSource(if (enabled) ColorSource.DYNAMIC else ColorSource.STATIC)
    }
}
