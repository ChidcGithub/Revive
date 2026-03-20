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
                sampleRate >= 48000 -> "%.1f kHz".format(sampleRate / 1000.0)
                else -> "$sampleRate Hz"
            }
        }

    /**
     * Audio quality classification based on bitrate
     */
    val audioQuality: AudioQuality
        get() = when {
            bitrate >= 320 -> AudioQuality.LOSSLESS
            bitrate >= 256 -> AudioQuality.HIGH
            bitrate >= 192 -> AudioQuality.MEDIUM
            bitrate >= 128 -> AudioQuality.STANDARD
            bitrate > 0 -> AudioQuality.LOW
            else -> AudioQuality.UNKNOWN
        }
}

enum class AudioQuality(val displayName: String, val shortLabel: String) {
    LOSSLESS("无损音质", "SQ"),
    HIGH("高品质", "HQ"),
    MEDIUM("中品质", "MQ"),
    STANDARD("标准品质", "STD"),
    LOW("低品质", "LQ"),
    UNKNOWN("未知", "N/A")
}
