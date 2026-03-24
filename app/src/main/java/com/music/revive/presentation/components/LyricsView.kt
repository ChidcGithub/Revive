package com.music.revive.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.revive.R
import com.music.revive.domain.model.Lyric
import com.music.revive.domain.model.LyricLine
import kotlinx.coroutines.launch

/**
 * Main lyrics display component with time-synchronized scrolling
 */
@Composable
fun LyricsView(
    lyric: Lyric,
    currentPositionMs: Long,
    fontSizeMultiplier: Float = 1.0f,
    showTranslation: Boolean = true,
    isCentered: Boolean = true,
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
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Lyrics,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Text(
                text = stringResource(R.string.no_lyrics),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(R.string.no_lyrics_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

/**
 * Synced lyrics view with auto-scrolling and highlighting
 */
@Composable
private fun SyncedLyricsView(
    lyric: Lyric,
    currentPositionMs: Long,
    fontSizeMultiplier: Float,
    showTranslation: Boolean,
    isCentered: Boolean,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    
    // Track user scrolling to prevent auto-scroll interference
    var isUserScrolling by remember { mutableStateOf(false) }
    var lastUserScrollTime by remember { mutableLongStateOf(0L) }
    
    // Find current line index
    val currentLineIndex = remember(currentPositionMs, lyric.lines) {
        lyric.findCurrentLineIndex(currentPositionMs)
    }
    
    // Auto-scroll to current line
    LaunchedEffect(currentLineIndex) {
        if (currentLineIndex >= 0 && !isUserScrolling) {
            // Scroll to center the current line
            val visibleItems = listState.layoutInfo.visibleItemsInfo
            if (visibleItems.isNotEmpty()) {
                val centerOffset = listState.layoutInfo.viewportEndOffset / 2
                listState.animateScrollToItem(
                    index = currentLineIndex,
                    scrollOffset = -centerOffset + (visibleItems.first().size / 2)
                )
            }
        }
    }
    
    // Reset user scrolling flag after delay
    LaunchedEffect(isUserScrolling) {
        if (isUserScrolling) {
            kotlinx.coroutines.delay(3000)
            isUserScrolling = false
        }
    }
    
    Box(modifier = modifier.fillMaxSize()) {
        // Gradient overlays for smooth edges
        GradientOverlay(
            isTop = true,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(48.dp)
        )
        
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
                top = 100.dp,
                bottom = 100.dp
            ),
            horizontalAlignment = if (isCentered) Alignment.CenterHorizontally else Alignment.Start
        ) {
            itemsIndexed(
                items = lyric.lines,
                key = { index, _ -> index }
            ) { index, line ->
                val isActive = index == currentLineIndex
                val distance = if (currentLineIndex >= 0) (index - currentLineIndex).absoluteValue else Int.MAX_VALUE
                
                LyricLineItem(
                    line = line,
                    isActive = isActive,
                    distance = distance,
                    fontSizeMultiplier = fontSizeMultiplier,
                    showTranslation = showTranslation && lyric.hasTranslation,
                    textAlign = if (isCentered) TextAlign.Center else TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        
        GradientOverlay(
            isTop = false,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(48.dp)
        )
    }
}

/**
 * Individual lyric line item
 */
@Composable
private fun LyricLineItem(
    line: LyricLine,
    isActive: Boolean,
    distance: Int,
    fontSizeMultiplier: Float,
    showTranslation: Boolean,
    textAlign: TextAlign,
    modifier: Modifier = Modifier
) {
    // Animation for active state
    val scale by animateFloatAsState(
        targetValue = if (isActive) 1.05f else 1f,
        animationSpec = tween(300),
        label = "scale"
    )
    
    val alpha by animateFloatAsState(
        targetValue = when {
            isActive -> 1f
            distance == 1 -> 0.7f
            distance == 2 -> 0.5f
            distance == 3 -> 0.3f
            distance > 3 -> 0.2f
            else -> 0.5f
        },
        animationSpec = tween(200),
        label = "alpha"
    )
    
    val textColor = if (isActive) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onBackground
    }
    
    Column(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            }
            .padding(vertical = 12.dp),
        horizontalAlignment = when (textAlign) {
            TextAlign.Center -> Alignment.CenterHorizontally
            else -> Alignment.Start
        }
    ) {
        Text(
            text = line.text,
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = 18.sp * fontSizeMultiplier,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
            ),
            color = textColor,
            textAlign = textAlign,
            modifier = Modifier.fillMaxWidth()
        )
        
        // Translation
        AnimatedVisibility(
            visible = showTranslation && line.translation != null,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(200))
        ) {
            line.translation?.let { translation ->
                Text(
                    text = translation,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.sp * fontSizeMultiplier
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
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
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            colorScheme.background,
            colorScheme.background.copy(alpha = 0f)
        ),
        startY = if (isTop) 0f else Float.POSITIVE_INFINITY,
        endY = if (isTop) Float.POSITIVE_INFINITY else 0f
    )
    
    Box(
        modifier = modifier.background(backgroundGradient)
    )
}

// Extension property to get absolute value
private val Int.absoluteValue: Int
    get() = if (this < 0) -this else this
