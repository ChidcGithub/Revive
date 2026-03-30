package com.music.revive.presentation.screen.fullscreenlyrics

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
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
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.music.revive.R
import com.music.revive.data.local.LyricsPreferences
import com.music.revive.data.local.PlayerPreferences
import com.music.revive.data.lyric.LyricRepository
import com.music.revive.domain.model.Lyric
import com.music.revive.domain.model.LyricLine
import com.music.revive.domain.model.WordSegment
import com.music.revive.presentation.theme.rememberAlbumColors
import com.music.revive.service.MusicPlayer
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import kotlin.math.*

// ============================================================================
// Activity
// ============================================================================

@AndroidEntryPoint
class FullScreenLyricsActivity : ComponentActivity() {

    @Inject
    lateinit var musicPlayer: MusicPlayer
    
    @Inject
    lateinit var lyricsPreferences: LyricsPreferences
    
    @Inject
    lateinit var playerPreferences: PlayerPreferences
    
    @Inject
    lateinit var lyricRepository: LyricRepository
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        enableEdgeToEdge()
        
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior = 
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(android.view.WindowInsets.Type.systemBars())
        
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        setContent {
            AppleMusicFullScreenLyrics(
                musicPlayer = musicPlayer,
                lyricsPreferences = lyricsPreferences,
                playerPreferences = playerPreferences,
                lyricRepository = lyricRepository,
                onNavigateBack = { finish() }
            )
        }
    }
    
    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, FullScreenLyricsActivity::class.java)
        }
    }
}

// ============================================================================
// ViewModel
// ============================================================================

class FullScreenLyricsViewModel(
    private val musicPlayer: MusicPlayer,
    private val lyricsPreferences: LyricsPreferences,
    private val playerPreferences: PlayerPreferences,
    private val lyricRepository: LyricRepository
) : ViewModel() {

    val playerState = musicPlayer.playerState
    val currentSong = musicPlayer.currentSong
    
    private val _currentLyrics = MutableStateFlow<Lyric>(Lyric.Empty)
    val currentLyrics: StateFlow<Lyric> = _currentLyrics.asStateFlow()
    
    private val _isLoadingLyrics = MutableStateFlow(false)
    val isLoadingLyrics: StateFlow<Boolean> = _isLoadingLyrics.asStateFlow()
    
    val lyricsFontSize = lyricsPreferences.lyricsFontSize
    val showTranslation = lyricsPreferences.showTranslation
    val lyricsDisplayStyle = lyricsPreferences.lyricsDisplayStyle
    val enableGlow = lyricsPreferences.enableGlow
    val enableKaraoke = lyricsPreferences.enableKaraoke
    val enableBlur = lyricsPreferences.enableBlur
    val enableShaderEffect = playerPreferences.enableShaderEffect
    
    init {
        viewModelScope.launch {
            currentSong.collect { song ->
                if (song != null) {
                    loadLyrics(song)
                } else {
                    _currentLyrics.value = Lyric.Empty
                }
            }
        }
    }
    
    private fun loadLyrics(song: com.music.revive.domain.model.Song) {
        viewModelScope.launch {
            _isLoadingLyrics.value = true
            try {
                val lyrics = lyricRepository.loadLyrics(song)
                _currentLyrics.value = lyrics
            } catch (e: Exception) {
                _currentLyrics.value = Lyric.Empty
            } finally {
                _isLoadingLyrics.value = false
            }
        }
    }
}

// ============================================================================
// Main Composable - Apple Music Style Full Screen Lyrics
// ============================================================================

@Composable
private fun AppleMusicFullScreenLyrics(
    musicPlayer: MusicPlayer,
    lyricsPreferences: LyricsPreferences,
    playerPreferences: PlayerPreferences,
    lyricRepository: LyricRepository,
    onNavigateBack: () -> Unit
) {
    val viewModel = remember { 
        FullScreenLyricsViewModel(musicPlayer, lyricsPreferences, playerPreferences, lyricRepository) 
    }
    
    val playerState by viewModel.playerState.collectAsState()
    val currentLyrics by viewModel.currentLyrics.collectAsState()
    val isLoadingLyrics by viewModel.isLoadingLyrics.collectAsState()
    val lyricsFontSize by viewModel.lyricsFontSize.collectAsState(initial = 1.0f)
    val showTranslation by viewModel.showTranslation.collectAsState(initial = true)
    val lyricsDisplayStyle by viewModel.lyricsDisplayStyle.collectAsState(initial = 0)
    val enableGlow by viewModel.enableGlow.collectAsState(initial = true)
    val enableKaraoke by viewModel.enableKaraoke.collectAsState(initial = true)
    
    val song = playerState.currentSong
    val context = LocalContext.current
    
    // Dynamic colors from album art
    val isSystemInDarkTheme = androidx.compose.foundation.isSystemInDarkTheme()
    val albumColors = rememberAlbumColors(song?.albumArtUri, isSystemInDarkTheme)
    val accentColor = albumColors.primary
    
    // UI states
    var showControls by remember { mutableStateOf(true) }
    var isExiting by remember { mutableStateOf(false) }
    var isScrubbing by remember { mutableStateOf(false) }
    var scrubPosition by remember { mutableStateOf(0L) }
    
    // Auto-hide controls
    LaunchedEffect(showControls, isScrubbing) {
        if (showControls && !isScrubbing) {
            delay(5000)
            showControls = false
        }
    }
    
    // Animations
    val smoothEasing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1.0f)
    val controlsAlpha by animateFloatAsState(
        targetValue = if (showControls && !isExiting) 1f else 0f,
        animationSpec = tween(300, easing = smoothEasing),
        label = "controlsAlpha"
    )
    
    val scrubberAlpha by animateFloatAsState(
        targetValue = if (isScrubbing) 1f else 0f,
        animationSpec = tween(200),
        label = "scrubberAlpha"
    )

    // Main container
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { showControls = !showControls }
                )
            }
    ) {
        // 1. Dynamic animated background
        LyricsAmbientBackground(
            albumArtUri = song?.albumArtUri,
            albumColors = albumColors,
            isPlaying = playerState.isPlaying,
            modifier = Modifier.fillMaxSize()
        )
        
        // 2. Lyrics content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            if (isLoadingLyrics) {
                // Loading state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = accentColor,
                        modifier = Modifier.size(48.dp),
                        strokeWidth = 3.dp
                    )
                }
            } else if (currentLyrics.isEmpty) {
                // Empty lyrics state
                EmptyLyricsState(
                    accentColor = accentColor,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Lyrics display with gesture handling
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectVerticalDragGestures(
                                onDragStart = {
                                    isScrubbing = true
                                    scrubPosition = playerState.position
                                },
                                onDragEnd = {
                                    isScrubbing = false
                                    musicPlayer.seekTo(scrubPosition)
                                },
                                onVerticalDrag = { _, dragAmount ->
                                    val duration = playerState.duration
                                    if (duration > 0) {
                                        val dragPercent = -dragAmount / size.height
                                        val timeChange = (dragPercent * duration).toLong()
                                        scrubPosition = (scrubPosition + timeChange)
                                            .coerceIn(0L, duration)
                                    }
                                }
                            )
                        }
                ) {
                    // Main lyrics view
                    AppleMusicLyricsView(
                        lyric = currentLyrics,
                        currentPositionMs = if (isScrubbing) scrubPosition else playerState.position,
                        fontSizeMultiplier = lyricsFontSize * 1.2f,
                        showTranslation = showTranslation,
                        isCentered = lyricsDisplayStyle == 0,
                        enableGlow = enableGlow,
                        enableKaraoke = enableKaraoke,
                        accentColor = accentColor,
                        modifier = Modifier.fillMaxSize()
                    )
                    
                    // Scrub indicator
                    AnimatedVisibility(
                        visible = isScrubbing,
                        modifier = Modifier.align(Alignment.Center),
                        enter = fadeIn(tween(200)),
                        exit = fadeOut(tween(200))
                    ) {
                        ScrubIndicator(
                            position = scrubPosition,
                            duration = playerState.duration,
                            accentColor = accentColor,
                            modifier = Modifier.alpha(scrubberAlpha)
                        )
                    }
                }
            }
        }
        
        // 3. Top bar with song info
        LyricsTopBar(
            song = song,
            accentColor = accentColor,
            onBackClick = {
                isExiting = true
                onNavigateBack()
            },
            alpha = controlsAlpha,
            modifier = Modifier.align(Alignment.TopCenter)
        )
        
        // 4. Bottom playback controls
        LyricsBottomControls(
            song = song,
            isPlaying = playerState.isPlaying,
            accentColor = accentColor,
            onPreviousClick = { musicPlayer.playPrevious() },
            onPlayPauseClick = { musicPlayer.playPause() },
            onNextClick = { musicPlayer.playNext() },
            alpha = controlsAlpha,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

// ============================================================================
// Ambient Background
// ============================================================================

@Composable
private fun LyricsAmbientBackground(
    albumArtUri: String?,
    albumColors: com.music.revive.presentation.theme.AlbumColors,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // Breathing animation
    val transition = rememberInfiniteTransition(label = "ambient")
    val breatheAlpha by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breatheAlpha"
    )
    
    // Color shift animation
    val colorShift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "colorShift"
    )

    Box(modifier = modifier) {
        // Base gradient
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.background.copy(alpha = 0.95f)
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
                    .blur(100.dp)
                    .graphicsLayer {
                        alpha = if (isPlaying) breatheAlpha * 0.6f else 0.35f
                        scaleX = 1.5f
                        scaleY = 1.5f
                    },
                contentScale = ContentScale.Crop
            )
        }
        
        // Floating color orbs
        FloatingColorOrbs(
            primaryColor = albumColors.primary,
            secondaryColor = albumColors.secondary,
            tertiaryColor = albumColors.tertiary,
            isPlaying = isPlaying,
            colorShift = colorShift,
            modifier = Modifier.fillMaxSize()
        )
        
        // Gradient overlay for readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to MaterialTheme.colorScheme.background.copy(alpha = 0.7f),
                            0.3f to MaterialTheme.colorScheme.background.copy(alpha = 0.3f),
                            0.7f to MaterialTheme.colorScheme.background.copy(alpha = 0.3f),
                            1f to MaterialTheme.colorScheme.background.copy(alpha = 0.85f)
                        )
                    )
                )
        )
    }
}

@Composable
private fun FloatingColorOrbs(
    primaryColor: Color,
    secondaryColor: Color,
    tertiaryColor: Color,
    isPlaying: Boolean,
    colorShift: Float,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "orbs")
    
    val offsetX by transition.animateFloat(
        initialValue = 0f,
        targetValue = 60f,
        animationSpec = infiniteRepeatable(
            animation = tween(15000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offsetX"
    )
    
    val offsetY by transition.animateFloat(
        initialValue = 0f,
        targetValue = 40f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offsetY"
    )

    val alpha by animateFloatAsState(
        targetValue = if (isPlaying) 0.35f else 0.2f,
        animationSpec = tween(500),
        label = "orbAlpha"
    )

    Box(modifier = modifier) {
        // Primary orb - top left
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.toInt(), offsetY.toInt()) }
                .size(350.dp)
                .blur(80.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = alpha),
                            primaryColor.copy(alpha = alpha * 0.5f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )
        
        // Secondary orb - bottom right
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset { IntOffset((-offsetX * 0.7f).toInt(), (-offsetY * 0.7f).toInt()) }
                .size(280.dp)
                .blur(70.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            secondaryColor.copy(alpha = alpha * 0.8f),
                            secondaryColor.copy(alpha = alpha * 0.3f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )
        
        // Tertiary orb - center
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .offset { IntOffset((offsetX * 0.3f).toInt(), (offsetY * 0.5f).toInt()) }
                .size(200.dp)
                .blur(60.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            tertiaryColor.copy(alpha = alpha * 0.6f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )
    }
}

// ============================================================================
// Empty Lyrics State
// ============================================================================

@Composable
private fun EmptyLyricsState(
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                modifier = Modifier.size(100.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Lyrics,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
            
            Text(
                text = stringResource(R.string.no_lyrics),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Text(
                text = stringResource(R.string.no_lyrics_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
        }
    }
}

// ============================================================================
// Top Bar
// ============================================================================

@Composable
private fun LyricsTopBar(
    song: com.music.revive.domain.model.Song?,
    accentColor: Color,
    onBackClick: () -> Unit,
    alpha: Float,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .alpha(alpha),
        color = Color.Transparent
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
        ) {
            // Drag indicator
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 12.dp, bottom = 8.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .background(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                        shape = CircleShape
                    )
            )
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back button
                Surface(
                    onClick = onBackClick,
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.KeyboardArrowDown,
                            contentDescription = stringResource(R.string.back),
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                // Song info
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song?.title ?: "",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = song?.artist ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = accentColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// ============================================================================
// Bottom Controls
// ============================================================================

@Composable
private fun LyricsBottomControls(
    song: com.music.revive.domain.model.Song?,
    isPlaying: Boolean,
    accentColor: Color,
    onPreviousClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit,
    alpha: Float,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .alpha(alpha),
        color = Color.Transparent
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            // Album info row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Album art thumbnail
                Surface(
                    modifier = Modifier.size(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    if (song?.albumArtUri != null) {
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
                
                // Album details
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song?.album ?: "",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = song?.artist ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            // Playback controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Previous button
                Surface(
                    onClick = onPreviousClick,
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                    modifier = Modifier.size(52.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.SkipPrevious,
                            contentDescription = stringResource(R.string.previous),
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                
                // Play/Pause button - prominent
                Surface(
                    onClick = onPlayPauseClick,
                    shape = CircleShape,
                    color = accentColor,
                    modifier = Modifier.size(68.dp)
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
                                tint = Color.White,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                    }
                }
                
                // Next button
                Surface(
                    onClick = onNextClick,
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                    modifier = Modifier.size(52.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.SkipNext,
                            contentDescription = stringResource(R.string.next),
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    }
}

// ============================================================================
// Scrub Indicator
// ============================================================================

@Composable
private fun ScrubIndicator(
    position: Long,
    duration: Long,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 28.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Time display
            Text(
                text = formatTimeLyrics(position),
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = accentColor
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = " / ${formatTimeLyrics(duration)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Progress ring
            Box(
                modifier = Modifier.size(64.dp),
                contentAlignment = Alignment.Center
            ) {
                val progress = if (duration > 0) position.toFloat() / duration else 0f
                val backgroundColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Background circle
                    drawCircle(
                        color = backgroundColor,
                        style = Stroke(width = 5.dp.toPx())
                    )
                    
                    // Progress arc
                    if (progress > 0) {
                        drawArc(
                            color = accentColor,
                            startAngle = -90f,
                            sweepAngle = 360f * progress,
                            useCenter = false,
                            style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                }
                
                Icon(
                    imageVector = Icons.Rounded.SwipeVertical,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

private fun formatTimeLyrics(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}

// ============================================================================
// Apple Music Style Lyrics View
// ============================================================================

@Composable
private fun AppleMusicLyricsView(
    lyric: Lyric,
    currentPositionMs: Long,
    fontSizeMultiplier: Float,
    showTranslation: Boolean,
    isCentered: Boolean,
    enableGlow: Boolean,
    enableKaraoke: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    if (lyric.isSynced) {
        SyncedLyricsView(
            lyric = lyric,
            currentPositionMs = currentPositionMs,
            fontSizeMultiplier = fontSizeMultiplier,
            showTranslation = showTranslation,
            isCentered = isCentered,
            enableGlow = enableGlow,
            enableKaraoke = enableKaraoke,
            accentColor = accentColor,
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

// ============================================================================
// Synced Lyrics View
// ============================================================================

@Composable
private fun SyncedLyricsView(
    lyric: Lyric,
    currentPositionMs: Long,
    fontSizeMultiplier: Float,
    showTranslation: Boolean,
    isCentered: Boolean,
    enableGlow: Boolean,
    enableKaraoke: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    
    var isUserScrolling by remember { mutableStateOf(false) }
    var lastUserScrollTime by remember { mutableLongStateOf(0L) }
    
    val currentLineIndex = remember(currentPositionMs, lyric.lines) {
        lyric.findCurrentLineIndex(currentPositionMs)
    }
    
    val lineProgress = remember(currentPositionMs, currentLineIndex) {
        lyric.lineProgressAt(currentLineIndex, currentPositionMs)
    }
    
    // Auto-scroll with smooth animation
    LaunchedEffect(currentLineIndex) {
        if (currentLineIndex >= 0 && !isUserScrolling) {
            val viewportHeight = listState.layoutInfo.viewportEndOffset
            val targetOffset = -(viewportHeight * 0.38f).toInt()
            
            listState.animateScrollToItem(
                index = currentLineIndex,
                scrollOffset = targetOffset
            )
        }
    }
    
    // Reset user scrolling after delay
    LaunchedEffect(isUserScrolling) {
        if (isUserScrolling) {
            delay(4000)
            isUserScrolling = false
        }
    }

    Box(modifier = modifier) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp)
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
            contentPadding = PaddingValues(top = 180.dp, bottom = 200.dp),
            horizontalAlignment = if (isCentered) Alignment.CenterHorizontally else Alignment.Start,
            userScrollEnabled = true
        ) {
            itemsIndexed(
                items = lyric.lines,
                key = { index, _ -> index }
            ) { index, line ->
                val isActive = index == currentLineIndex
                val distance = if (currentLineIndex >= 0) {
                    abs(index - currentLineIndex)
                } else Int.MAX_VALUE
                
                val currentLineProgress = if (isActive && enableKaraoke) lineProgress else if (isActive) 1f else 0f
                
                LyricLineView(
                    line = line,
                    isActive = isActive,
                    distance = distance,
                    lineProgress = currentLineProgress,
                    currentPositionMs = if (isActive) currentPositionMs else 0L,
                    fontSizeMultiplier = fontSizeMultiplier,
                    showTranslation = showTranslation && lyric.hasTranslation,
                    textAlign = if (isCentered) TextAlign.Center else TextAlign.Start,
                    enableGlow = enableGlow,
                    enableKaraoke = enableKaraoke,
                    accentColor = accentColor,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        
        // Top gradient fade
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(120.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background.copy(alpha = 0.95f),
                            MaterialTheme.colorScheme.background.copy(alpha = 0.7f),
                            MaterialTheme.colorScheme.background.copy(alpha = 0.3f),
                            Color.Transparent
                        )
                    )
                )
        )
        
        // Bottom gradient fade
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(120.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            MaterialTheme.colorScheme.background.copy(alpha = 0.3f),
                            MaterialTheme.colorScheme.background.copy(alpha = 0.7f),
                            MaterialTheme.colorScheme.background.copy(alpha = 0.95f)
                        )
                    )
                )
        )
    }
}

// ============================================================================
// Lyric Line View
// ============================================================================

@Composable
private fun LyricLineView(
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
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    
    // Scale animation
    val scale by animateFloatAsState(
        targetValue = when {
            isActive -> 1f
            distance == 1 -> 0.95f
            distance == 2 -> 0.92f
            else -> 0.88f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "lineScale"
    )
    
    // Alpha animation
    val alpha by animateFloatAsState(
        targetValue = when {
            isActive -> 1f
            distance == 1 -> 0.7f
            distance == 2 -> 0.5f
            distance == 3 -> 0.35f
            distance == 4 -> 0.2f
            else -> 0.1f
        },
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "lineAlpha"
    )
    
    // Y offset for floating effect
    val yOffset by animateDpAsState(
        targetValue = when {
            isActive -> (-4).dp
            distance == 1 -> (-2).dp
            else -> 0.dp
        },
        animationSpec = tween(300),
        label = "lineYOffset"
    )
    
    // Glow intensity
    val glowAlpha by animateFloatAsState(
        targetValue = if (isActive && enableGlow) 0.2f else 0f,
        animationSpec = tween(400),
        label = "glowAlpha"
    )
    
    // Pulse animation when line becomes active
    val pulseAnimatable = remember { Animatable(1f) }
    LaunchedEffect(isActive) {
        if (isActive) {
            pulseAnimatable.animateTo(
                targetValue = 1.03f,
                animationSpec = tween(150, easing = LinearEasing)
            )
            pulseAnimatable.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
    }
    
    val pulseScale = pulseAnimatable.value
    
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
    
    // Colors
    val activeColor = accentColor
    val inactiveColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
    
    val density = LocalDensity.current
    
    Column(
        modifier = modifier
            .graphicsLayer {
                this.scaleX = scale * pulseScale
                this.scaleY = scale * pulseScale
                this.alpha = alpha
                this.translationY = with(density) { yOffset.toPx() }
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
        // Main lyric text with karaoke effect
        if (isActive && enableKaraoke && line.words != null && line.words.isNotEmpty()) {
            KaraokeText(
                text = line.text,
                words = line.words,
                lineProgress = lineProgress,
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
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    },
                    textAlign = textAlign,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// ============================================================================
// Karaoke Text with Dynamic Color Fill - Optimized
// ============================================================================

@Composable
private fun KaraokeText(
    text: String,
    words: List<WordSegment>,
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
    
    // Smooth animated progress
    val animatedProgress = remember { Animatable(0f) }
    val clipProgress = calculateWordProgress(words, currentPositionMs, measuredText)
    
    LaunchedEffect(clipProgress) {
        animatedProgress.snapTo(clipProgress)
    }
    
    val density = LocalDensity.current
    
    // Glow effect for active text
    val glowAlpha = 0.3f
    
    androidx.compose.foundation.Canvas(
        modifier = modifier
            .height(with(density) { textHeight.toDp() + 8.dp })
    ) {
        val canvasWidth = size.width
        val textX = when (textAlign) {
            TextAlign.Center -> (canvasWidth - textWidth) / 2f
            TextAlign.End -> canvasWidth - textWidth
            else -> 0f
        }
        
        val textY = 4.dp.toPx() // Center vertically with extra padding
        
        // Draw inactive text (base layer)
        drawText(
            textLayoutResult = measuredText,
            color = inactiveColor,
            topLeft = Offset(textX, textY)
        )
        
        // Draw active text with clip and glow
        if (animatedProgress.value > 0.01f) {
            // Glow effect layer (optional, under the active text)
            drawIntoCanvas { canvas ->
                canvas.save()
                canvas.clipRect(
                    left = textX,
                    top = 0f,
                    right = textX + textWidth * animatedProgress.value,
                    bottom = size.height
                )
                
                // Draw glow effect
                val glowPaint = androidx.compose.ui.graphics.Paint().apply {
                    this.color = activeColor.copy(alpha = glowAlpha)
                    this.asFrameworkPaint().apply {
                        setShadowLayer(8.dp.toPx(), 0f, 0f, activeColor.copy(alpha = 0.5f).toArgb())
                    }
                }
                
                // Draw text with glow
                drawText(
                    textLayoutResult = measuredText,
                    color = activeColor,
                    topLeft = Offset(textX, textY)
                )
                
                canvas.restore()
            }
            
            // Main active text layer with sharp clip
            clipRect(
                left = textX,
                top = 0f,
                right = textX + textWidth * animatedProgress.value,
                bottom = size.height
            ) {
                drawText(
                    textLayoutResult = measuredText,
                    color = activeColor,
                    topLeft = Offset(textX, textY)
                )
            }
        }
    }
}

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

// ============================================================================
// Plain Lyrics View
// ============================================================================

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
            .padding(horizontal = 28.dp),
        contentPadding = PaddingValues(vertical = 100.dp),
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
