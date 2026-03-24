package com.music.revive.data.lyric

import com.music.revive.domain.model.Lyric
import com.music.revive.domain.model.LyricSource
import com.music.revive.domain.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Online lyrics service that fetches lyrics from multiple sources
 * 
 * Supported APIs:
 * - NetEase Cloud Music API (for Chinese songs)
 * - LRCLIB (free, no API key required)
 */
@Singleton
class OnlineLyricService @Inject constructor() {
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(10_000, java.util.concurrent.TimeUnit.MILLISECONDS)
        .readTimeout(10_000, java.util.concurrent.TimeUnit.MILLISECONDS)
        .build()
    
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }
    
    /**
     * Search and fetch lyrics for a song
     */
    suspend fun fetchLyrics(song: Song): Lyric? = withContext(Dispatchers.IO) {
        // Try LRCLIB first (free, no API key required)
        fetchFromLrcLib(song)
            ?: fetchFromNetEase(song)
    }
    
    /**
     * Fetch lyrics from LRCLIB API
     * https://lrclib.net/docs
     */
    private suspend fun fetchFromLrcLib(song: Song): Lyric? = withContext(Dispatchers.IO) {
        try {
            val searchUrl = buildString {
                append("https://lrclib.net/api/search")
                append("?track_name=").append(java.net.URLEncoder.encode(song.title, "UTF-8"))
                if (song.artist.isNotBlank() && song.artist != "Unknown Artist") {
                    append("&artist_name=").append(java.net.URLEncoder.encode(song.artist, "UTF-8"))
                }
            }
            
            val searchRequest = Request.Builder()
                .url(searchUrl)
                .header("User-Agent", "Revive Music Player")
                .build()
            
            val searchResponse = client.newCall(searchRequest).execute()
            if (!searchResponse.isSuccessful) return@withContext null
            
            val searchBody = searchResponse.body?.string() ?: return@withContext null
            val searchResults = json.decodeFromString<List<LrcLibSearchResult>>(searchBody)
            
            // Find best match
            val bestMatch = searchResults.firstOrNull {
                it.syncedLyrics != null || it.plainLyrics != null
            } ?: return@withContext null
            
            // Prefer synced lyrics
            val lyrics = bestMatch.syncedLyrics ?: bestMatch.plainLyrics
            if (!lyrics.isNullOrBlank()) {
                return@withContext if (LrcParser.isLrcFormat(lyrics)) {
                    LrcParser.parse(lyrics, song.id, LyricSource.ONLINE)
                } else {
                    // Plain text lyrics
                    Lyric(
                        songId = song.id,
                        lines = lyrics.lines()
                            .filter { it.isNotBlank() }
                            .mapIndexed { _, line -> 
                                com.music.revive.domain.model.LyricLine(
                                    timeMs = 0, 
                                    text = line.trim()
                                ) 
                            },
                        source = LyricSource.ONLINE
                    )
                }
            }
            
            null
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Fetch lyrics from NetEase Cloud Music API
     * Using a public API endpoint
     */
    private suspend fun fetchFromNetEase(song: Song): Lyric? = withContext(Dispatchers.IO) {
        try {
            // Search for song
            val searchUrl = buildString {
                append("https://music.163.com/api/search/get")
                append("?s=").append(java.net.URLEncoder.encode("${song.title} ${song.artist}", "UTF-8"))
                append("&type=1")
                append("&limit=5")
            }
            
            val searchRequest = Request.Builder()
                .url(searchUrl)
                .header("User-Agent", "Mozilla/5.0")
                .header("Referer", "https://music.163.com")
                .build()
            
            val searchResponse = client.newCall(searchRequest).execute()
            if (!searchResponse.isSuccessful) return@withContext null
            
            val searchBody = searchResponse.body?.string() ?: return@withContext null
            val searchResult = json.decodeFromString<NetEaseSearchResult>(searchBody)
            
            val songId = searchResult.result?.songs?.firstOrNull()?.id ?: return@withContext null
            
            // Get lyrics
            val lyricUrl = "https://music.163.com/api/song/lyric?id=$songId&lv=1"
            
            val lyricRequest = Request.Builder()
                .url(lyricUrl)
                .header("User-Agent", "Mozilla/5.0")
                .header("Referer", "https://music.163.com")
                .build()
            
            val lyricResponse = client.newCall(lyricRequest).execute()
            if (!lyricResponse.isSuccessful) return@withContext null
            
            val lyricBody = lyricResponse.body?.string() ?: return@withContext null
            val lyricResult = json.decodeFromString<NetEaseLyricResult>(lyricBody)
            
            val lrcContent = lyricResult.lrc?.lyric
            val translation = lyricResult.tlyric?.lyric
            
            if (!lrcContent.isNullOrBlank()) {
                val lyric = LrcParser.parse(lrcContent, song.id, LyricSource.ONLINE)
                
                // Merge translation if available
                if (!translation.isNullOrBlank() && lyric.isSynced) {
                    val transLines = parseTranslation(translation)
                    val mergedLines = lyric.lines.map { line ->
                        line.copy(translation = transLines[line.timeMs])
                    }
                    lyric.copy(lines = mergedLines, hasTranslation = true)
                } else {
                    lyric
                }
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Parse translation LRC content
     */
    private fun parseTranslation(lrcContent: String): Map<Long, String> {
        val map = mutableMapOf<Long, String>()
        val timeTagRegex = Regex("""\[(\d{1,2}):(\d{1,2})[.:](\d{1,3})\](.*)""")
        
        lrcContent.lines().forEach { line ->
            timeTagRegex.find(line)?.let { match ->
                val (minutes, seconds, milliseconds) = match.destructured
                val timeMs = minutes.toLong() * 60 * 1000 + 
                             seconds.toLong() * 1000 + 
                             (milliseconds.toLongOrNull() ?: 0L) * 
                             if (milliseconds.length == 2) 10 else 1
                val text = match.groupValues[4].trim()
                if (text.isNotEmpty()) {
                    map[timeMs] = text
                }
            }
        }
        
        return map
    }
    
    // LRCLIB API response models
    @Serializable
    data class LrcLibSearchResult(
        val id: Long? = null,
        @SerialName("trackName")
        val trackName: String? = null,
        @SerialName("artistName")
        val artistName: String? = null,
        @SerialName("plainLyrics")
        val plainLyrics: String? = null,
        @SerialName("syncedLyrics")
        val syncedLyrics: String? = null
    )
    
    // NetEase Cloud Music API response models
    @Serializable
    data class NetEaseSearchResult(
        val result: NetEaseSearchResultData? = null
    )
    
    @Serializable
    data class NetEaseSearchResultData(
        val songs: List<NetEaseSong>? = null
    )
    
    @Serializable
    data class NetEaseSong(
        val id: Long,
        val name: String? = null
    )
    
    @Serializable
    data class NetEaseLyricResult(
        val lrc: NetEaseLyricContent? = null,
        val tlyric: NetEaseLyricContent? = null
    )
    
    @Serializable
    data class NetEaseLyricContent(
        val lyric: String? = null
    )
}
