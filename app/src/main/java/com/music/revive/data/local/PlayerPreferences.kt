package com.music.revive.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.playerDataStore: DataStore<Preferences> by preferencesDataStore(name = "player_preferences")

/**
 * Preferences for player settings
 */
@Singleton
class PlayerPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val FADE_IN_OUT = booleanPreferencesKey("fade_in_out")
        private val GAPLESS_PLAYBACK = booleanPreferencesKey("gapless_playback")
        private val VOLUME_NORMALIZATION = booleanPreferencesKey("volume_normalization")
        private val PLAYBACK_SPEED = floatPreferencesKey("playback_speed")
        private val SKIP_SILENCE = booleanPreferencesKey("skip_silence")
        private val ENABLE_HAPTIC_FEEDBACK = booleanPreferencesKey("enable_haptic_feedback")
    }
    
    // Fade in/out effect
    val fadeInOut: Flow<Boolean> = context.playerDataStore.data
        .map { preferences -> preferences[FADE_IN_OUT] ?: false }
    
    // Gapless playback
    val gaplessPlayback: Flow<Boolean> = context.playerDataStore.data
        .map { preferences -> preferences[GAPLESS_PLAYBACK] ?: true }
    
    // Volume normalization
    val volumeNormalization: Flow<Boolean> = context.playerDataStore.data
        .map { preferences -> preferences[VOLUME_NORMALIZATION] ?: false }
    
    // Playback speed (0.5 to 2.0)
    val playbackSpeed: Flow<Float> = context.playerDataStore.data
        .map { preferences -> preferences[PLAYBACK_SPEED] ?: 1.0f }
    
    // Skip silence
    val skipSilence: Flow<Boolean> = context.playerDataStore.data
        .map { preferences -> preferences[SKIP_SILENCE] ?: false }
    
    // Haptic feedback
    val enableHapticFeedback: Flow<Boolean> = context.playerDataStore.data
        .map { preferences -> preferences[ENABLE_HAPTIC_FEEDBACK] ?: true }
    
    suspend fun setFadeInOut(enabled: Boolean) {
        context.playerDataStore.edit { preferences ->
            preferences[FADE_IN_OUT] = enabled
        }
    }
    
    suspend fun setGaplessPlayback(enabled: Boolean) {
        context.playerDataStore.edit { preferences ->
            preferences[GAPLESS_PLAYBACK] = enabled
        }
    }
    
    suspend fun setVolumeNormalization(enabled: Boolean) {
        context.playerDataStore.edit { preferences ->
            preferences[VOLUME_NORMALIZATION] = enabled
        }
    }
    
    suspend fun setPlaybackSpeed(speed: Float) {
        context.playerDataStore.edit { preferences ->
            preferences[PLAYBACK_SPEED] = speed.coerceIn(0.5f, 2.0f)
        }
    }
    
    suspend fun setSkipSilence(enabled: Boolean) {
        context.playerDataStore.edit { preferences ->
            preferences[SKIP_SILENCE] = enabled
        }
    }
    
    suspend fun setEnableHapticFeedback(enabled: Boolean) {
        context.playerDataStore.edit { preferences ->
            preferences[ENABLE_HAPTIC_FEEDBACK] = enabled
        }
    }
}
