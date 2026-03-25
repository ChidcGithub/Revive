package com.music.revive.domain.model

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long?,
    val artistId: Long?,
    val duration: Long,
    val path: String,
    val dateAdded: Long,
    val albumArtUri: String?,
    val bitrate: Int = 0,           // kbps
    val sampleRate: Int = 0,        // Hz
    val fileSize: Long = 0,         // bytes
    val codec: String? = null,      // Audio codec (MP3, FLAC, AAC, etc.)
    val bitsPerSample: Int = 0,     // Bits per sample (16, 24, 32)
    val channelCount: Int = 0,      // Number of audio channels
    val isFavorite: Boolean = false
) {
    val formattedDuration: String
        get() {
            val totalSeconds = duration / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return String.format("%02d:%02d", minutes, seconds)
        }

    val formattedFileSize: String
        get() {
            return when {
                fileSize >= 1024 * 1024 * 1024 -> "%.1f GB".format(fileSize / (1024.0 * 1024 * 1024))
                fileSize >= 1024 * 1024 -> "%.1f MB".format(fileSize / (1024.0 * 1024))
                fileSize >= 1024 -> "%.1f KB".format(fileSize / 1024.0)
                else -> "$fileSize B"
            }
        }

    val formattedSampleRate: String
        get() {
            return when {
                sampleRate >= 96000 -> "%.1f kHz".format(sampleRate / 1000.0)
                sampleRate >= 1000 -> "%.1f kHz".format(sampleRate / 1000.0)
                else -> "$sampleRate Hz"
            }
        }

    val formattedBitrate: String
        get() {
            return if (bitrate > 0) "${bitrate} kbps" else "未知"
        }

    val formattedChannels: String
        get() {
            return when (channelCount) {
                1 -> "单声道"
                2 -> "立体声"
                6 -> "5.1 环绕声"
                8 -> "7.1 环绕声"
                in 3..5 -> "$channelCount 声道"
                else -> if (channelCount > 0) "$channelCount 声道" else "未知"
            }
        }

    val formattedBitDepth: String
        get() {
            return if (bitsPerSample > 0) "${bitsPerSample} bit" else "未知"
        }

    val formattedCodec: String
        get() {
            return codec?.uppercase() ?: detectCodecFromPath()
        }

    /**
     * Detect codec from file extension
     */
    private fun detectCodecFromPath(): String {
        val ext = path.substringAfterLast('.', "").lowercase()
        return when (ext) {
            "mp3" -> "MP3"
            "flac" -> "FLAC"
            "m4a", "m4b", "m4p", "mp4", "aac" -> "AAC"
            "ogg", "oga" -> "OGG Vorbis"
            "opus" -> "Opus"
            "wav", "wave" -> "WAV"
            "aiff", "aif", "aifc" -> "AIFF"
            "wma", "asf" -> "WMA"
            "ape" -> "APE"
            "wv" -> "WavPack"
            "dsf", "dff" -> "DSD"
            "tta" -> "TTA"
            "mpc", "mp+", "mpp" -> "Musepack"
            else -> ext.uppercase()
        }
    }

    /**
     * Audio quality classification based on multiple factors
     */
    val audioQuality: AudioQuality
        get() = determineAudioQuality()

    private fun determineAudioQuality(): AudioQuality {
        // Hi-Res: High sample rate (> 48kHz) or high bit depth (> 16)
        if (sampleRate >= 96000 || bitsPerSample >= 24) {
            return AudioQuality.HI_RES
        }
        
        // Lossless codec detection
        val losslessCodecs = setOf("FLAC", "WAV", "AIFF", "APE", "WavPack", "ALAC", "DSD")
        if (codec?.uppercase() in losslessCodecs || detectCodecFromPath() in losslessCodecs) {
            // For lossless, check sample rate
            return if (sampleRate >= 48000) {
                AudioQuality.HI_RES
            } else {
                AudioQuality.LOSSLESS
            }
        }
        
        // Lossy codecs - check bitrate
        return when {
            bitrate >= 320 -> AudioQuality.EXTREME
            bitrate >= 256 -> AudioQuality.HIGH
            bitrate >= 192 -> AudioQuality.GOOD
            bitrate >= 128 -> AudioQuality.STANDARD
            bitrate > 0 -> AudioQuality.LOW
            else -> AudioQuality.UNKNOWN
        }
    }

    /**
     * Get a detailed quality description
     */
    val qualityDescription: String
        get() {
            val parts = mutableListOf<String>()
            parts.add(formattedCodec)
            if (bitrate > 0) parts.add(formattedBitrate)
            if (sampleRate > 0) parts.add(formattedSampleRate)
            if (bitsPerSample > 0) parts.add(formattedBitDepth)
            if (channelCount > 0) parts.add(formattedChannels)
            return parts.joinToString(" • ")
        }
}

enum class AudioQuality(val displayName: String, val shortLabel: String, val color: String) {
    HI_RES("Hi-Res", "Hi-Res", "#FFD700"),      // Gold
    LOSSLESS("无损", "SQ", "#FF9500"),          // Orange
    EXTREME("极高", "EX", "#00C853"),           // Green
    HIGH("高品质", "HQ", "#4CAF50"),            // Light Green
    GOOD("优良", "GQ", "#8BC34A"),              // Lime
    STANDARD("标准", "STD", "#9E9E9E"),         // Gray
    LOW("低品质", "LQ", "#757575"),             // Dark Gray
    UNKNOWN("未知", "N/A", "#BDBDBD");          // Light Gray

    fun isHighQuality(): Boolean = this in listOf(HI_RES, LOSSLESS, EXTREME, HIGH)
}