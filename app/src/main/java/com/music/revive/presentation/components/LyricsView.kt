package com.music.revive.presentation.components

import android.content.Context
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.revive.R
import com.music.revive.domain.model.Lyric
import com.music.revive.domain.model.LyricLine
import com.music.revive.domain.model.WordSegment
import com.music.revive.presentation.theme.AlbumColors
import kotlinx.coroutines.launch

/**
 * Apple Music Style Lyrics View with Dynamic Colors
 * 
 * Design Principles:
 * - Large, prominent text for active lyrics
 * - Smooth fade and scale transitions
 * - Word-by-word karaoke effect with dynamic accent color
 * - Minimal UI distractions
 * - Centered, elegant layout
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
    useShaderRenderer: Boolean = true, // Use new shader-based renderer
    enableBalancedLines: Boolean = false,
    blurRadius: Float = 5f,
    blurTransitionDistance: Int = 3,
    modifier: Modifier = Modifier,
    accentColor: Color = Color.White // Dynamic accent color from album
) {
    if (lyric.isEmpty) {
        AppleMusicEmptyLyrics(modifier = modifier)
    } else if (lyric.isSynced) {
        AppleMusicSyncedLyrics(
            lyric = lyric,
            currentPositionMs = currentPositionMs,
            fontSizeMultiplier = fontSizeMultiplier,
            showTranslation = showTranslation,
            isCentered = isCentered,
            enableGlow = enableGlow,
            enableKaraoke = enableKaraoke,
            enableHapticFeedback = enableHapticFeedback,
            accentColor = accentColor,
            modifier = modifier
        )
    } else {
        AppleMusicPlainLyrics(
            lyric = lyric,
            fontSizeMultiplier = fontSizeMultiplier,
            isCentered = isCentered,
            modifier = modifier
        )
    }
}

/**
 * Overload for AlbumColors
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
    modifier: Modifier = Modifier,
    albumColors: AlbumColors? = null
) {
    LyricsView(
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
        modifier = modifier,
        accentColor = albumColors?.primary ?: Color.White
    )
}

/**
 * Empty state when no lyrics are available
 */
@Composable
private fun AppleMusicEmptyLyrics(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                modifier = Modifier.size(80.dp),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Lyrics,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
            
            Text(
                text = stringResource(R.string.no_lyrics),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
            
            Text(
                text = stringResource(R.string.no_lyrics_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}

/**
 * Synced lyrics with karaoke effect using dynamic accent color
 */
@Composable
private fun AppleMusicSyncedLyrics(
    lyric: Lyric,
    currentPositionMs: Long,
    fontSizeMultiplier: Float,
    showTranslation: Boolean,
    isCentered: Boolean,
    enableGlow: Boolean,
    enableKaraoke: Boolean,
    enableHapticFeedback: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val vibrator = remember { context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator }
    
    var isUserScrolling by remember { mutableStateOf(false) }
    var lastUserScrollTime by remember { mutableLongStateOf(0L) }
    var previousLineIndex by remember { mutableStateOf(-1) }
    
    val currentLineIndex = remember(currentPositionMs, lyric.lines) {
        lyric.findCurrentLineIndex(currentPositionMs)
    }
    
    val lineProgress = remember(currentPositionMs, currentLineIndex) {
        lyric.lineProgressAt(currentLineIndex, currentPositionMs)
    }
    
    // Haptic feedback
    LaunchedEffect(currentLineIndex) {
        if (enableHapticFeedback && currentLineIndex != previousLineIndex && currentLineIndex >= 0) {
            performHapticFeedback(vibrator, HapticFeedbackType.LINE_CHANGE)
        }
        previousLineIndex = currentLineIndex
    }
    
    // Auto-scroll
    LaunchedEffect(currentLineIndex) {
        if (currentLineIndex >= 0 && !isUserScrolling) {
            val viewportHeight = listState.layoutInfo.viewportEndOffset
            val targetOffset = -(viewportHeight * 0.35f).toInt()
            listState.animateScrollToItem(
                index = currentLineIndex,
                scrollOffset = targetOffset
            )
        }
    }
    
    // Reset user scrolling
    LaunchedEffect(isUserScrolling) {
        if (isUserScrolling) {
            kotlinx.coroutines.delay(4000)
            isUserScrolling = false
        }
    }
    
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
            contentPadding = PaddingValues(top = 150.dp, bottom = 150.dp),
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
                } else Int.MAX_VALUE
                
                val currentLineProgress = if (isActive && enableKaraoke) lineProgress else if (isActive) 1f else 0f
                val wordPositionMs = if (isActive) currentPositionMs else 0L
                
                AppleMusicLyricLine(
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
                    vibrator = vibrator,
                    enableHapticFeedback = enableHapticFeedback,
                    useShaderRenderer = useShaderRenderer,
                    accentColor = accentColor,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        
        // Gradient fades
        AppleMusicGradientFade(
            isTop = true,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(100.dp)
        )
        
        AppleMusicGradientFade(
            isTop = false,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(100.dp)
        )
    }
}

/**
 * Single lyric line with dynamic accent color
 */
@Composable
private fun AppleMusicLyricLine(
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
    vibrator: Vibrator?,
    enableHapticFeedback: Boolean,
    useShaderRenderer: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val smoothEasing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1.0f)
    
    // Scale animation
    val scale by animateFloatAsState(
        targetValue = when {
            isActive -> 1.0f
            distance == 1 -> 0.92f
            distance == 2 -> 0.88f
            else -> 0.85f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )
    
    // Alpha animation
    val alpha by animateFloatAsState(
        targetValue = when {
            isActive -> 1f
            distance == 1 -> 0.6f
            distance == 2 -> 0.4f
            distance == 3 -> 0.25f
            distance == 4 -> 0.15f
            else -> 0.08f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "alpha"
    )
    
    // Karaoke progress
    val animatedLineProgress by animateFloatAsState(
        targetValue = lineProgress,
        animationSpec = if (isActive) {
            tween(durationMillis = 100, easing = LinearEasing)
        } else {
            spring(stiffness = Spring.StiffnessHigh)
        },
        label = "lineProgress"
    )
    
    // Glow intensity
    val glowAlpha by animateFloatAsState(
        targetValue = if (isActive && enableGlow) 0.15f else 0f,
        animationSpec = tween(300),
        label = "glowAlpha"
    )
    
    // Text styles
    val textStyle = MaterialTheme.typography.headlineSmall.copy(
        fontSize = (24.sp * fontSizeMultiplier),
        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
        textAlign = textAlign
    )
    
    val translationStyle = MaterialTheme.typography.bodyMedium.copy(
        fontSize = (14.sp * fontSizeMultiplier),
        textAlign = textAlign
    )
    
    // Colors - use dynamic accent for active lyrics
    val activeColor = accentColor // Dynamic accent color from album
    val inactiveColor = Color.White.copy(alpha = 0.5f)
    
    Column(
        modifier = modifier
            .graphicsLayer {
                this.scaleX = scale
                this.scaleY = scale
                this.alpha = alpha
            }
            .then(
                if (glowAlpha > 0.01f) {
                    Modifier.drawBehind {
                        drawRoundRect(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    accentColor.copy(alpha = glowAlpha),
                                    Color.Transparent
                                ),
                                center = Offset(size.width / 2, size.height / 2),
                                radius = size.width * 0.6f
                            ),
                            cornerRadius = CornerRadius(16.dp.toPx())
                        )
                    }
                } else {
                    Modifier
                }
            )
            .padding(vertical = 12.dp),
        horizontalAlignment = when (textAlign) {
            TextAlign.Center -> Alignment.CenterHorizontally
            else -> Alignment.Start
        }
    ) {
        if (useShaderRenderer && isActive) {
            // Use new shader-based renderer for active line
            LyricsShaderRenderer(
                text = line.text,
                progress = animatedLineProgress,
                accentColor = activeColor,
                fontSize = textStyle.fontSize.value,
                enableGlow = enableGlow,
                enableKaraoke = enableKaraoke,
                modifier = Modifier.fillMaxWidth()
            )
        } else if (isActive && enableKaraoke) {
            AppleMusicKaraokeText(
                text = line.text,
                words = line.words,
                lineProgress = animatedLineProgress,
                currentPositionMs = currentPositionMs,
                textStyle = textStyle,
                activeColor = activeColor,
                inactiveColor = inactiveColor,
                textMeasurer = textMeasurer,
                textAlign = textAlign,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            Text(
                text = line.text,
                style = textStyle,
                color = if (isActive) activeColor else inactiveColor,
                textAlign = textAlign,
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        // Translation
        AnimatedVisibility(
            visible = showTranslation && line.translation != null,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(200))
        ) {
            line.translation?.let { translation ->
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = translation,
                    style = translationStyle,
                    color = if (isActive) {
                        accentColor.copy(alpha = 0.7f)
                    } else {
                        Color.White.copy(alpha = 0.4f)
                    },
                    textAlign = textAlign,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * Karaoke text with dynamic accent color fill
 */
@Composable
private fun AppleMusicKaraokeText(
    text: String,
    words: List<WordSegment>?,
    lineProgress: Float,
    currentPositionMs: Long,
    textStyle: TextStyle,
    activeColor: Color,
    inactiveColor: Color,
    textMeasurer: TextMeasurer,
    textAlign: TextAlign,
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
    
    val clipProgress = if (words != null && words.isNotEmpty()) {
        calculateWordProgress(words, currentPositionMs, measuredText)
    } else {
        lineProgress
    }
    
    val density = LocalDensity.current
    
    androidx.compose.foundation.Canvas(
        modifier = modifier
            .height(with(density) { textHeight.toDp() })
    ) {
        val canvasWidth = size.width
        val textX = when (textAlign) {
            TextAlign.Center -> (canvasWidth - textWidth) / 2f
            TextAlign.End -> canvasWidth - textWidth
            else -> 0f
        }
        
        // Draw inactive text
        drawText(
            textLayoutResult = measuredText,
            color = inactiveColor,
            topLeft = Offset(textX, 0f)
        )
        
        // Draw active text with clip
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
 * Calculate word progress
 */
private fun calculateWordProgress(
    words: List<WordSegment>,
    currentPositionMs: Long,
    textLayout: TextLayoutResult
): Float {
    if (words.isEmpty()) return 0f
    
    val totalWidth = textLayout.size.width.toFloat()
    if (totalWidth <= 0f) return 0f
    
    var charOffset = 0
    var progressWidth = 0f
    
    for (word in words) {
        val wordStart = charOffset
        val wordEnd = charOffset + word.text.length
        
        if (currentPositionMs < word.startTimeMs) break
        
        val wordProgress = word.progressAt(currentPositionMs)
        
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
 * Gradient fade overlay
 */
@Composable
private fun AppleMusicGradientFade(
    isTop: Boolean,
    modifier: Modifier = Modifier
) {
    val backgroundColor = MaterialTheme.colorScheme.background
    
    Box(
        modifier = modifier.background(
            Brush.verticalGradient(
                colors = if (isTop) {
                    listOf(
                        backgroundColor,
                        backgroundColor.copy(alpha = 0.8f),
                        backgroundColor.copy(alpha = 0.4f),
                        Color.Transparent
                    )
                } else {
                    listOf(
                        Color.Transparent,
                        backgroundColor.copy(alpha = 0.4f),
                        backgroundColor.copy(alpha = 0.8f),
                        backgroundColor
                    )
                }
            )
        )
    )
}

/**
 * Plain lyrics view
 */
@Composable
private fun AppleMusicPlainLyrics(
    lyric: Lyric,
    fontSizeMultiplier: Float,
    isCentered: Boolean,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        contentPadding = PaddingValues(vertical = 64.dp),
        horizontalAlignment = if (isCentered) Alignment.CenterHorizontally else Alignment.Start
    ) {
        itemsIndexed(lyric.lines) { _, line ->
            Text(
                text = line.text,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 18.sp * fontSizeMultiplier,
                    fontWeight = FontWeight.Normal
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

// Haptic feedback types
private enum class HapticFeedbackType {
    LINE_CHANGE,
    LINE_CLICK
}

private fun performHapticFeedback(vibrator: Vibrator?, type: HapticFeedbackType) {
    if (vibrator == null) return
    
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            when (type) {
                HapticFeedbackType.LINE_CHANGE -> {
                    vibrator.vibrate(VibrationEffect.createOneShot(10, VibrationEffect.DEFAULT_AMPLITUDE))
                }
                HapticFeedbackType.LINE_CLICK -> {
                    vibrator.vibrate(VibrationEffect.createOneShot(5, VibrationEffect.DEFAULT_AMPLITUDE))
                }
            }
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(10)
        }
    } catch (e: Exception) { }
}

// Compatibility functions
@Composable
fun GradientOverlay(isTop: Boolean, color: Color, modifier: Modifier = Modifier) {
    AppleMusicGradientFade(isTop = isTop, modifier = modifier.background(color))
}

fun DrawScope.drawGlowEffect(glowColor: Color, glowAlpha: Float, cornerRadius: Float) {
    drawRoundRect(color = glowColor.copy(alpha = glowAlpha), cornerRadius = CornerRadius(cornerRadius))
}

fun DrawScope.drawShaderEffect(primaryColor: Color, shaderAlpha: Float, shimmerOffset: Float, cornerRadius: Float) {
    drawRoundRect(
        brush = Brush.radialGradient(
            colors = listOf(
                primaryColor.copy(alpha = shaderAlpha * 0.5f),
                primaryColor.copy(alpha = shaderAlpha * 0.2f),
                Color.Transparent
            )
        ),
        cornerRadius = CornerRadius(cornerRadius)
    )
}