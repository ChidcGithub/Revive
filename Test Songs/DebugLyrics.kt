package com.music.revive.data.lyric

import com.music.revive.domain.model.Lyric

fun main() {
    // 使用实际 FLAC 文件中的歌词内容（包含中文）
    val actualLyrics = """
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
[00:18.160]我不断犯着愚蠢的错误
""".trimIndent()

    println("=== 测试实际歌词解析 ===")
    println("输入长度：${actualLyrics.length} 字符")
    println("输入行数：${actualLyrics.lines().size}")
    println()
    
    // 检查每一行的原始内容
    println("=== 原始行内容 ===")
    for ((idx, line) in actualLyrics.lines().withIndex()) {
        println("行 $idx: ${line.length} chars - ${line.take(60)}")
    }
    println()
    
    // 解析歌词
    val lyric = LrcParser.parse(actualLyrics, songId = 1L)
    
    println("=== 解析结果 ===")
    println("总行数：${lyric.lines.size}")
    println("是否同步：${lyric.isSynced}")
    println("是否为空：${lyric.isEmpty}")
    println()
    
    println("=== 解析后的歌词行 ===")
    for ((idx, line) in lyric.lines.withIndex()) {
        val timeStr = String.format("%02d:%02d.%03d", 
            line.timeMs / 60000, 
            (line.timeMs % 60000) / 1000,
            line.timeMs % 1000)
        
        println("行 $idx: [$timeStr] (${line.timeMs}ms)")
        println("  文本：'${line.text}'")
        println("  文本类型：${line.text::class.simpleName}")
        println("  文本长度：${line.text.length}")
        println("  文本哈希：${line.text.hashCode()}")
        
        // 检查文本是否是纯数字
        if (line.text.trim().all { it.isDigit() }) {
            println("  ⚠️ 警告：文本是纯数字！")
        }
        println()
    }
    
    // 特别检查时间标签 [00:18.160] 这一行
    println("=== 特别检查第 18 秒的歌词 ===")
    val line18 = lyric.lines.find { it.timeMs == 18160L }
    if (line18 != null) {
        println("找到时间戳 18160ms 的行:")
        println("  文本：'${line18.text}'")
        println("  文本 == \"1819942\": ${line18.text == "1819942"}")
        println("  文本.toIntOrNull(): ${line18.text.trim().toIntOrNull()}")
    } else {
        println("未找到时间戳 18160ms 的行")
    }
}
