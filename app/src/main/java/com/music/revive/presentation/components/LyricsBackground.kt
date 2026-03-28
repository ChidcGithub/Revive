package com.music.revive.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.music.revive.presentation.screen.player.PaletteColors

@Composable
fun AppleMusicLyricsBackground(
    colors: PaletteColors?,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "lyrics")
    
    // Animated color orb positions
    val offsetX1 by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(15000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offsetX1"
    )
    
    val offsetY1 by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offsetY1"
    )
    
    val offsetX2 by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(18000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offsetX2"
    )
    
    val offsetY2 by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(14000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offsetY2"
    )
    
    val offsetX3 by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(16000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offsetX3"
    )
    
    val offsetY3 by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(13000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offsetY3"
    )

    Box(modifier = modifier.fillMaxSize()) {
        if (colors != null) {
            // Dynamic gradient background with moving color orbs
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                colors.dominant.copy(alpha = 0.15f),
                                colors.muted.copy(alpha = 0.08f),
                                Color.Black
                            ),
                            center = Offset(offsetX1 * 1000f, offsetY1 * 1000f),
                            radius = 1200f
                        )
                    )
            )
            
            // Moving color orb 1 - Dominant color
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .blur(60.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                colors.dominant.copy(alpha = 0.4f),
                                colors.dominant.copy(alpha = 0.1f),
                                Color.Transparent
                            ),
                            center = Offset(offsetX1 * 1000f, offsetY1 * 1000f),
                            radius = 800f
                        )
                    )
            )
            
            // Moving color orb 2 - Vibrant color
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .blur(60.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                colors.vibrant.copy(alpha = 0.3f),
                                colors.lightVibrant.copy(alpha = 0.1f),
                                Color.Transparent
                            ),
                            center = Offset(offsetX2 * 1000f, offsetY2 * 1000f),
                            radius = 700f
                        )
                    )
            )
            
            // Moving color orb 3 - Dark vibrant color
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .blur(60.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                colors.darkVibrant.copy(alpha = 0.25f),
                                colors.muted.copy(alpha = 0.08f),
                                Color.Transparent
                            ),
                            center = Offset(offsetX3 * 1000f, offsetY3 * 1000f),
                            radius = 600f
                        )
                    )
            )
        } else {
            // Fallback gradient
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Black,
                                Color(0xFF1a1a1a),
                                Color(0xFF0d0d0d)
                            )
                        )
                    )
            )
        }
    }
}

private fun DrawScope.drawColorOrb(
    center: Offset,
    radius: Float,
    color: Color,
    alpha: Float = 0.3f
) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                color.copy(alpha = alpha),
                color.copy(alpha = alpha * 0.3f),
                Color.Transparent
            ),
            center = center,
            radius = radius
        ),
        center = center,
        radius = radius
    )
}
