package com.music.revive.data.lyric

import com.music.revive.domain.model.LyricLine
import com.music.revive.domain.model.Lyric
import com.music.revive.domain.model.LyricSource

/**
 * Parser for LRC format lyrics
 * 
 * LRC format examples:
 * [00:12.00]First line of lyrics
 * [00:17.20]Second line of lyrics
 * 
 * Also supports enhanced LRC with translations:
 * [00:12.00]原文歌词
 * [00:12.00][翻译]Translated lyrics
 */
object LrcParser {
    
    // Standard LRC time tag: [mm:ss.xx] or [mm:ss:xx] or [mm:ss.xxx]
    private val timeTagRegex = Regex("""\[(\d{1,2}):(\d{1,2})[.:](\d{1,3})\]""")
    
    // Enhanced format with translation marker
    private val translationMarkerRegex = Regex("""\[(\d{1,2}):(\d{1,2})[.:](\d{1,3})\]\[(?:翻译|trans|translation)\](.*)""", RegexOption.IGNORE_CASE)
    
    // Metadata tags like [ti:Song Title], [ar:Artist], [al:Album]
    private val metadataTagRegex = Regex("""\[(ti|ar|al|by|offset|length):(.*)\]""", RegexOption.IGNORE_CASE)
    
    /**
     * Parse LRC format string to Lyric object
     */
    fun parse(lrcContent: String, songId: Long, source: LyricSource = LyricSource.LOCAL_FILE): Lyric {
        if (lrcContent.isBlank()) {
            return Lyric.Empty
        }
        
        val lines = mutableListOf<LyricLine>()
        val translationMap = mutableMapOf<Long, String>()
        
        lrcContent.lines().forEach { line ->
            val trimmedLine = line.trim()
            if (trimmedLine.isEmpty()) return@forEach
            
            // Skip metadata tags
            if (metadataTagRegex.matches(trimmedLine)) return@forEach
            
            // Check for translation line
            translationMarkerRegex.find(trimmedLine)?.let { match ->
                val (minutes, seconds, milliseconds) = match.destructured
                val timeMs = parseTimeMs(minutes, seconds, milliseconds)
                translationMap[timeMs] = match.groupValues[4].trim()
                return@forEach
            }
            
            // Parse standard lyric line with time tags
            parseLyricLine(trimmedLine)?.let { lyricLine ->
                lines.add(lyricLine)
            }
        }
        
        // Merge translations into main lines
        val mergedLines = lines.map { line ->
            val translation = translationMap[line.timeMs]
            line.copy(translation = translation)
        }.sortedBy { it.timeMs }
        
        return Lyric(
            songId = songId,
            lines = mergedLines,
            source = source,
            hasTranslation = translationMap.isNotEmpty()
        )
    }
    
    /**
     * Parse a single LRC line
     * Supports multiple time tags for the same line: [00:12.00][00:45.30]Same lyrics
     */
    private fun parseLyricLine(line: String): List<LyricLine> {
        val timeTags = timeTagRegex.findAll(line).toList()
        if (timeTags.isEmpty()) return emptyList()
        
        // Extract text after all time tags
        val textStart = timeTags.last().range.last + 1
        val text = if (textStart < line.length) line.substring(textStart).trim() else ""
        
        if (text.isEmpty()) return emptyList()
        
        // Create a lyric line for each time tag
        return timeTags.map { match ->
            val (minutes, seconds, milliseconds) = match.destructured
            val timeMs = parseTimeMs(minutes, seconds, milliseconds)
            LyricLine(timeMs = timeMs, text = text)
        }
    }
    
    /**
     * Parse time components to milliseconds
     */
    private fun parseTimeMs(minutes: String, seconds: String, milliseconds: String): Long {
        val mins = minutes.toLongOrNull() ?: 0L
        val secs = seconds.toLongOrNull() ?: 0L
        val ms = milliseconds.toLongOrNull() ?: 0L
        
        // Normalize milliseconds (could be 2 or 3 digits)
        val normalizedMs = if (milliseconds.length == 2) ms * 10 else ms
        
        return mins * 60 * 1000 + secs * 1000 + normalizedMs
    }
    
    /**
     * Convert Lyric back to LRC format string
     */
    fun toLrc(lyric: Lyric): String {
        val sb = StringBuilder()
        
        lyric.lines.forEach { line ->
            val timeTag = formatTimeTag(line.timeMs)
            sb.append("$timeTag${line.text}\n")
            
            // Add translation as separate line with marker
            line.translation?.let { trans ->
                sb.append("$timeTag[翻译]$trans\n")
            }
        }
        
        return sb.toString()
    }
    
    /**
     * Format milliseconds to LRC time tag
     */
    private fun formatTimeTag(timeMs: Long): String {
        val totalSeconds = timeMs / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        val milliseconds = timeMs % 1000
        
        return "[%02d:%02d.%03d]".format(minutes, seconds, milliseconds)
    }
    
    /**
     * Check if content looks like LRC format
     */
    fun isLrcFormat(content: String): Boolean {
        return content.lines().any { line ->
            timeTagRegex.containsMatchIn(line)
        }
    }
}
