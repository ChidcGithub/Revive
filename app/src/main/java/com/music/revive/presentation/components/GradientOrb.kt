package com.music.revive.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlin.math.sin

/**
 * Animated Gradient Orb
 * 
 * Creates a floating, animated color blob for ambient background effects.
 * Used in Apple Music-style now playing screens.
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
    
    // Position animation - slow drift using sine waves
    val offsetX by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 100f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 8000,
                easing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1.0f)
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offsetX"
    )
    
    val offsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 80f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 7000 + (animationPhase * 2000),
                easing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1.0f)
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offsetY"
    )
    
    // Scale animation - subtle breathing effect
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 6000 + (animationPhase * 1500),
                easing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1.0f)
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    
    // Alpha animation - gentle pulsing
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 5000 + (animationPhase * 1000),
                easing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1.0f)
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    
    // Calculate position with phase offset
    val time = remember { mutableStateOf(0f) }
    LaunchedEffect(Unit) {
        while (true) {
            time.value += 0.01f
            kotlinx.coroutines.delay(16)
        }
    }
    
    val finalOffsetX = offsetX + sin(time.value + animationPhase) * 50
    val finalOffsetY = offsetY + sin(time.value * 1.2f + animationPhase) * 40
    
    Box(
        modifier = modifier
            .size(size.dp)
            .offset { 
                androidx.compose.ui.unit.IntOffset(
                    finalOffsetX.toInt(),
                    finalOffsetY.toInt()
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
                        color.copy(alpha = 0.8f),
                        color.copy(alpha = 0.3f),
                        Color.Transparent
                    ),
                    radius = size.dp.toPx() * scale
                ),
                shape = CircleShape
            )
    )
}

/**
 * Multiple Gradient Orbs Layer
 * 
 * Creates a layer of multiple animated gradient orbs for rich ambient backgrounds.
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
                size = 280 + (index * 40),
                animationPhase = index * 0.5f,
                blurRadius = 100,
                modifier = Modifier
                    .align(
                        when (index % 3) {
                            0 -> Alignment.TopStart
                            1 -> Alignment.TopEnd
                            2 -> Alignment.BottomCenter
                            else -> Alignment.Center
                        }
                    )
            )
        }
    }
}
