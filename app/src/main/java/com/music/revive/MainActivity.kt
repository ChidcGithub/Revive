package com.music.revive

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.music.revive.data.local.ColorSource
import com.music.revive.data.local.ThemePreferences
import com.music.revive.presentation.navigation.ReviveNavigation
import com.music.revive.presentation.theme.AlbumColors
import com.music.revive.presentation.theme.ColorExtractor
import com.music.revive.presentation.theme.ReviveTheme
import com.music.revive.presentation.theme.ThemeMode
import com.music.revive.service.MusicPlayer
import com.music.revive.service.MusicService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var themePreferences: ThemePreferences
    
    @Inject
    lateinit var musicPlayer: MusicPlayer

    private var hasPermissions by mutableStateOf(false)

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        hasPermissions = allGranted
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Enable edge-to-edge for full screen immersive experience
        enableEdgeToEdge()
        
        // Set transparent status and navigation bars
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }

        checkAndRequestPermissions()

        setContent {
            // Read persisted theme settings
            var themeMode by remember { mutableStateOf(ThemeMode.SYSTEM) }
            var colorSource by remember { mutableStateOf(ColorSource.DYNAMIC) }
            var albumColors by remember { mutableStateOf<AlbumColors?>(null) }
            
            // Collect theme preferences
            LaunchedEffect(Unit) {
                lifecycleScope.launch {
                    themePreferences.themeMode.collect { mode ->
                        themeMode = mode
                    }
                }
                lifecycleScope.launch {
                    themePreferences.colorSource.collect { source ->
                        colorSource = source
                    }
                }
            }
            
            // Extract album colors when color source is ALBUM
            val currentSong by musicPlayer.currentSong.collectAsState()
            val isDarkTheme = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }
            
            LaunchedEffect(currentSong?.albumArtUri, colorSource, isDarkTheme) {
                if (colorSource == ColorSource.ALBUM && currentSong?.albumArtUri != null) {
                    val colors = ColorExtractor.extractFromUri(
                        this@MainActivity,
                        currentSong?.albumArtUri,
                        isDarkTheme
                    )
                    albumColors = colors
                } else if (colorSource != ColorSource.ALBUM) {
                    albumColors = null
                }
            }
            
            ReviveTheme(
                themeMode = themeMode,
                colorSource = colorSource,
                albumColors = albumColors
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (hasPermissions) {
                        ReviveNavigation()
                    } else {
                        PermissionRequestScreen(
                            onRequestPermissions = { checkAndRequestPermissions() }
                        )
                    }
                }
            }
        }
    }
    
    @Composable
    private fun isSystemInDarkTheme(): Boolean {
        return androidx.compose.foundation.isSystemInDarkTheme()
    }

    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.READ_MEDIA_AUDIO)
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isEmpty()) {
            hasPermissions = true
        } else {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    override fun onStart() {
        super.onStart()
        val serviceIntent = Intent(this, MusicService::class.java)
        startService(serviceIntent)
    }
}

@Composable
fun PermissionRequestScreen(
    onRequestPermissions: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Icon
            Surface(
                modifier = Modifier.size(120.dp),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MusicNote,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = "Revive",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "需要存储权限才能访问您的音乐文件",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            FilledTonalButton(
                onClick = onRequestPermissions,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = MaterialTheme.shapes.large
            ) {
                Text(
                    text = "授予权限",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}
