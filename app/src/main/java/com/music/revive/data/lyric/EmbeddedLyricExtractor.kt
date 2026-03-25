package com.music.revive.data.lyric

import android.content.Context
import android.media.MediaMetadataRetriever
import android.util.Log
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
 * Supported formats:
 * - MP3 (ID3v2.2/2.3/2.4 tags, ID3v1)
 * - FLAC (Vorbis Comments)
 * - M4A/MP4/M4B (iTunes-style metadata)
 * - OGG/OPUS (Vorbis Comments)
 * - APE (APEv2 tags)
 * - WAV (RIFF INFO chunks)
 * - AIFF (ID3v2 tags)
 * - WMA/ASF (ASF metadata)
 * - WavPack (APEv2 tags)
 * - DSF (ID3v2 tags)
 * 
 * Supported tag frames:
 * - USLT/ULT - Unsynchronized lyrics
 * - SYLT/SLT - Synchronized lyrics (time-synced)
 * - TXXX - Custom text with descriptions like "LYRICS", "SYNCEDLYRICS"
 * - COMM - Comments (sometimes contain lyrics)
 * - Vorbis: LYRICS, UNSYNCEDLYRICS, SYNCEDLYRICS, LYRIC, META_LYRICS
 * - MP4: @lyr, ©lyr, lyrics
 * - APE: LYRICS, UNSYNCED LYRICS, SYNCED LYRICS
 */
@Singleton
class EmbeddedLyricExtractor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    companion object {
        private const val TAG = "EmbeddedLyricExtractor"
        
        // ID3v2 TXXX descriptions that may contain lyrics
        private val LYRIC_TXXX_DESCS = setOf(
            "lyrics", "lyric", "syncedlyrics", "synced lyrics",
            "unsyncedlyrics", "unsynced lyrics", "song lyrics",
            "embedded lyrics", "内嵌歌词", "歌词", "lyrics-xxx",
            "syncedlyrics", "syncedlyrics.com", "minilyrics",
            "lyrics eng", "lyricstext", "lyricsplus"
        )
        
        // Vorbis Comment field names for lyrics
        private val VORBIS_LYRIC_FIELDS = setOf(
            "LYRICS", "UNSYNCEDLYRICS", "SYNCEDLYRICS", "LYRIC",
            "META_LYRICS", "LYRICS_UNSYNCED", "LYRICS_SYNCED",
            "UNSYNCED LYRICS", "SYNCED LYRICS", "SONG LYRICS",
            "ESLyrics", "LYRICIST", "LYRICSXXX", "SYNCED_LYRICS",
            "UNSYNCED_LYRICS", "LYRIC_TEXT", "SONGLYRICS",
            "LYRICS_ENG", "LYRICSSYNC", "LYRICSUNSYNCED"
        )
        
        // MP4/M4A atom names for lyrics
        private val MP4_LYRIC_ATOMS = setOf(
            "@lyr", "lyr ", "©lyr", "lyrics", "xid ",
            "lrc ", "©lrc", "snc ", "syncedlyrics"
        )
        
        // APE tag field names for lyrics
        private val APE_LYRIC_FIELDS = setOf(
            "LYRICS", "UNSYNCED LYRICS", "SYNCED LYRICS",
            "LYRIC", "SONG LYRICS", "SYNCEDLYRICS", "UNSYNCEDLYRICS",
            "LYRICS SYNCED", "LYRICS UNSYNCED", "ESLyrics"
        )
        
        // WMA/ASF lyric field names
        private val WMA_LYRIC_FIELDS = setOf(
            "wm/lyrics", "lyrics", "wm/lyrics_synchronised",
            "wm/lyrics_synchronized", "wm/lyricist"
        )
    }
    
    /**
     * Extract embedded lyrics from an audio file
     */
    suspend fun extractLyrics(song: Song): Lyric? = withContext(Dispatchers.IO) {
        try {
            val file = File(song.path)
            if (!file.exists()) {
                Log.d(TAG, "File not found: ${song.path}")
                return@withContext null
            }
            
            val extension = file.extension.lowercase()
            Log.d(TAG, "Extracting lyrics from ${file.name} (format: $extension)")
            
            // Method 1: Try MediaMetadataRetriever first (most reliable for common formats)
            extractWithMediaMetadataRetriever(song)?.let { 
                Log.d(TAG, "Found lyrics via MediaMetadataRetriever")
                return@withContext it 
            }
            
            // Method 2: Manual parsing based on file extension
            when (extension) {
                "mp3", "mp2", "mp1" -> extractFromMp3(file, song.id)
                "flac" -> extractFromFlac(file, song.id)
                "m4a", "mp4", "m4b", "m4p", "m4r", "aac" -> extractFromMp4(file, song.id)
                "ogg", "oga" -> extractFromOgg(file, song.id)
                "opus" -> extractFromOpus(file, song.id)
                "wav", "wave" -> extractFromWav(file, song.id)
                "aiff", "aif", "aifc" -> extractFromAiff(file, song.id)
                "wma", "asf" -> extractFromWma(file, song.id)
                "ape" -> extractFromApe(file, song.id)
                "wv" -> extractFromWavPack(file, song.id)
                "dsf", "dff" -> extractFromDsf(file, song.id)
                "tta" -> extractFromTta(file, song.id)
                "mpc", "mp+", "mpp" -> extractFromMusepack(file, song.id)
                "sf2", "sf3" -> null // SoundFont files - no lyrics
                "mid", "midi" -> null // MIDI files - no embedded lyrics typically
                else -> {
                    Log.d(TAG, "Unsupported format: $extension")
                    null
                }
            }?.let { 
                Log.d(TAG, "Found lyrics via manual parsing for $extension")
                return@withContext it 
            }
            
            Log.d(TAG, "No lyrics found in ${file.name}")
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting lyrics from ${song.path}", e)
            null
        }
    }
    
    /**
     * Extract lyrics using Android's MediaMetadataRetriever
     * Note: METADATA_KEY_LYRICS (value 20) is only available in API 29+
     */
    private fun extractWithMediaMetadataRetriever(song: Song): Lyric? {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q) {
            return null
        }
        
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(song.path)
            
            // METADATA_KEY_LYRICS = 20
            val lyrics = retriever.extractMetadata(20)
            
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
        
        return null
    }
    
    /**
     * Process raw lyrics text - detect format and parse
     */
    private fun processRawLyrics(rawLyrics: String, songId: Long): Lyric? {
        val trimmed = rawLyrics.trim()
        if (trimmed.isBlank()) return null
        
        // Remove common BOM markers
        val cleaned = removeBom(trimmed)
        
        // Check if it's LRC format with timestamps
        if (LrcParser.isLrcFormat(cleaned)) {
            return LrcParser.parse(cleaned, songId, LyricSource.EMBEDDED)
        }
        
        // Plain text - create lines with time 0
        val lines = cleaned.lines()
            .filter { it.isNotBlank() }
            .map { LyricLine(timeMs = 0, text = it.trim()) }
        
        return if (lines.isNotEmpty()) {
            Lyric(songId = songId, lines = lines, source = LyricSource.EMBEDDED)
        } else null
    }
    
    /**
     * Remove BOM (Byte Order Mark) characters
     */
    private fun removeBom(text: String): String {
        return text.removePrefix("\uFEFF")  // UTF-8 BOM
            .removePrefix("\uFFFE")         // UTF-16 LE BOM
            .removePrefix("\uFEFF")         // UTF-16 BE BOM
    }
    
    // ==================== MP3 / ID3v2 ====================
    
    /**
     * Extract lyrics from MP3 file (ID3v2 tags)
     */
    private fun extractFromMp3(file: File, songId: Long): Lyric? {
        RandomAccessFile(file, "r").use { raf ->
            // Check for ID3v2 header
            val header = ByteArray(3)
            raf.read(header)
            
            if (String(header, Charsets.ISO_8859_1) == "ID3") {
                val version = raf.readByte().toInt() and 0xFF
                val flags = raf.readByte().toInt() and 0xFF
                
                val size = readSyncSafeInt(raf)
                
                // Check for extended header
                val extendedHeaderSize = if ((flags and 0x40) != 0 && version >= 3) {
                    val extSize = raf.readInt()
                    raf.skipBytes(extSize - 4)
                    extSize
                } else 0
                
                val dataSize = size - extendedHeaderSize
                if (dataSize > 0) {
                    val data = ByteArray(dataSize)
                    raf.read(data)
                    
                    parseId3Frames(data, songId, version)?.let { return it }
                }
            }
            
            // Try APEv2 tag at end of file (some MP3s have this)
            extractApeTagFromEnd(file, songId)?.let { return it }
            
            return null
        }
    }
    
    /**
     * Parse ID3v2 frames to extract lyrics
     */
    private fun parseId3Frames(data: ByteArray, songId: Long, version: Int): Lyric? {
        var offset = 0
        var lyrics: String? = null
        var syncedLyrics: List<LyricLine>? = null
        var lrcLyrics: String? = null  // For TXXX with LRC format
        
        while (offset < data.size - 10) {
            try {
                val frameIdLength = if (version >= 3) 4 else 3
                if (offset + frameIdLength > data.size) break
                
                val frameId = String(data, offset, frameIdLength, Charsets.ISO_8859_1)
                
                // Check for padding
                if (frameId.all { it == '\u0000' }) break
                if (frameId.any { it.code < 32 || it.code > 127 }) break
                
                offset += frameIdLength
                
                val frameSize = when {
                    version >= 4 -> readSyncSafeIntFromBytes(data, offset)
                    version == 3 -> readBigEndianInt(data, offset)
                    else -> readId3v22Size(data, offset)
                }
                
                if (frameSize <= 0 || frameSize > data.size - offset) break
                offset += if (version >= 3) 4 else 3
                
                // Skip flags
                if (version >= 3) {
                    val flags = if (offset + 2 <= data.size) {
                        val f1 = data[offset].toInt() and 0xFF
                        val f2 = data[offset + 1].toInt() and 0xFF
                        offset += 2
                        Pair(f1, f2)
                    } else {
                        offset += 2
                        Pair(0, 0)
                    }
                    
                    // Skip if compression or encryption is enabled
                    if ((flags.first and 0x80) != 0 || (flags.first and 0x40) != 0) {
                        offset += frameSize
                        continue
                    }
                }
                
                if (offset + frameSize > data.size) break
                
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
                        val frameData = data.copyOfRange(offset, offset + frameSize)
                        val result = parseTxxxFrame(frameData)
                        if (result != null && LYRIC_TXXX_DESCS.contains(result.first.lowercase())) {
                            if (lrcLyrics.isNullOrBlank()) {
                                lrcLyrics = result.second
                            }
                        }
                    }
                    "COMM" -> {
                        val frameData = data.copyOfRange(offset, offset + frameSize)
                        val comment = parseCommentFrame(frameData)
                        if (!comment.isNullOrBlank() && lyrics.isNullOrBlank()) {
                            // Check if comment looks like lyrics
                            if (comment.lines().size > 3 || 
                                comment.contains("[") ||
                                comment.lowercase().contains("lyric") ||
                                looksLikeLyrics(comment)) {
                                lyrics = comment
                            }
                        }
                    }
                    "WXXX" -> {
                        // Could contain lyrics URL, skip for now
                    }
                }
                
                offset += frameSize
            } catch (e: Exception) {
                break
            }
        }
        
        // Priority: synced > LRC format > plain lyrics
        return when {
            !syncedLyrics.isNullOrEmpty() -> Lyric(
                songId = songId,
                lines = syncedLyrics,
                source = LyricSource.EMBEDDED
            )
            !lrcLyrics.isNullOrBlank() -> processRawLyrics(lrcLyrics, songId)
            !lyrics.isNullOrBlank() -> processRawLyrics(lyrics, songId)
            else -> null
        }
    }
    
    /**
     * Check if text looks like lyrics (has typical lyrics patterns)
     */
    private fun looksLikeLyrics(text: String): Boolean {
        val lines = text.lines()
        if (lines.size < 2) return false
        
        // Check for common lyrics patterns
        val lowerText = text.lowercase()
        val lyricPatterns = listOf("verse", "chorus", "bridge", "intro", "outro", 
                                   "[01:", "[00:", "副歌", "主歌")
        
        return lyricPatterns.any { lowerText.contains(it) }
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
            
            // Content type (1 = lyrics, 2 = transcription, etc.)
            val contentType = data[offset++].toInt()
            
            // Content descriptor (null-terminated)
            val descriptorEnd = findNullTerminator(data, offset, encoding)
            offset = descriptorEnd + getNullTerminatorSize(encoding)
            
            val lines = mutableListOf<LyricLine>()
            
            while (offset < data.size - 4) {
                val textStart = offset
                val textEnd = findNullTerminator(data, offset, encoding)
                
                if (textEnd >= data.size - 4) break
                
                val text = String(data, textStart, textEnd - textStart, charset).trim()
                offset = textEnd + getNullTerminatorSize(encoding)
                
                if (offset + 4 > data.size) break
                
                val timeStamp = readBigEndianInt(data, offset)
                offset += 4
                
                val timeMs = if (timeStampFormat == 1) timeStamp.toLong() 
                             else (timeStamp * 1000L / 75)
                
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
            
            val descEnd = findNullTerminator(data, offset, encoding)
            if (descEnd > data.size) return null
            val description = String(data, offset, descEnd - offset, charset)
            offset = descEnd + getNullTerminatorSize(encoding)
            
            val value = if (offset < data.size) {
                String(data, offset, data.size - offset, charset)
            } else ""
            
            return Pair(description.trim(), value.trim())
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
            
            val descEnd = findNullTerminator(data, offset, encoding)
            offset = descEnd + getNullTerminatorSize(encoding)
            
            return if (offset < data.size) {
                String(data, offset, data.size - offset, charset).trim()
            } else null
        } catch (e: Exception) {
            return null
        }
    }
    
    // ==================== FLAC ====================
    
    /**
     * Extract lyrics from FLAC file (Vorbis Comments)
     */
    private fun extractFromFlac(file: File, songId: Long): Lyric? {
        RandomAccessFile(file, "r").use { raf ->
            val magic = ByteArray(4)
            raf.read(magic)
            
            if (String(magic, Charsets.ISO_8859_1) == "fLaC") {
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
                    } else if (blockType == 0) {
                        // STREAMINFO - skip
                        raf.skipBytes(blockSize)
                    } else {
                        raf.skipBytes(blockSize)
                    }
                    
                    hasMore = !isLast
                }
            }
            
            return null
        }
    }
    
    // ==================== OGG / OPUS ====================
    
    /**
     * Extract lyrics from OGG file (Vorbis Comments)
     */
    private fun extractFromOgg(file: File, songId: Long): Lyric? {
        RandomAccessFile(file, "r").use { raf ->
            // OGG page header
            val magic = ByteArray(4)
            raf.read(magic)
            
            if (String(magic, Charsets.ISO_8859_1) == "OggS") {
                // Skip to Vorbis identification header
                // Structure: OggS page -> Vorbis identification -> Vorbis comment
                raf.seek(0)
                
                // Read through pages to find comment header
                while (raf.filePointer < raf.length() - 4) {
                    val pageMagic = ByteArray(4)
                    raf.read(pageMagic)
                    
                    if (String(pageMagic, Charsets.ISO_8859_1) != "OggS") {
                        continue
                    }
                    
                    // Parse OGG page header
                    raf.skipBytes(22) // version, flags, granule, serial, seq, crc
                    
                    val numSegments = raf.readByte().toInt() and 0xFF
                    var pageDataSize = 0
                    for (i in 0 until numSegments) {
                        pageDataSize += raf.readByte().toInt() and 0xFF
                    }
                    
                    if (pageDataSize < 7) {
                        raf.skipBytes(pageDataSize)
                        continue
                    }
                    
                    // Check header type
                    val headerType = raf.readByte().toInt() and 0xFF
                    raf.seek(raf.filePointer - 1)
                    
                    // Read page data
                    val pageData = ByteArray(pageDataSize)
                    raf.read(pageData)
                    
                    // Check for Vorbis comment header (0x03 "vorbis") or Opus tags (0x4F "OpusTags")
                    if (pageData.size > 7) {
                        val packetHeader = String(pageData, 0, minOf(7, pageData.size), Charsets.ISO_8859_1)
                        
                        if (pageData[0] == 0x03.toByte() && packetHeader.contains("vorbis")) {
                            // Vorbis comment: [0x03 "vorbis"] + comment data
                            return parseVorbisComment(pageData.copyOfRange(7, pageData.size), songId)
                        } else if (pageData[0] == 0x4F.toByte() && packetHeader.startsWith("OpusTag")) {
                            // Opus tags: "OpusTags" + vendor string + comments
                            if (pageData.size > 8) {
                                return parseVorbisComment(pageData.copyOfRange(8, pageData.size), songId)
                            }
                        }
                    }
                }
            }
            
            return null
        }
    }
    
    /**
     * Extract lyrics from Opus file
     */
    private fun extractFromOpus(file: File, songId: Long): Lyric? {
        // Opus uses the same container as OGG
        return extractFromOgg(file, songId)
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
            
            if (offset + 4 > data.size) return null
            
            // Number of comments
            val numComments = readLittleEndianInt(data, offset)
            offset += 4
            
            var foundLyrics: String? = null
            
            for (i in 0 until numComments) {
                if (offset + 4 > data.size) break
                
                val commentLen = readLittleEndianInt(data, offset)
                offset += 4
                
                if (offset + commentLen > data.size) break
                
                val comment = String(data, offset, commentLen, Charsets.UTF_8)
                offset += commentLen
                
                val eqIndex = comment.indexOf('=')
                if (eqIndex > 0) {
                    val key = comment.substring(0, eqIndex).uppercase()
                    val value = comment.substring(eqIndex + 1)
                    
                    if (key in VORBIS_LYRIC_FIELDS && foundLyrics.isNullOrBlank()) {
                        foundLyrics = value
                    }
                }
            }
            
            return foundLyrics?.let { processRawLyrics(it, songId) }
        } catch (e: Exception) {
            return null
        }
    }
    
    // ==================== MP4 / M4A ====================
    
    /**
     * Extract lyrics from MP4/M4A file (iTunes-style metadata)
     */
    private fun extractFromMp4(file: File, songId: Long): Lyric? {
        RandomAccessFile(file, "r").use { raf ->
            return parseMp4Atoms(raf, songId)
        }
    }
    
    /**
     * Parse MP4 atoms to find lyrics
     */
    private fun parseMp4Atoms(raf: RandomAccessFile, songId: Long): Lyric? {
        var foundLyrics: String? = null
        
        fun parseAtoms(endOffset: Long) {
            while (raf.filePointer < endOffset) {
                val start = raf.filePointer
                if (start + 8 > endOffset) break
                
                val size = raf.readInt().toLong() and 0xFFFFFFFF
                val typeBytes = ByteArray(4)
                raf.read(typeBytes)
                val type = String(typeBytes, Charsets.ISO_8859_1)
                
                // Handle extended size
                val actualSize = if (size == 1L) {
                    val high = raf.readInt().toLong() and 0xFFFFFFFF
                    val low = raf.readInt().toLong() and 0xFFFFFFFF
                    (high shl 32) or low
                } else size
                
                if (actualSize < 8 || actualSize > endOffset - start) break
                
                val atomEnd = start + actualSize
                val atomContentSize = actualSize - 8
                
                when (type) {
                    "moov", "trak", "mdia", "udta", "ilst" -> {
                        parseAtoms(atomEnd)
                    }
                    "meta" -> {
                        raf.skipBytes(4) // Version/flags
                        parseAtoms(atomEnd)
                    }
                    in MP4_LYRIC_ATOMS -> {
                        if (atomContentSize > 8) {
                            val data = ByteArray(atomContentSize.toInt())
                            raf.read(data)
                            val parsed = parseMp4DataAtom(data)
                            if (!parsed.isNullOrBlank() && foundLyrics.isNullOrBlank()) {
                                foundLyrics = parsed
                            }
                        }
                    }
                    "----" -> {
                        if (atomContentSize > 16) {
                            val data = ByteArray(atomContentSize.toInt())
                            raf.read(data)
                            val parsed = parseItunesCustomTag(data)
                            if (parsed != null && LYRIC_TXXX_DESCS.contains(parsed.first.lowercase()) 
                                && foundLyrics.isNullOrBlank()) {
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
            // Ignore
        }
        
        return foundLyrics?.let { processRawLyrics(it, songId) }
    }
    
    /**
     * Parse MP4 data atom content
     */
    private fun parseMp4DataAtom(data: ByteArray): String? {
        try {
            if (data.size < 8) return null
            
            var offset = 0
            val atomSize = readBigEndianInt(data, offset)
            offset += 4
            
            val atomType = String(data, offset, 4, Charsets.ISO_8859_1)
            offset += 4
            
            if (atomType != "data") return null
            
            // Flags (4 bytes)
            val flags = readBigEndianInt(data, offset)
            offset += 4
            
            // Reserved (4 bytes)
            offset += 4
            
            if (offset >= data.size) return null
            
            return when (flags and 0xFF) {
                1 -> String(data, offset, data.size - offset, Charsets.UTF_8)
                2 -> String(data, offset, data.size - offset, Charsets.UTF_16BE)
                3 -> String(data, offset, data.size - offset, Charsets.UTF_16LE)
                else -> String(data, offset, data.size - offset, Charsets.UTF_8)
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
            var offset = 0
            
            // 'mean' atom
            if (offset + 8 > data.size) return null
            val meanSize = readBigEndianInt(data, offset)
            val meanType = String(data, offset + 4, 4, Charsets.ISO_8859_1)
            if (meanType != "mean") return null
            offset += meanSize
            
            // 'name' atom
            if (offset + 8 > data.size) return null
            val nameSize = readBigEndianInt(data, offset)
            val nameType = String(data, offset + 4, 4, Charsets.ISO_8859_1)
            if (nameType != "name") return null
            
            val name = if (nameSize > 12) {
                String(data, offset + 12, nameSize - 12, Charsets.UTF_8)
            } else ""
            offset += nameSize
            
            // 'data' atom
            if (offset + 8 > data.size) return null
            val dataSize = readBigEndianInt(data, offset)
            val dataType = String(data, offset + 4, 4, Charsets.ISO_8859_1)
            if (dataType != "data") return null
            
            val value = if (dataSize > 16) {
                String(data, offset + 16, minOf(dataSize - 16, data.size - offset - 16), Charsets.UTF_8)
            } else ""
            
            return Pair(name.trim(), value.trim())
        } catch (e: Exception) {
            return null
        }
    }
    
    // ==================== WAV / AIFF ====================
    
    /**
     * Extract lyrics from WAV file
     */
    private fun extractFromWav(file: File, songId: Long): Lyric? {
        RandomAccessFile(file, "r").use { raf ->
            val riff = ByteArray(4)
            raf.read(riff)
            
            if (String(riff, Charsets.ISO_8859_1) == "RIFF") {
                raf.skipBytes(4) // File size
                
                val wave = ByteArray(4)
                raf.read(wave)
                
                if (String(wave, Charsets.ISO_8859_1) == "WAVE") {
                    // Parse chunks
                    while (raf.filePointer < raf.length() - 8) {
                        val chunkId = ByteArray(4)
                        raf.read(chunkId)
                        val chunkSize = raf.readInt() and 0xFFFFFFFF.toInt()
                        
                        when (String(chunkId, Charsets.ISO_8859_1)) {
                            "LIST" -> {
                                val listType = ByteArray(4)
                                raf.read(listType)
                                
                                if (String(listType, Charsets.ISO_8859_1) == "INFO") {
                                    // Parse INFO chunk for lyrics
                                    val infoData = ByteArray(chunkSize - 4)
                                    raf.read(infoData)
                                    val lyrics = parseRiffInfo(infoData)
                                    if (!lyrics.isNullOrBlank()) {
                                        return processRawLyrics(lyrics, songId)
                                    }
                                } else {
                                    raf.skipBytes(chunkSize - 4)
                                }
                            }
                            "id3 ", "ID3 " -> {
                                // ID3 chunk
                                val id3Data = ByteArray(chunkSize)
                                raf.read(id3Data)
                                // ID3 in WAV starts with ID3 header
                                if (String(id3Data, 0, 3, Charsets.ISO_8859_1) == "ID3") {
                                    return parseId3Frames(
                                        id3Data.copyOfRange(10, id3Data.size),
                                        songId,
                                        id3Data[3].toInt() and 0xFF
                                    )
                                }
                            }
                            else -> {
                                raf.skipBytes(chunkSize)
                            }
                        }
                    }
                }
            }
            
            return null
        }
    }
    
    /**
     * Parse RIFF INFO chunk
     */
    private fun parseRiffInfo(data: ByteArray): String? {
        var offset = 0
        
        while (offset + 8 <= data.size) {
            val infoId = String(data, offset, 4, Charsets.ISO_8859_1)
            val infoSize = readLittleEndianInt(data, offset + 4)
            offset += 8
            
            if (offset + infoSize > data.size) break
            
            when (infoId) {
                "LYRICS", "lyrics" -> {
                    return String(data, offset, infoSize, Charsets.UTF_8).trim()
                }
                "ICMT", "cmt " -> {
                    // Comment - might contain lyrics
                    val comment = String(data, offset, infoSize, Charsets.UTF_8).trim()
                    if (looksLikeLyrics(comment)) {
                        return comment
                    }
                }
            }
            
            offset += infoSize
            // Align to word boundary
            if (infoSize % 2 != 0) offset++
        }
        
        return null
    }
    
    /**
     * Extract lyrics from AIFF file (ID3v2 tags)
     */
    private fun extractFromAiff(file: File, songId: Long): Lyric? {
        RandomAccessFile(file, "r").use { raf ->
            val form = ByteArray(4)
            raf.read(form)
            
            if (String(form, Charsets.ISO_8859_1) == "FORM") {
                raf.skipBytes(4) // File size
                
                val aiff = ByteArray(4)
                raf.read(aiff)
                
                if (String(aiff, Charsets.ISO_8859_1) in listOf("AIFF", "AIFC")) {
                    while (raf.filePointer < raf.length() - 8) {
                        val chunkId = ByteArray(4)
                        raf.read(chunkId)
                        val chunkSize = raf.readInt() and 0xFFFFFFFF.toInt()
                        
                        if (String(chunkId, Charsets.ISO_8859_1) == "ID3 " ||
                            String(chunkId, Charsets.ISO_8859_1) == "id3 ") {
                            val id3Data = ByteArray(chunkSize)
                            raf.read(id3Data)
                            
                            if (String(id3Data, 0, 3, Charsets.ISO_8859_1) == "ID3") {
                                val version = id3Data[3].toInt() and 0xFF
                                val size = readSyncSafeIntFromBytes(id3Data, 6)
                                return parseId3Frames(
                                    id3Data.copyOfRange(10, 10 + size),
                                    songId,
                                    version
                                )
                            }
                        } else {
                            raf.skipBytes(chunkSize)
                        }
                    }
                }
            }
            
            return null
        }
    }
    
    // ==================== APE / WavPack ====================
    
    /**
     * Extract lyrics from APE file (APEv2 tags)
     */
    private fun extractFromApe(file: File, songId: Long): Lyric? {
        // APE files have APEv2 tags at the end
        return extractApeTagFromEnd(file, songId)
    }
    
    /**
     * Extract lyrics from WavPack file (APEv2 tags)
     */
    private fun extractFromWavPack(file: File, songId: Long): Lyric? {
        return extractApeTagFromEnd(file, songId)
    }
    
    /**
     * Extract APEv2 tag from end of file
     */
    private fun extractApeTagFromEnd(file: File, songId: Long): Lyric? {
        RandomAccessFile(file, "r").use { raf ->
            // APE tag footer is 32 bytes at the end
            if (raf.length() < 32) return null
            
            raf.seek(raf.length() - 32)
            
            val preamble = ByteArray(8)
            raf.read(preamble)
            
            if (String(preamble, Charsets.ISO_8859_1) != "APETAGEX") return null
            
            // Version
            raf.skipBytes(4)
            
            // Tag size
            val tagSize = raf.readInt() and 0xFFFFFFFF.toInt()
            
            // Item count
            val itemCount = raf.readInt() and 0xFFFF
            
            // Flags
            raf.skipBytes(4)
            
            // Reserved
            raf.skipBytes(8)
            
            // Read tag data
            raf.seek(raf.length() - tagSize)
            val tagData = ByteArray(tagSize - 32) // Exclude footer
            
            var foundLyrics: String? = null
            
            for (i in 0 until itemCount) {
                if (raf.filePointer >= raf.length() - 32) break
                
                val itemSize = raf.readInt() and 0xFFFFFFFF.toInt()
                val itemFlags = raf.readInt()
                
                // Read key (null-terminated)
                val keyBuilder = StringBuilder()
                var b: Byte
                while (raf.readByte().also { b = it } != 0.toByte()) {
                    keyBuilder.append(b.toInt().toChar())
                }
                val key = keyBuilder.toString().uppercase()
                
                // Read value
                if (itemSize > 0) {
                    val value = ByteArray(itemSize)
                    raf.read(value)
                    
                    if (key in APE_LYRIC_FIELDS && foundLyrics.isNullOrBlank()) {
                        // Check if it's text (not binary)
                        if ((itemFlags and 0x06) == 0x00) { // UTF-8 text
                            foundLyrics = String(value, Charsets.UTF_8).trim()
                        }
                    }
                }
            }
            
            return foundLyrics?.let { processRawLyrics(it, songId) }
        }
    }
    
    // ==================== WMA / ASF ====================
    
    /**
     * Extract lyrics from WMA file (ASF format)
     */
    private fun extractFromWma(file: File, songId: Long): Lyric? {
        RandomAccessFile(file, "r").use { raf ->
            // ASF header GUID
            val headerGuid = ByteArray(16)
            raf.read(headerGuid)
            
            val asfHeaderGuid = byteArrayOf(
                0x30, 0x26, 0xB2.toByte(), 0x75, 0x8E.toByte(), 0x66, 0xCF.toByte(), 0x11,
                0xA6.toByte(), 0xD9.toByte(), 0x00, 0xAA.toByte(), 0x00, 0x62.toByte(), 0xCE.toByte(), 0x6C.toByte()
            )
            
            if (!headerGuid.contentEquals(asfHeaderGuid)) return null
            
            // Header size
            val headerSize = readLittleEndianLong(raf)
            raf.skipBytes(4) // Number of header objects
            
            val headerEnd = raf.filePointer + headerSize - 24
            
            while (raf.filePointer < headerEnd) {
                val guid = ByteArray(16)
                raf.read(guid)
                val objSize = readLittleEndianLong(raf)
                
                if (objSize < 24) break
                
                // Content Description Object GUID
                val contentDescGuid = byteArrayOf(
                    0x33, 0x26, 0xB2.toByte(), 0x75, 0x8E.toByte(), 0x66, 0xCF.toByte(), 0x11,
                    0xA6.toByte(), 0xD9.toByte(), 0x00, 0xAA.toByte(), 0x00, 0x62.toByte(), 0xCE.toByte(), 0x6C.toByte()
                )
                
                // Extended Content Description Object GUID
                val extContentDescGuid = byteArrayOf(
                    0x40, 0xA4.toByte(), 0xD0.toByte(), 0xD2.toByte(), 0x07, 0xE3.toByte(), 0xD2.toByte(), 0x11,
                    0x97.toByte(), 0xF0.toByte(), 0x00, 0xA0.toByte(), 0xC9.toByte(), 0x5E.toByte(), 0xA3.toByte(), 0x4B.toByte()
                )
                
                when {
                    guid.contentEquals(contentDescGuid) -> {
                        // Content Description Object
                        val titleLen = raf.readUnsignedShort()
                        val authorLen = raf.readUnsignedShort()
                        val copyrightLen = raf.readUnsignedShort()
                        val descLen = raf.readUnsignedShort()
                        val ratingLen = raf.readUnsignedShort()
                        
                        raf.skipBytes(titleLen + authorLen + copyrightLen)
                        
                        if (descLen > 0) {
                            val desc = ByteArray(descLen)
                            raf.read(desc)
                            val description = String(desc, Charsets.UTF_16LE).trim()
                            if (looksLikeLyrics(description)) {
                                return processRawLyrics(description, songId)
                            }
                        }
                    }
                    guid.contentEquals(extContentDescGuid) -> {
                        // Extended Content Description Object
                        val descriptorCount = raf.readUnsignedShort()
                        
                        for (i in 0 until descriptorCount) {
                            val nameLen = raf.readUnsignedShort()
                            val nameBytes = ByteArray(nameLen)
                            raf.read(nameBytes)
                            val name = String(nameBytes, Charsets.UTF_16LE)
                            
                            val dataType = raf.readUnsignedShort()
                            val valueLen = raf.readUnsignedShort()
                            val valueBytes = ByteArray(valueLen)
                            raf.read(valueBytes)
                            
                            if (name.lowercase() == "wm/lyrics" || 
                                name.lowercase() == "lyrics" ||
                                name.lowercase() == "wm/lyrics_synchronised") {
                                
                                val value = when (dataType) {
                                    0 -> String(valueBytes, Charsets.UTF_16LE).trim()
                                    1 -> String(valueBytes, Charsets.UTF_16LE).trim()
                                    else -> null
                                }
                                
                                if (!value.isNullOrBlank()) {
                                    return processRawLyrics(value, songId)
                                }
                            }
                        }
                    }
                    else -> {
                        raf.skipBytes((objSize - 24).toInt())
                    }
                }
            }
            
            return null
        }
    }
    
    // ==================== DSF ====================
    
    /**
     * Extract lyrics from DSF file (ID3v2 tags)
     */
    private fun extractFromDsf(file: File, songId: Long): Lyric? {
        RandomAccessFile(file, "r").use { raf ->
            // DSD chunk
            val dsd = ByteArray(4)
            raf.read(dsd)
            
            if (String(dsd, Charsets.ISO_8859_1) == "DSD ") {
                val chunkSize = readLittleEndianLong(raf)
                raf.skipBytes((chunkSize - 12).toInt())
                
                // Find ID3 chunk
                while (raf.filePointer < raf.length() - 12) {
                    val chunkId = ByteArray(4)
                    raf.read(chunkId)
                    val size = readLittleEndianLong(raf)
                    
                    if (String(chunkId, Charsets.ISO_8859_1) == "ID3 ") {
                        val id3Data = ByteArray(size.toInt())
                        raf.read(id3Data)
                        
                        if (String(id3Data, 0, 3, Charsets.ISO_8859_1) == "ID3") {
                            val version = id3Data[3].toInt() and 0xFF
                            val tagSize = readSyncSafeIntFromBytes(id3Data, 6)
                            return parseId3Frames(
                                id3Data.copyOfRange(10, 10 + tagSize),
                                songId,
                                version
                            )
                        }
                    } else {
                        raf.skipBytes(size.toInt())
                    }
                }
            }
            
            return null
        }
    }
    
    // ==================== TTA / Musepack ====================
    
    /**
     * Extract lyrics from TTA file (ID3v2 tags)
     */
    private fun extractFromTta(file: File, songId: Long): Lyric? {
        RandomAccessFile(file, "r").use { raf ->
            val header = ByteArray(4)
            raf.read(header)
            
            if (String(header, Charsets.ISO_8859_1) == "ID3 ") {
                return extractId3FromStart(file, songId)
            }
            
            // TTA1 signature
            if (String(header, Charsets.ISO_8859_1) == "TTA1") {
                // Skip to end for ID3v1 or check for ID3v2
                return extractId3FromStart(file, songId)
            }
            
            return null
        }
    }
    
    /**
     * Extract lyrics from Musepack file (APEv2 tags)
     */
    private fun extractFromMusepack(file: File, songId: Long): Lyric? {
        return extractApeTagFromEnd(file, songId)
    }
    
    /**
     * Extract ID3v2 tag from start of file
     */
    private fun extractId3FromStart(file: File, songId: Long): Lyric? {
        RandomAccessFile(file, "r").use { raf ->
            val header = ByteArray(3)
            raf.read(header)
            
            if (String(header, Charsets.ISO_8859_1) == "ID3") {
                val version = raf.readByte().toInt() and 0xFF
                raf.skipBytes(1) // flags
                
                val size = readSyncSafeInt(raf)
                val data = ByteArray(size)
                raf.read(data)
                
                return parseId3Frames(data, songId, version)
            }
            
            return null
        }
    }
    
    // ==================== Utility functions ====================
    
    private fun getCharset(encoding: Int): Charset {
        return when (encoding) {
            0 -> Charsets.ISO_8859_1
            1 -> Charsets.UTF_16
            2 -> Charsets.UTF_16BE
            3 -> Charsets.UTF_8
            4 -> Charsets.UTF_8  // UTF-8 with BOM
            else -> Charsets.ISO_8859_1
        }
    }
    
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
    
    private fun getNullTerminatorSize(encoding: Int): Int {
        return if (encoding == 1 || encoding == 2) 2 else 1
    }
    
    private fun readSyncSafeInt(raf: RandomAccessFile): Int {
        val bytes = ByteArray(4)
        raf.read(bytes)
        return readSyncSafeIntFromBytes(bytes, 0)
    }
    
    private fun readSyncSafeIntFromBytes(bytes: ByteArray, offset: Int): Int {
        return ((bytes[offset].toInt() and 0x7F) shl 21) or
               ((bytes[offset + 1].toInt() and 0x7F) shl 14) or
               ((bytes[offset + 2].toInt() and 0x7F) shl 7) or
               (bytes[offset + 3].toInt() and 0x7F)
    }
    
    private fun readBigEndianInt(data: ByteArray, offset: Int): Int {
        return ((data[offset].toInt() and 0xFF) shl 24) or
               ((data[offset + 1].toInt() and 0xFF) shl 16) or
               ((data[offset + 2].toInt() and 0xFF) shl 8) or
               (data[offset + 3].toInt() and 0xFF)
    }
    
    private fun readLittleEndianInt(data: ByteArray, offset: Int): Int {
        return (data[offset].toInt() and 0xFF) or
               ((data[offset + 1].toInt() and 0xFF) shl 8) or
               ((data[offset + 2].toInt() and 0xFF) shl 16) or
               ((data[offset + 3].toInt() and 0xFF) shl 24)
    }
    
    private fun readLittleEndianLong(raf: RandomAccessFile): Long {
        val bytes = ByteArray(8)
        raf.read(bytes)
        var result = 0L
        for (i in 0 until 8) {
            result = result or ((bytes[i].toLong() and 0xFF) shl (i * 8))
        }
        return result
    }
    
    private fun readId3v22Size(data: ByteArray, offset: Int): Int {
        return ((data[offset].toInt() and 0xFF) shl 16) or
               ((data[offset + 1].toInt() and 0xFF) shl 8) or
               (data[offset + 2].toInt() and 0xFF)
    }
}
