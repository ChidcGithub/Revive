package com.music.revive.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private val Context.historyDataStore: DataStore<Preferences> by preferencesDataStore(name = "history_preferences")

/**
 * Preferences and storage for listening history
 */
@Singleton
class HistoryPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val HISTORY_LIST = stringPreferencesKey("history_list")
        private val MAX_HISTORY_SIZE = intPreferencesKey("max_history_size")
        private val LAST_PLAYED_TIME = longPreferencesKey("last_played_time")
    }
    
    private val json = Json { ignoreUnknownKeys = true }
    
    // Get all history items as a Flow
    val historyItems: Flow<List<HistoryItem>> = context.historyDataStore.data
        .map { preferences ->
            val jsonString = preferences[HISTORY_LIST] ?: ""
            if (jsonString.isEmpty()) {
                emptyList()
            } else {
                try {
                    json.decodeFromString(jsonString)
                } catch (e: Exception) {
                    emptyList()
                }
            }
        }
    
    // Get max history size (default 100)
    val maxHistorySize: Flow<Int> = context.historyDataStore.data
        .map { preferences -> preferences[MAX_HISTORY_SIZE] ?: 100 }
    
    // Get last played time
    val lastPlayedTime: Flow<Long> = context.historyDataStore.data
        .map { preferences -> preferences[LAST_PLAYED_TIME] ?: 0L }
    
    /**
     * Add a song to listening history
     */
    suspend fun addToHistory(songId: Long, title: String, artist: String, album: String, albumArtUri: String?) {
        val currentHistory = historyItems.value.toMutableList()
        
        // Remove if already exists (to update position)
        currentHistory.removeAll { it.songId == songId }
        
        // Add to front
        val newItem = HistoryItem(
            songId = songId,
            title = title,
            artist = artist,
            album = album,
            albumArtUri = albumArtUri,
            playCount = 1,
            firstPlayedAt = System.currentTimeMillis(),
            lastPlayedAt = System.currentTimeMillis()
        )
        currentHistory.add(0, newItem)
        
        // Limit size
        val maxSize = maxHistorySize.value
        while (currentHistory.size > maxSize) {
            currentHistory.removeAt(currentHistory.size - 1)
        }
        
        // Save
        context.historyDataStore.edit { preferences ->
            preferences[HISTORY_LIST] = json.encodeToString(currentHistory)
            preferences[LAST_PLAYED_TIME] = System.currentTimeMillis()
        }
    }
    
    /**
     * Update play count for existing song
     */
    suspend fun updatePlayCount(songId: Long) {
        val currentHistory = historyItems.value.toMutableList()
        val index = currentHistory.indexOfFirst { it.songId == songId }
        
        if (index >= 0) {
            val item = currentHistory[index]
            currentHistory[index] = item.copy(
                playCount = item.playCount + 1,
                lastPlayedAt = System.currentTimeMillis()
            )
            
            context.historyDataStore.edit { preferences ->
                preferences[HISTORY_LIST] = json.encodeToString(currentHistory)
                preferences[LAST_PLAYED_TIME] = System.currentTimeMillis()
            }
        }
    }
    
    /**
     * Set max history size
     */
    suspend fun setMaxHistorySize(size: Int) {
        context.historyDataStore.edit { preferences ->
            preferences[MAX_HISTORY_SIZE] = size.coerceIn(10, 500)
        }
        
        // Trim history if needed
        val currentHistory = historyItems.value
        if (currentHistory.size > size) {
            val trimmed = currentHistory.take(size)
            context.historyDataStore.edit { preferences ->
                preferences[HISTORY_LIST] = json.encodeToString(trimmed)
            }
        }
    }
    
    /**
     * Clear all history
     */
    suspend fun clearAllHistory() {
        context.historyDataStore.edit { preferences ->
            preferences.remove(HISTORY_LIST)
            preferences.remove(LAST_PLAYED_TIME)
        }
    }
    
    /**
     * Remove specific item from history
     */
    suspend fun removeFromHistory(songId: Long) {
        val currentHistory = historyItems.value.toMutableList()
        currentHistory.removeAll { it.songId == songId }
        
        context.historyDataStore.edit { preferences ->
            preferences[HISTORY_LIST] = json.encodeToString(currentHistory)
        }
    }
    
    /**
     * Get recent history (last N items)
     */
    suspend fun getRecentHistory(limit: Int = 10): List<HistoryItem> {
        return historyItems.value.take(limit)
    }
}

/**
 * Represents a single listening history item
 */
@Serializable
data class HistoryItem(
    val songId: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumArtUri: String?,
    val playCount: Int = 1,
    val firstPlayedAt: Long = System.currentTimeMillis(),
    val lastPlayedAt: Long = System.currentTimeMillis()
) {
    /**
     * Get formatted play count display
     */
    fun getPlayCountDisplay(): String {
        return when {
            playCount == 1 -> "播放 1 次"
            playCount < 100 -> "播放 $playCount 次"
            else -> String.format("%.1fk", playCount / 1000f)
        }
    }
    
    /**
     * Get relative time string for last played
     */
    fun getLastPlayedRelativeTime(): String {
        val now = System.currentTimeMillis()
        val diff = now - lastPlayedAt
        
        return when {
            diff < 60_000 -> "刚刚"
            diff < 3600_000 -> "${diff / 60_000}分钟前"
            diff < 86400_000 -> "${diff / 3600_000}小时前"
            diff < 604800_000 -> "${diff / 86400_000}天前"
            else -> "${java.text.SimpleDateFormat("MM-dd", java.util.Locale.getDefault()).format(java.util.Date(lastPlayedAt))}"
        }
    }
}
