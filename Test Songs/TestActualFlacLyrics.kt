package com.music.revive.data.lyric

fun main() {
    // This is what the actual FLAC file contains (from our Python extraction)
    // Note: Chinese characters may show as garbled due to encoding
    val actualFlacLyrics = """
        [00:00.000]Loveless Generation (Explicit) - Caslow/Sierra Annie/Jack The Underdog/Ventaria
        [00:00.000]以下歌词翻译由文曲大模型提供
        [00:02.430](We're all just ** faking)
        [00:02.430](我们都在假装)
        [00:03.990](We're all just ** faking it)
        [00:03.990](我们都在强颜欢笑)
        [00:06.600]Another night went all alone
        [00:06.600]独自一人的又一个夜晚
        [00:09.320]But can't wait to used to feeling numb
        [00:09.320]却已习惯麻木的感觉
        [00:12.400]When did we got so wrong?
        [00:12.400]我们何时走错了方向
        [00:15.430]When did it stop being fun?
        [00:15.430]何时开始失去了乐趣
        [00:18.160]I don't make it nothing but hardy mistakes
    """.trimIndent()
    
    println("=== Testing with actual FLAC lyrics content ===")
    println("Input length: ${actualFlacLyrics.length} chars")
    println("Input lines: ${actualFlacLyrics.lines().size}")
    println()
    
    val lyric = LrcParser.parse(actualFlacLyrics, songId = 123L)
    
    println("Parsed result:")
    println("  Total lines: ${lyric.lines.size}")
    println("  Is synced: ${lyric.isSynced}")
    println("  Is empty: ${lyric.isEmpty}")
    println()
    
    println("All parsed lines:")
    for ((idx, line) in lyric.lines.withIndex()) {
        val timeStr = String.format("%02d:%02d.%03d", 
            line.timeMs / 60000, 
            (line.timeMs % 60000) / 1000,
            line.timeMs % 1000)
        println("  $idx: [$timeStr] (${line.timeMs}ms) ${line.text.take(50)}")
    }
    
    // Check if any line has text that's just numbers
    println()
    println("=== Checking for numeric-only text ===")
    for ((idx, line) in lyric.lines.withIndex()) {
        val trimmedText = line.text.trim()
        if (trimmedText.all { it.isDigit() }) {
            println("  WARNING: Line $idx has numeric-only text: '$trimmedText'")
        }
    }
}
