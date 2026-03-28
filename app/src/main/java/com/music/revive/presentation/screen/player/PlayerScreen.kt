package com.music.revive.presentation.screen.player

import android.content.Intent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.music.revive.R
import com.music.revive.data.repository.MusicRepository
import com.music.revive.domain.model.Lyric
import com.music.revive.domain.model.Playlist
import com.music.revive.domain.model.RepeatMode
import com.music.revive.domain.model.Song
import com.music.revive.presentation.components.AddToPlaylistDialog
import com.music.revive.presentation.components.CreatePlaylistDialog
import com.music.revive.presentation.screen.fullscreenlyrics.FullScreenLyricsActivity
import com.music.revive.presentation.theme.rememberAlbumColors
import com.music.revive.service.MusicPlayer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class QueueViewModel @Inject constructor(
    private val repository: MusicRepository,
    val musicPlayer: MusicPlayer
) : ViewModel() {
    val queue: StateFlow<List<Song>> = musicPlayer.queue
    val currentIndex: StateFlow<Int> = musicPlayer.playerState
        .map { it.queueIndex }
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)
}

/**
 * Apple Music Style Player Screen with Dynamic Colors
 * 
 * Design Principles:
 * - Clean, minimal layout with generous spacing
 * - Large album artwork as the visual focal point
 * - Dynamic colors extracted from album art
 * - Elegant, thin progress slider
 * - Prominent play button with album's accent color
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    onNavigateBack: () -> Unit,
    onQueueClick: () -> Unit,
    onSongDetailClick: (Long) -> Unit = {},
    onAlbumClick: (Long?) -> Unit = {},
    onArtistClick: (Long?) -> Unit = {},
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val playerState by viewModel.playerState.collectAsState()
    val isFavorite by viewModel.isFavorite.collectAsState()
    val song = playerState.currentSong ?: return

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Extract dynamic colors from album art
    val isSystemInDarkTheme = androidx.compose.foundation.isSystemInDarkTheme()
    val albumColors = rememberAlbumColors(song.albumArtUri, isSystemInDarkTheme)
    
    // Use album's primary color for accents
    val accentColor = albumColors.primary
    val accentContainerColor = albumColors.primaryContainer
    val onAccentColor = albumColors.onPrimary
    
    // Animation states
    var isVisible by remember { mutableStateOf(false) }
    val smoothEasing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1.0f)
    
    // Staggered entry animations
    val albumAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(500, easing = smoothEasing),
        label = "albumAlpha"
    )
    
    val albumScale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.9f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "albumScale"
    )
    
    val infoAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(400, 150, easing = smoothEasing),
        label = "infoAlpha"
    )
    
    val controlsAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(400, 200, easing = smoothEasing),
        label = "controlsAlpha"
    )

    // Trigger entry animation when song changes
    LaunchedEffect(song.id) {
        isVisible = true
    }

    // Slider state
    var currentSliderValue by remember { mutableFloatStateOf(0f) }
    var isUserDragging by remember { mutableStateOf(false) }

    // Dialog states
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }
    var showMoreOptionsSheet by remember { mutableStateOf(false) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }

    // Update slider value
    LaunchedEffect(playerState.position, playerState.duration) {
        if (!isUserDragging && playerState.duration > 0) {
            currentSliderValue = playerState.position.toFloat() / playerState.duration
        }
    }

    // Auto-update position
    LaunchedEffect(playerState.isPlaying) {
        while (playerState.isPlaying) {
            delay(100)
            viewModel.updatePosition()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Dynamic blurred background with album colors
        DynamicAlbumBackground(
            albumArtUri = song.albumArtUri,
            albumColors = albumColors,
            modifier = Modifier.fillMaxSize()
        )
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Top navigation bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowDown,
                        contentDescription = stringResource(R.string.back),
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                Text(
                    text = stringResource(R.string.playing),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.weight(1f))
                
                IconButton(onClick = { 
                    val intent = FullScreenLyricsActivity.newIntent(context)
                    context.startActivity(intent)
                }) {
                    Icon(
                        imageVector = Icons.Rounded.Lyrics,
                        contentDescription = stringResource(R.string.show_lyrics),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                IconButton(onClick = onQueueClick) {
                    Icon(
                        imageVector = Icons.Rounded.QueueMusic,
                        contentDescription = stringResource(R.string.queue),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Main content area
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                
                // Album Art with dynamic shadow
                DynamicAlbumArt(
                    song = song,
                    isPlaying = playerState.isPlaying,
                    albumAlpha = albumAlpha,
                    albumScale = albumScale,
                    accentColor = accentColor,
                    modifier = Modifier
                        .fillMaxWidth(0.88f)
                        .aspectRatio(1f)
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Song Info
                DynamicSongInfo(
                    song = song,
                    infoAlpha = infoAlpha,
                    accentColor = accentColor,
                    onArtistClick = { onArtistClick(song.artistId) },
                    onAlbumClick = { onAlbumClick(song.albumId) },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Bottom controls section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { alpha = controlsAlpha }
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp)
            ) {
                // Progress Slider with accent color
                DynamicProgressSlider(
                    position = playerState.position,
                    duration = playerState.duration,
                    formattedPosition = playerState.formattedPosition,
                    formattedDuration = song.formattedDuration,
                    accentColor = accentColor,
                    onSeek = { viewModel.seekTo(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Main Playback Controls with dynamic accent
                DynamicPlaybackControls(
                    isPlaying = playerState.isPlaying,
                    isShuffleEnabled = playerState.isShuffleEnabled,
                    repeatMode = playerState.repeatMode,
                    accentColor = accentColor,
                    onPlayPause = { viewModel.playPause() },
                    onPrevious = { viewModel.playPrevious() },
                    onNext = { viewModel.playNext() },
                    onToggleShuffle = { viewModel.toggleShuffle() },
                    onCycleRepeat = { viewModel.cycleRepeatMode() },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Bottom Action Row
                DynamicBottomActions(
                    song = song,
                    isFavorite = isFavorite,
                    accentColor = accentColor,
                    onToggleFavorite = { viewModel.toggleFavorite() },
                    onAddToPlaylist = { showAddToPlaylistDialog = true },
                    onMoreOptions = { showMoreOptionsSheet = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp)
                )
            }
        }
    }

    // Dialogs
    if (showAddToPlaylistDialog) {
        val playlistViewModel: PlayerPlaylistViewModel = hiltViewModel()
        val playlists by playlistViewModel.playlists.collectAsState()
        
        AddToPlaylistDialog(
            playlists = playlists,
            onDismiss = { showAddToPlaylistDialog = false },
            onPlaylistSelected = { playlistId ->
                playlistViewModel.addSongToPlaylist(playlistId, song.id)
                showAddToPlaylistDialog = false
                Toast.makeText(context, context.getString(R.string.added_to_playlist), Toast.LENGTH_SHORT).show()
            },
            onCreateNew = {
                showAddToPlaylistDialog = false
                showCreatePlaylistDialog = true
            }
        )
    }

    if (showCreatePlaylistDialog) {
        val playlistViewModel: PlayerPlaylistViewModel = hiltViewModel()
        CreatePlaylistDialog(
            onDismiss = { showCreatePlaylistDialog = false },
            onConfirm = { name ->
                playlistViewModel.createPlaylistAndAddSong(name, song.id)
                showCreatePlaylistDialog = false
                Toast.makeText(context, context.getString(R.string.playlist_created), Toast.LENGTH_SHORT).show()
            }
        )
    }

    // More options bottom sheet
    if (showMoreOptionsSheet) {
        DynamicMoreOptionsSheet(
            song = song,
            onDismiss = { showMoreOptionsSheet = false },
            onAlbumClick = {
                showMoreOptionsSheet = false
                onAlbumClick(song.albumId)
            },
            onArtistClick = {
                showMoreOptionsSheet = false
                onArtistClick(song.artistId)
            },
            onSongDetailClick = {
                showMoreOptionsSheet = false
                onSongDetailClick(song.id)
            },
            onShare = {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "audio/*"
                    putExtra(Intent.EXTRA_STREAM, android.net.Uri.parse(song.path))
                    putExtra(Intent.EXTRA_SUBJECT, song.title)
                    putExtra(Intent.EXTRA_TEXT, "${song.title} - ${song.artist}")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.share)))
            }
        )
    }
}

/**
 * Dynamic background with album art blur and color orbs
 */
@Composable
private fun DynamicAlbumBackground(
    albumArtUri: String?,
    albumColors: com.music.revive.presentation.theme.AlbumColors,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // Animated gradient positions
    val infiniteTransition = rememberInfiniteTransition(label = "background")
    
    val gradientOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "gradientOffset"
    )

    Box(modifier = modifier) {
        // Blurred album art background
        if (albumArtUri != null) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(albumArtUri)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(80.dp)
                    .graphicsLayer {
                        scaleX = 1.5f
                        scaleY = 1.5f
                        alpha = 0.4f
                    },
                contentScale = ContentScale.Crop
            )
        }
        
        // Gradient overlay for depth
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.3f),
                            Color.Black.copy(alpha = 0.5f),
                            Color.Black.copy(alpha = 0.7f),
                            Color.Black.copy(alpha = 0.85f)
                        )
                    )
                )
        )
        
        // Dynamic color orbs from album colors
        ColorOrb(
            color = albumColors.primary,
            offsetX = 0.2f + gradientOffset * 0.1f,
            offsetY = 0.3f,
            size = 400f,
            alpha = 0.15f,
            modifier = Modifier.fillMaxSize()
        )
        
        ColorOrb(
            color = albumColors.secondary,
            offsetX = 0.7f - gradientOffset * 0.1f,
            offsetY = 0.6f,
            size = 350f,
            alpha = 0.12f,
            modifier = Modifier.fillMaxSize()
        )
    }
}

/**
 * Animated color orb
 */
@Composable
private fun ColorOrb(
    color: Color,
    offsetX: Float,
    offsetY: Float,
    size: Float,
    alpha: Float,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            color.copy(alpha = alpha),
                            color.copy(alpha = alpha * 0.5f),
                            Color.Transparent
                        ),
                        center = Offset(size * offsetX, size * offsetY),
                        radius = size
                    ),
                    center = Offset(size * offsetX, size * offsetY),
                    radius = size
                )
            }
    )
}

/**
 * Album art with dynamic shadow color
 */
@Composable
private fun DynamicAlbumArt(
    song: Song,
    isPlaying: Boolean,
    albumAlpha: Float,
    albumScale: Float,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val playingScale by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0.98f,
        animationSpec = tween(300, easing = LinearEasing),
        label = "playingScale"
    )
    
    val shadowElevation by animateDpAsState(
        targetValue = if (isPlaying) 40.dp else 24.dp,
        animationSpec = tween(300),
        label = "shadowElevation"
    )
    
    Surface(
        modifier = modifier
            .graphicsLayer {
                scaleX = albumScale * playingScale
                scaleY = albumScale * playingScale
                this.alpha = albumAlpha
                this.shadowElevation = shadowElevation.toPx()
                shape = RoundedCornerShape(12.dp)
                clip = true
            },
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        if (song.albumArtUri != null) {
            AsyncImage(
                model = song.albumArtUri,
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
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}

/**
 * Song info with dynamic accent color
 */
@Composable
private fun DynamicSongInfo(
    song: Song,
    infoAlpha: Float,
    accentColor: Color,
    onArtistClick: () -> Unit,
    onAlbumClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .graphicsLayer { alpha = infoAlpha }
            .padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = song.title,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            ),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground
        )
        
        Spacer(modifier = Modifier.height(6.dp))
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
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
                modifier = Modifier.size(18.dp),
                tint = accentColor
            )
        }
        
        if (song.album.isNotBlank() && song.album != song.title) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = song.album,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.clickable(onClick = onAlbumClick)
            )
        }
        
        // Audio quality badge
        if (song.audioQuality != com.music.revive.domain.model.AudioQuality.UNKNOWN) {
            Spacer(modifier = Modifier.height(12.dp))
            AudioQualityBadge(quality = song.audioQuality)
        }
    }
}

/**
 * Progress slider with dynamic accent color
 */
@Composable
private fun DynamicProgressSlider(
    position: Long,
    duration: Long,
    formattedPosition: String,
    formattedDuration: String,
    accentColor: Color,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragPosition by remember { mutableLongStateOf(position) }
    
    val progress = if (duration > 0) {
        (if (isDragging) dragPosition else position).toFloat() / duration
    } else 0f

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .clickable { },
            contentAlignment = Alignment.Center
        ) {
            val interactionSource = remember { MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()
            
            // Background track
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (isPressed || isDragging) 5.dp else 3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f))
            )
            
            // Active track with accent color
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .height(if (isPressed || isDragging) 5.dp else 3.dp)
                    .align(Alignment.CenterStart)
                    .clip(RoundedCornerShape(2.dp))
                    .background(accentColor) // Dynamic accent
            )
            
            // Thumb
            if (isPressed || isDragging) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = (progress * 100).dp - 8.dp)
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .graphicsLayer {
                            shadowElevation = 4.dp.toPx()
                        }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formattedPosition,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = formattedDuration,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Playback controls with dynamic accent color
 */
@Composable
private fun DynamicPlaybackControls(
    isPlaying: Boolean,
    isShuffleEnabled: Boolean,
    repeatMode: RepeatMode,
    accentColor: Color,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Shuffle button
        DynamicControlButton(
            icon = Icons.Rounded.Shuffle,
            isActive = isShuffleEnabled,
            accentColor = accentColor,
            size = 40.dp,
            onClick = onToggleShuffle
        )
        
        // Previous button
        IconButton(
            onClick = onPrevious,
            modifier = Modifier.size(56.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.SkipPrevious,
                contentDescription = stringResource(R.string.previous),
                modifier = Modifier.size(36.dp),
                tint = MaterialTheme.colorScheme.onBackground
            )
        }
        
        // Play/Pause - uses dynamic accent color
        FilledIconButton(
            onClick = onPlayPause,
            modifier = Modifier
                .size(72.dp)
                .graphicsLayer {
                    shadowElevation = 8.dp.toPx()
                    shape = CircleShape
                },
            shape = CircleShape,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = accentColor, // Dynamic accent color
                contentColor = Color.White
            )
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = if (isPlaying) stringResource(R.string.pause) else stringResource(R.string.play),
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
                contentDescription = stringResource(R.string.next),
                modifier = Modifier.size(36.dp),
                tint = MaterialTheme.colorScheme.onBackground
            )
        }
        
        // Repeat button
        DynamicControlButton(
            icon = when (repeatMode) {
                RepeatMode.ONE -> Icons.Rounded.RepeatOne
                else -> Icons.Rounded.Repeat
            },
            isActive = repeatMode != RepeatMode.OFF,
            accentColor = accentColor,
            size = 40.dp,
            onClick = onCycleRepeat
        )
    }
}

/**
 * Control button with dynamic accent color
 */
@Composable
private fun DynamicControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isActive: Boolean,
    accentColor: Color,
    size: Dp,
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
        targetValue = if (isActive) {
            Color.White
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(200),
        label = "contentColor"
    )
    
    FilledIconButton(
        onClick = onClick,
        modifier = Modifier.size(size),
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

/**
 * Bottom action row
 */
@Composable
private fun DynamicBottomActions(
    song: Song,
    isFavorite: Boolean,
    accentColor: Color,
    onToggleFavorite: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onMoreOptions: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onToggleFavorite) {
            Icon(
                imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                contentDescription = if (isFavorite) stringResource(R.string.remove_from_favorites) else stringResource(R.string.add_to_favorites),
                tint = if (isFavorite) accentColor else MaterialTheme.colorScheme.onSurfaceVariant // Dynamic accent when favorited
            )
        }
        
        IconButton(onClick = onAddToPlaylist) {
            Icon(
                imageVector = Icons.Rounded.PlaylistAdd,
                contentDescription = stringResource(R.string.add_to_playlist),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        IconButton(onClick = onMoreOptions) {
            Icon(
                imageVector = Icons.Rounded.MoreHoriz,
                contentDescription = stringResource(R.string.more_options),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Audio quality badge
 */
@Composable
private fun AudioQualityBadge(
    quality: com.music.revive.domain.model.AudioQuality
) {
    val (backgroundColor, textColor) = when (quality) {
        com.music.revive.domain.model.AudioQuality.HI_RES -> Color(0xFFFFD60A) to Color.Black
        com.music.revive.domain.model.AudioQuality.LOSSLESS -> Color(0xFFBF5AF2) to Color.White
        com.music.revive.domain.model.AudioQuality.EXTREME -> Color(0xFF30D158) to Color.White
        else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }
    
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = backgroundColor
    ) {
        Text(
            text = quality.shortLabel,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold
            ),
            color = textColor,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

/**
 * More options bottom sheet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DynamicMoreOptionsSheet(
    song: Song,
    onDismiss: () -> Unit,
    onAlbumClick: () -> Unit,
    onArtistClick: () -> Unit,
    onSongDetailClick: () -> Unit,
    onShare: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            ListItem(
                headlineContent = {
                    Text(
                        text = song.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                },
                supportingContent = {
                    Text(
                        text = song.artist,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                leadingContent = {
                    Surface(
                        modifier = Modifier.size(56.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        if (song.albumArtUri != null) {
                            AsyncImage(
                                model = song.albumArtUri,
                                contentDescription = null,
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
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            )
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            if (song.albumId != null) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.go_to_album)) },
                    leadingContent = { Icon(Icons.Rounded.Album, contentDescription = null) },
                    modifier = Modifier.clickable(onClick = onAlbumClick)
                )
            }

            if (song.artistId != null) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.go_to_artist)) },
                    leadingContent = { Icon(Icons.Rounded.Person, contentDescription = null) },
                    modifier = Modifier.clickable(onClick = onArtistClick)
                )
            }

            ListItem(
                headlineContent = { Text(stringResource(R.string.song_info)) },
                leadingContent = { Icon(Icons.Rounded.Info, contentDescription = null) },
                modifier = Modifier.clickable(onClick = onSongDetailClick)
            )

            ListItem(
                headlineContent = { Text(stringResource(R.string.share)) },
                leadingContent = { Icon(Icons.Rounded.Share, contentDescription = null) },
                modifier = Modifier.clickable(onClick = onShare)
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * Queue screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueScreen(
    onNavigateBack: () -> Unit,
    viewModel: QueueViewModel = hiltViewModel()
) {
    val queue by viewModel.queue.collectAsState()
    val playerState by viewModel.musicPlayer.playerState.collectAsState()
    val currentIndex = playerState.queueIndex
    val listState = rememberLazyListState()

    LaunchedEffect(queue) {
        if (currentIndex >= 0 && currentIndex < queue.size) {
            listState.animateScrollToItem(currentIndex)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = stringResource(R.string.queue),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { paddingValues ->
        if (queue.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Rounded.QueueMusic,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.queue_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                state = listState
            ) {
                itemsIndexed(queue) { index, song ->
                    DynamicQueueItem(
                        song = song,
                        isPlaying = index == currentIndex,
                        position = index + 1,
                        onClick = { viewModel.musicPlayer.playSong(song, queue) },
                        onRemove = { viewModel.musicPlayer.removeFromQueue(index) }
                    )
                }
            }
        }
    }
}

/**
 * Queue item
 */
@Composable
private fun DynamicQueueItem(
    song: Song,
    isPlaying: Boolean,
    position: Int,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }
    
    val itemAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(250, position * 20),
        label = "itemAlpha"
    )

    LaunchedEffect(Unit) { isVisible = true }

    ListItem(
        headlineContent = {
            Text(
                text = song.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                fontWeight = if (isPlaying) FontWeight.SemiBold else FontWeight.Normal
            )
        },
        supportingContent = {
            Text(
                text = song.artist,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        leadingContent = {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(6.dp),
                color = if (isPlaying) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (isPlaying) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            repeat(3) {
                                Box(
                                    modifier = Modifier
                                        .width(3.dp)
                                        .height((8 + it * 4).dp)
                                        .background(
                                            MaterialTheme.colorScheme.primary,
                                            RoundedCornerShape(1.dp)
                                        )
                                )
                            }
                        }
                    } else {
                        Text(
                            text = position.toString(),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        trailingContent = {
            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = stringResource(R.string.remove),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        modifier = Modifier
            .clickable(onClick = onClick)
            .graphicsLayer { alpha = itemAlpha }
    )
}

@HiltViewModel
class PlayerPlaylistViewModel @Inject constructor(
    private val repository: MusicRepository
) : ViewModel() {
    private val _playlists = MutableStateFlow<List<Playlist>>(emptyList())
    val playlists: StateFlow<List<Playlist>> = _playlists.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getAllPlaylists().collect { _playlists.value = it }
        }
    }

    fun addSongToPlaylist(playlistId: Long, songId: Long) {
        viewModelScope.launch {
            repository.addSongToPlaylist(playlistId, songId)
        }
    }

    fun createPlaylistAndAddSong(name: String, songId: Long) {
        viewModelScope.launch {
            val playlistId = repository.createPlaylist(name)
            repository.addSongToPlaylist(playlistId, songId)
        }
    }
}
