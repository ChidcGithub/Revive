package com.music.revive.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.revive.R
import com.music.revive.domain.model.Lyric
import com.music.revive.domain.model.LyricLine
import com.music.revive.domain.model.WordSegment
import kotlinx.coroutines.launch

/**
 * Apple Music 风格全屏歌词组件
 * 特性:
 * - 逐字高亮卡拉 OK 效果
 * - 平滑的滚动动画
 * - 模糊背景效果
 * - 当前行辉光效果
 */
@Composable
fun AppleMusicLyricsView(
    lyric: Lyric,
    currentPositionMs: Long,
    fontSizeMultiplier: Float = 1.0f,
    showTranslation: Boolean = true,
    isCentered: Boolean = true,
    enableGlow: Boolean = true,
    enableKaraoke: Boolean = true,
    enableBlur: Boolean = false,
    paletteColors: PaletteColors? = null,
    modifier: Modifier = Modifier
) {
    if (lyric.isEmpty) {
        EmptyLyricsView(modifier = modifier)
        return
    }
    
    if (!lyric.isSynced) {
        PlainLyricsView(
            lyric = lyric,
            fontSizeMultiplier = fontSizeMultiplier,
            isCentered = isCentered,
            modifier = modifier
        )
        return
    }
    
    SyncedAppleMusicLyricsView(
        lyric = lyric,
        currentPositionMs = currentPositionMs,
        fontSizeMultiplier = fontSizeMultiplier,
        showTranslation = showTranslation,
        isCentered = isCentered,
        enableGlow = enableGlow,
        enableKaraoke = enableKaraoke,
        enableBlur = enableBlur,
        paletteColors = paletteColors,
        modifier = modifier
    )
}

/**
 * 同步歌词视图 - Apple Music 风格
 */
@Composable
private fun SyncedAppleMusicLyricsView(
    lyric: Lyric,
    currentPositionMs: Long,
    fontSizeMultiplier: Float,
    showTranslation: Boolean,
    isCentered: Boolean,
    enableGlow: Boolean,
    enableKaraoke: Boolean,
    enableBlur: Boolean,
    paletteColors: PaletteColors?,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var isUserScrolling by remember { mutableStateOf(false) }
    var lastUserScrollTime by remember { mutableLongStateOf(0L) }
    var previousLineIndex by remember { mutableStateOf(-1) }
    
    // 查找当前歌词行索引
    val currentLineIndex = remember(currentPositionMs, lyric.lines) {
        lyric.findCurrentLineIndex(currentPositionMs)
    }
    
    // 计算当前行的进度（用于卡拉 OK 效果）
    val lineProgress = remember(currentPositionMs, currentLineIndex) {
        lyric.lineProgressAt(currentLineIndex, currentPositionMs)
    }
    
    // 自动滚动到当前行
    LaunchedEffect(currentLineIndex) {
        if (currentLineIndex >= 0 && !isUserScrolling) {
            val viewportHeight = listState.layoutInfo.viewportEndOffset
            val centerOffset = (viewportHeight * 0.4f).toInt()
            listState.animateScrollToItem(
                index = currentLineIndex,
                scrollOffset = -centerOffset
            )
        }
    }
    
    // 用户滚动后重置标志
    LaunchedEffect(isUserScrolling) {
        if (isUserScrolling) {
            kotlinx.coroutines.delay(4000)
            isUserScrolling = false
        }
    }
    
    val primaryColor = MaterialTheme.colorScheme.primary
    val onBackgroundColor = MaterialTheme.colorScheme.onBackground
    
    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragStart = {
                            isUserScrolling = true
                            lastUserScrollTime = System.currentTimeMillis()
                        },
                        onDragEnd = {
                            lastUserScrollTime = System.currentTimeMillis()
                        }
                    ) { _, _ ->
                        isUserScrolling = true
                        lastUserScrollTime = System.currentTimeMillis()
                    }
                },
            contentPadding = PaddingValues(
                top = 80.dp,
                bottom = 80.dp
            ),
            horizontalAlignment = if (isCentered) Alignment.CenterHorizontally else Alignment.Start
        ) {
            itemsIndexed(
                items = lyric.lines,
                key = { index, _ -> index }
            ) { index, line ->
                val isActive = index == currentLineIndex
                val distance = if (currentLineIndex >= 0) {
                    kotlin.math.abs(index - currentLineIndex)
                } else {
                    Int.MAX_VALUE
                }
                
                val currentLineProgress = if (isActive && enableKaraoke) lineProgress else if (isActive) 1f else 0f
                
                AppleMusicLyricLine(
                    line = line,
                    isActive = isActive,
                    distance = distance,
                    lineProgress = currentLineProgress,
                    fontSizeMultiplier = fontSizeMultiplier,
                    showTranslation = showTranslation && lyric.hasTranslation,
                    textAlign = if (isCentered) TextAlign.Center else TextAlign.Start,
                    enableGlow = enableGlow,
                    enableKaraoke = enableKaraoke,
                    enableBlur = enableBlur,
                    primaryColor = primaryColor,
                    onBackgroundColor = onBackgroundColor,
                    paletteColors = paletteColors,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        
        // 顶部渐变淡出
        GradientFadeOverlay(
            isTop = true,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(60.dp)
        )
        
        // 底部渐变淡出
        GradientFadeOverlay(
            isTop = false,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(60.dp)
        )
    }
}

/**
 * Apple Music 风格的单行歌词组件
 */
@Composable
private fun AppleMusicLyricLine(
    line: LyricLine,
    isActive: Boolean,
    distance: Int,
    lineProgress: Float,
    fontSizeMultiplier: Float,
    showTranslation: Boolean,
    textAlign: TextAlign,
    enableGlow: Boolean,
    enableKaraoke: Boolean,
    enableBlur: Boolean,
    primaryColor: Color,
    onBackgroundColor: Color,
    paletteColors: PaletteColors?,
    modifier: Modifier = Modifier
) {
    // 基于距离的模糊效果
    val blurAlpha by animateFloatAsState(
        targetValue = when {
            isActive -> 0f
            enableBlur && distance <= 3 -> (distance - 1).toFloat() / 2f
            else -> 1f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "blurAlpha"
    )
    
    // 缩放动画
    val scale by animateFloatAsState(
        targetValue = if (isActive) 1.1f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )
    
    // 透明度动画
    val alpha by animateFloatAsState(
        targetValue = when {
            isActive -> 1f
            distance == 1 -> 0.6f
            distance == 2 -> 0.4f
            else -> 0.3f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "alpha"
    )
    
    // 辉光强度
    val glowAlpha by animateFloatAsState(
        targetValue = if (isActive && enableGlow) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "glowAlpha"
    )
    
    // 平滑的卡拉 OK 进度动画
    val animatedProgress by animateFloatAsState(
        targetValue = lineProgress,
        animationSpec = if (isActive) {
            tween(durationMillis = 100, easing = FastOutSlowInEasing)
        } else {
            spring(stiffness = Spring.StiffnessHigh)
        },
        label = "progress"
    )
    
    val textStyle = TextStyle(
        fontSize = 28.sp * fontSizeMultiplier,
        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
        textAlign = textAlign,
        lineHeight = 36.sp * fontSizeMultiplier
    )
    
    val translationStyle = TextStyle(
        fontSize = 16.sp * fontSizeMultiplier,
        fontWeight = FontWeight.Normal,
        textAlign = textAlign,
        lineHeight = 22.sp * fontSizeMultiplier
    )
    
    Column(
        modifier = modifier
            .padding(vertical = 6.dp)
            .graphicsLayer {
                this.scaleX = scale
                this.scaleY = scale
                this.alpha = alpha
            }
    ) {
        // 主歌词文本（带卡拉 OK 效果）
        if (enableKaraoke && !line.words.isNullOrEmpty()) {
            WordByWordLyrics(
                line = line,
                textStyle = textStyle,
                activeColor = primaryColor,
                inactiveColor = onBackgroundColor.copy(alpha = 0.5f),
                progress = animatedProgress,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            // 简单的整行渐变效果
            GradientText(
                text = line.text,
                style = textStyle,
                progress = animatedProgress,
                activeColor = primaryColor,
                inactiveColor = onBackgroundColor.copy(alpha = 0.5f)
            )
        }
        
        // 翻译文本
        if (showTranslation && !line.translation.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = line.translation!!,
                style = translationStyle,
                color = onBackgroundColor.copy(alpha = 0.7f),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * 逐字卡拉 OK 歌词渲染
 */
@Composable
private fun WordByWordLyrics(
    line: LyricLine,
    textStyle: TextStyle,
    activeColor: Color,
    inactiveColor: Color,
    progress: Float,
    modifier: Modifier = Modifier
) {
    val words = line.words ?: run {
        Text(
            text = line.text,
            style = textStyle,
            color = inactiveColor,
            modifier = modifier
        )
        return
    }
    
    if (words.isEmpty()) {
        Text(
            text = line.text,
            style = textStyle,
            color = inactiveColor,
            modifier = modifier
        )
        return
    }
    
    Row(modifier = modifier) {
        val totalDuration = words.sumOf { it.endTimeMs - it.startTimeMs }
        var accumulatedTime = 0L
        
        words.forEach { word ->
            val wordProgress = if (totalDuration > 0) {
                val wordStart = accumulatedTime
                val wordEnd = wordStart + (word.endTimeMs - word.startTimeMs)
                val currentTime = progress * totalDuration
                
                when {
                    currentTime < wordStart -> 0f
                    currentTime > wordEnd -> 1f
                    else -> (currentTime - wordStart) / (wordEnd - wordStart)
                }
            } else {
                0f
            }
            
            if (wordProgress > 0) {
                // 部分高亮的单词
                GradientText(
                    text = word.text,
                    style = textStyle,
                    progress = wordProgress,
                    activeColor = activeColor,
                    inactiveColor = inactiveColor
                )
            } else {
                // 未高亮的单词
                Text(
                    text = word.text,
                    style = textStyle,
                    color = inactiveColor
                )
            }
            
            accumulatedTime += (word.endTimeMs - word.startTimeMs)
        }
    }
}

/**
 * 带渐变效果的文本组件
 */
@Composable
private fun GradientText(
    text: String,
    style: TextStyle,
    progress: Float,
    activeColor: Color,
    inactiveColor: Color,
    modifier: Modifier = Modifier
) {
    if (progress <= 0) {
        Text(
            text = text,
            style = style,
            color = inactiveColor,
            modifier = modifier
        )
        return
    }
    
    if (progress >= 1) {
        Text(
            text = text,
            style = style,
            color = activeColor,
            modifier = modifier
        )
        return
    }
    
    // 使用 Box 叠加实现渐变效果
    Box(modifier = modifier) {
        // 底层：未完成颜色
        Text(
            text = text,
            style = style,
            color = inactiveColor
        )
        
        // 上层：已完成颜色（使用 clip 裁剪）
        Text(
            text = text,
            style = style,
            color = activeColor,
            modifier = Modifier
                .graphicsLayer {
                    clip = true
                }
                .drawBehind {
                    drawRect(
                        color = activeColor,
                        size = androidx.compose.ui.geometry.Size(size.width * progress, size.height)
                    )
                }
        )
    }
}

/**
 * 空歌词状态
 */
@Composable
fun EmptyLyricsView(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Lyrics,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
            Text(
                text = "暂无歌词",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

/**
 * 非同步歌词视图
 */
@Composable
fun PlainLyricsView(
    lyric: Lyric,
    fontSizeMultiplier: Float = 1.0f,
    isCentered: Boolean = true,
    modifier: Modifier = Modifier
) {
    val textStyle = MaterialTheme.typography.bodyLarge.copy(
        fontSize = 18.sp * fontSizeMultiplier,
        textAlign = if (isCentered) TextAlign.Center else TextAlign.Start
    )
    
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        contentPadding = PaddingValues(vertical = 40.dp),
        horizontalAlignment = if (isCentered) Alignment.CenterHorizontally else Alignment.Start
    ) {
        items(lyric.lines.size) { index ->
            Text(
                text = lyric.lines[index].text,
                style = textStyle,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            )
        }
    }
}

/**
 * 渐变淡出覆盖层
 */
@Composable
private fun GradientFadeOverlay(
    isTop: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.background(
            Brush.verticalGradient(
                colors = if (isTop) {
                    listOf(
                        MaterialTheme.colorScheme.background.copy(alpha = 0.9f),
                        MaterialTheme.colorScheme.background.copy(alpha = 0.5f),
                        Color.Transparent
                    )
                } else {
                    listOf(
                        Color.Transparent,
                        MaterialTheme.colorScheme.background.copy(alpha = 0.5f),
                        MaterialTheme.colorScheme.background.copy(alpha = 0.9f)
                    )
                }
            )
        )
    )
}

/**
 * 从图片 URI 提取调色板颜色
 */
suspend fun extractPaletteColors(context: android.content.Context, uri: String): PaletteColors? {
    return withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val imageLoader = coil.ImageLoader(context)
            val request = coil.request.ImageRequest.Builder(context)
                .data(uri)
                .allowHardware(false)
                .build()

            val result = imageLoader.execute(request)
            val drawable = result.drawable ?: return@withContext null
            val bitmap = drawable.toBitmap()

            val palette = androidx.palette.graphics.Palette.from(bitmap)
                .maximumColorCount(24)
                .generate()

            val defaultColor = android.graphics.Color.GRAY
            
            PaletteColors(
                dominant = Color(palette.dominantSwatch?.rgb ?: palette.getDominantColor(defaultColor)),
                vibrant = Color(palette.vibrantSwatch?.rgb ?: palette.getVibrantColor(defaultColor)),
                lightVibrant = Color(palette.lightVibrantSwatch?.rgb ?: palette.getLightVibrantColor(defaultColor)),
                darkVibrant = Color(palette.darkVibrantSwatch?.rgb ?: palette.getDarkVibrantColor(defaultColor)),
                muted = Color(palette.mutedSwatch?.rgb ?: palette.getMutedColor(defaultColor))
            )
        } catch (e: Exception) {
            null
        }
    }
}
