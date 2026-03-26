package com.music.revive.domain.model

/**
 * Represents a single word/segment within a lyric line with precise timing.
 * Used for word-by-word karaoke effects.
 */
data class WordSegment(
    val text: String,
    val startTimeMs: Long,
    val endTimeMs: Long
) {
    /**
     * Calculate progress (0.0 to 1.0) of this word at the given position.
     */
    fun progressAt(positionMs: Long): Float {
        if (positionMs <= startTimeMs) return 0f
        if (positionMs >= endTimeMs) return 1f
        val duration = (endTimeMs - startTimeMs).toFloat()
        if (duration <= 0f) return 1f
        return ((positionMs - startTimeMs).toFloat() / duration).coerceIn(0f, 1f)
    }
}

/**
 * Represents a single line of lyrics with timing information
 */
data class LyricLine(
    val timeMs: Long,        // Time in milliseconds
    val text: String,        // Lyric text
    val translation: String? = null,  // Translation text (if available)
    val words: List<WordSegment>? = null  // Word-by-word timing (if available)
) : Comparable<LyricLine> {
    override fun compareTo(other: LyricLine): Int = timeMs.compareTo(other.timeMs)
    
    /** Whether this line has word-level timing information */
    val hasWordTiming: Boolean get() = !words.isNullOrEmpty()
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
     * Calculate the progress (0.0 to 1.0) through the current line.
     * Used for line-level karaoke sweep effect when word-level timing is unavailable.
     */
    fun lineProgressAt(lineIndex: Int, positionMs: Long): Float {
        if (lineIndex < 0 || lineIndex >= lines.size) return 0f
        val lineStart = lines[lineIndex].timeMs
        val lineEnd = if (lineIndex + 1 < lines.size) lines[lineIndex + 1].timeMs else lineStart + 5000L
        val duration = (lineEnd - lineStart).toFloat()
        if (duration <= 0f) return 1f
        return ((positionMs - lineStart).toFloat() / duration).coerceIn(0f, 1f)
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
