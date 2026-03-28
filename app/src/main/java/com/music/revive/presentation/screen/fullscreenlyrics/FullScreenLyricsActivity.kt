package com.music.revive.presentation.screen.fullscreenlyrics

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.ImageLoader
import coil.request.ImageRequest
import androidx.core.graphics.drawable.toBitmap
import com.music.revive.data.local.LyricsPreferences
import com.music.revive.domain.model.Lyric
import com.music.revive.domain.model.LyricLine
import com.music.revive.presentation.components.AppleMusicLyricsBackground
import com.music.revive.presentation.components.LyricsView
import com.music.revive.presentation.screen.player.PaletteColors
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

@AndroidEntryPoint
class FullScreenLyricsActivity : ComponentActivity() {

    @Inject lateinit var musicPlayer: MusicPlayer
    @Inject lateinit var lyricsPreferences: LyricsPreferences
    @Inject lateinit var playerPreferences: com.music.revive.data.local.PlayerPreferences
    
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
            MaterialTheme {
                FullScreenLyricsScreen(
                    musicPlayer = musicPlayer,
                    lyricsPreferences = lyricsPreferences,
                    playerPreferences = playerPreferences,
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

class FullScreenLyricsViewModel(
    private val musicPlayer: MusicPlayer,
    private val lyricsPreferences: LyricsPreferences,
    private val playerPreferences: com.music.revive.data.local.PlayerPreferences
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
                _currentLyrics.value = Lyric.Empty
            }
        }
    }
    
    fun updateLyrics(lyrics: Lyric) {
        _currentLyrics.value = lyrics
    }
    
    fun setLoadingLoading(isLoading: Boolean) {
        _isLoadingLyrics.value = isLoading
    }
}

@Composable
fun FullScreenLyricsScreen(
    musicPlayer: MusicPlayer,
    lyricsPreferences: LyricsPreferences,
    playerPreferences: com.music.revive.data.local.PlayerPreferences,
    onNavigateBack: () -> Unit
) {
    val viewModel = remember { FullScreenLyricsViewModel(musicPlayer, lyricsPreferences, playerPreferences) }
    
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
    
    var showControls by remember { mutableStateOf(true) }
    var isExiting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    // Extract palette colors for background
    var paletteColors by remember { mutableStateOf<PaletteColors?>(null) }
    
    LaunchedEffect(song?.albumArtUri) {
        song?.albumArtUri?.let { uri ->
            paletteColors = extractPaletteColors(context, uri)
        }
    }
    
    LaunchedEffect(Unit) {
        delay(3000)
        showControls = false
    }
    
    val smoothEasing = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f)
    
    val topBarAlpha by animateFloatAsState(
        targetValue = if (showControls && !isExiting) 1f else 0f,
        animationSpec = tween(300, easing = smoothEasing),
        label = "topBarAlpha"
    )
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
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
        // Apple Music-style dynamic background with moving color orbs
        AppleMusicLyricsBackground(
            colors = paletteColors,
            modifier = Modifier.fillMaxSize()
        )
        
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { alpha = topBarAlpha },
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.8f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        isExiting = true
                        onNavigateBack()
                    }) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowBack,
                            contentDescription = "返回",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    Text(
                        text = song?.title ?: "",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        fontSize = 16.sp
                    )
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    Spacer(modifier = Modifier.size(48.dp))
                }
            }
            
            Box(
                modifier = Modifier.weight(1f)
            ) {
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
                        fontSizeMultiplier = lyricsFontSize * 1.2f,
                        showTranslation = showTranslation,
                        isCentered = lyricsDisplayStyle == 0,
                        enableGlow = enableGlow,
                        enableKaraoke = enableKaraoke,
                        enableHapticFeedback = false,
                        enableBlur = enableBlur,
                        enableShader = enableShaderEffect,
                        blurRadius = 8f,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            
            // Bottom info bar with song details
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { alpha = topBarAlpha },
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = song?.artist ?: "",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            fontSize = 14.sp
                        )
                        Text(
                            text = song?.album ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            fontSize = 12.sp
                        )
                    }
                    
                    // Mini player controls
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { 
                            musicPlayer.playPrevious()
                        }) {
                            Icon(
                                imageVector = Icons.Rounded.SkipPrevious,
                                contentDescription = "上一首",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        
                        IconButton(onClick = { 
                            musicPlayer.playPause()
                        }) {
                            Icon(
                                imageVector = if (playerState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = if (playerState.isPlaying) "暂停" else "播放",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        
                        IconButton(onClick = { 
                            musicPlayer.playNext()
                        }) {
                            Icon(
                                imageVector = Icons.Rounded.SkipNext,
                                contentDescription = "下一首",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

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
