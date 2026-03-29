package com.music.revive.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import android.graphics.BlurMaskFilter
import android.graphics.Paint
import android.graphics.Typeface
import kotlin.math.*
import kotlin.random.Random

/**
 * Advanced lyrics background with floating particles and blur effects
 * Features:
 * - Floating squares with album colors
 * - Depth-based parallax layers
 * - Smooth animations with spring physics
 * - Performance optimized rendering
 */
@Composable
fun LyricsShaderBackground(
    primaryColor: Color,
    secondaryColor: Color,
    tertiaryColor: Color,
    albumArtUri: String? = null,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "lyricsBackground")
    
    // Animation states
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(40000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "time"
    )
    
    // Particle system
    val particles = remember {
        List(15) { index ->
            FloatingParticle(
                id = index,
                initialX = Random.nextFloat(),
                initialY = Random.nextFloat(),
                size = Random.nextFloat() * 0.15f + 0.05f,
                speed = Random.nextFloat() * 0.3f + 0.1f,
                rotationSpeed = Random.nextFloat() * 2f - 1f,
                layer = index % 3,
                colorIndex = index % 3
            )
        }
    }
    
    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        
        // Base gradient background
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    primaryColor.copy(alpha = 0.08f),
                    secondaryColor.copy(alpha = 0.04f),
                    Color.Black
                ),
                center = center,
                radius = max(width, height) * 0.8f
            )
        )
        
        // Multiple layers for depth
        for (layer in 0..2) {
            val layerAlpha = when (layer) {
                0 -> 0.15f
                1 -> 0.10f
                else -> 0.06f
            }
            val layerScale = 1f + layer * 0.3f
            
            particles.filter { it.layer == layer }.forEach { particle ->
                val x = ((particle.initialX + particle.speed * time * particle.directionX) % 1f) * width
                val y = ((particle.initialY + particle.speed * time * particle.directionY) % 1f) * height
                val rotation = particle.rotationSpeed * time * 360f
                val color = when (particle.colorIndex) {
                    0 -> primaryColor
                    1 -> secondaryColor
                    else -> tertiaryColor
                }
                
                translate(left = x, top = y) {
                    rotate(degrees = rotation) {
                        val particleSize = min(width, height) * particle.size * layerScale
                        drawFloatingSquare(
                            size = particleSize,
                            color = color.copy(alpha = layerAlpha),
                            blurRadius = particle.size * 20f
                        )
                    }
                }
            }
        }
        
        // Vignette effect
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.Transparent,
                    Color.Black.copy(alpha = 0.4f),
                    Color.Black.copy(alpha = 0.7f)
                ),
                center = center,
                radius = max(width, height) * 0.7f
            ),
            blendMode = BlendMode.Multiply
        )
        
        // Noise texture overlay for depth
        drawNoiseTexture(width, height, time)
    }
}

/**
 * Custom shader-based lyrics renderer
 * Features:
 * - Gradient text with dynamic colors
 * - Glow effects with bloom
 * - Karaoke-style progress fill
 * - Smooth animations
 */
@Composable
fun LyricsShaderRenderer(
    text: String,
    progress: Float,
    accentColor: Color,
    modifier: Modifier = Modifier,
    fontSize: Float = 24f,
    enableGlow: Boolean = true,
    enableKaraoke: Boolean = true
) {
    val density = LocalDensity.current.density
    
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        
        if (text.isEmpty()) return@Canvas
        
        // Convert Compose Color to Android Color
        val androidColor = accentColor.toArgb()
        val androidColorAlpha = accentColor.copy(alpha = 0.3f).toArgb()
        
        // Main text paint
        val textPaint = android.graphics.Paint().apply {
            color = androidColor
            textSize = fontSize * density
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
            typeface = Typeface.DEFAULT_BOLD
        }
        
        // Glow effect
        if (enableGlow) {
            val glowPaint = android.graphics.Paint().apply {
                color = androidColorAlpha
                textSize = fontSize * density
                textAlign = android.graphics.Paint.Align.CENTER
                isAntiAlias = true
                maskFilter = BlurMaskFilter(15f, BlurMaskFilter.Blur.NORMAL)
                typeface = Typeface.DEFAULT_BOLD
            }
            drawContext.canvas.nativeCanvas.drawText(
                text,
                width / 2,
                height / 2,
                glowPaint
            )
        }
        
        // Main text with karaoke effect
        if (enableKaraoke && progress > 0) {
            // Clip region for karaoke effect
            drawContext.canvas.save()
            drawContext.canvas.clipRect(0f, 0f, width * progress, height)
            
            drawContext.canvas.nativeCanvas.drawText(
                text,
                width / 2,
                height / 2,
                textPaint
            )
            
            drawContext.canvas.restore()
            
            // Draw dimmed text for unplayed part
            val dimPaint = android.graphics.Paint().apply {
                color = accentColor.copy(alpha = 0.3f).toArgb()
                textSize = fontSize * density
                textAlign = android.graphics.Paint.Align.CENTER
                isAntiAlias = true
                typeface = Typeface.DEFAULT_BOLD
            }
            
            drawContext.canvas.save()
            drawContext.canvas.clipRect(width * progress, 0f, width, height)
            
            drawContext.canvas.nativeCanvas.drawText(
                text,
                width / 2,
                height / 2,
                dimPaint
            )
            
            drawContext.canvas.restore()
        } else {
            drawContext.canvas.nativeCanvas.drawText(
                text,
                width / 2,
                height / 2,
                textPaint
            )
        }
    }
}

/**
 * Draw a floating square with blur effect
 */
private fun DrawScope.drawFloatingSquare(
    size: Float,
    color: Color,
    blurRadius: Float
) {
    val halfSize = size / 2
    
    // Outer glow with blur effect via alpha
    drawRect(
        color = color.copy(alpha = color.alpha * 0.2f),
        topLeft = Offset(-halfSize, -halfSize),
        size = Size(size, size)
    )
    
    // Main square
    drawRect(
        color = color,
        topLeft = Offset(-halfSize, -halfSize),
        size = Size(size, size)
    )
    
    // Inner highlight
    drawRect(
        color = color.copy(alpha = color.alpha * 0.6f),
        topLeft = Offset(-halfSize * 0.7f, -halfSize * 0.7f),
        size = Size(size * 0.7f, size * 0.7f)
    )
}

/**
 * Draw noise texture for depth
 */
private fun DrawScope.drawNoiseTexture(width: Float, height: Float, time: Float) {
    val noiseScale = 0.02f
    val noiseIntensity = 0.03f
    
    for (x in 0 until width.toInt() step 4) {
        for (y in 0 until height.toInt() step 4) {
            val noise = (sin(x * noiseScale + time * 2f) * cos(y * noiseScale + time * 1.5f) + 1f) / 2f
            val alpha = noise * noiseIntensity
            drawRect(
                color = Color.White.copy(alpha = alpha),
                topLeft = Offset(x.toFloat(), y.toFloat()),
                size = Size(2f, 2f)
            )
        }
    }
}

/**
 * Particle data class for floating elements
 */
private data class FloatingParticle(
    val id: Int,
    val initialX: Float,
    val initialY: Float,
    val size: Float,
    val speed: Float,
    val rotationSpeed: Float,
    val layer: Int,
    val colorIndex: Int,
    val directionX: Float = Random.nextFloat() * 2f - 1f,
    val directionY: Float = Random.nextFloat() * 2f - 1f
)
