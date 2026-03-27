package com.music.revive.data.lyric

import com.music.revive.domain.model.LyricLine
import com.music.revive.domain.model.Lyric
import com.music.revive.domain.model.LyricSource
import com.music.revive.domain.model.WordSegment

/**
 * Parser for LRC format lyrics
 * 
 * Standard LRC format examples:
 * [00:12.00]First line of lyrics
 * [00:17.20]Second line of lyrics
 * 
 * Enhanced LRC with translations:
 * [00:12.00]原文歌词
 * [00:12.00][翻译]Translated lyrics
 * 
 * Word-by-word (Enhanced) LRC format:
 * [00:01.00]<00:01.00>This><00:01.50> is><00:02.00> word-by-word>
 * 
 * Also supports SRT subtitle format
 */
object LrcParser {
    
    // Standard LRC time tag: [mm:ss.xx] or [mm:ss:xx] or [mm:ss.xxx]
    private val timeTagRegex = Regex("""\[(\d{1,2}):(\d{1,2})[.:](\d{1,3})\]""")
    
    // Enhanced format with translation marker
    private val translationMarkerRegex = Regex("""\[(\d{1,2}):(\d{1,2})[.:](\d{1,3})\]\[(?:翻译|trans|translation)\](.*)""", RegexOption.IGNORE_CASE)
    
    // Metadata tags like [ti:Song Title], [ar:Artist], [al:Album]
    private val metadataTagRegex = Regex("""\[(ti|ar|al|by|offset|length|re|ve):(.*)\]""", RegexOption.IGNORE_CASE)
    
    // Word-by-word time tag: <mm:ss.xx> or <mm:ss:xx>
    private val wordTimeTagRegex = Regex("""<(\d{1,2}):(\d{1,2})[.:](\d{1,3})>([^<]*)""")
    
    // SRT time format: 00:01:23,456 --> 00:01:25,789
    private val srtTimeRegex = Regex("""(\d{2}):(\d{2}):(\d{2}),(\d{3})\s*-->\s*(\d{2}):(\d{2}):(\d{2}),(\d{3})""")
    
    // SRT index line
    private val srtIndexRegex = Regex("""^\d+$""")
    
    /**
     * Parse LRC format string to Lyric object
     */
    fun parse(lrcContent: String, songId: Long, source: LyricSource = LyricSource.LOCAL_FILE): Lyric {
        if (lrcContent.isBlank()) {
            return Lyric.Empty
        }
        
        // Detect format and parse accordingly
        return when {
            isSrtFormat(lrcContent) -> parseSrt(lrcContent, songId, source)
            else -> parseLrc(lrcContent, songId, source)
        }
    }
    
    /**
     * Parse standard LRC format
     */
    private fun parseLrc(lrcContent: String, songId: Long, source: LyricSource): Lyric {
        val lines = mutableListOf<LyricLine>()
        val translationMap = mutableMapOf<Long, String>()
        var offsetMs = 0L
        
        lrcContent.lines().forEach { line ->
            val trimmedLine = line.trim()
            if (trimmedLine.isEmpty()) return@forEach
            
            // Check for offset metadata
            metadataTagRegex.find(trimmedLine)?.let { match ->
                if (match.groupValues[1].lowercase() == "offset") {
                    offsetMs = match.groupValues[2].toLongOrNull() ?: 0L
                }
                return@forEach
            }
            
            // Skip other metadata tags
            if (metadataTagRegex.matches(trimmedLine)) return@forEach
            
            // Check for translation line
            translationMarkerRegex.find(trimmedLine)?.let { match ->
                val (minutes, seconds, milliseconds) = match.destructured
                val timeMs = parseTimeMs(minutes, seconds, milliseconds)
                translationMap[timeMs] = match.groupValues[4].trim()
                return@forEach
            }
            
            // Parse standard lyric line with time tags
            parseLyricLine(trimmedLine, offsetMs)?.let { lyricLines ->
                lines.addAll(lyricLines)
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
     * Supports word-by-word format: [00:01.00]<00:01.00>This <00:01.50>is <00:02.00>word-by-word
     */
    private fun parseLyricLine(line: String, offsetMs: Long = 0L): List<LyricLine> {
        val timeTags = timeTagRegex.findAll(line).toList()
        if (timeTags.isEmpty()) return emptyList()
        
        // Extract text after all time tags
        val textStart = timeTags.last().range.last + 1
        val rawText = if (textStart < line.length) line.substring(textStart).trim() else ""
        
        if (rawText.isEmpty()) return emptyList()
        
        // Check for word-by-word format
        val wordMatches = wordTimeTagRegex.findAll(rawText).toList()
        
        if (wordMatches.isNotEmpty()) {
            // Build word segments with timing
            val wordSegments = mutableListOf<WordSegment>()
            for (i in wordMatches.indices) {
                val match = wordMatches[i]
                val minutes = match.groupValues[1]
                val seconds = match.groupValues[2]
                val milliseconds = match.groupValues[3]
                val wordText = match.groupValues[4]
                
                val startMs = parseTimeMs(minutes, seconds, milliseconds) + offsetMs
                
                // End time is the start of the next word, or estimated
                val endMs = if (i + 1 < wordMatches.size) {
                    val nextMatch = wordMatches[i + 1]
                    parseTimeMs(
                        nextMatch.groupValues[1],
                        nextMatch.groupValues[2],
                        nextMatch.groupValues[3]
                    ) + offsetMs
                } else {
                    startMs + 500L // Default 500ms for last word
                }
                
                if (wordText.isNotBlank()) {
                    wordSegments.add(WordSegment(
                        text = wordText,
                        startTimeMs = startMs,
                        endTimeMs = endMs
                    ))
                }
            }
            
            val fullText = wordSegments.joinToString("") { it.text }.trim()
            
            if (fullText.isNotEmpty()) {
                return timeTags.map { match ->
                    val minutes = match.groupValues[1]
                    val seconds = match.groupValues[2]
                    val milliseconds = match.groupValues[3]
                    val timeMs = parseTimeMs(minutes, seconds, milliseconds) + offsetMs
                    LyricLine(
                        timeMs = timeMs,
                        text = fullText,
                        words = wordSegments
                    )
                }
            }
            // Word-by-word parsing failed (empty text), fall through to standard parsing
        }
        
        // Standard format - create a lyric line for each time tag
        return timeTags.map { match ->
            val minutes = match.groupValues[1]
            val seconds = match.groupValues[2]
            val milliseconds = match.groupValues[3]
            val timeMs = parseTimeMs(minutes, seconds, milliseconds) + offsetMs
            LyricLine(timeMs = timeMs, text = rawText)
        }
    }
    
    /**
     * Parse SRT subtitle format
     */
    private fun parseSrt(srtContent: String, songId: Long, source: LyricSource): Lyric {
        val lines = mutableListOf<LyricLine>()
        val contentLines = srtContent.lines()
        var i = 0
        
        while (i < contentLines.size) {
            val line = contentLines[i].trim()
            
            // Skip empty lines
            if (line.isEmpty()) {
                i++
                continue
            }
            
            // Skip index
            if (srtIndexRegex.matches(line)) {
                i++
                continue
            }
            
            // Look for time line
            val timeMatch = srtTimeRegex.find(line)
            if (timeMatch != null) {
                val (startH, startM, startS, startMs, endH, endM, endS, endMs) = timeMatch.destructured
                val startTimeMs = parseSrtTime(startH, startM, startS, startMs)
                
                // Collect text lines until empty line or next time
                val textBuilder = StringBuilder()
                i++
                while (i < contentLines.size) {
                    val textLine = contentLines[i].trim()
                    if (textLine.isEmpty() || srtIndexRegex.matches(textLine) || srtTimeRegex.containsMatchIn(textLine)) {
                        break
                    }
                    if (textBuilder.isNotEmpty()) textBuilder.append(" ")
                    textBuilder.append(textLine)
                    i++
                }
                
                val text = textBuilder.toString().trim()
                if (text.isNotEmpty()) {
                    lines.add(LyricLine(timeMs = startTimeMs, text = text))
                }
            } else {
                i++
            }
        }
        
        return Lyric(
            songId = songId,
            lines = lines.sortedBy { it.timeMs },
            source = source
        )
    }
    
    /**
     * Parse SRT time components to milliseconds
     */
    private fun parseSrtTime(hours: String, minutes: String, seconds: String, milliseconds: String): Long {
        val h = hours.toLongOrNull() ?: 0L
        val m = minutes.toLongOrNull() ?: 0L
        val s = seconds.toLongOrNull() ?: 0L
        val ms = milliseconds.toLongOrNull() ?: 0L
        
        return h * 3600 * 1000 + m * 60 * 1000 + s * 1000 + ms
    }
    
    /**
     * Parse time components to milliseconds
     * Supports formats: mm:ss.xx, mm:ss:xx, mm:ss.xxx
     */
    private fun parseTimeMs(minutes: String, seconds: String, milliseconds: String): Long {
        val mins = minutes.toLongOrNull()?.coerceAtMost(59) ?: 0L
        val secs = seconds.toLongOrNull()?.coerceAtMost(59) ?: 0L
        
        // Properly handle milliseconds (could be 1, 2, or 3 digits)
        val ms = when (milliseconds.length) {
            1 -> milliseconds.toLongOrNull()?.times(100) ?: 0L  // "5" -> 500ms
            2 -> milliseconds.toLongOrNull()?.times(10) ?: 0L   // "20" -> 200ms
            3 -> milliseconds.toLongOrNull() ?: 0L              // "200" -> 200ms
            else -> 0L
        }.coerceAtMost(999L)
        
        return mins * 60 * 1000 + secs * 1000 + ms
    }
    
    /**
     * Convert Lyric back to LRC format string
     */
    fun toLrc(lyric: Lyric): String {
        val sb = StringBuilder()
        
        // Add metadata
        sb.append("[re:Revive]\n")
        sb.append("[ve:1.0]\n")
        
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
    
    /**
     * Check if content looks like SRT format
     */
    fun isSrtFormat(content: String): Boolean {
        return content.lines().any { line ->
            srtTimeRegex.containsMatchIn(line)
        }
    }
    
    /**
     * Detect format and return appropriate parser info
     */
    fun detectFormat(content: String): LyricFormat {
        return when {
            isSrtFormat(content) -> LyricFormat.SRT
            isLrcFormat(content) -> LyricFormat.LRC
            else -> LyricFormat.PLAIN_TEXT
        }
    }
    
    /**
     * Lyric format types
     */
    enum class LyricFormat {
        LRC,        // Standard LRC
        SRT,        // SubRip subtitle
        PLAIN_TEXT  // Plain text without timing
    }
}