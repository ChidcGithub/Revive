package com.music.revive.presentation.components

import android.content.Context
import android.graphics.BlurMaskFilter
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.revive.R
import com.music.revive.domain.model.Lyric
import com.music.revive.domain.model.LyricLine
import kotlinx.coroutines.launch

/**
 * Main lyrics display component with BetterLyrics-inspired effects:
 * - Karaoke gradient sweep (line-level or word-level)
 * - Dynamic glow effect on active line
 * - Spring-based animations for scale and alpha
 * - Immersive background integration
 */
@Composable
fun LyricsView(
    lyric: Lyric,
    currentPositionMs: Long,
    fontSizeMultiplier: Float = 1.0f,
    showTranslation: Boolean = true,
    isCentered: Boolean = true,
    enableGlow: Boolean = true,
    enableKaraoke: Boolean = true,
    enableHapticFeedback: Boolean = true,
    enableBlur: Boolean = false,
    enableShader: Boolean = false,
    enableBalancedLines: Boolean = false,
    blurRadius: Float = 5f,
    blurTransitionDistance: Int = 3,
    modifier: Modifier = Modifier
) {
    if (lyric.isEmpty) {
        EmptyLyricsView(modifier = modifier)
    } else if (lyric.isSynced) {
        SyncedLyricsView(
            lyric = lyric,
            currentPositionMs = currentPositionMs,
            fontSizeMultiplier = fontSizeMultiplier,
            showTranslation = showTranslation,
            isCentered = isCentered,
            enableGlow = enableGlow,
            enableKaraoke = enableKaraoke,
            enableHapticFeedback = enableHapticFeedback,
            enableBlur = enableBlur,
            enableShader = enableShader,
            enableBalancedLines = enableBalancedLines,
            blurRadius = blurRadius,
            blurTransitionDistance = blurTransitionDistance,
            modifier = modifier
        )
    } else {
        PlainLyricsView(
            lyric = lyric,
            fontSizeMultiplier = fontSizeMultiplier,
            isCentered = isCentered,
            modifier = modifier
        )
    }
}

/**
 * Empty state when no lyrics are available
 */
@Composable
fun EmptyLyricsView(
    modifier: Modifier = Modifier
) {
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
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
            Text(
                text = stringResource(R.string.no_lyrics),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
            Text(
                text = stringResource(R.string.no_lyrics_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
        }
    }
}

/**
 * Synced lyrics view with karaoke effects, glow, and spring animations
 */
@Composable
private fun SyncedLyricsView(
    lyric: Lyric,
    currentPositionMs: Long,
    fontSizeMultiplier: Float,
    showTranslation: Boolean,
    isCentered: Boolean,
    enableGlow: Boolean,
    enableKaraoke: Boolean,
    enableHapticFeedback: Boolean,
    enableBlur: Boolean,
    enableShader: Boolean,
    enableBalancedLines: Boolean,
    blurRadius: Float,
    blurTransitionDistance: Int,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val context = LocalContext.current
    val vibrator = remember { context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator }
    
    // Track user scrolling to prevent auto-scroll interference
    var isUserScrolling by remember { mutableStateOf(false) }
    var lastUserScrollTime by remember { mutableLongStateOf(0L) }
    var previousLineIndex by remember { mutableStateOf(-1) }
    
    // Find current line index
    val currentLineIndex = remember(currentPositionMs, lyric.lines) {
        lyric.findCurrentLineIndex(currentPositionMs)
    }
    
    // Haptic feedback when line changes
    LaunchedEffect(currentLineIndex) {
        if (enableHapticFeedback && currentLineIndex != previousLineIndex && currentLineIndex >= 0) {
            performHapticFeedback(vibrator, HapticFeedbackType.LINE_CHANGE)
        }
        previousLineIndex = currentLineIndex
    }
    
    // Calculate line progress for karaoke effect
    val lineProgress = remember(currentPositionMs, currentLineIndex) {
        lyric.lineProgressAt(currentLineIndex, currentPositionMs)
    }
    
    // Auto-scroll to current line with spring animation
    LaunchedEffect(currentLineIndex) {
        if (currentLineIndex >= 0 && !isUserScrolling) {
            val visibleItems = listState.layoutInfo.visibleItemsInfo
            if (visibleItems.isNotEmpty()) {
                // Calculate dynamic offset based on viewport height for better centering
                val viewportHeight = listState.layoutInfo.viewportEndOffset
                // Position current line at approximately 40% from top for better visibility
                val centerOffset = (viewportHeight * 0.4f).toInt()
                listState.animateScrollToItem(
                    index = currentLineIndex,
                    scrollOffset = -centerOffset
                )
            }
        }
    }
    
    // Reset user scrolling flag after delay
    LaunchedEffect(isUserScrolling) {
        if (isUserScrolling) {
            kotlinx.coroutines.delay(4000)
            isUserScrolling = false
        }
    }
    
    // Theme colors
    val primaryColor = MaterialTheme.colorScheme.primary
    val onBackgroundColor = MaterialTheme.colorScheme.onBackground
    val surfaceColor = MaterialTheme.colorScheme.surface
    val glowColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
    
    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
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
                top = 120.dp,
                bottom = 120.dp
            ),
            horizontalAlignment = if (isCentered) Alignment.CenterHorizontally else Alignment.Start,
            userScrollEnabled = true
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
                
                // Per-line progress: only the active line gets karaoke sweep
                val currentLineProgress = if (isActive && enableKaraoke) lineProgress else if (isActive) 1f else 0f
                
                // Word-level progress override
                val wordPositionMs = if (isActive) currentPositionMs else 0L
                
                KaraokeLyricLine(
                    line = line,
                    isActive = isActive,
                    distance = distance,
                    lineProgress = currentLineProgress,
                    currentPositionMs = wordPositionMs,
                    fontSizeMultiplier = fontSizeMultiplier,
                    showTranslation = showTranslation && lyric.hasTranslation,
                    textAlign = if (isCentered) TextAlign.Center else TextAlign.Start,
                    enableGlow = enableGlow,
                    enableKaraoke = enableKaraoke,
                    enableHapticFeedback = enableHapticFeedback,
                    enableBlur = enableBlur,
                    enableShader = enableShader,
                    enableBalancedLines = enableBalancedLines,
                    blurRadius = blurRadius,
                    blurTransitionDistance = blurTransitionDistance,
                    vibrator = vibrator,
                    primaryColor = primaryColor,
                    onBackgroundColor = onBackgroundColor,
                    glowColor = glowColor,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        
        // Top gradient fade
        GradientOverlay(
            isTop = true,
            color = surfaceColor,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(80.dp)
        )
        
        // Bottom gradient fade
        GradientOverlay(
            isTop = false,
            color = surfaceColor,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(80.dp)
        )
    }
}

/**
 * A single lyric line with karaoke gradient sweep and glow effect.
 * 
 * For word-by-word LRC: highlights each word as it's being sung
 * For standard LRC: sweeps a gradient across the entire line based on time progress
 */
@Composable
private fun KaraokeLyricLine(
    line: LyricLine,
    isActive: Boolean,
    distance: Int,
    lineProgress: Float,
    currentPositionMs: Long,
    fontSizeMultiplier: Float,
    showTranslation: Boolean,
    textAlign: TextAlign,
    enableGlow: Boolean,
    enableKaraoke: Boolean,
    enableHapticFeedback: Boolean,
    enableBlur: Boolean,
    enableShader: Boolean,
    enableBalancedLines: Boolean,
    blurRadius: Float,
    blurTransitionDistance: Int,
    vibrator: Vibrator?,
    primaryColor: Color,
    onBackgroundColor: Color,
    glowColor: Color,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val context = LocalContext.current
    
    // Calculate blur alpha based on distance from active line with enhanced transition
    val blurAlpha by animateFloatAsState(
        targetValue = when {
            isActive -> 0f
            enableBlur && distance <= blurTransitionDistance -> {
                // Enhanced smooth transition curve
                val normalizedDistance = (distance - 1).toFloat() / (blurTransitionDistance - 1)
                normalizedDistance.coerceIn(0f, 1f)
            }
            else -> 1f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "blurAlpha"
    )
    
    // Haptic feedback on line click
    val performLineHaptic = {
        if (enableHapticFeedback) {
            performHapticFeedback(vibrator, HapticFeedbackType.LINE_CLICK)
        }
    }
    
    // Spring-based scale animation (more organic than tween)
    val scale by animateFloatAsState(
        targetValue = if (isActive) 1.12f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )
    
    // Smooth alpha with distance-based cascade
    val alpha by animateFloatAsState(
        targetValue = when {
            isActive -> 1f
            distance == 1 -> 0.55f
            distance == 2 -> 0.35f
            distance == 3 -> 0.25f
            else -> 0.15f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "alpha"
    )
    
    // Animated glow intensity
    val glowAlpha by animateFloatAsState(
        targetValue = if (isActive && enableGlow) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "glowAlpha"
    )
    
    // Animated karaoke progress for smooth sweep
    val animatedLineProgress by animateFloatAsState(
        targetValue = lineProgress,
        animationSpec = if (isActive) {
            tween(durationMillis = 120, easing = FastOutSlowInEasing)
        } else {
            spring(stiffness = Spring.StiffnessHigh)
        },
        label = "lineProgress"
    )
    
    val textStyle = MaterialTheme.typography.titleLarge.copy(
        fontSize = 22.sp * fontSizeMultiplier,
        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
        textAlign = textAlign
    )
    
    val translationStyle = MaterialTheme.typography.bodyMedium.copy(
        fontSize = 14.sp * fontSizeMultiplier,
        textAlign = textAlign
    )
    
    val dimmedColor = onBackgroundColor.copy(alpha = 0.5f)
    val density = LocalDensity.current
    
    // Balanced line mode: adjust line width and spacing based on content length
    val lineWidthFraction = if (enableBalancedLines) {
        val lineLength = line.text.length
        when {
            lineLength < 10 -> 0.6f  // Short lines: narrower width for better appearance
            lineLength < 20 -> 0.8f  // Medium lines
            else -> 1.0f             // Long lines: full width
        }
    } else {
        1.0f
    }
    
    // Adjust vertical spacing based on line length
    val verticalPadding = if (enableBalancedLines) {
        val lineLength = line.text.length
        when {
            lineLength < 10 -> 12.dp  // More spacing for short lines
            lineLength < 20 -> 10.dp
            else -> 8.dp              // Less spacing for long lines
        }
    } else {
        8.dp
    }
    
    // Shader effect for active line - creates gradient shimmer effect
    val shaderAlpha by animateFloatAsState(
        targetValue = if (isActive && enableShader) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "shaderAlpha"
    )
    
    // Animated shimmer offset using InfiniteTransition for smooth continuous animation
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val animatedShimmerOffset by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerOffset"
    )
    
    Column(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            }
            .then(
                if (enableBlur && blurAlpha > 0.01f) {
                    Modifier.blur((blurRadius * blurAlpha).dp)
                } else {
                    Modifier
                }
            )
            .padding(vertical = verticalPadding)
            .then(
                if (glowAlpha > 0.01f) {
                    Modifier.drawBehind {
                        drawGlowEffect(
                            glowColor = glowColor,
                            glowAlpha = glowAlpha,
                            cornerRadius = 16.dp.toPx()
                        )
                    }
                } else {
                    Modifier
                }
            )
            .then(
                if (shaderAlpha > 0.01f && enableShader) {
                    Modifier.drawBehind {
                        drawShaderEffect(
                            primaryColor = primaryColor,
                            shaderAlpha = shaderAlpha,
                            shimmerOffset = animatedShimmerOffset,
                            cornerRadius = 16.dp.toPx()
                        )
                    }
                } else {
                    Modifier
                }
            )
            .padding(vertical = 14.dp, horizontal = 4.dp),
        horizontalAlignment = when (textAlign) {
            TextAlign.Center -> Alignment.CenterHorizontally
            else -> Alignment.Start
        }
    ) {
        if (isActive && enableKaraoke) {
            // Karaoke text with gradient sweep
            KaraokeText(
                text = line.text,
                words = line.words,
                lineProgress = animatedLineProgress,
                currentPositionMs = currentPositionMs,
                textStyle = textStyle,
                activeColor = primaryColor,
                inactiveColor = dimmedColor,
                textMeasurer = textMeasurer,
                textAlign = textAlign,
                onHapticFeedback = performLineHaptic,
                enableHapticFeedback = enableHapticFeedback,
                vibrator = vibrator,
                modifier = Modifier.fillMaxWidth(lineWidthFraction)
            )
        } else {
            // Standard text rendering
            Text(
                text = line.text,
                style = textStyle,
                color = if (isActive) primaryColor else onBackgroundColor,
                textAlign = textAlign,
                modifier = Modifier
                    .fillMaxWidth(lineWidthFraction)
                    .clickable { performLineHaptic() }
            )
        }
        
        // Translation
        AnimatedVisibility(
            visible = showTranslation && line.translation != null,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(200))
        ) {
            line.translation?.let { translation ->
                Text(
                    text = translation,
                    style = translationStyle,
                    color = if (isActive) {
                        primaryColor.copy(alpha = 0.7f)
                    } else {
                        onBackgroundColor.copy(alpha = 0.4f)
                    },
                    textAlign = textAlign,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                )
            }
        }
    }
}

/**
 * Karaoke text with gradient sweep effect.
 * Supports both line-level sweep (standard LRC) and word-level highlighting (enhanced LRC).
 * 
 * Implementation: Draws the text twice - once dimmed, then clips and draws the highlighted
 * portion on top, creating the karaoke fill effect.
 */
@Composable
private fun KaraokeText(
    text: String,
    words: List<com.music.revive.domain.model.WordSegment>?,
    lineProgress: Float,
    currentPositionMs: Long,
    textStyle: TextStyle,
    activeColor: Color,
    inactiveColor: Color,
    textMeasurer: TextMeasurer,
    textAlign: TextAlign,
    onHapticFeedback: () -> Unit,
    enableHapticFeedback: Boolean,
    vibrator: Vibrator?,
    modifier: Modifier = Modifier
) {
    val measuredText = remember(text, textStyle) {
        textMeasurer.measure(
            text = text,
            style = textStyle,
            overflow = TextOverflow.Visible
        )
    }
    
    val textWidth = measuredText.size.width.toFloat()
    val textHeight = measuredText.size.height.toFloat()
    
    // Calculate the clip width for karaoke effect
    val clipProgress = if (words != null && words.isNotEmpty()) {
        // Word-level: calculate progress based on individual word timing
        calculateWordProgress(words, currentPositionMs, measuredText)
    } else {
        // Line-level: simple linear sweep
        lineProgress
    }
    
    androidx.compose.foundation.Canvas(
        modifier = modifier
            .height(with(LocalDensity.current) { textHeight.toDp() })
            .clickable { 
                if (enableHapticFeedback) {
                    onHapticFeedback()
                }
            }
    ) {
        val canvasWidth = size.width
        
        // Calculate text x offset for alignment
        val textX = when (textAlign) {
            TextAlign.Center -> (canvasWidth - textWidth) / 2f
            TextAlign.End -> canvasWidth - textWidth
            else -> 0f
        }
        
        // Draw dimmed (inactive) text
        drawText(
            textLayoutResult = measuredText,
            color = inactiveColor,
            topLeft = Offset(textX, 0f)
        )
        
        // Draw highlighted (active) text with clip
        if (clipProgress > 0f) {
            clipRect(
                left = textX,
                top = 0f,
                right = textX + textWidth * clipProgress,
                bottom = textHeight
            ) {
                drawText(
                    textLayoutResult = measuredText,
                    color = activeColor,
                    topLeft = Offset(textX, 0f)
                )
            }
        }
    }
}

/**
 * Calculate the horizontal progress for word-by-word karaoke.
 * Maps word timing to horizontal text position using text layout measurements.
 */
private fun calculateWordProgress(
    words: List<com.music.revive.domain.model.WordSegment>,
    currentPositionMs: Long,
    textLayout: TextLayoutResult
): Float {
    if (words.isEmpty()) return 0f
    
    val fullText = words.joinToString("") { it.text }
    if (fullText.isEmpty()) return 0f
    
    val totalWidth = textLayout.size.width.toFloat()
    if (totalWidth <= 0f) return 0f
    
    var charOffset = 0
    var progressWidth = 0f
    
    for (word in words) {
        val wordStart = charOffset
        val wordEnd = charOffset + word.text.length
        
        if (currentPositionMs < word.startTimeMs) {
            // Haven't reached this word yet
            break
        }
        
        val wordProgress = word.progressAt(currentPositionMs)
        
        // Get the horizontal bounds of this word from text layout
        val wordStartX = if (wordStart < textLayout.layoutInput.text.length) {
            textLayout.getHorizontalPosition(wordStart, true)
        } else 0f
        
        val wordEndX = if (wordEnd <= textLayout.layoutInput.text.length) {
            textLayout.getHorizontalPosition(wordEnd, true)
        } else totalWidth
        
        val wordWidth = wordEndX - wordStartX
        progressWidth = wordStartX + wordWidth * wordProgress
        
        charOffset = wordEnd
    }
    
    return (progressWidth / totalWidth).coerceIn(0f, 1f)
}

/**
 * Draw a soft glow effect behind the active lyric line.
 * Uses BlurMaskFilter for a natural-looking glow.
 */
private fun DrawScope.drawGlowEffect(
    glowColor: Color,
    glowAlpha: Float,
    cornerRadius: Float
) {
    // Cache paint object to reduce allocations
    val glowPaint = android.graphics.Paint().apply {
        color = glowColor.copy(alpha = glowAlpha * 0.6f).toArgb()
        isAntiAlias = true
        maskFilter = BlurMaskFilter(
            cornerRadius * 2f,
            BlurMaskFilter.Blur.NORMAL
        )
    }
    
    drawIntoCanvas { canvas ->
        val padding = cornerRadius
        canvas.nativeCanvas.drawRoundRect(
            -padding,
            -padding * 0.5f,
            size.width + padding,
            size.height + padding * 0.5f,
            cornerRadius,
            cornerRadius,
            glowPaint
        )
    }
}

/**
 * Draw a shader-based shimmer effect for active lyric line.
 * Creates a gradient overlay with animated shimmer that flows across the text.
 */
private fun DrawScope.drawShaderEffect(
    primaryColor: Color,
    shaderAlpha: Float,
    shimmerOffset: Float,
    cornerRadius: Float
) {
    // Cache paint objects to reduce allocations
    val shaderPaint = android.graphics.Paint().apply {
        isAntiAlias = true
        alpha = (shaderAlpha * 255).toInt()
    }
    
    // Draw rounded rectangle background with shader
    val rect = android.graphics.RectF(
        0f, 0f, size.width, size.height
    )
    
    drawIntoCanvas { canvas ->
        canvas.nativeCanvas.saveLayer(rect, shaderPaint)
        
        // Create linear gradient directly with cached values
        val colors = intArrayOf(
            primaryColor.copy(alpha = 0f).toArgb(),
            primaryColor.copy(alpha = 0.3f * shaderAlpha).toArgb(),
            primaryColor.copy(alpha = 0.6f * shaderAlpha).toArgb(),
            primaryColor.copy(alpha = 0.3f * shaderAlpha).toArgb(),
            primaryColor.copy(alpha = 0f).toArgb()
        )
        val positions = floatArrayOf(0f, 0.25f, 0.5f, 0.75f, 1f)
        
        val gradient = android.graphics.LinearGradient(
            shimmerOffset * size.width,
            0f,
            (shimmerOffset + 2f) * size.width,
            size.height,
            colors,
            positions,
            android.graphics.Shader.TileMode.CLAMP
        )
        
        shaderPaint.shader = gradient
        canvas.nativeCanvas.drawRoundRect(
            rect,
            cornerRadius,
            cornerRadius,
            shaderPaint
        )
        
        canvas.nativeCanvas.restore()
    }
}

/**
 * Plain lyrics view (no timing information)
 */
@Composable
private fun PlainLyricsView(
    lyric: Lyric,
    fontSizeMultiplier: Float,
    isCentered: Boolean,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        contentPadding = PaddingValues(vertical = 48.dp),
        horizontalAlignment = if (isCentered) Alignment.CenterHorizontally else Alignment.Start
    ) {
        itemsIndexed(lyric.lines) { _, line ->
            Text(
                text = line.text,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 16.sp * fontSizeMultiplier
                ),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                textAlign = if (isCentered) TextAlign.Center else TextAlign.Start,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )
        }
    }
}

/**
 * Gradient overlay for smooth transitions at edges
 */
@Composable
private fun GradientOverlay(
    isTop: Boolean,
    color: Color = MaterialTheme.colorScheme.background,
    modifier: Modifier = Modifier
) {
    val backgroundGradient = Brush.verticalGradient(
        colors = if (isTop) {
            listOf(color, color.copy(alpha = 0f))
        } else {
            listOf(color.copy(alpha = 0f), color)
        }
    )
    
    Box(
        modifier = modifier.background(backgroundGradient)
    )
}

/**
 * Haptic feedback types for lyrics interaction
 */
enum class HapticFeedbackType {
    LINE_CHANGE,      // Light tap when current line changes
    LINE_CLICK,       // Medium click when user taps a line
    SCROLL_EDGE,      // Stronger feedback when reaching edge while scrolling
}

/**
 * Perform haptic feedback based on type and Android version
 */
private fun performHapticFeedback(vibrator: Vibrator?, type: HapticFeedbackType) {
    if (vibrator == null || !vibrator.hasVibrator()) return
    
    // Check for vibration permission on Android 13+
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        // On Android 12+, hasVibrator() already checks permissions implicitly
        // but we add an extra safety check
        try {
            vibrator.vibrate(VibrationEffect.createOneShot(1, VibrationEffect.DEFAULT_AMPLITUDE))
        } catch (e: SecurityException) {
            // Permission denied, skip haptic feedback
            return
        }
    }
    
    try {
        when (type) {
            HapticFeedbackType.LINE_CHANGE -> {
                // Light, quick tap for line change
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    vibrator.vibrate(VibrationEffect.createOneShot(15, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(15)
                }
            }
            HapticFeedbackType.LINE_CLICK -> {
                // Medium click for user interaction
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    vibrator.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(30)
                }
            }
            HapticFeedbackType.SCROLL_EDGE -> {
                // Stronger feedback for edge detection
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(50)
                }
            }
        }
    } catch (e: Exception) {
        // Ignore haptic feedback errors
    }
}
