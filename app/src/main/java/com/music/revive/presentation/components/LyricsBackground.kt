package com.music.revive.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.music.revive.presentation.theme.AlbumColors

/**
 * Apple Music Style Lyrics Background
 * 
 * Creates a dynamic, animated background for lyrics display.
 * Features:
 * - Multiple animated color orbs
 * - Smooth gradient transitions
 * - Optimized for performance
 */
@Composable
fun AppleMusicLyricsBackground(
    colors: AlbumColors?,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "lyrics")
    
    // Animated positions for each color orb
    val offsetX1 by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offsetX1"
    )
    
    val offsetY1 by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(15000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offsetY1"
    )
    
    val offsetX2 by infiniteTransition.animateFloat(
        initialValue = 0.75f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(18000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offsetX2"
    )
    
    val offsetY2 by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(16000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offsetY2"
    )
    
    val offsetX3 by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(22000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offsetX3"
    )
    
    val offsetY3 by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 0.55f,
        animationSpec = infiniteRepeatable(
            animation = tween(14000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offsetY3"
    )

    Box(modifier = modifier.fillMaxSize()) {
        if (colors != null) {
            // Base gradient
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                colors.primary.copy(alpha = 0.12f),
                                colors.secondary.copy(alpha = 0.06f),
                                Color.Black
                            ),
                            center = Offset(offsetX1 * 1000f, offsetY1 * 1000f),
                            radius = 1200f
                        )
                    )
            )
            
            // Vibrant color orb (top area)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .blur(70.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                colors.primary.copy(alpha = 0.35f),
                                colors.primary.copy(alpha = 0.12f),
                                Color.Transparent
                            ),
                            center = Offset(offsetX1 * 1000f, offsetY1 * 1000f),
                            radius = 700f
                        )
                    )
            )
            
            // Secondary color orb (right area)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .blur(60.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                colors.secondary.copy(alpha = 0.25f),
                                colors.secondary.copy(alpha = 0.08f),
                                Color.Transparent
                            ),
                            center = Offset(offsetX2 * 1000f, offsetY2 * 1000f),
                            radius = 600f
                        )
                    )
            )
            
            // Tertiary color orb (bottom area)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .blur(65.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                colors.tertiary.copy(alpha = 0.2f),
                                colors.secondary.copy(alpha = 0.06f),
                                Color.Transparent
                            ),
                            center = Offset(offsetX3 * 1000f, offsetY3 * 1000f),
                            radius = 550f
                        )
                    )
            )
        } else {
            // Fallback gradient when no palette colors
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF1a1a1a),
                                Color.Black,
                                Color(0xFF0d0d0d)
                            )
                        )
                    )
            )
        }
    }
}

/**
 * Simple gradient overlay for fading edges
 */
@Composable
fun GradientFadeOverlay(
    isTop: Boolean,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color.Black
) {
    Box(
        modifier = modifier.background(
            Brush.verticalGradient(
                colors = if (isTop) {
                    listOf(
                        backgroundColor,
                        backgroundColor.copy(alpha = 0.6f),
                        backgroundColor.copy(alpha = 0.3f),
                        Color.Transparent
                    )
                } else {
                    listOf(
                        Color.Transparent,
                        backgroundColor.copy(alpha = 0.3f),
                        backgroundColor.copy(alpha = 0.6f),
                        backgroundColor
                    )
                }
            )
        )
    )
}
