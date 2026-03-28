package com.music.revive.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.music.revive.R
import com.music.revive.domain.model.Song
import com.music.revive.presentation.theme.AppleMusicPrimary
import com.music.revive.presentation.theme.NowPlayingTitleStyle

/**
 * Apple Music Style Immersive Album Art Display
 * 
 * Features:
 * - Full-bleed blurred background
 * - Foreground album art with shadow
 * - Animated gradient orbs behind
 */
@Composable
fun ImmersiveAlbumArt(
    song: Song,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    onAlbumClick: () -> Unit = {}
) {
    var isVisible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        isVisible = true
    }
    
    // Animation states
    val albumScale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.85f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "albumScale"
    )
    
    val albumAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(600, easing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1.0f)),
        label = "albumAlpha"
    )
    
    // Subtle rotation when playing
    val rotation by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ).let { if (!isPlaying) tween(1000) else it },
        label = "rotation"
    )
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        // Blurred background
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(song.albumArtUri)
                .crossfade(true)
                .build(),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .blur(40.dp)
                .scale(1.2f)
                .graphicsLayer {
                    alpha = albumAlpha
                },
            contentScale = ContentScale.Crop,
            alpha = 0.6f
        )
        
        // Gradient overlay for depth
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.3f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.5f)
                        )
                    )
                )
                .graphicsLayer {
                    alpha = albumAlpha
                }
        )
        
        // Animated gradient orbs (ambient effect)
        GradientOrbsLayer(
            colors = listOf(
                AppleMusicPrimary.copy(alpha = 0.4f),
                Color.Blue.copy(alpha = 0.3f),
                Color.Magenta.copy(alpha = 0.2f)
            ),
            modifier = Modifier.graphicsLayer {
                this.alpha = albumAlpha * 0.5f
            }
        )
        
        // Foreground album art
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .aspectRatio(1f)
                .scale(albumScale)
                .graphicsLayer {
                    shadowElevation = 20.dp.toPx()
                    shape = RoundedCornerShape(12.dp)
                }
                .clickable(onClick = onAlbumClick),
            shape = RoundedCornerShape(12.dp),
            color = Color.Transparent
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(song.albumArtUri)
                    .crossfade(true)
                    .build(),
                contentDescription = song.album,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
    }
}

/**
 * Apple Music Style Song Information Display
 */
@Composable
fun NowPlayingInfo(
    song: Song,
    modifier: Modifier = Modifier,
    onArtistClick: () -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Title
        Text(
            text = song.title,
            style = NowPlayingTitleStyle,
            fontWeight = FontWeight(700),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        // Artist with clickable interaction
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight(510),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.clickable(onClick = onArtistClick)
            )
            
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        // Album name (if different from title)
        if (song.album != song.title) {
            Text(
                text = song.album,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight(510),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

/**
 * Apple Music Style Progress Slider
 */
@Composable
fun AppleMusicProgressSlider(
    position: Long,
    duration: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragPosition by remember { mutableStateOf(position) }
    
    val progress = if (duration > 0) position.toFloat() / duration else 0f
    
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        // Custom slider track
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp) // Larger touch target
                .clickable { }
        ) {
            // Background track
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .align(Alignment.Center),
                shape = RoundedCornerShape(1.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f)
            ) {}
            
            // Active progress
            Surface(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .height(2.dp)
                    .align(Alignment.CenterStart),
                shape = RoundedCornerShape(1.dp),
                color = AppleMusicPrimary
            ) {}
            
            // Thumb (visible on hover/drag)
            Surface(
                modifier = Modifier
                    .size(12.dp)
                    .align(Alignment.CenterStart)
                    .offset(x = ((progress * 100).toInt().dp - 6.dp)),
                shape = CircleShape,
                color = Color.White,
                tonalElevation = 2.dp
            ) {}
        }
        
        // Time labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatTime(position),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight(510),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = formatTime(duration),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight(510),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Apple Music Style Playback Controls
 */
@Composable
fun AppleMusicPlaybackControls(
    isPlaying: Boolean,
    isShuffleEnabled: Boolean,
    repeatMode: com.music.revive.domain.model.RepeatMode,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Shuffle button
        FilledIconButton(
            onClick = onToggleShuffle,
            modifier = Modifier.size(40.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = if (isShuffleEnabled) AppleMusicPrimary else Color.Transparent,
                contentColor = if (isShuffleEnabled) Color.White else MaterialTheme.colorScheme.onSurface
            )
        ) {
            Icon(
                imageVector = Icons.Rounded.Shuffle,
                contentDescription = "Shuffle",
                modifier = Modifier.size(20.dp)
            )
        }
        
        // Previous button
        FilledTonalIconButton(
            onClick = onPrevious,
            modifier = Modifier.size(56.dp),
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        ) {
            Icon(
                imageVector = Icons.Rounded.SkipPrevious,
                contentDescription = "Previous",
                modifier = Modifier.size(28.dp)
            )
        }
        
        // Play/Pause button (larger)
        FilledIconButton(
            onClick = onPlayPause,
            modifier = Modifier.size(72.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = AppleMusicPrimary,
                contentColor = Color.White
            )
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                modifier = Modifier.size(36.dp)
            )
        }
        
        // Next button
        FilledTonalIconButton(
            onClick = onNext,
            modifier = Modifier.size(56.dp),
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        ) {
            Icon(
                imageVector = Icons.Rounded.SkipNext,
                contentDescription = "Next",
                modifier = Modifier.size(28.dp)
            )
        }
        
        // Repeat button
        FilledIconButton(
            onClick = onCycleRepeat,
            modifier = Modifier.size(40.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = when (repeatMode) {
                    com.music.revive.domain.model.RepeatMode.ONE -> AppleMusicPrimary
                    com.music.revive.domain.model.RepeatMode.ALL -> AppleMusicPrimary
                    else -> Color.Transparent
                },
                contentColor = if (repeatMode != com.music.revive.domain.model.RepeatMode.OFF) Color.White else MaterialTheme.colorScheme.onSurface
            )
        ) {
            Icon(
                imageVector = Icons.Rounded.Repeat,
                contentDescription = "Repeat",
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}
