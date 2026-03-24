package com.music.revive.data.lyric

import android.content.Context
import com.music.revive.domain.model.Lyric
import com.music.revive.domain.model.Song
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for fetching lyrics from multiple sources
 * 
 * Priority order:
 * 1. Embedded lyrics (ID3 tags, Vorbis Comments, MP4 atoms)
 * 2. Local .lrc file (same directory as the song)
 */
@Singleton
class LyricRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val embeddedLyricExtractor: EmbeddedLyricExtractor
) {
    // Cache for loaded lyrics
    private val lyricsCache = mutableMapOf<Long, Lyric>()
    
    // Current lyrics state
    private val _currentLyrics = MutableStateFlow<Lyric>(Lyric.Empty)
    val currentLyrics: StateFlow<Lyric> = _currentLyrics.asStateFlow()
    
    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    /**
     * Load lyrics for a song from all available sources
     */
    suspend fun loadLyrics(song: Song): Lyric {
        // Check cache first
        lyricsCache[song.id]?.let { 
            _currentLyrics.value = it
            return it 
        }
        
        _isLoading.value = true
        
        try {
            // 1. Try embedded lyrics first
            val embeddedLyrics = embeddedLyricExtractor.extractLyrics(song)
            if (embeddedLyrics != null && !embeddedLyrics.isEmpty) {
                cacheAndReturn(song.id, embeddedLyrics)
                return embeddedLyrics
            }
            
            // 2. Try local .lrc file
            val localLyrics = loadFromLocalFile(song)
            if (localLyrics != null && !localLyrics.isEmpty) {
                cacheAndReturn(song.id, localLyrics)
                return localLyrics
            }
            
            // No lyrics found
            _currentLyrics.value = Lyric.Empty
            return Lyric.Empty
        } finally {
            _isLoading.value = false
        }
    }
    
    /**
     * Load lyrics from a local .lrc file
     */
    private suspend fun loadFromLocalFile(song: Song): Lyric? = withContext(Dispatchers.IO) {
        try {
            val songFile = File(song.path)
            val parentDir = songFile.parentFile ?: return@withContext null
            
            // Try different naming conventions for .lrc file
            val possibleNames = listOf(
                songFile.nameWithoutExtension + ".lrc",
                songFile.nameWithoutExtension + ".LRC",
                songFile.nameWithoutExtension + ".lrc.txt",
                song.title + ".lrc",
                song.title + " - " + song.artist + ".lrc"
            )
            
            for (name in possibleNames) {
                val lrcFile = File(parentDir, name)
                if (lrcFile.exists()) {
                    val content = lrcFile.readText()
                    return@withContext LrcParser.parse(content, song.id, com.music.revive.domain.model.LyricSource.LOCAL_FILE)
                }
            }
            
            null
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Cache lyrics and update state
     */
    private fun cacheAndReturn(songId: Long, lyrics: Lyric): Lyric {
        lyricsCache[songId] = lyrics
        _currentLyrics.value = lyrics
        return lyrics
    }
    
    /**
     * Clear cache for a specific song
     */
    fun clearCache(songId: Long) {
        lyricsCache.remove(songId)
        if (_currentLyrics.value.songId == songId) {
            _currentLyrics.value = Lyric.Empty
        }
    }
    
    /**
     * Clear all cache
     */
    fun clearAllCache() {
        lyricsCache.clear()
        _currentLyrics.value = Lyric.Empty
    }
    
    /**
     * Check if lyrics are available for a song (without loading)
     * Note: Only checks for local file, embedded requires loading
     */
    suspend fun hasLyrics(song: Song): Boolean {
        // Check cache
        if (lyricsCache.containsKey(song.id)) {
            return lyricsCache[song.id]?.isEmpty == false
        }
        
        // Check local file (quick check)
        val songFile = File(song.path)
        val parentDir = songFile.parentFile ?: return false
        val lrcFile = File(parentDir, songFile.nameWithoutExtension + ".lrc")
        if (lrcFile.exists()) return true
        
        // Would need to check embedded, but that requires loading
        // For now, just return false and let loadLyrics do the work
        return false
    }
    
    /**
     * Get cached lyrics for a song
     */
    fun getCachedLyrics(songId: Long): Lyric? = lyricsCache[songId]
}