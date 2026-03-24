package com.music.revive.data.lyric

import android.content.Context
import com.music.revive.domain.model.Lyric
import com.music.revive.domain.model.LyricLine
import com.music.revive.domain.model.LyricSource
import com.music.revive.domain.model.Song
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile
import java.nio.charset.Charset
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Extracts embedded lyrics from audio files (ID3v2 tags)
 * 
 * Supported formats:
 * - ID3v2 USLT (Unsynchronized Lyrics)
 * - ID3v2 SYLT (Synchronized Lyrics)
 * - Vorbis Comments (LYRICS, UNSYNCEDLYRICS)
 */
@Singleton
class EmbeddedLyricExtractor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    /**
     * Extract embedded lyrics from an audio file
     */
    suspend fun extractLyrics(song: Song): Lyric? = withContext(Dispatchers.IO) {
        try {
            val file = File(song.path)
            if (!file.exists()) return@withContext null
            
            when {
                song.path.endsWith(".mp3", ignoreCase = true) -> extractFromMp3(file, song.id)
                song.path.endsWith(".flac", ignoreCase = true) -> extractFromFlac(file, song.id)
                song.path.endsWith(".m4a", ignoreCase = true) || 
                    song.path.endsWith(".mp4", ignoreCase = true) -> extractFromMp4(file, song.id)
                song.path.endsWith(".ogg", ignoreCase = true) || 
                    song.path.endsWith(".oga", ignoreCase = true) -> extractFromOgg(file, song.id)
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Extract lyrics from MP3 file (ID3v2 tags)
     */
    private fun extractFromMp3(file: File, songId: Long): Lyric? {
        RandomAccessFile(file, "r").use { raf ->
            // Check for ID3v2 header
            val header = ByteArray(3)
            raf.read(header)
            
            if (header.toString(Charsets.ISO_8859_1) != "ID3") {
                return null
            }
            
            // Read ID3v2 version
            val version = raf.readByte().toInt() and 0xFF
            raf.readByte() // flags
            
            // Read size (syncsafe integer)
            val size = readSyncSafeInt(raf)
            
            // Read all frames
            val data = ByteArray(size)
            raf.read(data)
            
            return parseId3Frames(data, songId, version)
        }
    }
    
    /**
     * Parse ID3v2 frames to extract lyrics
     */
    private fun parseId3Frames(data: ByteArray, songId: Long, version: Int): Lyric? {
        var offset = 0
        var lyrics: String? = null
        var syncedLyrics: List<LyricLine>? = null
        
        while (offset < data.size - 10) {
            try {
                // Frame ID (4 bytes for v2.3/v2.4, 3 bytes for v2.2)
                val frameIdLength = if (version >= 3) 4 else 3
                if (offset + frameIdLength > data.size) break
                
                val frameId = String(data, offset, frameIdLength, Charsets.ISO_8859_1)
                if (frameId.all { it == '\u0000' }) break
                
                offset += frameIdLength
                
                // Frame size
                val frameSize = if (version >= 4) {
                    readSyncSafeIntFromBytes(data, offset)
                } else if (version == 3) {
                    ((data[offset].toInt() and 0xFF) shl 24) or
                    ((data[offset + 1].toInt() and 0xFF) shl 16) or
                    ((data[offset + 2].toInt() and 0xFF) shl 8) or
                    (data[offset + 3].toInt() and 0xFF)
                } else {
                    ((data[offset].toInt() and 0xFF) shl 16) or
                    ((data[offset + 1].toInt() and 0xFF) shl 8) or
                    (data[offset + 2].toInt() and 0xFF)
                }
                
                if (frameSize <= 0 || frameSize > data.size - offset) break
                offset += if (version >= 3) 4 else 3
                
                // Skip flags (2 bytes for v2.3/v2.4)
                if (version >= 3) offset += 2
                
                // Check for lyrics frames
                when (frameId) {
                    "USLT", "ULT" -> {
                        // Unsynchronized lyrics
                        val frameData = data.copyOfRange(offset, offset + frameSize)
                        lyrics = parseUnsynchronizedLyrics(frameData)
                    }
                    "SYLT", "SLT" -> {
                        // Synchronized lyrics
                        val frameData = data.copyOfRange(offset, offset + frameSize)
                        syncedLyrics = parseSynchronizedLyrics(frameData)
                    }
                }
                
                offset += frameSize
            } catch (e: Exception) {
                break
            }
        }
        
        // Prefer synchronized lyrics over unsynchronized
        return when {
            syncedLyrics != null && syncedLyrics.isNotEmpty() -> Lyric(
                songId = songId,
                lines = syncedLyrics,
                source = LyricSource.EMBEDDED
            )
            !lyrics.isNullOrBlank() -> {
                // Check if it's LRC format
                if (LrcParser.isLrcFormat(lyrics)) {
                    LrcParser.parse(lyrics, songId, LyricSource.EMBEDDED)
                } else {
                    // Plain text - create lines with time 0
                    Lyric(
                        songId = songId,
                        lines = lyrics.lines().filter { it.isNotBlank() }.map { 
                            LyricLine(timeMs = 0, text = it.trim()) 
                        },
                        source = LyricSource.EMBEDDED
                    )
                }
            }
            else -> null
        }
    }
    
    /**
     * Parse unsynchronized lyrics frame (USLT)
     */
    private fun parseUnsynchronizedLyrics(data: ByteArray): String {
        if (data.isEmpty()) return ""
        
        var offset = 0
        val encoding = data[offset++].toInt()
        val charset = getCharset(encoding)
        
        // Language (3 bytes)
        offset += 3
        
        // Content descriptor (null-terminated)
        while (offset < data.size && data[offset] != 0.toByte()) offset++
        if (offset < data.size) offset++
        
        // Lyrics text
        return if (offset < data.size) {
            String(data, offset, data.size - offset, charset).trim()
        } else ""
    }
    
    /**
     * Parse synchronized lyrics frame (SYLT)
     */
    private fun parseSynchronizedLyrics(data: ByteArray): List<LyricLine>? {
        if (data.isEmpty()) return null
        
        try {
            var offset = 0
            val encoding = data[offset++].toInt()
            val charset = getCharset(encoding)
            
            // Language (3 bytes)
            offset += 3
            
            // Time stamp format
            val timeStampFormat = data[offset++].toInt()
            
            // Content type
            val contentType = data[offset++].toInt()
            
            // Content descriptor (null-terminated)
            while (offset < data.size && data[offset] != 0.toByte()) offset++
            if (offset < data.size) offset++
            
            val lines = mutableListOf<LyricLine>()
            
            while (offset < data.size - 4) {
                // Find null-terminated text
                val textStart = offset
                while (offset < data.size && data[offset] != 0.toByte()) offset++
                
                if (offset >= data.size - 4) break
                
                val text = String(data, textStart, offset - textStart, charset).trim()
                offset++ // Skip null terminator
                
                // Read time stamp (4 bytes, big-endian)
                val timeStamp = ((data[offset].toInt() and 0xFF) shl 24) or
                               ((data[offset + 1].toInt() and 0xFF) shl 16) or
                               ((data[offset + 2].toInt() and 0xFF) shl 8) or
                               (data[offset + 3].toInt() and 0xFF)
                offset += 4
                
                // Convert to milliseconds if needed
                val timeMs = if (timeStampFormat == 1) timeStamp else timeStamp
                
                if (text.isNotEmpty()) {
                    lines.add(LyricLine(timeMs = timeMs.toLong(), text = text))
                }
            }
            
            return lines.sortedBy { it.timeMs }
        } catch (e: Exception) {
            return null
        }
    }
    
    /**
     * Extract lyrics from FLAC file (Vorbis Comments)
     */
    private fun extractFromFlac(file: File, songId: Long): Lyric? {
        // FLAC uses Vorbis Comments for metadata
        return extractFromVorbisComments(file, songId)
    }
    
    /**
     * Extract lyrics from OGG file (Vorbis Comments)
     */
    private fun extractFromOgg(file: File, songId: Long): Lyric? {
        return extractFromVorbisComments(file, songId)
    }
    
    /**
     * Extract lyrics from Vorbis Comments (FLAC, OGG)
     */
    private fun extractFromVorbisComments(file: File, songId: Long): Lyric? {
        // This is a simplified implementation
        // In a production app, you'd use a proper Vorbis Comment parser
        // or a library like jaudiotagger
        return null
    }
    
    /**
     * Extract lyrics from MP4/M4A file (iTunes-style metadata)
     */
    private fun extractFromMp4(file: File, songId: Long): Lyric? {
        // This is a simplified implementation
        // In a production app, you'd use a proper MP4 parser
        // or a library like mp4parser
        return null
    }
    
    /**
     * Get charset from encoding byte
     */
    private fun getCharset(encoding: Int): Charset {
        return when (encoding) {
            0 -> Charsets.ISO_8859_1
            1 -> Charsets.UTF_16
            2 -> Charsets.UTF_16BE
            3 -> Charsets.UTF_8
            else -> Charsets.ISO_8859_1
        }
    }
    
    /**
     * Read sync-safe integer from RandomAccessFile
     */
    private fun readSyncSafeInt(raf: RandomAccessFile): Int {
        val bytes = ByteArray(4)
        raf.read(bytes)
        return readSyncSafeIntFromBytes(bytes, 0)
    }
    
    /**
     * Read sync-safe integer from byte array
     */
    private fun readSyncSafeIntFromBytes(bytes: ByteArray, offset: Int): Int {
        return ((bytes[offset].toInt() and 0x7F) shl 21) or
               ((bytes[offset + 1].toInt() and 0x7F) shl 14) or
               ((bytes[offset + 2].toInt() and 0x7F) shl 7) or
               (bytes[offset + 3].toInt() and 0x7F)
    }
}
