package com.music.revive.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.lyricsDataStore: DataStore<Preferences> by preferencesDataStore(name = "lyrics_preferences")

/**
 * Preferences for lyrics display settings
 */
@Singleton
class LyricsPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val LYRICS_FONT_SIZE = floatPreferencesKey("lyrics_font_size")
        private val SHOW_TRANSLATION = booleanPreferencesKey("show_translation")
        private val AUTO_SCROLL = booleanPreferencesKey("auto_scroll")
        private val LYRICS_DISPLAY_STYLE = intPreferencesKey("lyrics_display_style") // 0: centered, 1: left-aligned
    }
    
    // Font size multiplier (0.8 to 1.5)
    val lyricsFontSize: Flow<Float> = context.lyricsDataStore.data
        .map { preferences -> preferences[LYRICS_FONT_SIZE] ?: 1.0f }
    
    // Show translation lyrics
    val showTranslation: Flow<Boolean> = context.lyricsDataStore.data
        .map { preferences -> preferences[SHOW_TRANSLATION] ?: true }
    
    // Auto scroll to current line
    val autoScroll: Flow<Boolean> = context.lyricsDataStore.data
        .map { preferences -> preferences[AUTO_SCROLL] ?: true }
    
    // Display style (0: centered, 1: left-aligned)
    val lyricsDisplayStyle: Flow<Int> = context.lyricsDataStore.data
        .map { preferences -> preferences[LYRICS_DISPLAY_STYLE] ?: 0 }
    
    suspend fun setLyricsFontSize(size: Float) {
        context.lyricsDataStore.edit { preferences ->
            preferences[LYRICS_FONT_SIZE] = size.coerceIn(0.8f, 1.5f)
        }
    }
    
    suspend fun setShowTranslation(show: Boolean) {
        context.lyricsDataStore.edit { preferences ->
            preferences[SHOW_TRANSLATION] = show
        }
    }
    
    suspend fun setAutoScroll(enabled: Boolean) {
        context.lyricsDataStore.edit { preferences ->
            preferences[AUTO_SCROLL] = enabled
        }
    }
    
    suspend fun setLyricsDisplayStyle(style: Int) {
        context.lyricsDataStore.edit { preferences ->
            preferences[LYRICS_DISPLAY_STYLE] = style
        }
    }
}