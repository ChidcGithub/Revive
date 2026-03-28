package com.music.revive.presentation.components

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.music.revive.R
import com.music.revive.domain.model.RepeatMode
import com.music.revive.domain.model.Song
import com.music.revive.presentation.theme.AlbumColors

/**
 * Apple Music Style Immersive Album Art Display with Dynamic Colors
 */
@Composable
fun ImmersiveAlbumArt(
    song: Song,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    onAlbumClick: () -> Unit = {},
    albumColors: AlbumColors? = null
) {
    var isVisible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) { isVisible = true }
    
    val albumScale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.9f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "albumScale"
    )
    
    val albumAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(500, easing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1.0f)),
        label = "albumAlpha"
    )
    
    val playingScale by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0.98f,
        animationSpec = tween(300),
        label = "playingScale"
    )
    
    // Use album colors for orbs
    val orbColors = albumColors?.let {
        listOf(
            it.primary.copy(alpha = 0.3f),
            it.secondary.copy(alpha = 0.25f),
            it.tertiary.copy(alpha = 0.2f)
        )
    } ?: listOf(
        MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
        MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f),
        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)
    )
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        // Blurred background
        if (song.albumArtUri != null) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(song.albumArtUri)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(50.dp)
                    .scale(1.3f)
                    .graphicsLayer { alpha = albumAlpha * 0.5f },
                contentScale = ContentScale.Crop
            )
        }
        
        // Gradient overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.25f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.4f)
                        )
                    )
                )
                .graphicsLayer { alpha = albumAlpha }
        )
        
        // Gradient orbs with dynamic colors
        GradientOrbsLayer(
            colors = orbColors,
            modifier = Modifier.graphicsLayer { alpha = albumAlpha * 0.4f }
        )
        
        // Foreground album art
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .aspectRatio(1f)
                .scale(albumScale * playingScale)
                .graphicsLayer {
                    shadowElevation = 24.dp.toPx()
                    shape = RoundedCornerShape(12.dp)
                }
                .clickable(onClick = onAlbumClick),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            if (song.albumArtUri != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(song.albumArtUri)
                        .crossfade(true)
                        .build(),
                    contentDescription = song.album,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MusicNote,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

/**
 * Song Information Display with Dynamic Accent Color
 */
@Composable
fun NowPlayingInfo(
    song: Song,
    modifier: Modifier = Modifier,
    onArtistClick: () -> Unit = {},
    accentColor: Color = MaterialTheme.colorScheme.primary
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = song.title,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold
            ),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onBackground
        )
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.clickable(onClick = onArtistClick)
        ) {
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = accentColor // Dynamic accent color
            )
            
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = accentColor
            )
        }
        
        if (song.album != song.title && song.album.isNotBlank()) {
            Text(
                text = song.album,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

/**
 * Progress Slider with Dynamic Accent Color
 */
@Composable
fun AppleMusicProgressSlider(
    position: Long,
    duration: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primary
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragPosition by remember { mutableLongStateOf(position) }
    
    val progress = if (duration > 0) {
        (if (isDragging) dragPosition else position).toFloat() / duration
    } else 0f

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp),
            contentAlignment = Alignment.Center
        ) {
            // Background track
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
                shape = RoundedCornerShape(1.5.dp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
            ) {}
            
            // Active progress with dynamic accent color
            Surface(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .height(3.dp),
                shape = RoundedCornerShape(1.5.dp),
                color = accentColor // Dynamic accent color
            ) {}
            
            // Thumb when dragging
            if (isDragging) {
                Surface(
                    modifier = Modifier
                        .size(14.dp)
                        .offset { 
                            androidx.compose.ui.unit.IntOffset(
                                (progress * 100).dp.roundToPx() - 7.dp.roundToPx(),
                                0
                            )
                        },
                    shape = CircleShape,
                    color = Color.White,
                    tonalElevation = 4.dp
                ) {}
            }
        }
        
        // Time labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatTime(if (isDragging) dragPosition else position),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = formatTime(duration),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Playback Controls with Dynamic Accent Color
 */
@Composable
fun AppleMusicPlaybackControls(
    isPlaying: Boolean,
    isShuffleEnabled: Boolean,
    repeatMode: RepeatMode,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primary
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Shuffle button
        DynamicSmallControlButton(
            icon = Icons.Rounded.Shuffle,
            isActive = isShuffleEnabled,
            accentColor = accentColor,
            onClick = onToggleShuffle
        )
        
        // Previous button
        IconButton(
            onClick = onPrevious,
            modifier = Modifier.size(56.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.SkipPrevious,
                contentDescription = "Previous",
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.onBackground
            )
        }
        
        // Play/Pause with dynamic accent color
        FilledIconButton(
            onClick = onPlayPause,
            modifier = Modifier
                .size(72.dp)
                .graphicsLayer { shadowElevation = 8.dp.toPx() },
            shape = CircleShape,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = accentColor, // Dynamic accent color
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
        IconButton(
            onClick = onNext,
            modifier = Modifier.size(56.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.SkipNext,
                contentDescription = "Next",
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.onBackground
            )
        }
        
        // Repeat button
        DynamicSmallControlButton(
            icon = when (repeatMode) {
                RepeatMode.ONE -> Icons.Rounded.RepeatOne
                else -> Icons.Rounded.Repeat
            },
            isActive = repeatMode != RepeatMode.OFF,
            accentColor = accentColor,
            onClick = onCycleRepeat
        )
    }
}

/**
 * Small control button with dynamic accent color
 */
@Composable
private fun DynamicSmallControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isActive: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    val containerColor by animateColorAsState(
        targetValue = if (isActive) {
            accentColor // Dynamic accent when active
        } else {
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
        },
        animationSpec = tween(200),
        label = "containerColor"
    )
    
    val contentColor by animateColorAsState(
        targetValue = if (isActive) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(200),
        label = "contentColor"
    )
    
    FilledIconButton(
        onClick = onClick,
        modifier = Modifier.size(40.dp),
        shape = CircleShape,
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = containerColor,
            contentColor = contentColor
        )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}