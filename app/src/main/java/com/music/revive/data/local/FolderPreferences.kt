package com.music.revive.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "folder_preferences")

@Singleton
class FolderPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val EXCLUDED_FOLDERS_KEY = stringSetPreferencesKey("excluded_folders")
    }

    /**
     * Get the set of excluded folder paths
     */
    val excludedFolders: Flow<Set<String>>
        get() = context.dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(androidx.datastore.preferences.core.emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                preferences[EXCLUDED_FOLDERS_KEY] ?: emptySet()
            }

    /**
     * Add a folder to the exclusion list
     */
    suspend fun excludeFolder(folderPath: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[EXCLUDED_FOLDERS_KEY] ?: emptySet()
            preferences[EXCLUDED_FOLDERS_KEY] = current + folderPath
        }
    }

    /**
     * Remove a folder from the exclusion list (include it again)
     */
    suspend fun includeFolder(folderPath: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[EXCLUDED_FOLDERS_KEY] ?: emptySet()
            preferences[EXCLUDED_FOLDERS_KEY] = current - folderPath
        }
    }

    /**
     * Set the entire exclusion list
     */
    suspend fun setExcludedFolders(folders: Set<String>) {
        context.dataStore.edit { preferences ->
            preferences[EXCLUDED_FOLDERS_KEY] = folders
        }
    }

    /**
     * Check if a folder is excluded
     */
    fun isFolderExcluded(folderPath: String): Flow<Boolean> = excludedFolders
        .map { excluded -> excluded.contains(folderPath) }
}
