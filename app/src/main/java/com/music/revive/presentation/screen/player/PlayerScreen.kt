package com.music.revive.presentation.screen.player

import android.content.Intent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.unit.dp
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
import com.music.revive.presentation.components.LyricsView
import com.music.revive.presentation.screen.fullscreenlyrics.FullScreenLyricsActivity
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

    // Palette colors extraction from album art
    var paletteColors by remember { mutableStateOf<PaletteColors?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Lyrics state
    var showLyrics by remember { mutableStateOf(false) }
    val currentLyrics by viewModel.currentLyrics.collectAsState()
    val isLoadingLyrics by viewModel.isLoadingLyrics.collectAsState()
    val lyricsFontSize by viewModel.lyricsFontSize.collectAsState()
    val showTranslation by viewModel.showTranslation.collectAsState()
    val lyricsDisplayStyle by viewModel.lyricsDisplayStyle.collectAsState()
    val enableGlow by viewModel.enableGlow.collectAsState()
    val enableKaraoke by viewModel.enableKaraoke.collectAsState()
    val enableHapticFeedback by viewModel.enableHapticFeedback.collectAsState()
    val enableBlur by viewModel.enableBlur.collectAsState()
    val enableFullScreenLyricsButton by viewModel.enableFullScreenLyricsButton.collectAsState()
    val enableShaderEffect by viewModel.enableShaderEffect.collectAsState()
    val enableBalancedLines by viewModel.enableBalancedLines.collectAsState()

    // Entry animation states
    var isVisible by remember { mutableStateOf(false) }
    
    // Custom easing for entry animations
    val smoothEasing = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f)
    
    // Staggered entry animations
    val albumAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(400, easing = smoothEasing),
        label = "albumAlpha"
    )
    
    val albumScale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.8f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "albumScale"
    )
    
    val infoAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(400, 100, easing = smoothEasing),
        label = "infoAlpha"
    )
    
    val infoOffset by animateDpAsState(
        targetValue = if (isVisible) 0.dp else 20.dp,
        animationSpec = tween(400, 100, easing = smoothEasing),
        label = "infoOffset"
    )
    
    val controlsAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(400, 200, easing = smoothEasing),
        label = "controlsAlpha"
    )
    
    val bottomAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(400, 300, easing = smoothEasing),
        label = "bottomAlpha"
    )

    // Trigger entry animation only when song changes
    LaunchedEffect(song.id) {
        isVisible = true
    }

    LaunchedEffect(song.albumArtUri) {
        song.albumArtUri?.let { uri ->
            scope.launch {
                paletteColors = extractPaletteColors(context, uri)
            }
        }
    }

    // Album scale animation based on playing state
    val playingScale by animateFloatAsState(
        targetValue = if (playerState.isPlaying) 1f else 0.95f,
        animationSpec = tween(400, easing = LinearEasing),
        label = "playingScale"
    )

    // Album shadow animation
    val albumShadow by animateDpAsState(
        targetValue = if (playerState.isPlaying) 32.dp else 16.dp,
        animationSpec = tween(400, easing = LinearEasing),
        label = "shadow"
    )

    var currentSliderValue by remember { mutableFloatStateOf(0f) }
    var isUserDragging by remember { mutableStateOf(false) }

    // Dialog states
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }
    var showMoreOptionsSheet by remember { mutableStateOf(false) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }

    // Update slider value based on player position
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
        modifier = Modifier.fillMaxSize()
    ) {
        // Ambient background with floating color blobs
        AmbientBackground(
            colors = paletteColors,
            modifier = Modifier.fillMaxSize()
        )
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Top bar - Cleaner design
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowDown,
                        contentDescription = stringResource(R.string.back),
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                Text(
                    text = if (showLyrics) stringResource(R.string.lyrics) else stringResource(R.string.playing),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.weight(1f))
                
                // Toggle lyrics/album art button
                IconButton(onClick = { 
                    if (showLyrics) {
                        val intent = FullScreenLyricsActivity.newIntent(context)
                        context.startActivity(intent)
                    } else {
                        showLyrics = !showLyrics
                    }
                }) {
                    Icon(
                        imageVector = if (showLyrics) Icons.Rounded.Album else Icons.Rounded.Lyrics,
                        contentDescription = if (showLyrics) stringResource(R.string.show_album_art) else stringResource(R.string.show_lyrics),
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

            // Album art or Lyrics view with enhanced animation
            val albumShape = RoundedCornerShape(16.dp)
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                val currentPaletteColors = paletteColors
                
                // Album art view
                if (!showLyrics) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        // Enhanced glow effect behind album art
                        if (playerState.isPlaying && currentPaletteColors != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize(0.85f)
                                    .aspectRatio(1f)
                                    .graphicsLayer {
                                        scaleX = albumScale * playingScale
                                        scaleY = albumScale * playingScale
                                        alpha = albumAlpha * 0.5f
                                    }
                                    .blur(20.dp)
                                    .background(
                                        currentPaletteColors.dominant.copy(alpha = 0.5f),
                                        CircleShape
                                    )
                            )
                        }

                        // Main album art with shadow and rounded corners
                        Surface(
                            modifier = Modifier
                                .fillMaxSize(0.9f)
                                .aspectRatio(1f)
                                .graphicsLayer {
                                    scaleX = albumScale * playingScale
                                    scaleY = albumScale * playingScale
                                    shadowElevation = (albumShadow.toPx() * 1.5).toFloat()
                                    clip = true
                                    shape = albumShape
                                    alpha = albumAlpha
                                },
                            tonalElevation = 8.dp,
                            shape = albumShape
                        ) {
                            if (song.albumArtUri != null) {
                                AsyncImage(
                                    model = song.albumArtUri,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Image(
                                    painter = painterResource(id = R.mipmap.ic_launcher),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }
                } else {
                    // Lyrics view with immersive background
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { alpha = albumAlpha }
                    ) {
                        val currentPaletteForLyrics = paletteColors
                        if (currentPaletteForLyrics != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .drawBehind {
                                        drawCircle(
                                            brush = Brush.radialGradient(
                                                colors = listOf(
                                                    currentPaletteForLyrics.dominant.copy(alpha = 0.08f),
                                                    Color.Transparent
                                                ),
                                                center = Offset(size.width * 0.5f, size.height * 0.3f),
                                                radius = size.width * 0.8f
                                            )
                                        )
                                    }
                            )
                        }
                        
                        if (isLoadingLyrics) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        } else {
                            LyricsView(
                                lyric = currentLyrics,
                                currentPositionMs = playerState.position,
                                fontSizeMultiplier = lyricsFontSize,
                                showTranslation = showTranslation,
                                isCentered = lyricsDisplayStyle == 0,
                                enableGlow = enableGlow,
                                enableKaraoke = enableKaraoke,
                                enableHapticFeedback = enableHapticFeedback,
                                enableBlur = enableBlur,
                                enableShader = enableShaderEffect,
                                enableBalancedLines = enableBalancedLines,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }

            // Song info - Centered and cleaner
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
                    .graphicsLayer {
                        alpha = infoAlpha
                        translationY = infoOffset.toPx()
                    },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Title with larger font
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(6.dp))
                
                // Artist name with primary color
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { onArtistClick(song.artistId) }
                )
                
                // Album name
                if (song.album.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = song.album,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.clickable { onAlbumClick(song.albumId) }
                    )
                }
                
                // Audio quality badge
                if (song.audioQuality != com.music.revive.domain.model.AudioQuality.UNKNOWN) {
                    Spacer(modifier = Modifier.height(10.dp))
                    com.music.revive.presentation.screen.song.AudioQualityBadge(
                        quality = song.audioQuality
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Modern progress slider with enhanced design
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .graphicsLayer { alpha = controlsAlpha }
            ) {
                Slider(
                    value = currentSliderValue,
                    onValueChange = { newValue ->
                        isUserDragging = true
                        currentSliderValue = newValue
                    },
                    onValueChangeFinished = {
                        isUserDragging = false
                        viewModel.seekTo((currentSliderValue * playerState.duration).toLong())
                    },
                    modifier = Modifier.fillMaxWidth(),
                    thumb = {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .background(
                                    MaterialTheme.colorScheme.primary,
                                    CircleShape
                                )
                        )
                    },
                    track = { sliderState ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(currentSliderValue)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                        }
                    }
                )
                
                // Time labels
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = playerState.formattedPosition,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = song.formattedDuration,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Main controls - Enhanced design
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Shuffle - Larger touch target
                FilledTonalIconButton(
                    onClick = { viewModel.toggleShuffle() },
                    modifier = Modifier.size(48.dp),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = if (playerState.isShuffleEnabled)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Shuffle,
                        contentDescription = stringResource(R.string.shuffle),
                        modifier = Modifier.size(24.dp),
                        tint = if (playerState.isShuffleEnabled)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Previous
                IconButton(onClick = { viewModel.playPrevious() }) {
                    Icon(
                        imageVector = Icons.Rounded.SkipPrevious,
                        contentDescription = stringResource(R.string.previous),
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Play/Pause - Extra large FAB
                FilledIconButton(
                    onClick = { viewModel.playPause() },
                    modifier = Modifier.size(80.dp),
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(
                        imageVector = if (playerState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (playerState.isPlaying) stringResource(R.string.pause) else stringResource(R.string.play),
                        modifier = Modifier.size(40.dp)
                    )
                }

                // Next
                IconButton(onClick = { viewModel.playNext() }) {
                    Icon(
                        imageVector = Icons.Rounded.SkipNext,
                        contentDescription = stringResource(R.string.next),
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Repeat
                FilledTonalIconButton(
                    onClick = { viewModel.cycleRepeatMode() },
                    modifier = Modifier.size(48.dp),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = when (playerState.repeatMode) {
                            RepeatMode.OFF -> MaterialTheme.colorScheme.surfaceVariant
                            else -> MaterialTheme.colorScheme.primaryContainer
                        }
                    )
                ) {
                    Icon(
                        imageVector = when (playerState.repeatMode) {
                            RepeatMode.ONE -> Icons.Rounded.RepeatOne
                            else -> Icons.Rounded.Repeat
                        },
                        contentDescription = stringResource(R.string.repeat),
                        modifier = Modifier.size(24.dp),
                        tint = when (playerState.repeatMode) {
                            RepeatMode.OFF -> MaterialTheme.colorScheme.onSurfaceVariant
                            else -> MaterialTheme.colorScheme.primary
                        }
                    )
                }
            }

            // Bottom actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .navigationBarsPadding()
                    .graphicsLayer { alpha = bottomAlpha },
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Favorite
                IconButton(onClick = { viewModel.toggleFavorite() }) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = if (isFavorite) stringResource(R.string.remove_from_favorites) else stringResource(R.string.add_to_favorites),
                        tint = if (isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                // Add to playlist
                IconButton(onClick = { showAddToPlaylistDialog = true }) {
                    Icon(
                        imageVector = Icons.Rounded.PlaylistAdd,
                        contentDescription = stringResource(R.string.add_to_playlist),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                // Share
                IconButton(onClick = {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "audio/*"
                        putExtra(Intent.EXTRA_STREAM, android.net.Uri.parse(song.path))
                        putExtra(Intent.EXTRA_SUBJECT, song.title)
                        putExtra(Intent.EXTRA_TEXT, "${song.title} - ${song.artist}")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.share)))
                }) {
                    Icon(
                        imageVector = Icons.Rounded.Share,
                        contentDescription = stringResource(R.string.share),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                // More options
                IconButton(onClick = { showMoreOptionsSheet = true }) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = stringResource(R.string.more_options),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    // Add to playlist dialog
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

    // Create playlist dialog
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
        ModalBottomSheet(
            onDismissRequest = { showMoreOptionsSheet = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                // Song info header
                ListItem(
                    headlineContent = {
                        Text(
                            text = song.title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
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
                                Image(
                                    painter = painterResource(id = R.mipmap.ic_launcher),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }
                )
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // Options
                if (song.albumId != null) {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.go_to_album)) },
                        leadingContent = {
                            Icon(Icons.Rounded.Album, contentDescription = null)
                        },
                        modifier = Modifier.clickable {
                            showMoreOptionsSheet = false
                            onAlbumClick(song.albumId)
                        }
                    )
                }

                if (song.artistId != null) {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.go_to_artist)) },
                        leadingContent = {
                            Icon(Icons.Rounded.Person, contentDescription = null)
                        },
                        modifier = Modifier.clickable {
                            showMoreOptionsSheet = false
                            onArtistClick(song.artistId)
                        }
                    )
                }

                ListItem(
                    headlineContent = { Text(stringResource(R.string.song_info)) },
                    leadingContent = {
                        Icon(Icons.Rounded.Info, contentDescription = null)
                    },
                    modifier = Modifier.clickable {
                        showMoreOptionsSheet = false
                        onSongDetailClick(song.id)
                    }
                )

                ListItem(
                    headlineContent = { Text(stringResource(R.string.sleep_timer)) },
                    leadingContent = {
                        Icon(Icons.Rounded.Timer, contentDescription = null)
                    },
                    modifier = Modifier.clickable {
                        showMoreOptionsSheet = false
                        Toast.makeText(context, "Sleep timer coming soon", Toast.LENGTH_SHORT).show()
                    }
                )

                ListItem(
                    headlineContent = { Text(stringResource(R.string.equalizer)) },
                    leadingContent = {
                        Icon(Icons.Rounded.Equalizer, contentDescription = null)
                    },
                    modifier = Modifier.clickable {
                        showMoreOptionsSheet = false
                        Toast.makeText(context, "Equalizer coming soon", Toast.LENGTH_SHORT).show()
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

/**
 * Queue screen composable
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
                title = { Text(stringResource(R.string.queue)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = stringResource(R.string.back))
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
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
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
                    QueueItem(
                        song = song,
                        isPlaying = index == currentIndex,
                        position = index + 1,
                        onClick = {
                            viewModel.musicPlayer.playSong(song, queue)
                        },
                        onRemove = {
                            viewModel.musicPlayer.removeFromQueue(index)
                        }
                    )
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
    // Staggered entry animation
    var isVisible by remember { mutableStateOf(false) }
    
    val smoothEasing = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f)
    
    val itemAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(250, position * 20, easing = smoothEasing),
        label = "itemAlpha"
    )
    
    val itemOffset by animateDpAsState(
        targetValue = if (isVisible) 0.dp else 12.dp,
        animationSpec = tween(250, position * 20, easing = smoothEasing),
        label = "itemOffset"
    )

    LaunchedEffect(Unit) {
        isVisible = true
    }

    ListItem(
        headlineContent = {
            Text(
                text = song.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
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
                shape = RoundedCornerShape(8.dp),
                color = if (isPlaying) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (isPlaying) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
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
                    contentDescription = stringResource(R.string.remove)
                )
            }
        },
        modifier = Modifier
            .clickable(onClick = onClick)
            .graphicsLayer {
                alpha = itemAlpha
                translationY = itemOffset.toPx()
            }
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

/**
 * Ambient background with blurred floating color blobs
 */
@Composable
fun AmbientBackground(
    colors: PaletteColors?,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ambient")
    
    // Optimize: Only animate 4 key positions instead of 8 for better performance
    // Use longer durations and smoother easing for ambient effect
    val offsetX1 by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "offsetX1"
    )
    
    val offsetY1 by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(15000, easing = LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "offsetY1"
    )
    
    val offsetX2 by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(18000, easing = LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "offsetX2"
    )
    
    val offsetY2 by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(14000, easing = LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "offsetY2"
    )

    val backgroundColor = MaterialTheme.colorScheme.background
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                // Draw base background
                drawRect(backgroundColor)
                
                if (colors != null) {
                    val width = size.width
                    val height = size.height
                    
                    // Draw blurred color blobs with radial gradient
                    // Blob 1 - Dominant color (top-left area)
                    val center1 = androidx.compose.ui.geometry.Offset(
                        width * offsetX1,
                        height * offsetY1
                    )
                    val radius1 = width * 0.5f
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                colors.dominant.copy(alpha = 0.35f),
                                colors.dominant.copy(alpha = 0.15f),
                                Color.Transparent
                            ),
                            center = center1,
                            radius = radius1
                        ),
                        center = center1,
                        radius = radius1
                    )
                    
                    // Blob 2 - Vibrant color (top-right area)
                    val center2 = androidx.compose.ui.geometry.Offset(
                        width * offsetX2,
                        height * offsetY2
                    )
                    val radius2 = width * 0.45f
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                colors.vibrant.copy(alpha = 0.3f),
                                colors.vibrant.copy(alpha = 0.1f),
                                Color.Transparent
                            ),
                            center = center2,
                            radius = radius2
                        ),
                        center = center2,
                        radius = radius2
                    )
                }
            }
    )
}

/**
 * Data class to hold extracted palette colors from album art
 */
data class PaletteColors(
    val dominant: Color,
    val vibrant: Color,
    val lightVibrant: Color,
    val darkVibrant: Color,
    val muted: Color
)

/**
 * Extract multiple colors from image URI using Palette
 */
private suspend fun extractPaletteColors(context: android.content.Context, uri: String): PaletteColors? {
    return withContext(Dispatchers.IO) {
        try {
            val imageLoader = coil.ImageLoader(context)
            val request = coil.request.ImageRequest.Builder(context)
                .data(uri)
                .allowHardware(false)
                .build()

            val result = imageLoader.execute(request)
            val drawable = result.drawable ?: return@withContext null
            val bitmap = drawable.toBitmap()

            val palette = androidx.palette.graphics.Palette.from(bitmap)
                .maximumColorCount(24)
                .generate()

            val defaultColor = android.graphics.Color.GRAY
            
            PaletteColors(
                dominant = Color(palette.dominantSwatch?.rgb ?: palette.getDominantColor(defaultColor)),
                vibrant = Color(palette.vibrantSwatch?.rgb ?: palette.getVibrantColor(defaultColor)),
                lightVibrant = Color(palette.lightVibrantSwatch?.rgb ?: palette.getLightVibrantColor(defaultColor)),
                darkVibrant = Color(palette.darkVibrantSwatch?.rgb ?: palette.getDarkVibrantColor(defaultColor)),
                muted = Color(palette.mutedSwatch?.rgb ?: palette.getMutedColor(defaultColor))
            )
        } catch (e: Exception) {
            null
        }
    }
}
