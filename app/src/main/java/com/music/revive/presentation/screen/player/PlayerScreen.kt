@file:OptIn(
    ExperimentalAnimationApi::class,
    ExperimentalSharedTransitionApi::class
)

package com.music.revive.presentation.screen.player

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.vector.ImageVector
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import com.music.revive.presentation.theme.AlbumColors
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

private val AppleMusicColorFlowSpec: AnimationSpec<Color> = tween(720, easing = FastOutSlowInEasing)
private val AppleMusicTapSpring: AnimationSpec<Float> = spring(
    dampingRatio = Spring.DampingRatioMediumBouncy,
    stiffness = Spring.StiffnessHigh
)

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

    val controlsFade = remember { Animatable(1f) }
    LaunchedEffect(song.id) {
        controlsFade.snapTo(0.78f)
        controlsFade.animateTo(1f, tween(420, easing = FastOutSlowInEasing))
    }

    var currentSliderValue by remember { mutableFloatStateOf(0f) }
    var isUserDragging by remember { mutableStateOf(false) }

    // Dialog states
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }
    var showMoreOptionsSheet by remember { mutableStateOf(false) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }

    LaunchedEffect(playerState.position, playerState.duration, isUserDragging) {
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

    val chromeTint = Color.White.copy(alpha = 0.92f)
    val chromeMuted = Color.White.copy(alpha = 0.48f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        NowPlayingAmbientBackdrop(
            albumArtUri = song.albumArtUri,
            albumColors = albumColors,
            isPlaying = playerState.isPlaying,
            modifier = Modifier.fillMaxSize()
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PlayerPressableIconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowDown,
                        contentDescription = stringResource(R.string.back),
                        modifier = Modifier.size(30.dp),
                        tint = chromeTint
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = stringResource(R.string.playing),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = chromeMuted,
                    letterSpacing = 0.6.sp
                )
                Spacer(modifier = Modifier.weight(1f))
                PlayerPressableIconButton(onClick = {
                    val intent = FullScreenLyricsActivity.newIntent(context)
                    context.startActivity(intent)
                }) {
                    Icon(
                        imageVector = Icons.Rounded.Lyrics,
                        contentDescription = stringResource(R.string.show_lyrics),
                        tint = chromeTint
                    )
                }
                PlayerPressableIconButton(onClick = onQueueClick) {
                    Icon(
                        imageVector = Icons.Rounded.QueueMusic,
                        contentDescription = stringResource(R.string.queue),
                        tint = chromeTint
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Spacer(modifier = Modifier.weight(0.15f))
                AppleMusicNowPlayingArtwork(
                    song = song,
                    isPlaying = playerState.isPlaying,
                    accentColor = accentColor,
                    sharedTransitionScope = sharedTransitionScope,
                    animatedContentScope = animatedContentScope,
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .aspectRatio(1f)
                )
                Spacer(modifier = Modifier.height(28.dp))
                AppleMusicNowPlayingMeta(
                    song = song,
                    accentColor = accentColor,
                    onArtistClick = { onArtistClick(song.artistId) },
                    onAlbumClick = { onAlbumClick(song.albumId) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.weight(0.2f))
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { alpha = controlsFade.value }
                    .navigationBarsPadding()
                    .padding(bottom = 20.dp)
            ) {
                AppleMusicNowPlayingSlider(
                    formattedPosition = playerState.formattedPosition,
                    formattedDuration = song.formattedDuration,
                    accentColor = accentColor,
                    sliderValue = currentSliderValue,
                    isScrubbing = isUserDragging,
                    enabled = playerState.duration > 0,
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                AppleMusicNowPlayingTransport(
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

                Spacer(modifier = Modifier.height(8.dp))

                AppleMusicNowPlayingSecondaryRow(
                    isFavorite = isFavorite,
                    accentColor = accentColor,
                    onToggleFavorite = { viewModel.toggleFavorite() },
                    onAddToPlaylist = { showAddToPlaylistDialog = true },
                    onMore = { showMoreOptionsSheet = true },
                    chromeTint = chromeTint,
                    chromeMuted = chromeMuted,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 28.dp)
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

@Composable
private fun PlayerPressableIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.9f else 1f,
        animationSpec = AppleMusicTapSpring,
        label = "chromeIconPress"
    )
    IconButton(
        onClick = onClick,
        interactionSource = interaction,
        modifier = modifier.scale(scale)
    ) { content() }
}

@Composable
private fun NowPlayingAmbientBackdrop(
    albumArtUri: String?,
    albumColors: AlbumColors,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val primaryA by animateColorAsState(
        albumColors.primary,
        AppleMusicColorFlowSpec,
        label = "ambPrim"
    )
    val secondaryA by animateColorAsState(
        albumColors.secondary,
        AppleMusicColorFlowSpec,
        label = "ambSec"
    )
    val tertiaryA by animateColorAsState(
        albumColors.tertiary,
        AppleMusicColorFlowSpec,
        label = "ambTer"
    )
    val breath = rememberInfiniteTransition(label = "amb")
    val shift by breath.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(18_000, easing = LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "shift"
    )
    val kbDrift = rememberInfiniteTransition(label = "ken")
    val kb by kbDrift.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(8_000, easing = FastOutSlowInEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "kb"
    )
    val bgLiveScale = if (isPlaying) 1.33f + 0.06f * kb else 1.31f
    val bgAlpha by animateFloatAsState(
        targetValue = if (isPlaying) 0.54f else 0.48f,
        animationSpec = tween(480, easing = FastOutSlowInEasing),
        label = "bgAlphaEase"
    )

    Box(modifier = modifier.background(Color.Black)) {
        if (albumArtUri != null) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(albumArtUri)
                    .crossfade(520)
                    .build(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(100.dp)
                    .graphicsLayer {
                        scaleX = bgLiveScale
                        scaleY = bgLiveScale
                        alpha = bgAlpha
                    },
                contentScale = ContentScale.Crop
            )
        }
        Box(
            Modifier
                .fillMaxSize()
                .drawBehind {
                    val w = size.width
                    val h = size.height
                    val t = shift
                    drawRect(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                primaryA.copy(alpha = 0.5f + 0.07f * t),
                                secondaryA.copy(alpha = 0.32f + 0.09f * (1f - t)),
                                tertiaryA.copy(alpha = 0.24f),
                                Color.Black.copy(alpha = 0.82f)
                            ),
                            start = Offset.Zero,
                            end = Offset(w * (0.74f + 0.22f * t), h * 1.06f)
                        )
                    )
                }
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to Color.Black.copy(alpha = 0.1f),
                            0.35f to Color.Transparent,
                            0.72f to Color.Black.copy(alpha = 0.45f),
                            1f to Color.Black.copy(alpha = 0.9f)
                        )
                    )
                )
        )
    }
}

@Composable
private fun AppleMusicNowPlayingArtwork(
    song: Song,
    isPlaying: Boolean,
    accentColor: Color,
    sharedTransitionScope: SharedTransitionScope?,
    animatedContentScope: AnimatedContentScope?,
    modifier: Modifier = Modifier
) {
    val accentGlow by animateColorAsState(
        accentColor,
        AppleMusicColorFlowSpec,
        label = "artAccent"
    )
    val playingEase by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0.988f,
        animationSpec = tween(480, easing = FastOutSlowInEasing),
        label = "playingEase"
    )
    val breathe = rememberInfiniteTransition(label = "cover")
    val pulse by breathe.animateFloat(
        initialValue = 1f,
        targetValue = 1.012f,
        animationSpec = infiniteRepeatable(
            animation = tween(3_200, easing = FastOutSlowInEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "pulse"
    )
    val livePulse = if (isPlaying) pulse else 1f
    val corner = 16.dp
    val shape = RoundedCornerShape(corner)
    val fullArtShared = if (sharedTransitionScope != null && animatedContentScope != null) {
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
        modifier = modifier
            .graphicsLayer {
                scaleX = playingEase * livePulse
                scaleY = playingEase * livePulse
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .fillMaxSize(0.98f)
                .aspectRatio(1f)
                .drawBehind {
                    val r = minOf(size.width, size.height) * 0.5f
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                accentGlow.copy(alpha = 0.5f),
                                Color.Transparent
                            ),
                            center = center,
                            radius = r * 1.2f
                        )
                    )
                }
        )
        AnimatedContent(
            targetState = song.id,
            transitionSpec = {
                (fadeIn(tween(380, easing = FastOutSlowInEasing)) +
                    scaleIn(
                        initialScale = 0.92f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    ) + slideInVertically { (it * 0.06f).toInt() }) togetherWith
                    (fadeOut(tween(260, easing = FastOutSlowInEasing)) +
                        scaleOut(
                            targetScale = 1.04f,
                            animationSpec = tween(280, easing = FastOutSlowInEasing)
                        ) +
                        slideOutVertically { (-it * 0.04f).toInt() })
            },
            label = "albumArt"
        ) { _ ->
            Surface(
                modifier = Modifier
                    .then(fullArtShared)
                    .fillMaxSize()
                    .shadow(
                        elevation = 28.dp,
                        shape = shape,
                        spotColor = accentGlow.copy(alpha = 0.55f)
                    ),
                shape = shape,
                color = Color(0xFF2C2C2E)
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
                            .background(Color(0xFF2C2C2E)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MusicNote,
                            contentDescription = null,
                            modifier = Modifier.size(88.dp),
                            tint = Color.White.copy(alpha = 0.28f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AppleMusicNowPlayingMeta(
    song: Song,
    accentColor: Color,
    onArtistClick: () -> Unit,
    onAlbumClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val titleColor = Color.White
    val albumSubColor = Color.White.copy(alpha = 0.55f)
    val accentLine by animateColorAsState(accentColor, AppleMusicColorFlowSpec, label = "metaAccent")

    AnimatedContent(
        targetState = song.id,
        transitionSpec = {
            (
                fadeIn(tween(340, delayMillis = 40, easing = FastOutSlowInEasing)) +
                    slideInVertically(
                        animationSpec = tween(380, easing = FastOutSlowInEasing),
                        initialOffsetY = { fullH -> (fullH * 0.1f).toInt() }
                    )
                ) togetherWith (
                fadeOut(tween(220, easing = FastOutSlowInEasing)) +
                    slideOutVertically(
                        animationSpec = tween(220, easing = FastOutSlowInEasing),
                        targetOffsetY = { fullH -> (-fullH * 0.06f).toInt() }
                    )
                )
        },
        modifier = modifier,
        label = "meta"
    ) { _ ->
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 23.sp,
                    lineHeight = 28.sp,
                    letterSpacing = (-0.35).sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                color = titleColor,
                modifier = Modifier.basicMarquee(
                    iterations = Int.MAX_VALUE,
                    initialDelayMillis = 2_800
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.clickable(onClick = onArtistClick)
            ) {
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = accentLine
                )
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = accentLine.copy(alpha = 0.9f)
                )
            }
            if (song.album.isNotBlank() && song.album != song.title) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = song.album,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    color = albumSubColor,
                    modifier = Modifier
                        .clickable(onClick = onAlbumClick)
                        .padding(horizontal = 8.dp)
                )
            }
            if (song.audioQuality != com.music.revive.domain.model.AudioQuality.UNKNOWN) {
                Spacer(modifier = Modifier.height(14.dp))
                AudioQualityBadge(quality = song.audioQuality)
            }
        }
    }
}

@Composable
private fun AppleMusicNowPlayingSlider(
    formattedPosition: String,
    formattedDuration: String,
    accentColor: Color,
    sliderValue: Float,
    isScrubbing: Boolean,
    enabled: Boolean,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val timeMuted = Color.White.copy(alpha = 0.45f)
    val scrubLift by animateFloatAsState(
        targetValue = if (isScrubbing) 1.045f else 1f,
        animationSpec = AppleMusicTapSpring,
        label = "scrubLift"
    )
    val trackAccent by animateColorAsState(
        accentColor,
        AppleMusicColorFlowSpec,
        label = "sliderAccent"
    )
    Column(modifier = modifier.scale(scrubLift)) {
        Slider(
            value = sliderValue.coerceIn(0f, 1f),
            onValueChange = onValueChange,
            onValueChangeFinished = { onValueChangeFinished() },
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp),
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = trackAccent,
                inactiveTrackColor = Color.White.copy(alpha = 0.28f),
                disabledActiveTrackColor = trackAccent.copy(alpha = 0.35f),
                disabledInactiveTrackColor = Color.White.copy(alpha = 0.12f),
                disabledThumbColor = Color.White.copy(alpha = 0.35f)
            )
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formattedPosition,
                style = MaterialTheme.typography.labelMedium,
                color = timeMuted
            )
            Text(
                text = formattedDuration,
                style = MaterialTheme.typography.labelMedium,
                color = timeMuted
            )
        }
    }
}

@Composable
private fun AppleMusicNowPlayingTransport(
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
    val transportTint = Color.White.copy(alpha = 0.95f)
    val subtleGlyph = Color.White.copy(alpha = 0.4f)
    val playTint by animateColorAsState(accentColor, AppleMusicColorFlowSpec, label = "playFill")
    val playContent by animateColorAsState(onAccentContent, AppleMusicColorFlowSpec, label = "playOn")

    val playInteraction = remember { MutableInteractionSource() }
    val playPressed by playInteraction.collectIsPressedAsState()
    val playScale by animateFloatAsState(
        targetValue = if (playPressed) 0.94f else 1f,
        animationSpec = AppleMusicTapSpring,
        label = "playPress"
    )

    Row(
        modifier = modifier.padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppleMusicModeGlyph(
            icon = Icons.Rounded.Shuffle,
            isOn = isShuffleEnabled,
            accentColor = accentColor,
            offTint = subtleGlyph,
            onClick = onToggleShuffle
        )
        PlayerPressableIconButton(
            onClick = onPrevious,
            modifier = Modifier.size(56.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.SkipPrevious,
                contentDescription = stringResource(R.string.previous),
                modifier = Modifier.size(38.dp),
                tint = transportTint
            )
        }
        FilledIconButton(
            onClick = onPlayPause,
            interactionSource = playInteraction,
            modifier = Modifier
                .size(78.dp)
                .scale(playScale),
            shape = CircleShape,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = playTint,
                contentColor = playContent
            )
        ) {
            Crossfade(
                targetState = isPlaying,
                animationSpec = tween(320, easing = FastOutSlowInEasing),
                label = "playPauseIcon"
            ) { playing ->
                Icon(
                    imageVector = if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (playing) {
                        stringResource(R.string.pause)
                    } else {
                        stringResource(R.string.play)
                    },
                    modifier = Modifier.size(40.dp)
                )
            }
        }
        PlayerPressableIconButton(
            onClick = onNext,
            modifier = Modifier.size(56.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.SkipNext,
                contentDescription = stringResource(R.string.next),
                modifier = Modifier.size(38.dp),
                tint = transportTint
            )
        }
        AppleMusicModeGlyph(
            icon = when (repeatMode) {
                RepeatMode.ONE -> Icons.Rounded.RepeatOne
                else -> Icons.Rounded.Repeat
            },
            isOn = repeatMode != RepeatMode.OFF,
            accentColor = accentColor,
            offTint = subtleGlyph,
            onClick = onCycleRepeat
        )
    }
}

@Composable
private fun AppleMusicModeGlyph(
    icon: ImageVector,
    isOn: Boolean,
    accentColor: Color,
    offTint: Color,
    onClick: () -> Unit
) {
    val tint by animateColorAsState(
        targetValue = if (isOn) accentColor else offTint,
        animationSpec = tween(380, easing = FastOutSlowInEasing),
        label = "modeGlyph"
    )
    val activePulse by animateFloatAsState(
        targetValue = if (isOn) 1.06f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "glyphPulse"
    )
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.88f else 1f,
        animationSpec = AppleMusicTapSpring,
        label = "glyphPress"
    )
    IconButton(
        onClick = onClick,
        interactionSource = interaction,
        modifier = Modifier
            .size(48.dp)
            .scale(pressScale * activePulse)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(23.dp),
            tint = tint
        )
    }
}

@Composable
private fun AppleMusicNowPlayingSecondaryRow(
    isFavorite: Boolean,
    accentColor: Color,
    onToggleFavorite: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onMore: () -> Unit,
    chromeTint: Color,
    chromeMuted: Color,
    modifier: Modifier = Modifier
) {
    val favTint by animateColorAsState(
        targetValue = if (isFavorite) accentColor else chromeMuted,
        animationSpec = tween(420, easing = FastOutSlowInEasing),
        label = "favTint"
    )
    val favHeart by animateFloatAsState(
        targetValue = if (isFavorite) 1.12f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "favBeat"
    )
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        PlayerPressableIconButton(
            onClick = onToggleFavorite,
            modifier = Modifier.scale(favHeart)
        ) {
            Crossfade(
                targetState = isFavorite,
                animationSpec = tween(280, easing = FastOutSlowInEasing),
                label = "favIcon"
            ) { fav ->
                Icon(
                    imageVector = if (fav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = if (fav) {
                        stringResource(R.string.remove_from_favorites)
                    } else {
                        stringResource(R.string.add_to_favorites)
                    },
                    tint = favTint
                )
            }
        }
        PlayerPressableIconButton(onClick = onAddToPlaylist) {
            Icon(
                imageVector = Icons.Rounded.PlaylistAdd,
                contentDescription = stringResource(R.string.add_to_playlist),
                tint = chromeTint.copy(alpha = 0.88f)
            )
        }
        PlayerPressableIconButton(onClick = onMore) {
            Icon(
                imageVector = Icons.Rounded.MoreHoriz,
                contentDescription = stringResource(R.string.more_options),
                tint = chromeTint.copy(alpha = 0.88f)
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
