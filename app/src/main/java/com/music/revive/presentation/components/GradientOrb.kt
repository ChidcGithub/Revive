package com.music.revive.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment

/**
 * Apple Music Style Gradient Orb
 * 
 * Creates floating, animated color blobs for ambient background effects.
 * Optimized for performance with minimal re-compositions.
 */
@Composable
fun GradientOrb(
    color: Color,
    size: Int = 300,
    modifier: Modifier = Modifier,
    animationPhase: Float = 0f,
    blurRadius: Int = 80
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orbAnimation")
    
    // Slow, smooth position animations
    val offsetX by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 60f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 10000 + (animationPhase * 2000).toInt(),
                easing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1.0f)
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offsetX"
    )
    
    val offsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 50f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 8000 + (animationPhase * 1500).toInt(),
                easing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1.0f)
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offsetY"
    )
    
    // Subtle breathing scale animation
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 6000 + (animationPhase * 1000).toInt(),
                easing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1.0f)
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    
    // Gentle alpha pulsing
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 5000 + (animationPhase * 800).toInt(),
                easing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1.0f)
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    
    Box(
        modifier = modifier
            .size(size.dp)
            .offset { 
                IntOffset(
                    offsetX.toInt(),
                    offsetY.toInt()
                )
            }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            }
            .blur(blurRadius.dp)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        color.copy(alpha = 0.7f),
                        color.copy(alpha = 0.3f),
                        Color.Transparent
                    ),
                    radius = size.toFloat()
                ),
                shape = CircleShape
            )
    )
}

/**
 * Apple Music Style Gradient Orbs Layer
 * 
 * Creates a layered ambient background with multiple floating color orbs.
 * Each orb has a unique animation phase for organic movement.
 */
@Composable
fun GradientOrbsLayer(
    colors: List<Color>,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        colors.forEachIndexed { index, color ->
            GradientOrb(
                color = color,
                size = 250 + (index * 35),
                animationPhase = index * 0.4f,
                blurRadius = 90,
                modifier = Modifier
                    .align(
                        when (index % 4) {
                            0 -> Alignment.TopStart
                            1 -> Alignment.TopEnd
                            2 -> Alignment.BottomStart
                            3 -> Alignment.BottomEnd
                            else -> Alignment.Center
                        }
                    )
            )
        }
    }
}

/**
 * Apple Music Style Ambient Background
 * 
 * A simplified, performant ambient background for use in player screens.
 */
@Composable
fun AppleMusicAmbientBackground(
    primaryColor: Color,
    secondaryColor: Color? = null,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ambient")
    
    val offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(25000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offset"
    )
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                // Primary color orb
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.25f),
                            primaryColor.copy(alpha = 0.1f),
                            Color.Transparent
                        ),
                        center = Offset(
                            size.width * (0.25f + offset * 0.1f),
                            size.height * (0.3f + (1f - offset) * 0.05f)
                        ),
                        radius = size.width * 0.5f
                    ),
                    center = Offset(
                        size.width * (0.25f + offset * 0.1f),
                        size.height * (0.3f + (1f - offset) * 0.05f)
                    ),
                    radius = size.width * 0.5f
                )
                
                // Secondary color orb (if provided)
                if (secondaryColor != null) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                secondaryColor.copy(alpha = 0.2f),
                                secondaryColor.copy(alpha = 0.08f),
                                Color.Transparent
                            ),
                            center = Offset(
                                size.width * (0.7f - offset * 0.1f),
                                size.height * (0.6f + offset * 0.05f)
                            ),
                            radius = size.width * 0.4f
                        ),
                        center = Offset(
                            size.width * (0.7f - offset * 0.1f),
                            size.height * (0.6f + offset * 0.05f)
                        ),
                        radius = size.width * 0.4f
                    )
                }
            }
    )
}
