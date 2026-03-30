@file:OptIn(
    ExperimentalAnimationApi::class,
    ExperimentalSharedTransitionApi::class,
    ExperimentalMaterial3Api::class
)

package com.music.revive.presentation.screen.player

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.music.revive.R
import com.music.revive.data.repository.MusicRepository
import com.music.revive.domain.model.Playlist
import com.music.revive.domain.model.RepeatMode
import com.music.revive.domain.model.Song
import com.music.revive.presentation.components.AddToPlaylistDialog
import com.music.revive.presentation.components.CreatePlaylistDialog
import com.music.revive.presentation.navigation.NowPlayingSharedKeys
import com.music.revive.presentation.screen.fullscreenlyrics.FullScreenLyricsActivity
import com.music.revive.presentation.theme.rememberAlbumColors
import com.music.revive.service.MusicPlayer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

// ============================================================================
// Animation Specs - Apple Music Style Smooth Animations
// ============================================================================

private val AppleMusicSpring = spring<Float>(
    dampingRatio = Spring.DampingRatioMediumBouncy,
    stiffness = Spring.StiffnessMediumLow
)

private val AppleMusicTween = tween<Float>(
    durationMillis = 300,
    easing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1.0f)
)

private val AppleMusicColorTween = tween<Color>(
    durationMillis = 500,
    easing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1.0f)
)

// ============================================================================
// ViewModels (PlayerViewModel is in separate file: PlayerViewModel.kt)
// ============================================================================

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
        viewModelScope.launch { repository.addSongToPlaylist(playlistId, songId) }
    }

    fun createPlaylistAndAddSong(name: String, songId: Long) {
        viewModelScope.launch {
            val playlistId = repository.createPlaylist(name)
            repository.addSongToPlaylist(playlistId, songId)
        }
    }
}

// ============================================================================
// Main Player Screen - Apple Music Style
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    onNavigateBack: () -> Unit,
    onQueueClick: () -> Unit,
    onSongDetailClick: (Long) -> Unit = {},
    onAlbumClick: (Long?) -> Unit = {},
    onArtistClick: (Long?) -> Unit = {},
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedContentScope: AnimatedContentScope? = null,
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val playerState by viewModel.playerState.collectAsState()
    val isFavorite by viewModel.isFavorite.collectAsState()
    val song = playerState.currentSong ?: return

    val context = LocalContext.current
    val isSystemInDarkTheme = androidx.compose.foundation.isSystemInDarkTheme()
    val albumColors = rememberAlbumColors(song.albumArtUri, isSystemInDarkTheme)
    val accentColor = albumColors.primary
    val onAccentContent = albumColors.onPrimary

    // Dialog states
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }
    var showMoreOptionsSheet by remember { mutableStateOf(false) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var showQueue by remember { mutableStateOf(false) }

    // Slider state
    var currentSliderValue by remember { mutableFloatStateOf(0f) }
    var isUserDragging by remember { mutableStateOf(false) }

    // Animations
    val controlsAlpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        label = "controlsAlpha"
    )

    LaunchedEffect(playerState.position, playerState.duration, isUserDragging) {
        if (!isUserDragging && playerState.duration > 0) {
            currentSliderValue = playerState.position.toFloat() / playerState.duration
        }
    }

    LaunchedEffect(playerState.isPlaying) {
        while (playerState.isPlaying) {
            delay(100)
            viewModel.updatePosition()
        }
    }

    // Main container with gradient background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Ambient background with album art blur
        AmbientBackground(
            albumArtUri = song.albumArtUri,
            accentColor = accentColor,
            isPlaying = playerState.isPlaying,
            modifier = Modifier.fillMaxSize()
        )

        // Content column
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // 1. Top Navigation Bar (56dp height)
            TopNavigationBar(
                onBackClick = onNavigateBack,
                onLyricsClick = {
                    val intent = FullScreenLyricsActivity.newIntent(context)
                    context.startActivity(intent)
                },
                onQueueClick = { showQueue = true },
                modifier = Modifier.fillMaxWidth()
            )

            // 2. Main content area
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.weight(0.1f))

                // 3. Album Art with rotation animation
                AlbumArtSection(
                    song = song,
                    isPlaying = playerState.isPlaying,
                    accentColor = accentColor,
                    sharedTransitionScope = sharedTransitionScope,
                    animatedContentScope = animatedContentScope,
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .aspectRatio(1f)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 4. Song Info Section
                SongInfoSection(
                    song = song,
                    accentColor = accentColor,
                    onArtistClick = { onArtistClick(song.artistId) },
                    onAlbumClick = { onAlbumClick(song.albumId) },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.weight(0.15f))

                // 5. Progress Slider
                ProgressSliderSection(
                    position = playerState.position,
                    duration = playerState.duration,
                    accentColor = accentColor,
                    sliderValue = currentSliderValue,
                    isUserDragging = isUserDragging,
                    onValueChange = { v ->
                        isUserDragging = true
                        currentSliderValue = v
                    },
                    onValueChangeFinished = {
                        isUserDragging = false
                        val d = playerState.duration
                        if (d > 0) {
                            viewModel.seekTo((currentSliderValue * d).toLong().coerceIn(0L, d))
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                // 6. Playback Controls
                PlaybackControlsSection(
                    isPlaying = playerState.isPlaying,
                    isShuffleEnabled = playerState.isShuffleEnabled,
                    repeatMode = playerState.repeatMode,
                    accentColor = accentColor,
                    onAccentContent = onAccentContent,
                    onPlayPause = { viewModel.playPause() },
                    onPrevious = { viewModel.playPrevious() },
                    onNext = { viewModel.playNext() },
                    onToggleShuffle = { viewModel.toggleShuffle() },
                    onCycleRepeat = { viewModel.cycleRepeatMode() },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 7. Secondary controls (favorite, playlist, more)
                SecondaryControlsSection(
                    isFavorite = isFavorite,
                    accentColor = accentColor,
                    onToggleFavorite = { viewModel.toggleFavorite() },
                    onAddToPlaylist = { showAddToPlaylistDialog = true },
                    onMore = { showMoreOptionsSheet = true },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.weight(0.1f))
            }
        }

        // Queue bottom sheet
        if (showQueue) {
            QueueBottomSheet(
                onDismiss = { showQueue = false },
                modifier = Modifier.fillMaxSize()
            )
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

    if (showMoreOptionsSheet) {
        MoreOptionsSheet(
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

// ============================================================================
// Ambient Background
// ============================================================================

@Composable
private fun AmbientBackground(
    albumArtUri: String?,
    accentColor: Color,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // Breathing animation
    val breatheTransition = rememberInfiniteTransition(label = "breathe")
    val breatheAlpha by breatheTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = FastOutSlowInEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "breatheAlpha"
    )

    Box(modifier = modifier) {
        // Gradient overlay from surface to surfaceVariant
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
        )

        // Blurred album art
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
                        alpha = if (isPlaying) breatheAlpha * 0.6f else 0.4f
                        scaleX = 1.3f
                        scaleY = 1.3f
                    },
                contentScale = ContentScale.Crop
            )
        }

        // Accent color glow orbs
        AccentGlowOrbs(
            accentColor = accentColor,
            isPlaying = isPlaying,
            modifier = Modifier.fillMaxSize()
        )

        // Dark overlay for better contrast
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to Color.Transparent,
                            0.5f to MaterialTheme.colorScheme.background.copy(alpha = 0.3f),
                            1f to MaterialTheme.colorScheme.background.copy(alpha = 0.9f)
                        )
                    )
                )
        )
    }
}

@Composable
private fun AccentGlowOrbs(
    accentColor: Color,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "orbs")
    
    val offsetX1 by transition.animateFloat(
        initialValue = 0f,
        targetValue = 50f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "offsetX1"
    )
    
    val offsetY1 by transition.animateFloat(
        initialValue = 0f,
        targetValue = 30f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "offsetY1"
    )

    val alpha by animateFloatAsState(
        targetValue = if (isPlaying) 0.25f else 0.15f,
        animationSpec = tween(500),
        label = "orbAlpha"
    )

    Box(modifier = modifier) {
        // Primary glow orb
        Box(
            modifier = Modifier
                .offset { androidx.compose.ui.unit.IntOffset(offsetX1.toInt(), offsetY1.toInt()) }
                .size(300.dp)
                .blur(60.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            accentColor.copy(alpha = alpha),
                            accentColor.copy(alpha = alpha * 0.5f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        // Secondary glow orb
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset { androidx.compose.ui.unit.IntOffset(-offsetX1.toInt() / 2, -offsetY1.toInt()) }
                .size(200.dp)
                .blur(50.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            accentColor.copy(alpha = alpha * 0.6f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )
    }
}

// ============================================================================
// Top Navigation Bar (56dp)
// ============================================================================

@Composable
private fun TopNavigationBar(
    onBackClick: () -> Unit,
    onLyricsClick: () -> Unit,
    onQueueClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.height(56.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back button
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = stringResource(R.string.back),
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Lyrics button
            IconButton(onClick = onLyricsClick) {
                Icon(
                    imageVector = Icons.Rounded.Lyrics,
                    contentDescription = stringResource(R.string.show_lyrics),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }

            // Queue button
            IconButton(onClick = onQueueClick) {
                Icon(
                    imageVector = Icons.Rounded.QueueMusic,
                    contentDescription = stringResource(R.string.queue),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}

// ============================================================================
// Album Art Section
// ============================================================================

@Composable
private fun AlbumArtSection(
    song: Song,
    isPlaying: Boolean,
    accentColor: Color,
    sharedTransitionScope: SharedTransitionScope?,
    animatedContentScope: AnimatedContentScope?,
    modifier: Modifier = Modifier
) {
    // Rotation animation - 360° in 12 seconds when playing
    val infiniteTransition = rememberInfiniteTransition(label = "albumRotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Restart
        ),
        label = "rotation"
    )

    // Breathing scale animation
    val breatheScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "breathe"
    )

    // Glow animation
    val glowAlpha by animateFloatAsState(
        targetValue = if (isPlaying) 0.3f else 0.15f,
        animationSpec = tween(500),
        label = "glowAlpha"
    )

    val shape = RoundedCornerShape(32.dp)
    val sharedModifier = if (sharedTransitionScope != null && animatedContentScope != null) {
        with(sharedTransitionScope) {
            Modifier.sharedElement(
                state = rememberSharedContentState(key = NowPlayingSharedKeys.albumArt(song.id)),
                animatedVisibilityScope = animatedContentScope
            )
        }
    } else {
        Modifier
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Outer glow effect
        Box(
            modifier = Modifier
                .fillMaxSize(1.1f)
                .blur(16.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            accentColor.copy(alpha = glowAlpha),
                            accentColor.copy(alpha = glowAlpha * 0.5f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        // Album art with rotation
        Surface(
            modifier = Modifier
                .then(sharedModifier)
                .fillMaxSize()
                .graphicsLayer {
                    rotationZ = if (isPlaying) rotation else 0f
                    scaleX = if (isPlaying) breatheScale else 1f
                    scaleY = if (isPlaying) breatheScale else 1f
                }
                .shadow(24.dp, shape, spotColor = accentColor.copy(alpha = 0.3f)),
            shape = shape,
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
                // Placeholder with music icon
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Audiotrack,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

// ============================================================================
// Song Info Section
// ============================================================================

@Composable
private fun SongInfoSection(
    song: Song,
    accentColor: Color,
    onArtistClick: () -> Unit,
    onAlbumClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Song title
        Text(
            text = song.title,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.fillMaxWidth()
        )

        // Artist with click
        Row(
            modifier = Modifier
                .clickable(onClick = onArtistClick)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            if (song.album.isNotBlank() && song.album != song.title) {
                Text(
                    text = " · ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = song.album,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clickable(onClick = onAlbumClick)
                )
            }
        }

        // Audio quality badge
        if (song.audioQuality != com.music.revive.domain.model.AudioQuality.UNKNOWN) {
            Spacer(modifier = Modifier.height(8.dp))
            AudioQualityBadge(quality = song.audioQuality)
        }
    }
}

@Composable
private fun AudioQualityBadge(
    quality: com.music.revive.domain.model.AudioQuality
) {
    val (backgroundColor, textColor) = when (quality) {
        com.music.revive.domain.model.AudioQuality.HI_RES -> 
            Color(0xFFFFD60A) to Color.Black
        com.music.revive.domain.model.AudioQuality.LOSSLESS -> 
            Color(0xFFBF5AF2) to Color.White
        com.music.revive.domain.model.AudioQuality.EXTREME -> 
            Color(0xFF30D158) to Color.White
        com.music.revive.domain.model.AudioQuality.HIGH -> 
            Color(0xFF4CAF50) to Color.White
        else -> 
            MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
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

// ============================================================================
// Progress Slider Section
// ============================================================================

@Composable
private fun ProgressSliderSection(
    position: Long,
    duration: Long,
    accentColor: Color,
    sliderValue: Float,
    isUserDragging: Boolean,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(horizontal = 8.dp)) {
        // Slider
        Slider(
            value = sliderValue.coerceIn(0f, 1f),
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            enabled = duration > 0,
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = accentColor,
                inactiveTrackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                disabledActiveTrackColor = accentColor.copy(alpha = 0.5f),
                disabledInactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                disabledThumbColor = accentColor.copy(alpha = 0.5f)
            ),
            thumb = { sliderState ->
                // Custom thumb with scale animation
                val interaction = remember { MutableInteractionSource() }
                val isPressed by interaction.collectIsPressedAsState()
                val scale by animateFloatAsState(
                    targetValue = if (isPressed || isUserDragging) 1.2f else 1f,
                    animationSpec = AppleMusicSpring,
                    label = "thumbScale"
                )
                
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .scale(scale)
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = CircleShape
                        )
                        .shadow(4.dp, CircleShape)
                )
            }
        )

        // Time labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatTime(position),
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

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}

// ============================================================================
// Playback Controls Section
// ============================================================================

@Composable
private fun PlaybackControlsSection(
    isPlaying: Boolean,
    isShuffleEnabled: Boolean,
    repeatMode: RepeatMode,
    accentColor: Color,
    onAccentContent: Color,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Shuffle button (48dp)
        ControlButton(
            icon = Icons.Rounded.Shuffle,
            isActive = isShuffleEnabled,
            accentColor = accentColor,
            size = 48.dp,
            onClick = onToggleShuffle
        )

        // Previous button (48dp)
        ControlButton(
            icon = Icons.Rounded.SkipPrevious,
            isActive = false,
            accentColor = MaterialTheme.colorScheme.onSurface,
            size = 48.dp,
            onClick = onPrevious
        )

        // Play/Pause button (56dp) - prominent
        PlayPauseButton(
            isPlaying = isPlaying,
            accentColor = accentColor,
            onAccentContent = onAccentContent,
            onClick = onPlayPause
        )

        // Next button (48dp)
        ControlButton(
            icon = Icons.Rounded.SkipNext,
            isActive = false,
            accentColor = MaterialTheme.colorScheme.onSurface,
            size = 48.dp,
            onClick = onNext
        )

        // Repeat button (48dp)
        ControlButton(
            icon = when (repeatMode) {
                RepeatMode.ONE -> Icons.Rounded.RepeatOne
                else -> Icons.Rounded.Repeat
            },
            isActive = repeatMode != RepeatMode.OFF,
            accentColor = accentColor,
            size = 48.dp,
            onClick = onCycleRepeat
        )
    }
}

@Composable
private fun ControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isActive: Boolean,
    accentColor: Color,
    size: Dp,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = AppleMusicSpring,
        label = "buttonScale"
    )

    val containerColor by animateColorAsState(
        targetValue = if (isActive) accentColor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
        animationSpec = AppleMusicColorTween,
        label = "containerColor"
    )

    val contentColor by animateColorAsState(
        targetValue = if (isActive) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = AppleMusicColorTween,
        label = "contentColor"
    )

    Surface(
        onClick = onClick,
        interactionSource = interaction,
        modifier = Modifier.size(size).scale(scale),
        shape = CircleShape,
        color = containerColor
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(size * 0.5f),
                tint = contentColor
            )
        }
    }
}

@Composable
private fun PlayPauseButton(
    isPlaying: Boolean,
    accentColor: Color,
    onAccentContent: Color,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = AppleMusicSpring,
        label = "playScale"
    )

    Surface(
        onClick = onClick,
        interactionSource = interaction,
        modifier = Modifier
            .size(56.dp)
            .scale(scale)
            .shadow(8.dp, CircleShape, spotColor = accentColor.copy(alpha = 0.3f)),
        shape = CircleShape,
        color = accentColor
    ) {
        Box(contentAlignment = Alignment.Center) {
            Crossfade(
                targetState = isPlaying,
                animationSpec = tween(300, easing = FastOutSlowInEasing),
                label = "playPauseIcon"
            ) { playing ->
                Icon(
                    imageVector = if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (playing) stringResource(R.string.pause) else stringResource(R.string.play),
                    modifier = Modifier.size(28.dp),
                    tint = onAccentContent
                )
            }
        }
    }
}

// ============================================================================
// Secondary Controls Section
// ============================================================================

@Composable
private fun SecondaryControlsSection(
    isFavorite: Boolean,
    accentColor: Color,
    onToggleFavorite: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(horizontal = 32.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Favorite button
        val favColor by animateColorAsState(
            targetValue = if (isFavorite) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
            animationSpec = AppleMusicColorTween,
            label = "favColor"
        )
        
        IconButton(onClick = onToggleFavorite) {
            Crossfade(
                targetState = isFavorite,
                animationSpec = tween(300),
                label = "favIcon"
            ) { fav ->
                Icon(
                    imageVector = if (fav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = if (fav) stringResource(R.string.remove_from_favorites) else stringResource(R.string.add_to_favorites),
                    tint = favColor
                )
            }
        }

        // Add to playlist
        IconButton(onClick = onAddToPlaylist) {
            Icon(
                imageVector = Icons.Rounded.PlaylistAdd,
                contentDescription = stringResource(R.string.add_to_playlist),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }

        // More options
        IconButton(onClick = onMore) {
            Icon(
                imageVector = Icons.Rounded.MoreHoriz,
                contentDescription = stringResource(R.string.more_options),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

// ============================================================================
// Queue Bottom Sheet
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QueueBottomSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
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

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // Header
            Text(
                text = stringResource(R.string.queue),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            if (queue.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
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
                    state = listState,
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    itemsIndexed(queue) { index, song ->
                        QueueItem(
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
}

@Composable
private fun QueueItem(
    song: Song,
    isPlaying: Boolean,
    position: Int,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isPlaying) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else Color.Transparent,
        animationSpec = tween(200),
        label = "itemBackground"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .background(backgroundColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Current playing indicator or position
        Box(
            modifier = Modifier.width(32.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isPlaying) {
                // Playing indicator
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    repeat(3) { i ->
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height((8 + i * 4).dp)
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
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Album art thumbnail
        Surface(
            modifier = Modifier.size(48.dp),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            if (song.albumArtUri != null) {
                AsyncImage(
                    model = song.albumArtUri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.MusicNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Song info
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Duration
        Text(
            text = song.formattedDuration,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Remove button
        IconButton(
            onClick = onRemove,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = stringResource(R.string.remove),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }

    // Divider line
    if (!isPlaying) {
        HorizontalDivider(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 60.dp),
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
        )
    }
}

// ============================================================================
// More Options Sheet
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MoreOptionsSheet(
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
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            // Song header
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
                        shape = RoundedCornerShape(12.dp)
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

// ============================================================================
// Queue Screen (Separate screen version)
// ============================================================================

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
                state = listState,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                itemsIndexed(queue) { index, song ->
                    QueueItem(
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