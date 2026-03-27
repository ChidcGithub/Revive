package com.music.revive.data.lyric

fun main() {
    val testLrc = """
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
    """.trimIndent()
    
    println("=== Testing LRC Parser ===")
    println("Input length: ${testLrc.length} chars")
    println("Input lines: ${testLrc.lines().size}")
    println()
    
    val lyric = LrcParser.parse(testLrc, songId = 123L)
    
    println("Result:")
    println("  Total lines: ${lyric.lines.size}")
    println("  Is synced: ${lyric.isSynced}")
    println("  Is empty: ${lyric.isEmpty}")
    println()
    
    println("First 5 parsed lines:")
    for ((idx, line) in lyric.lines.take(5).withIndex()) {
        println("  $idx: [${line.timeMs}] ${line.text.take(40)}")
    }
}
