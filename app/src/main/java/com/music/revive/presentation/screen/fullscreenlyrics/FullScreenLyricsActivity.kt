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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
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
import com.music.revive.presentation.components.LyricsView
import com.music.revive.presentation.theme.rememberAlbumColors
import com.music.revive.service.MusicPlayer
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import kotlin.math.*

/**
 * Modern Apple Music Style Full Screen Lyrics Activity
 * Features:
 * - Dynamic background with animated color orbs
 * - Smooth gesture controls (tap, swipe, pinch)
 * - Floating controls with auto-hide
 * - Progress scrubbing via vertical swipe
 * - Dynamic accent colors from album art
 */
@AndroidEntryPoint
class FullScreenLyricsActivity : ComponentActivity() {

    @Inject lateinit var musicPlayer: MusicPlayer
    @Inject lateinit var lyricsPreferences: LyricsPreferences
    @Inject lateinit var playerPreferences: PlayerPreferences
    @Inject lateinit var lyricRepository: LyricRepository
    
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
            ModernFullScreenLyrics(
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

@Composable
private fun ModernFullScreenLyrics(
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
    val enableBlur by viewModel.enableBlur.collectAsState(initial = false)
    val enableShaderEffect by viewModel.enableShaderEffect.collectAsState(initial = false)
    
    val song = playerState.currentSong
    val context = LocalContext.current
    
    // Extract dynamic colors from album art
    val isSystemInDarkTheme = androidx.compose.foundation.isSystemInDarkTheme()
    val albumColors = rememberAlbumColors(song?.albumArtUri, isSystemInDarkTheme)
    val accentColor = albumColors.primary
    
    // Gesture states
    var showControls by remember { mutableStateOf(false) }
    var isExiting by remember { mutableStateOf(false) }
    var isScrubbing by remember { mutableStateOf(false) }
    var scrubPosition by remember { mutableStateOf(0L) }
    
    // Auto-hide controls
    LaunchedEffect(showControls, isScrubbing) {
        if (showControls && !isScrubbing) {
            delay(4000)
            showControls = false
        }
    }
    
    // Smooth animations
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
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { showControls = !showControls }
                )
            }
    ) {
        // Dynamic animated background
        ModernLyricsBackground(
            albumArtUri = song?.albumArtUri,
            albumColors = albumColors,
            modifier = Modifier.fillMaxSize()
        )
        
        // Lyrics content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            if (isLoadingLyrics) {
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
            } else {
                // Lyrics with gesture handling
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
                    LyricsView(
                        lyric = currentLyrics,
                        currentPositionMs = if (isScrubbing) scrubPosition else playerState.position,
                        fontSizeMultiplier = lyricsFontSize * 1.15f,
                        showTranslation = showTranslation,
                        isCentered = lyricsDisplayStyle == 0,
                        enableGlow = enableGlow,
                        enableKaraoke = enableKaraoke,
                        enableHapticFeedback = false,
                        enableBlur = enableBlur,
                        enableShader = enableShaderEffect,
                        blurRadius = 6f,
                        accentColor = accentColor,
                        modifier = Modifier.fillMaxSize()
                    )
                    
                    // Scrub indicator
                    AnimatedVisibility(
                        visible = isScrubbing,
                        modifier = Modifier.align(Alignment.Center)
                    ) {
                        ModernScrubIndicator(
                            position = scrubPosition,
                            duration = playerState.duration,
                            accentColor = accentColor,
                            modifier = Modifier.alpha(scrubberAlpha)
                        )
                    }
                }
            }
        }
        
        // Top bar
        ModernLyricsTopBar(
            song = song,
            accentColor = accentColor,
            onBackClick = {
                isExiting = true
                onNavigateBack()
            },
            alpha = controlsAlpha,
            modifier = Modifier.align(Alignment.TopCenter)
        )
        
        // Bottom bar
        ModernLyricsBottomBar(
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

/**
 * Modern animated background with color orbs
 */
@Composable
private fun ModernLyricsBackground(
    albumArtUri: String?,
    albumColors: com.music.revive.presentation.theme.AlbumColors,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "background")
    
    // Multiple animated orbs for depth
    val orb1Offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(25000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb1Offset"
    )
    
    val orb2Offset by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(30000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb2Offset"
    )
    
    val orb3Offset by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb3Offset"
    )
    
    Box(modifier = modifier.fillMaxSize()) {
        // Base dark gradient
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0a0a0a),
                            Color.Black,
                            Color(0xFF050505)
                        )
                    )
                )
        )
        
        // Blurred album art backdrop
        if (albumArtUri != null) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(albumArtUri)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(80.dp)
                    .graphicsLayer {
                        alpha = 0.4f
                        scaleX = 1.5f
                        scaleY = 1.5f
                    },
                contentScale = ContentScale.Crop
            )
        }
        
        // Animated color orbs
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val x = size.width * (0.2f + orb1Offset * 0.3f)
                    val y = size.height * (0.3f + orb1Offset * 0.2f)
                    translationX = x - size.width / 2
                    translationY = y - size.height / 2
                }
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                albumColors.primary.copy(alpha = 0.25f),
                                albumColors.primary.copy(alpha = 0.08f),
                                Color.Transparent
                            )
                        ),
                        radius = size.width * 0.6f
                    )
                }
        )
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val x = size.width * (0.8f - orb2Offset * 0.3f)
                    val y = size.height * (0.6f - orb2Offset * 0.2f)
                    translationX = x - size.width / 2
                    translationY = y - size.height / 2
                }
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                albumColors.secondary.copy(alpha = 0.2f),
                                albumColors.secondary.copy(alpha = 0.06f),
                                Color.Transparent
                            )
                        ),
                        radius = size.width * 0.5f
                    )
                }
        )
        
        // Vignette effect
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.3f),
                            Color.Black.copy(alpha = 0.6f)
                        ),
                        center = Offset(0f, 0f),
                        radius = Float.POSITIVE_INFINITY
                    )
                )
        )
    }
}

/**
 * Modern top bar with improved design
 */
@Composable
private fun ModernLyricsTopBar(
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
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Drag indicator
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .width(32.dp)
                    .height(4.dp)
                    .background(
                        color = Color.White.copy(alpha = 0.3f),
                        shape = CircleShape
                    )
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back button with subtle background
                Surface(
                    onClick = onBackClick,
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.4f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.KeyboardArrowDown,
                            contentDescription = "返回",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                // Song info
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = song?.title ?: "",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = Color.White,
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

/**
 * Modern bottom bar with improved controls
 */
@Composable
private fun ModernLyricsBottomBar(
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
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // Album info
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Album art thumbnail
                if (song?.albumArtUri != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(song.albumArtUri)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                    
                    Spacer(modifier = Modifier.width(12.dp))
                }
                
                // Album details
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = song?.album ?: "",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = song?.artist ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f),
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
                    color = Color.Black.copy(alpha = 0.4f),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.SkipPrevious,
                            contentDescription = "上一首",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                
                // Play/Pause button (prominent)
                Surface(
                    onClick = onPlayPauseClick,
                    shape = CircleShape,
                    color = accentColor,
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (isPlaying) "暂停" else "播放",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
                
                // Next button
                Surface(
                    onClick = onNextClick,
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.4f),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.SkipNext,
                            contentDescription = "下一首",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Modern scrub indicator for gesture feedback
 */
@Composable
private fun ModernScrubIndicator(
    position: Long,
    duration: Long,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = Color.Black.copy(alpha = 0.7f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = formatTime(position),
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = accentColor
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = formatTime(duration),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Progress ring
            Box(
                modifier = Modifier.size(60.dp),
                contentAlignment = Alignment.Center
            ) {
                val progress = if (duration > 0) position.toFloat() / duration else 0f
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Background circle
                    drawCircle(
                        color = Color.White.copy(alpha = 0.2f),
                        style = Stroke(width = 4.dp.toPx())
                    )
                    
                    // Progress arc
                    if (progress > 0) {
                        drawArc(
                            color = accentColor,
                            startAngle = -90f,
                            sweepAngle = 360f * progress,
                            useCenter = false,
                            style = Stroke(width = 4.dp.toPx())
                        )
                    }
                }
                
                Icon(
                    imageVector = Icons.Rounded.SwipeVertical,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

/**
 * Format milliseconds to MM:SS
 */
private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}