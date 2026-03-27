package com.music.revive.data.lyric

import com.music.revive.domain.model.LyricLine

fun main() {
    // Test case 1: Normal LRC with 3-digit milliseconds
    val testLrc1 = """
        [00:00.000]Title
        [00:02.430]Line 1
        [00:18.160]Line 2
    """.trimIndent()
    
    println("=== Test 1: Normal LRC ===")
    val lyric1 = LrcParser.parse(testLrc1, songId = 1L)
    println("Lines: ${lyric1.lines.size}")
    for (line in lyric1.lines) {
        println("  [${line.timeMs}] ${line.text}")
    }
    println()
    
    // Test case 2: What if regex doesn't match?
    val testLrc2 = """
        [00:00.000]Title
        This line has no time tag
        [00:02.430]Line with time
    """.trimIndent()
    
    println("=== Test 2: Mixed content ===")
    val lyric2 = LrcParser.parse(testLrc2, songId = 2L)
    println("Lines: ${lyric2.lines.size}")
    for (line in lyric2.lines) {
        println("  [${line.timeMs}] ${line.text}")
    }
    println()
    
    // Test case 3: Check parseTimeMs directly
    println("=== Test 3: parseTimeMs direct test ===")
    val testCases = listOf(
        Triple("00", "00", "000"),
        Triple("00", "02", "430"),
        Triple("00", "18", "160"),
        Triple("01", "21", "994"),  // Could this produce 1819942?
        Triple("18", "19", "942"),  // Or this?
    )
    
    for ((mins, secs, ms) in testCases) {
        val result = parseTimeMs(mins, secs, ms)
        println("  [$mins:$secs.$ms] -> $result ms")
        
        // Also test buggy version (no millisecond normalization)
        val buggyMs = ms.toLongOrNull() ?: 0L
        val buggyTotal = mins.toLong() * 60 * 1000 + secs.toLong() * 1000 + buggyMs
        println("    Buggy (direct): $buggyTotal ms")
    }
}

// Copy of the parseTimeMs function for testing
private fun parseTimeMs(minutes: String, seconds: String, milliseconds: String): Long {
    val mins = minutes.toLongOrNull()?.coerceAtMost(59) ?: 0L
    val secs = seconds.toLongOrNull()?.coerceAtMost(59) ?: 0L
    
    val ms = when (milliseconds.length) {
        1 -> milliseconds.toLongOrNull()?.times(100) ?: 0L
        2 -> milliseconds.toLongOrNull()?.times(10) ?: 0L
        3 -> milliseconds.toLongOrNull() ?: 0L
        else -> 0L
    }.coerceAtMost(999L)
    
    return mins * 60 * 1000 + secs * 1000 + ms
}
