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
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.music.revive.data.local.LyricsPreferences
import com.music.revive.data.local.PlayerPreferences
import com.music.revive.data.lyric.LyricRepository
import com.music.revive.domain.model.Lyric
import com.music.revive.presentation.components.LyricsView
import com.music.revive.service.MusicPlayer
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Apple Music Style Full Screen Lyrics Activity
 * 
 * Design Principles:
 * - Immersive full-screen experience
 * - Dynamic blurred album art background
 * - Large, elegant lyrics display
 * - Minimal controls that fade when not in use
 * - Tap anywhere to show/hide controls
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
        
        // Full immersive mode
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior = 
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(android.view.WindowInsets.Type.systemBars())
        
        // Keep screen on while viewing lyrics
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        setContent {
            MaterialTheme {
                AppleMusicFullScreenLyrics(
                    musicPlayer = musicPlayer,
                    lyricsPreferences = lyricsPreferences,
                    playerPreferences = playerPreferences,
                    lyricRepository = lyricRepository,
                    onNavigateBack = { finish() }
                )
            }
        }
    }
    
    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, FullScreenLyricsActivity::class.java)
        }
    }
}

/**
 * View model for full screen lyrics
 */
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

/**
 * Apple Music-style full screen lyrics screen
 */
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
    val enableBlur by viewModel.enableBlur.collectAsState(initial = false)
    val enableShaderEffect by viewModel.enableShaderEffect.collectAsState(initial = false)
    
    val song = playerState.currentSong
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Controls visibility state
    var showControls by remember { mutableStateOf(false) }
    var isExiting by remember { mutableStateOf(false) }
    
    // Extract palette colors for background
    var paletteColors by remember { mutableStateOf<PaletteColors?>(null) }
    
    LaunchedEffect(song?.albumArtUri) {
        song?.albumArtUri?.let { uri ->
            paletteColors = extractPaletteColors(context, uri)
        }
    }
    
    // Auto-hide controls after delay
    LaunchedEffect(showControls) {
        if (showControls) {
            delay(4000)
            showControls = false
        }
    }
    
    // Animation states
    val smoothEasing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1.0f)
    
    val controlsAlpha by animateFloatAsState(
        targetValue = if (showControls && !isExiting) 1f else 0f,
        animationSpec = tween(300, easing = smoothEasing),
        label = "controlsAlpha"
    )
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        showControls = !showControls
                    },
                    onDoubleTap = { 
                        isExiting = true
                        onNavigateBack()
                    }
                )
            }
    ) {
        // Dynamic blurred background
        AppleMusicLyricsBackground(
            song = song,
            colors = paletteColors,
            modifier = Modifier.fillMaxSize()
        )
        
        // Main lyrics content
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
                        color = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                }
            } else {
                // Lyrics view
                LyricsView(
                    lyric = currentLyrics,
                    currentPositionMs = playerState.position,
                    fontSizeMultiplier = lyricsFontSize * 1.15f, // Slightly larger in full screen
                    showTranslation = showTranslation,
                    isCentered = lyricsDisplayStyle == 0,
                    enableGlow = enableGlow,
                    enableKaraoke = enableKaraoke,
                    enableHapticFeedback = false,
                    enableBlur = enableBlur,
                    enableShader = enableShaderEffect,
                    blurRadius = 6f,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        
        // Top bar - Song info and back button
        AnimatedVisibility(
            visible = showControls && !isExiting,
            enter = fadeIn(tween(300)) + slideInVertically(
                initialOffsetY = { -it },
                animationSpec = tween(300, easing = smoothEasing)
            ),
            exit = fadeOut(tween(200)) + slideOutVertically(
                targetOffsetY = { -it },
                animationSpec = tween(200, easing = smoothEasing)
            ),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            AppleMusicLyricsTopBar(
                song = song,
                onBackClick = {
                    isExiting = true
                    onNavigateBack()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
            )
        }
        
        // Bottom bar - Playback controls
        AnimatedVisibility(
            visible = showControls && !isExiting,
            enter = fadeIn(tween(300)) + slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(300, easing = smoothEasing)
            ),
            exit = fadeOut(tween(200)) + slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(200, easing = smoothEasing)
            ),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            AppleMusicLyricsBottomBar(
                song = song,
                isPlaying = playerState.isPlaying,
                onPreviousClick = { musicPlayer.playPrevious() },
                onPlayPauseClick = { musicPlayer.playPause() },
                onNextClick = { musicPlayer.playNext() },
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            )
        }
    }
}

/**
 * Apple Music-style lyrics background with blurred album art
 */
@Composable
private fun AppleMusicLyricsBackground(
    song: com.music.revive.domain.model.Song?,
    colors: PaletteColors?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // Animated gradient offset for subtle movement
    val infiniteTransition = rememberInfiniteTransition(label = "background")
    
    val gradientOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(30000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gradientOffset"
    )

    Box(modifier = modifier) {
        // Blurred album art
        if (song?.albumArtUri != null) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(song.albumArtUri)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(100.dp)
                    .graphicsLayer {
                        scaleX = 1.8f
                        scaleY = 1.8f
                        alpha = 0.5f
                    },
                contentScale = ContentScale.Crop
            )
        }
        
        // Dark gradient overlay for readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.6f),
                            Color.Black.copy(alpha = 0.75f),
                            Color.Black.copy(alpha = 0.85f),
                            Color.Black.copy(alpha = 0.75f),
                            Color.Black.copy(alpha = 0.6f)
                        )
                    )
                )
        )
        
        // Dynamic color orbs
        if (colors != null) {
            // Primary color orb
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationX = size.width * 0.1f * gradientOffset
                        translationY = size.height * 0.05f * (1f - gradientOffset)
                    }
                    .drawBehind {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    colors.vibrant.copy(alpha = 0.2f),
                                    colors.vibrant.copy(alpha = 0.1f),
                                    Color.Transparent
                                ),
                                center = Offset(size.width * 0.3f, size.height * 0.3f),
                                radius = size.width * 0.5f
                            ),
                            center = Offset(size.width * 0.3f, size.height * 0.3f),
                            radius = size.width * 0.5f
                        )
                    }
            )
            
            // Secondary color orb
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationX = size.width * -0.1f * gradientOffset
                        translationY = size.height * 0.05f * gradientOffset
                    }
                    .drawBehind {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    colors.dominant.copy(alpha = 0.15f),
                                    colors.dominant.copy(alpha = 0.05f),
                                    Color.Transparent
                                ),
                                center = Offset(size.width * 0.7f, size.height * 0.6f),
                                radius = size.width * 0.4f
                            ),
                            center = Offset(size.width * 0.7f, size.height * 0.6f),
                            radius = size.width * 0.4f
                        )
                    }
            )
        }
    }
}

/**
 * Apple Music-style top bar for lyrics screen
 */
@Composable
private fun AppleMusicLyricsTopBar(
    song: com.music.revive.domain.model.Song?,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = Color.Black.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back button
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = "返回",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
            
            // Song info
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = song?.title ?: "",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song?.artist ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            // Spacer for balance
            Spacer(modifier = Modifier.size(48.dp))
        }
    }
}

/**
 * Apple Music-style bottom bar with playback controls
 */
@Composable
private fun AppleMusicLyricsBottomBar(
    song: com.music.revive.domain.model.Song?,
    isPlaying: Boolean,
    onPreviousClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = Color.Black.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Song info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = song?.artist ?: "",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song?.album ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            // Playback controls
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Previous
                IconButton(onClick = onPreviousClick) {
                    Icon(
                        imageVector = Icons.Rounded.SkipPrevious,
                        contentDescription = "上一首",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
                
                // Play/Pause
                FilledIconButton(
                    onClick = onPlayPauseClick,
                    modifier = Modifier.size(56.dp),
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black
                    )
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (isPlaying) "暂停" else "播放",
                        modifier = Modifier.size(28.dp)
                    )
                }
                
                // Next
                IconButton(onClick = onNextClick) {
                    Icon(
                        imageVector = Icons.Rounded.SkipNext,
                        contentDescription = "下一首",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

/**
 * Palette colors data class
 */
data class PaletteColors(
    val dominant: Color,
    val vibrant: Color,
    val lightVibrant: Color,
    val darkVibrant: Color,
    val muted: Color
)

/**
 * Extract colors from album art using Palette
 */
private suspend fun extractPaletteColors(context: Context, uri: String): PaletteColors? {
    return withContext(Dispatchers.IO) {
        try {
            val imageLoader = ImageLoader(context)
            val request = ImageRequest.Builder(context)
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