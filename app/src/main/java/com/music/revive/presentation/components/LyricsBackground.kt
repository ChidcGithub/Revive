package com.music.revive.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Apple Music 风格的歌词背景
 * 特性:
 * - 基于专辑封面的动态颜色
 * - 缓慢移动的渐变光球效果
 * - 模糊背景层
 */
@Composable
fun AppleMusicLyricsBackground(
    paletteColors: PaletteColors?,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "background")
    
    // 多个光球的动画位置
    val offsetX1 by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offsetX1"
    )
    
    val offsetY1 by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(25000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offsetY1"
    )
    
    val offsetX2 by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(22000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offsetX2"
    )
    
    val offsetY2 by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(18000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offsetY2"
    )
    
    val offsetX3 by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(28000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offsetX3"
    )
    
    val offsetY3 by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(24000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offsetY3"
    )
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 绘制动态渐变光球
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    val width = size.width
                    val height = size.height
                    
                    // 基础背景色
                    drawRect(MaterialTheme.colorScheme.background)
                    
                    if (paletteColors != null) {
                        // 主色调光球（最大）
                        val center1 = Offset(width * offsetX1, height * offsetY1)
                        val radius1 = width * 0.8f
                        
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    paletteColors.dominant.copy(alpha = 0.25f),
                                    paletteColors.dominant.copy(alpha = 0.08f),
                                    Color.Transparent
                                ),
                                center = center1,
                                radius = radius1
                            ),
                            center = center1,
                            radius = radius1
                        )
                        
                        // 鲜艳色调光球
                        val center2 = Offset(width * offsetX2, height * offsetY2)
                        val radius2 = width * 0.6f
                        
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    paletteColors.vibrant.copy(alpha = 0.2f),
                                    paletteColors.vibrant.copy(alpha = 0.05f),
                                    Color.Transparent
                                ),
                                center = center2,
                                radius = radius2
                            ),
                            center = center2,
                            radius = radius2
                        )
                        
                        // 暗色调光球
                        val center3 = Offset(width * offsetX3, height * offsetY3)
                        val radius3 = width * 0.5f
                        
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    paletteColors.darkVibrant.copy(alpha = 0.15f),
                                    paletteColors.darkVibrant.copy(alpha = 0.03f),
                                    Color.Transparent
                                ),
                                center = center3,
                                radius = radius3
                            ),
                            center = center3,
                            radius = radius3
                        )
                    } else {
                        // 默认光球（无专辑封面时）
                        val primaryColor = MaterialTheme.colorScheme.primary
                        
                        val center1 = Offset(width * offsetX1, height * offsetY1)
                        val radius1 = width * 0.7f
                        
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    primaryColor.copy(alpha = 0.15f),
                                    primaryColor.copy(alpha = 0.05f),
                                    Color.Transparent
                                ),
                                center = center1,
                                radius = radius1
                            ),
                            center = center1,
                            radius = radius1
                        )
                    }
                }
        )
        
        // 模糊层（性能优化：只在需要时应用）
        if (paletteColors != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .blur(40.dp)
            )
        }
        
        // 顶部渐变遮罩
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background.copy(alpha = 0.8f),
                            MaterialTheme.colorScheme.background.copy(alpha = 0.3f),
                            Color.Transparent
                        )
                    )
                )
        )
        
        // 底部渐变遮罩
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            MaterialTheme.colorScheme.background.copy(alpha = 0.3f),
                            MaterialTheme.colorScheme.background.copy(alpha = 0.8f)
                        )
                    )
                )
        )
    }
}

/**
 * 动画播放指示器 - Apple Music 风格
 */
@Composable
fun AnimatedPlayingIndicator(
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        PlayingBar(isActive = isPlaying, delay = 0)
        PlayingBar(isActive = isPlaying, delay = 150)
        PlayingBar(isActive = isPlaying, delay = 300)
        PlayingBar(isActive = isPlaying, delay = 450)
    }
}

@Composable
private fun PlayingBar(
    isActive: Boolean,
    delay: Int,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "playing")
    
    val height by infiniteTransition.animateFloat(
        initialValue = 4f,
        targetValue = if (isActive) 18f else 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 700,
                delayMillis = delay,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "barHeight"
    )
    
    Box(
        modifier = modifier
            .width(3.5.dp)
            .height(height.dp)
            .background(
                color = if (isActive) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                },
                shape = RoundedCornerShape(2.dp)
            )
    )
}
