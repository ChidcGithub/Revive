package com.music.revive.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.music.revive.R
import com.music.revive.domain.model.PlayerState
import com.music.revive.domain.model.Song
import kotlinx.coroutines.delay

@Composable
fun BottomPlayerBar(
    playerState: PlayerState,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onBarClick: () -> Unit,
    onFavoriteClick: () -> Unit = {},
    isFavorite: Boolean = false,
    modifier: Modifier = Modifier
) {
    val song = playerState.currentSong ?: return

    var currentProgress by remember { mutableFloatStateOf(playerState.progress) }
    val animatedProgress by animateFloatAsState(
        targetValue = currentProgress,
        animationSpec = tween(50, easing = LinearEasing),
        label = "progress"
    )

    // Update progress based on position
    LaunchedEffect(playerState.isPlaying, playerState.position) {
        if (playerState.isPlaying) {
            while (true) {
                delay(100)
                currentProgress = playerState.progress
            }
        }
    }

    LaunchedEffect(playerState.position, playerState.duration) {
        currentProgress = playerState.progress
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onBarClick
            ),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        tonalElevation = 3.dp,
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Animated progress bar with custom drawing
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .drawBehind {
                        // Background track
                        drawRect(
                            color = androidx.compose.ui.graphics.Color.Transparent
                        )
                        // Progress with rounded cap
                        if (animatedProgress > 0) {
                            drawRoundRect(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.tertiary
                                    )
                                ),
                                topLeft = Offset.Zero,
                                size = Size(size.width * animatedProgress, size.height),
                                cornerRadius = CornerRadius(size.height / 2, size.height / 2)
                            )
                        }
                    }
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .navigationBarsPadding(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Album art with subtle elevation and playing animation
                val albumScale by animateFloatAsState(
                    targetValue = if (playerState.isPlaying) 1f else 0.95f,
                    animationSpec = tween(200),
                    label = "albumScale"
                )
                
                Surface(
                    modifier = Modifier
                        .size(52.dp)
                        .graphicsLayer {
                            scaleX = albumScale
                            scaleY = albumScale
                        },
                    shape = RoundedCornerShape(12.dp),
                    tonalElevation = 2.dp
                ) {
                    AsyncImage(
                        model = song.albumArtUri,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    if (song.albumArtUri == null) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.MusicNote,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Song info with marquee for long titles
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        modifier = Modifier.basicMarquee(
                            iterations = Int.MAX_VALUE,
                            initialDelayMillis = 3000
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = song.artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Compact controls with wave animation when playing
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Previous
                    IconButton(
                        onClick = onPreviousClick,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SkipPrevious,
                            contentDescription = stringResource(R.string.previous),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Play/Pause - Filled button for emphasis
                    FilledIconButton(
                        onClick = onPlayPauseClick,
                        modifier = Modifier.size(44.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = if (playerState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (playerState.isPlaying) stringResource(R.string.pause) else stringResource(R.string.play),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Next
                    IconButton(
                        onClick = onNextClick,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SkipNext,
                            contentDescription = stringResource(R.string.next),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Alternative mini player bar with waveform visualization
 */
@Composable
fun MiniPlayerBar(
    playerState: PlayerState,
    onPlayPauseClick: () -> Unit,
    onBarClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val song = playerState.currentSong ?: return

    var currentProgress by remember { mutableFloatStateOf(playerState.progress) }

    LaunchedEffect(playerState.isPlaying) {
        if (playerState.isPlaying) {
            while (true) {
                delay(100)
                currentProgress = playerState.progress
            }
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .clickable(onClick = onBarClick),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Waveform animation when playing
            if (playerState.isPlaying) {
                WaveformAnimation(
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .width(40.dp)
                        .height(24.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.MusicNote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            IconButton(onClick = onPlayPauseClick) {
                Icon(
                    imageVector = if (playerState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = null
                )
            }
        }
    }
}

@Composable
private fun WaveformAnimation(
    color: Color,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "waveform")

    val scale1 by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(300, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wave1"
    )

    val scale2 by transition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wave2"
    )

    val scale3 by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(350, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wave3"
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(4) { index ->
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(24.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(color.copy(alpha = when (index) {
                        0 -> scale1
                        1 -> scale2
                        2 -> scale3
                        else -> scale1 * scale2
                    }))
            )
        }
    }
}