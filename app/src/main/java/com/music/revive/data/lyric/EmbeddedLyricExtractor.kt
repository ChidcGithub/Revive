package com.music.revive.data.lyric

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
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
 * Extracts embedded lyrics from audio files
 * 
 * Supported methods:
 * 1. MediaMetadataRetriever (Android native)
 * 2. ID3v2 tags (USLT, SYLT, TXXX with LYRICS)
 * 3. Vorbis Comments (LYRICS, UNSYNCEDLYRICS)
 * 4. MP4/M4A iTunes atoms (@lyr)
 */
@Singleton
class EmbeddedLyricExtractor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    companion object {
        private const val TAG = "EmbeddedLyricExtractor"
        
        // ID3v2 frame IDs for lyrics
        private val LYRIC_FRAME_IDS = setOf("USLT", "ULT", "SYLT", "SLT")
        private val LYRIC_TXXX_DESCS = setOf("LYRICS", "LYRIC", "lyrics", "lyric")
    }
    
    /**
     * Extract embedded lyrics from an audio file
     */
    suspend fun extractLyrics(song: Song): Lyric? = withContext(Dispatchers.IO) {
        try {
            val file = File(song.path)
            if (!file.exists()) return@withContext null
            
            // Method 1: Try MediaMetadataRetriever first (most reliable)
            extractWithMediaMetadataRetriever(song)?.let { return@withContext it }
            
            // Method 2: Manual parsing based on file extension
            when {
                song.path.endsWith(".mp3", ignoreCase = true) -> extractFromMp3(file, song.id)
                song.path.endsWith(".flac", ignoreCase = true) -> extractFromFlac(file, song.id)
                song.path.endsWith(".m4a", ignoreCase = true) || 
                    song.path.endsWith(".mp4", ignoreCase = true) ||
                    song.path.endsWith(".m4b", ignoreCase = true) ||
                    song.path.endsWith(".m4p", ignoreCase = true) -> extractFromMp4(file, song.id)
                song.path.endsWith(".ogg", ignoreCase = true) || 
                    song.path.endsWith(".oga", ignoreCase = true) -> extractFromOgg(file, song.id)
                song.path.endsWith(".wav", ignoreCase = true) -> extractFromWav(file, song.id)
                song.path.endsWith(".wma", ignoreCase = true) -> extractFromWma(file, song.id)
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Extract lyrics using Android's MediaMetadataRetriever
     * This is the most reliable method as it uses system codecs
     */
    private fun extractWithMediaMetadataRetriever(song: Song): Lyric? {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, Uri.parse(song.albumArtUri ?: Uri.fromFile(File(song.path)).toString()))
            
            // Try different metadata keys for lyrics
            val lyrics = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_LYRICS)
            
            if (!lyrics.isNullOrBlank()) {
                return processRawLyrics(lyrics, song.id)
            }
        } catch (e: Exception) {
            // Fallback to manual parsing
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {
                // Ignore
            }
        }
        
        // Try with file path directly
        return try {
            val retriever2 = MediaMetadataRetriever()
            retriever2.setDataSource(song.path)
            val lyrics = retriever2.extractMetadata(MediaMetadataRetriever.METADATA_KEY_LYRICS)
            retriever2.release()
            
            if (!lyrics.isNullOrBlank()) {
                processRawLyrics(lyrics, song.id)
            } else null
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Process raw lyrics text - detect format and parse
     */
    private fun processRawLyrics(rawLyrics: String, songId: Long): Lyric? {
        val trimmed = rawLyrics.trim()
        if (trimmed.isBlank()) return null
        
        // Check if it's LRC format with timestamps
        if (LrcParser.isLrcFormat(trimmed)) {
            return LrcParser.parse(trimmed, songId, LyricSource.EMBEDDED)
        }
        
        // Plain text - create lines with time 0
        val lines = trimmed.lines()
            .filter { it.isNotBlank() }
            .map { LyricLine(timeMs = 0, text = it.trim()) }
        
        return if (lines.isNotEmpty()) {
            Lyric(songId = songId, lines = lines, source = LyricSource.EMBEDDED)
        } else null
    }
    
    /**
     * Extract lyrics from MP3 file (ID3v2 and ID3v1 tags)
     */
    private fun extractFromMp3(file: File, songId: Long): Lyric? {
        RandomAccessFile(file, "r").use { raf ->
            // Check for ID3v2 header
            val header = ByteArray(3)
            raf.read(header)
            
            if (String(header, Charsets.ISO_8859_1) == "ID3") {
                val version = raf.readByte().toInt() and 0xFF
                raf.readByte() // flags
                
                val size = readSyncSafeInt(raf)
                val data = ByteArray(size)
                raf.read(data)
                
                parseId3Frames(data, songId, version)?.let { return it }
            }
            
            // Try ID3v1 at the end of file
            return extractFromId3v1(file, songId)
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
                if (frameId.any { it.code < 32 || it.code > 127 }) break
                
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
                    // v2.2
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
                        val frameData = data.copyOfRange(offset, offset + frameSize)
                        val parsed = parseUnsynchronizedLyrics(frameData)
                        if (!parsed.isNullOrBlank() && lyrics.isNullOrBlank()) {
                            lyrics = parsed
                        }
                    }
                    "SYLT", "SLT" -> {
                        val frameData = data.copyOfRange(offset, offset + frameSize)
                        val parsed = parseSynchronizedLyrics(frameData)
                        if (!parsed.isNullOrEmpty() && syncedLyrics.isNullOrEmpty()) {
                            syncedLyrics = parsed
                        }
                    }
                    "TXXX" -> {
                        // TXXX frame may contain lyrics with description "LYRICS"
                        val frameData = data.copyOfRange(offset, offset + frameSize)
                        val txxResult = parseTxxxFrame(frameData)
                        if (txxResult != null && LYRIC_TXXX_DESCS.contains(txxResult.first.lowercase())) {
                            if (lyrics.isNullOrBlank()) {
                                lyrics = txxResult.second
                            }
                        }
                    }
                    "COMM" -> {
                        // Comments may sometimes contain lyrics
                        val frameData = data.copyOfRange(offset, offset + frameSize)
                        val comment = parseCommentFrame(frameData)
                        // Only use if it looks like lyrics (multi-line or contains lyrics keywords)
                        if (!comment.isNullOrBlank() && lyrics.isNullOrBlank()) {
                            if (comment.lines().size > 3 || 
                                comment.lowercase().contains("lyric") ||
                                comment.contains("[")) {
                                lyrics = comment
                            }
                        }
                    }
                }
                
                offset += frameSize
            } catch (e: Exception) {
                break
            }
        }
        
        // Prefer synchronized lyrics over unsynchronized
        return when {
            !syncedLyrics.isNullOrEmpty() -> Lyric(
                songId = songId,
                lines = syncedLyrics,
                source = LyricSource.EMBEDDED
            )
            !lyrics.isNullOrBlank() -> processRawLyrics(lyrics, songId)
            else -> null
        }
    }
    
    /**
     * Parse unsynchronized lyrics frame (USLT)
     */
    private fun parseUnsynchronizedLyrics(data: ByteArray): String? {
        if (data.isEmpty()) return null
        
        try {
            var offset = 0
            val encoding = data[offset++].toInt()
            val charset = getCharset(encoding)
            
            // Language (3 bytes)
            offset += 3
            
            // Content descriptor (null-terminated)
            val descriptorEnd = findNullTerminator(data, offset, encoding)
            offset = descriptorEnd + getNullTerminatorSize(encoding)
            
            // Lyrics text
            return if (offset < data.size) {
                String(data, offset, data.size - offset, charset).trim()
            } else null
        } catch (e: Exception) {
            return null
        }
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
            
            // Time stamp format (1 = ms, 0 = frames)
            val timeStampFormat = data[offset++].toInt()
            
            // Content type
            offset++ // contentType
            
            // Content descriptor (null-terminated)
            val descriptorEnd = findNullTerminator(data, offset, encoding)
            offset = descriptorEnd + getNullTerminatorSize(encoding)
            
            val lines = mutableListOf<LyricLine>()
            
            while (offset < data.size - 4) {
                // Find null-terminated text
                val textStart = offset
                val textEnd = findNullTerminator(data, offset, encoding)
                
                if (textEnd >= data.size - 4) break
                
                val text = String(data, textStart, textEnd - textStart, charset).trim()
                offset = textEnd + getNullTerminatorSize(encoding)
                
                if (offset + 4 > data.size) break
                
                // Read time stamp (4 bytes, big-endian)
                val timeStamp = ((data[offset].toInt() and 0xFF) shl 24) or
                               ((data[offset + 1].toInt() and 0xFF) shl 16) or
                               ((data[offset + 2].toInt() and 0xFF) shl 8) or
                               (data[offset + 3].toInt() and 0xFF)
                offset += 4
                
                // Convert to milliseconds if in frames
                val timeMs = if (timeStampFormat == 1) timeStamp.toLong() 
                             else (timeStamp * 1000L / 75) // frames to ms (75 fps)
                
                if (text.isNotEmpty()) {
                    lines.add(LyricLine(timeMs = timeMs, text = text))
                }
            }
            
            return lines.sortedBy { it.timeMs }.takeIf { it.isNotEmpty() }
        } catch (e: Exception) {
            return null
        }
    }
    
    /**
     * Parse TXXX frame to get description and value
     */
    private fun parseTxxxFrame(data: ByteArray): Pair<String, String>? {
        if (data.isEmpty()) return null
        
        try {
            var offset = 0
            val encoding = data[offset++].toInt()
            val charset = getCharset(encoding)
            
            // Description (null-terminated)
            val descEnd = findNullTerminator(data, offset, encoding)
            val description = String(data, offset, descEnd - offset, charset)
            offset = descEnd + getNullTerminatorSize(encoding)
            
            // Value
            val value = if (offset < data.size) {
                String(data, offset, data.size - offset, charset)
            } else ""
            
            return Pair(description, value)
        } catch (e: Exception) {
            return null
        }
    }
    
    /**
     * Parse COMM (comment) frame
     */
    private fun parseCommentFrame(data: ByteArray): String? {
        if (data.isEmpty()) return null
        
        try {
            var offset = 0
            val encoding = data[offset++].toInt()
            val charset = getCharset(encoding)
            
            // Language (3 bytes)
            offset += 3
            
            // Description (null-terminated)
            val descEnd = findNullTerminator(data, offset, encoding)
            offset = descEnd + getNullTerminatorSize(encoding)
            
            // Comment text
            return if (offset < data.size) {
                String(data, offset, data.size - offset, charset).trim()
            } else null
        } catch (e: Exception) {
            return null
        }
    }
    
    /**
     * Extract lyrics from ID3v1 tag (at end of file)
     */
    private fun extractFromId3v1(file: File, songId: Long): Lyric? {
        // ID3v1 doesn't support lyrics, but ID3v1.1 might have a comment
        // that could contain lyrics (very rare)
        return null
    }
    
    /**
     * Extract lyrics from FLAC file (Vorbis Comments)
     */
    private fun extractFromFlac(file: File, songId: Long): Lyric? {
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
     * Format: KEY=VALUE
     */
    private fun extractFromVorbisComments(file: File, songId: Long): Lyric? {
        RandomAccessFile(file, "r").use { raf ->
            // For FLAC: Check for "fLaC" magic, then find VORBIS_COMMENT block
            val magic = ByteArray(4)
            raf.read(magic)
            
            if (String(magic, Charsets.ISO_8859_1) == "fLaC") {
                // Parse FLAC metadata blocks
                var hasMore = true
                while (hasMore) {
                    val header = raf.readInt()
                    val isLast = (header ushr 24) and 0x80 != 0
                    val blockType = (header ushr 24) and 0x7F
                    val blockSize = header and 0xFFFFFF
                    
                    if (blockType == 4) { // VORBIS_COMMENT
                        val data = ByteArray(blockSize)
                        raf.read(data)
                        return parseVorbisComment(data, songId)
                    } else {
                        raf.skipBytes(blockSize)
                    }
                    
                    hasMore = !isLast
                }
            }
            
            // For OGG: Different format, need to find Vorbis Comment section
            // This is simplified - would need proper OGG parsing for production
            
            return null
        }
    }
    
    /**
     * Parse Vorbis Comment block
     */
    private fun parseVorbisComment(data: ByteArray, songId: Long): Lyric? {
        try {
            var offset = 0
            
            // Vendor string length (little-endian)
            val vendorLen = readLittleEndianInt(data, offset)
            offset += 4 + vendorLen
            
            // Number of comments
            val numComments = readLittleEndianInt(data, offset)
            offset += 4
            
            // Read each comment
            for (i in 0 until numComments) {
                if (offset + 4 > data.size) break
                
                val commentLen = readLittleEndianInt(data, offset)
                offset += 4
                
                if (offset + commentLen > data.size) break
                
                val comment = String(data, offset, commentLen, Charsets.UTF_8)
                offset += commentLen
                
                // Check for lyrics fields
                val eqIndex = comment.indexOf('=')
                if (eqIndex > 0) {
                    val key = comment.substring(0, eqIndex).uppercase()
                    val value = comment.substring(eqIndex + 1)
                    
                    if (key in listOf("LYRICS", "UNSYNCEDLYRICS", "LYRIC", "META_LYRICS")) {
                        return processRawLyrics(value, songId)
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore parsing errors
        }
        
        return null
    }
    
    /**
     * Extract lyrics from MP4/M4A file (iTunes-style metadata)
     */
    private fun extractFromMp4(file: File, songId: Long): Lyric? {
        RandomAccessFile(file, "r").use { raf ->
            // MP4 uses atoms/boxes structure
            // Look for @lyr atom (lyrics) under moov/udta/meta/ilst
            return parseMp4Atoms(raf, songId)
        }
    }
    
    /**
     * Parse MP4 atoms to find lyrics
     */
    private fun parseMp4Atoms(raf: RandomAccessFile, songId: Long): Lyric? {
        val atomHeaderSize = 8
        var foundLyrics: String? = null
        
        fun parseAtoms(endOffset: Long) {
            while (raf.filePointer < endOffset) {
                val start = raf.filePointer
                if (start + atomHeaderSize > endOffset) break
                
                val size = raf.readInt().toLong() and 0xFFFFFFFF
                val typeBytes = ByteArray(4)
                raf.read(typeBytes)
                val type = String(typeBytes, Charsets.ISO_8859_1)
                
                if (size < atomHeaderSize || size > endOffset - start) break
                
                val atomEnd = start + size
                val atomContentSize = size - atomHeaderSize
                
                when (type) {
                    "moov", "udta", "meta", "ilst", "trak", "mdia" -> {
                        // Container atoms - recurse into them
                        if (type == "meta") {
                            // Meta atom has 4 extra bytes (version/flags)
                            raf.skipBytes(4)
                            parseAtoms(atomEnd)
                        } else {
                            parseAtoms(atomEnd)
                        }
                    }
                    "@lyr", "lyr " -> {
                        // Lyrics atom
                        if (atomContentSize > 16) { // Minimum meaningful size
                            val data = ByteArray(atomContentSize.toInt())
                            raf.read(data)
                            foundLyrics = parseMp4DataAtom(data)
                        }
                    }
                    "----" -> {
                        // iTunes custom tag - could contain lyrics
                        if (atomContentSize > 16) {
                            val data = ByteArray(atomContentSize.toInt())
                            raf.read(data)
                            val parsed = parseItunesCustomTag(data)
                            if (parsed?.first?.lowercase() == "lyrics" && foundLyrics == null) {
                                foundLyrics = parsed.second
                            }
                        }
                    }
                    else -> {
                        raf.seek(atomEnd)
                    }
                }
            }
        }
        
        try {
            parseAtoms(raf.length())
        } catch (e: Exception) {
            // Ignore parsing errors
        }
        
        return foundLyrics?.let { processRawLyrics(it, songId) }
    }
    
    /**
     * Parse MP4 data atom content
     */
    private fun parseMp4DataAtom(data: ByteArray): String? {
        try {
            if (data.size < 16) return null
            
            var offset = 0
            // Size (4 bytes) - usually matches what we have
            offset += 4
            // Type (4 bytes) - should be "data"
            val type = String(data, offset, 4, Charsets.ISO_8859_1)
            offset += 4
            if (type != "data") return null
            
            // Flags (4 bytes) - tells us the data type
            val flags = ((data[offset].toInt() and 0xFF) shl 16) or
                       ((data[offset + 1].toInt() and 0xFF) shl 8) or
                       (data[offset + 2].toInt() and 0xFF)
            offset += 4
            
            // Reserved (4 bytes)
            offset += 4
            
            // Data
            return when (flags) {
                1 -> String(data, offset, data.size - offset, Charsets.UTF_8) // UTF-8
                2 -> String(data, offset, data.size - offset, Charsets.UTF_16BE) // UTF-16 BE
                else -> String(data, offset, data.size - offset, Charsets.UTF_8) // Default to UTF-8
            }.trim()
        } catch (e: Exception) {
            return null
        }
    }
    
    /**
     * Parse iTunes custom tag (---- atom)
     */
    private fun parseItunesCustomTag(data: ByteArray): Pair<String, String>? {
        try {
            if (data.size < 16) return null
            
            var offset = 0
            
            // First sub-atom is usually 'mean' (meaning/domain)
            if (offset + 8 > data.size) return null
            val meanSize = ((data[offset].toInt() and 0xFF) shl 24) or
                          ((data[offset + 1].toInt() and 0xFF) shl 16) or
                          ((data[offset + 2].toInt() and 0xFF) shl 8) or
                          (data[offset + 3].toInt() and 0xFF)
            val meanType = String(data, offset + 4, 4, Charsets.ISO_8859_1)
            offset += 8
            
            if (meanType != "mean" || offset + meanSize - 8 > data.size) return null
            offset += meanSize - 8
            
            // Next sub-atom is 'name'
            if (offset + 8 > data.size) return null
            val nameSize = ((data[offset].toInt() and 0xFF) shl 24) or
                          ((data[offset + 1].toInt() and 0xFF) shl 16) or
                          ((data[offset + 2].toInt() and 0xFF) shl 8) or
                          (data[offset + 3].toInt() and 0xFF)
            val nameType = String(data, offset + 4, 4, Charsets.ISO_8859_1)
            offset += 8
            
            if (nameType != "name" || offset + nameSize - 8 > data.size) return null
            val name = String(data, offset, nameSize - 8, Charsets.UTF_8)
            offset += nameSize - 8
            
            // Finally, 'data' atom
            if (offset + 8 > data.size) return null
            val dataSize = ((data[offset].toInt() and 0xFF) shl 24) or
                          ((data[offset + 1].toInt() and 0xFF) shl 16) or
                          ((data[offset + 2].toInt() and 0xFF) shl 8) or
                          (data[offset + 3].toInt() and 0xFF)
            val dataType = String(data, offset + 4, 4, Charsets.ISO_8859_1)
            offset += 8
            
            if (dataType != "data") return null
            
            // Skip flags and reserved (8 bytes)
            offset += 8
            
            val value = if (offset < data.size) {
                String(data, offset, minOf(dataSize - 16, data.size - offset), Charsets.UTF_8)
            } else ""
            
            return Pair(name, value.trim())
        } catch (e: Exception) {
            return null
        }
    }
    
    /**
     * Extract lyrics from WAV file
     */
    private fun extractFromWav(file: File, songId: Long): Lyric? {
        // WAV files use INFO/LIST chunks for metadata
        // Very rarely contain lyrics
        return null
    }
    
    /**
     * Extract lyrics from WMA file
     */
    private fun extractFromWma(file: File, songId: Long): Lyric? {
        // WMA uses ASF format with metadata in Content Description Object
        // Would need proper ASF parser
        return null
    }
    
    // === Utility functions ===
    
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
     * Find null terminator in byte array
     */
    private fun findNullTerminator(data: ByteArray, start: Int, encoding: Int): Int {
        val terminatorSize = if (encoding == 1 || encoding == 2) 2 else 1
        
        for (i in start until data.size - terminatorSize + 1) {
            if (terminatorSize == 1) {
                if (data[i] == 0.toByte()) return i
            } else {
                if (data[i] == 0.toByte() && data[i + 1] == 0.toByte()) return i
            }
        }
        return data.size
    }
    
    /**
     * Get null terminator size based on encoding
     */
    private fun getNullTerminatorSize(encoding: Int): Int {
        return if (encoding == 1 || encoding == 2) 2 else 1
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
    
    /**
     * Read little-endian integer from byte array
     */
    private fun readLittleEndianInt(data: ByteArray, offset: Int): Int {
        return (data[offset].toInt() and 0xFF) or
               ((data[offset + 1].toInt() and 0xFF) shl 8) or
               ((data[offset + 2].toInt() and 0xFF) shl 16) or
               ((data[offset + 3].toInt() and 0xFF) shl 24)
    }
}