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
 * Preferences for theme settings
 */
@Singleton
class ThemePreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val THEME_MODE = intPreferencesKey("theme_mode") // 0: System, 1: Light, 2: Dark
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
    
    // Dynamic colors enabled
    val dynamicColorsEnabled: Flow<Boolean> = context.themeDataStore.data
        .map { preferences -> preferences[DYNAMIC_COLORS] ?: true }
    
    suspend fun setThemeMode(mode: ThemeMode) {
        context.themeDataStore.edit { preferences ->
            preferences[THEME_MODE] = when (mode) {
                ThemeMode.LIGHT -> 1
                ThemeMode.DARK -> 2
                ThemeMode.SYSTEM -> 0
            }
        }
    }
    
    suspend fun setDynamicColorsEnabled(enabled: Boolean) {
        context.themeDataStore.edit { preferences ->
            preferences[DYNAMIC_COLORS] = enabled
        }
    }
}
