package com.music.revive.data.lyric

import android.content.Context
import android.util.Log
import com.music.revive.domain.model.Lyric
import com.music.revive.domain.model.Song
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.nio.charset.Charset
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
    companion object {
        private const val TAG = "LyricRepository"
    }
    
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
                Log.d(TAG, "Found embedded lyrics for: ${song.title}")
                cacheAndReturn(song.id, embeddedLyrics)
                return embeddedLyrics
            }
            
            // 2. Try local .lrc file
            val localLyrics = loadFromLocalFile(song)
            if (localLyrics != null && !localLyrics.isEmpty) {
                Log.d(TAG, "Found local file lyrics for: ${song.title}")
                cacheAndReturn(song.id, localLyrics)
                return localLyrics
            }
            
            // No lyrics found
            Log.d(TAG, "No lyrics found for: ${song.title}")
            _currentLyrics.value = Lyric.Empty
            return Lyric.Empty
        } catch (e: Exception) {
            Log.e(TAG, "Error loading lyrics for ${song.title}", e)
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
            val possibleNames = buildList {
                // Standard naming
                add(songFile.nameWithoutExtension + ".lrc")
                add(songFile.nameWithoutExtension + ".LRC")
                add(songFile.nameWithoutExtension + ".lrc.txt")
                add(songFile.nameWithoutExtension + ".LRC.TXT")
                
                // Title-based naming
                val safeTitle = sanitizeFileName(song.title)
                add(safeTitle + ".lrc")
                add(safeTitle + ".LRC")
                
                // Title + Artist naming
                val safeArtist = sanitizeFileName(song.artist)
                add("$safeTitle - $safeArtist.lrc")
                add("$safeArtist - $safeTitle.lrc")
                
                // Artist + Title naming
                add("$safeArtist - $safeTitle.lrc")
                
                // Extended formats
                add(songFile.nameWithoutExtension + ".srt")  // SRT subtitle
                add(songFile.nameWithoutExtension + ".SRT")
            }
            
            for (name in possibleNames) {
                val lrcFile = File(parentDir, name)
                if (lrcFile.exists()) {
                    val content = readFileWithEncodingDetection(lrcFile)
                    if (content.isNotBlank()) {
                        val source = if (name.endsWith(".srt", ignoreCase = true)) {
                            com.music.revive.domain.model.LyricSource.LOCAL_FILE
                        } else {
                            com.music.revive.domain.model.LyricSource.LOCAL_FILE
                        }
                        return@withContext LrcParser.parse(content, song.id, source)
                    }
                }
            }
            
            // Also check for lyrics in common subdirectories
            val lyricSubdirs = listOf("lyrics", "Lyrics", "LYRICS", "lrc", "LRC")
            for (subdir in lyricSubdirs) {
                val lyricsDir = File(parentDir, subdir)
                if (lyricsDir.exists() && lyricsDir.isDirectory) {
                    for (name in possibleNames.take(5)) { // Only check first few patterns
                        val lrcFile = File(lyricsDir, name)
                        if (lrcFile.exists()) {
                            val content = readFileWithEncodingDetection(lrcFile)
                            if (content.isNotBlank()) {
                                return@withContext LrcParser.parse(content, song.id, com.music.revive.domain.model.LyricSource.LOCAL_FILE)
                            }
                        }
                    }
                }
            }
            
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error loading local file lyrics", e)
            null
        }
    }
    
    /**
     * Read file content with automatic encoding detection
     * Supports UTF-8, UTF-16, GBK, GB2312, and other common encodings
     */
    private fun readFileWithEncodingDetection(file: File): String {
        if (!file.exists()) return ""
        
        val bytes = FileInputStream(file).use { it.readBytes() }
        if (bytes.isEmpty()) return ""
        
        // Check for BOM
        val charset = detectCharset(bytes)
        
        return try {
            String(bytes, charset).removeBom()
        } catch (e: Exception) {
            // Fallback to UTF-8
            try {
                String(bytes, Charsets.UTF_8)
            } catch (e2: Exception) {
                ""
            }
        }
    }
    
    /**
     * Detect character encoding from byte array
     */
    private fun detectCharset(bytes: ByteArray): Charset {
        if (bytes.size < 2) return Charsets.UTF_8
        
        // Check BOM (Byte Order Mark)
        when {
            bytes.size >= 3 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte() -> {
                return Charsets.UTF_8
            }
            bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte() -> {
                return Charsets.UTF_16LE
            }
            bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte() -> {
                return Charsets.UTF_16BE
            }
        }
        
        // Try to detect if it's valid UTF-8
        if (isValidUtf8(bytes)) {
            return Charsets.UTF_8
        }
        
        // Check for Chinese text - try GBK/GB2312
        if (containsChineseText(bytes)) {
            return try {
                val gbkString = String(bytes, charset("GBK"))
                if (gbkString.isValidText()) {
                    charset("GBK")
                } else {
                    Charsets.UTF_8
                }
            } catch (e: Exception) {
                Charsets.UTF_8
            }
        }
        
        // Default to UTF-8
        return Charsets.UTF_8
    }
    
    /**
     * Check if byte array is valid UTF-8
     */
    private fun isValidUtf8(bytes: ByteArray): Boolean {
        var i = 0
        while (i < bytes.size) {
            val b = bytes[i].toInt() and 0xFF
            
            when {
                b < 0x80 -> i++
                b and 0xE0 == 0xC0 -> {
                    if (i + 1 >= bytes.size) return false
                    if (bytes[i + 1].toInt() and 0xC0 != 0x80) return false
                    i += 2
                }
                b and 0xF0 == 0xE0 -> {
                    if (i + 2 >= bytes.size) return false
                    if (bytes[i + 1].toInt() and 0xC0 != 0x80) return false
                    if (bytes[i + 2].toInt() and 0xC0 != 0x80) return false
                    i += 3
                }
                b and 0xF8 == 0xF0 -> {
                    if (i + 3 >= bytes.size) return false
                    if (bytes[i + 1].toInt() and 0xC0 != 0x80) return false
                    if (bytes[i + 2].toInt() and 0xC0 != 0x80) return false
                    if (bytes[i + 3].toInt() and 0xC0 != 0x80) return false
                    i += 4
                }
                else -> return false
            }
        }
        return true
    }
    
    /**
     * Check if byte array likely contains Chinese text
     */
    private fun containsChineseText(bytes: ByteArray): Boolean {
        // Check for common Chinese character byte patterns in GBK
        var chineseScore = 0
        var i = 0
        while (i < bytes.size - 1) {
            val b1 = bytes[i].toInt() and 0xFF
            val b2 = bytes[i + 1].toInt() and 0xFF
            
            // GBK encoding range: first byte 0x81-0xFE, second byte 0x40-0xFE (except 0x7F)
            if (b1 in 0x81..0xFE && b2 in 0x40..0xFE && b2 != 0x7F) {
                chineseScore++
                i += 2
            } else {
                i++
            }
        }
        
        // If more than 5% of bytes look like Chinese, assume GBK
        return chineseScore > bytes.size / 20
    }
    
    /**
     * Sanitize filename by removing invalid characters
     */
    private fun sanitizeFileName(name: String): String {
        return name.replace(Regex("[\\\\/:*?\"<>|]"), "_")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
    
    /**
     * Check if string contains valid readable text
     */
    private fun String.isValidText(): Boolean {
        var printable = 0
        for (c in this) {
            if (c.code in 32..126 || c.code > 127 || c == '\n' || c == '\r' || c == '\t') {
                printable++
            }
        }
        return printable.toFloat() / this.length > 0.8f
    }
    
    /**
     * Remove BOM characters from string
     */
    private fun String.removeBom(): String {
        return this.removePrefix("\uFEFF")
            .removePrefix("\uFFFE")
            .trimStart('\u0000')
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
        
        val possibleNames = listOf(
            songFile.nameWithoutExtension + ".lrc",
            songFile.nameWithoutExtension + ".LRC",
            songFile.nameWithoutExtension + ".srt",
            songFile.nameWithoutExtension + ".SRT"
        )
        
        for (name in possibleNames) {
            if (File(parentDir, name).exists()) return true
        }
        
        return false
    }
    
    /**
     * Get cached lyrics for a song
     */
    fun getCachedLyrics(songId: Long): Lyric? = lyricsCache[songId]
}
