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
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
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
    
    var showControls by remember { mutableStateOf(true) }
    var isExiting by remember { mutableStateOf(false) }
    
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
        FullScreenLyricsBackground(
            song = song,
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
            
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { alpha = topBarAlpha },
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.8f)
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
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        repeat(3) { index ->
                            PlayingBar(
                                isActive = playerState.isPlaying,
                                delay = index * 150
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayingBar(
    isActive: Boolean,
    delay: Int
) {
    val infiniteTransition = rememberInfiniteTransition(label = "playing")
    
    val height by infiniteTransition.animateFloat(
        initialValue = 4f,
        targetValue = if (isActive) 16f else 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 600,
                delayMillis = delay,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "barHeight"
    )
    
    Box(
        modifier = Modifier
            .width(4.dp)
            .height(height.dp)
            .background(
                color = if (isActive) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                shape = MaterialTheme.shapes.small
            )
    )
}

@Composable
private fun FullScreenLyricsBackground(
    song: com.music.revive.domain.model.Song?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var paletteColors by remember { mutableStateOf<PaletteColors?>(null) }
    val backgroundColor = MaterialTheme.colorScheme.background
    
    LaunchedEffect(song?.albumArtUri) {
        song?.albumArtUri?.let { uri ->
            paletteColors = extractPaletteColors(context, uri)
        }
    }
    
    val infiniteTransition = rememberInfiniteTransition(label = "background")
    
    val offsetX1 by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(15000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offsetX1"
    )
    
    val offsetY1 by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(18000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offsetY1"
    )
    
    val offsetX2 by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offsetX2"
    )
    
    val offsetY2 by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(16000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offsetY2"
    )
    
    Box(
        modifier = modifier.drawBehind {
            val width = size.width
            val height = size.height
            
            drawRect(backgroundColor)
            
            if (paletteColors != null) {
                val center1 = Offset(width * offsetX1, height * offsetY1)
                val radius1 = width * 0.7f
                
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            paletteColors!!.dominant.copy(alpha = 0.15f),
                            paletteColors!!.dominant.copy(alpha = 0.05f),
                            Color.Transparent
                        ),
                        center = center1,
                        radius = radius1
                    ),
                    center = center1,
                    radius = radius1
                )
                
                val center2 = Offset(width * offsetX2, height * offsetY2)
                val radius2 = width * 0.6f
                
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            paletteColors!!.vibrant.copy(alpha = 0.12f),
                            paletteColors!!.vibrant.copy(alpha = 0.03f),
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

data class PaletteColors(
    val dominant: Color,
    val vibrant: Color,
    val lightVibrant: Color,
    val darkVibrant: Color,
    val muted: Color
)

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
