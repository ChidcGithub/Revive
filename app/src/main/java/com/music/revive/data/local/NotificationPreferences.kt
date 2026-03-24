package com.music.revive.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.notificationDataStore: DataStore<Preferences> by preferencesDataStore(name = "notification_preferences")

/**
 * Preferences for notification and lock screen settings
 */
@Singleton
class NotificationPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val SHOW_NOTIFICATION = booleanPreferencesKey("show_notification")
        private val SHOW_ON_LOCK_SCREEN = booleanPreferencesKey("show_on_lock_screen")
        private val FLOATING_LYRICS = booleanPreferencesKey("floating_lyrics")
        private val NOTIFICATION_LYRICS = booleanPreferencesKey("notification_lyrics")
    }
    
    // Show notification
    val showNotification: Flow<Boolean> = context.notificationDataStore.data
        .map { preferences -> preferences[SHOW_NOTIFICATION] ?: true }
    
    // Show on lock screen
    val showOnLockScreen: Flow<Boolean> = context.notificationDataStore.data
        .map { preferences -> preferences[SHOW_ON_LOCK_SCREEN] ?: true }
    
    // Floating lyrics (requires overlay permission)
    val floatingLyrics: Flow<Boolean> = context.notificationDataStore.data
        .map { preferences -> preferences[FLOATING_LYRICS] ?: false }
    
    // Show lyrics in notification
    val notificationLyrics: Flow<Boolean> = context.notificationDataStore.data
        .map { preferences -> preferences[NOTIFICATION_LYRICS] ?: false }
    
    suspend fun setShowNotification(show: Boolean) {
        context.notificationDataStore.edit { preferences ->
            preferences[SHOW_NOTIFICATION] = show
        }
    }
    
    suspend fun setShowOnLockScreen(show: Boolean) {
        context.notificationDataStore.edit { preferences ->
            preferences[SHOW_ON_LOCK_SCREEN] = show
        }
    }
    
    suspend fun setFloatingLyrics(enabled: Boolean) {
        context.notificationDataStore.edit { preferences ->
            preferences[FLOATING_LYRICS] = enabled
        }
    }
    
    suspend fun setNotificationLyrics(enabled: Boolean) {
        context.notificationDataStore.edit { preferences ->
            preferences[NOTIFICATION_LYRICS] = enabled
        }
    }
}
