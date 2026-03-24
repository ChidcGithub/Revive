package com.music.revive.domain.model

/**
 * Represents a single line of lyrics with timing information
 */
data class LyricLine(
    val timeMs: Long,        // Time in milliseconds
    val text: String,        // Lyric text
    val translation: String? = null  // Translation text (if available)
) : Comparable<LyricLine> {
    override fun compareTo(other: LyricLine): Int = timeMs.compareTo(other.timeMs)
}

/**
 * Represents complete lyrics for a song
 */
data class Lyric(
    val songId: Long,
    val lines: List<LyricLine>,
    val source: LyricSource,
    val hasTranslation: Boolean = lines.any { it.translation != null }
) {
    val isSynced: Boolean = lines.isNotEmpty() && lines.all { it.timeMs >= 0 }
    
    val isPlain: Boolean = lines.isNotEmpty() && lines.all { it.timeMs == 0L }
    
    val isEmpty: Boolean = lines.isEmpty()
    
    /**
     * Find the current lyric line index based on playback position
     */
    fun findCurrentLineIndex(positionMs: Long): Int {
        if (isEmpty || !isSynced) return -1
        
        var index = -1
        for (i in lines.indices) {
            if (lines[i].timeMs <= positionMs) {
                index = i
            } else {
                break
            }
        }
        return index
    }
    
    /**
     * Get plain text version of lyrics
     */
    fun toPlainText(): String {
        return lines.joinToString("\n") { it.text }
    }
    
    companion object {
        val Empty = Lyric(-1, emptyList(), LyricSource.NONE)
    }
}

/**
 * Source of the lyrics
 */
enum class LyricSource {
    NONE,           // No lyrics available
    EMBEDDED,       // Embedded in audio file (ID3 tags)
    LOCAL_FILE,     // External .lrc file
    ONLINE          // Fetched from online API
}
