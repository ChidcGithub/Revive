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
        private val LYRICS_DISPLAY_STYLE = intPreferencesKey("lyrics_display_style")
        private val ENABLE_GLOW = booleanPreferencesKey("enable_glow")
        private val ENABLE_KARAOKE = booleanPreferencesKey("enable_karaoke")
        private val ENABLE_BLUR = booleanPreferencesKey("enable_blur")
        private val ENABLE_FULL_SCREEN_LYRICS = booleanPreferencesKey("enable_full_screen_lyrics")
        private val ENABLE_BALANCED_LINES = booleanPreferencesKey("enable_balanced_lines")
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
    
    // Enable glow effect on active lyric line
    val enableGlow: Flow<Boolean> = context.lyricsDataStore.data
        .map { preferences -> preferences[ENABLE_GLOW] ?: true }
    
    // Enable karaoke gradient sweep effect
    val enableKaraoke: Flow<Boolean> = context.lyricsDataStore.data
        .map { preferences -> preferences[ENABLE_KARAOKE] ?: true }
    
    // Enable blur effect on inactive lines
    val enableBlur: Flow<Boolean> = context.lyricsDataStore.data
        .map { preferences -> preferences[ENABLE_BLUR] ?: false }
    
    // Enable full screen lyrics button
    val enableFullScreenLyrics: Flow<Boolean> = context.lyricsDataStore.data
        .map { preferences -> preferences[ENABLE_FULL_SCREEN_LYRICS] ?: true }
    
    // Enable balanced lyrics lines mode
    val enableBalancedLines: Flow<Boolean> = context.lyricsDataStore.data
        .map { preferences -> preferences[ENABLE_BALANCED_LINES] ?: false }
    
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
    
    suspend fun setEnableGlow(enabled: Boolean) {
        context.lyricsDataStore.edit { preferences ->
            preferences[ENABLE_GLOW] = enabled
        }
    }
    
    suspend fun setEnableKaraoke(enabled: Boolean) {
        context.lyricsDataStore.edit { preferences ->
            preferences[ENABLE_KARAOKE] = enabled
        }
    }
    
    suspend fun setEnableBlur(enabled: Boolean) {
        context.lyricsDataStore.edit { preferences ->
            preferences[ENABLE_BLUR] = enabled
        }
    }
    
    suspend fun setEnableFullScreenLyrics(enabled: Boolean) {
        context.lyricsDataStore.edit { preferences ->
            preferences[ENABLE_FULL_SCREEN_LYRICS] = enabled
        }
    }
    
    suspend fun setEnableBalancedLines(enabled: Boolean) {
        context.lyricsDataStore.edit { preferences ->
            preferences[ENABLE_BALANCED_LINES] = enabled
        }
    }
}