package com.music.revive.presentation.screen.settings

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.music.revive.R
import com.music.revive.BuildConfig
import com.music.revive.data.local.LyricsPreferences
import com.music.revive.data.local.NotificationPreferences
import com.music.revive.data.local.PlayerPreferences
import com.music.revive.data.local.ThemePreferences
import com.music.revive.data.repository.MusicRepository
import com.music.revive.data.lyric.LyricRepository
import com.music.revive.domain.model.Folder
import com.music.revive.presentation.theme.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showThemeDialog by remember { mutableStateOf(false) }
    var showClearHistoryDialog by remember { mutableStateOf(false) }
    var showFolderManagerDialog by remember { mutableStateOf(false) }
    var showLyricsFontSlider by remember { mutableStateOf(false) }
    var showPlaybackSpeedSlider by remember { mutableStateOf(false) }
    var showClearLyricsCacheDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Appearance section
            item {
                Text(
                    text = stringResource(R.string.appearance),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.theme)) },
                    supportingContent = { Text(uiState.themeDisplayName) },
                    leadingContent = {
                        Icon(Icons.Default.Palette, contentDescription = null)
                    },
                    modifier = Modifier.clickable { showThemeDialog = true }
                )
            }

            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.use_dynamic_colors)) },
                    leadingContent = {
                        Icon(Icons.Default.ColorLens, contentDescription = null)
                    },
                    trailingContent = {
                        Switch(
                            checked = uiState.useDynamicColors,
                            onCheckedChange = { viewModel.setDynamicColors(it) }
                        )
                    }
                )
            }

            // Lyrics section
            item {
                Text(
                    text = stringResource(R.string.lyrics_settings),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.lyrics_font_size)) },
                    supportingContent = { Text("%.1fx".format(uiState.lyricsFontSize)) },
                    leadingContent = {
                        Icon(Icons.Default.TextFields, contentDescription = null)
                    },
                    modifier = Modifier.clickable { showLyricsFontSlider = true }
                )
            }

            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.show_translation)) },
                    leadingContent = {
                        Icon(Icons.Default.Translate, contentDescription = null)
                    },
                    trailingContent = {
                        Switch(
                            checked = uiState.showTranslation,
                            onCheckedChange = { viewModel.setShowTranslation(it) }
                        )
                    }
                )
            }

            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.auto_scroll)) },
                    leadingContent = {
                        Icon(Icons.Default.Scroll, contentDescription = null)
                    },
                    trailingContent = {
                        Switch(
                            checked = uiState.autoScroll,
                            onCheckedChange = { viewModel.setAutoScroll(it) }
                        )
                    }
                )
            }

            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.fetch_online_lyrics)) },
                    supportingContent = { Text(stringResource(R.string.lyrics_source_online)) },
                    leadingContent = {
                        Icon(Icons.Default.CloudDownload, contentDescription = null)
                    },
                    trailingContent = {
                        Switch(
                            checked = uiState.fetchOnlineLyrics,
                            onCheckedChange = { viewModel.setFetchOnlineLyrics(it) }
                        )
                    }
                )
            }

            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.lyrics_display_style)) },
                    supportingContent = { 
                        Text(if (uiState.lyricsDisplayStyle == 0) 
                            stringResource(R.string.lyrics_centered) 
                        else 
                            stringResource(R.string.lyrics_left_aligned)) 
                    },
                    leadingContent = {
                        Icon(Icons.Default.AlignHorizontalCenter, contentDescription = null)
                    },
                    modifier = Modifier.clickable { 
                        viewModel.setLyricsDisplayStyle(if (uiState.lyricsDisplayStyle == 0) 1 else 0) 
                    }
                )
            }

            // Playback section
            item {
                Text(
                    text = stringResource(R.string.playback_settings),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.fade_in_out)) },
                    supportingContent = { Text(stringResource(R.string.fade_in_out_description)) },
                    leadingContent = {
                        Icon(Icons.Default.GraphicEq, contentDescription = null)
                    },
                    trailingContent = {
                        Switch(
                            checked = uiState.fadeInOut,
                            onCheckedChange = { viewModel.setFadeInOut(it) }
                        )
                    }
                )
            }

            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.gapless_playback)) },
                    supportingContent = { Text(stringResource(R.string.gapless_playback_description)) },
                    leadingContent = {
                        Icon(Icons.Default.SkipNext, contentDescription = null)
                    },
                    trailingContent = {
                        Switch(
                            checked = uiState.gaplessPlayback,
                            onCheckedChange = { viewModel.setGaplessPlayback(it) }
                        )
                    }
                )
            }

            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.volume_normalization)) },
                    supportingContent = { Text(stringResource(R.string.volume_normalization_description)) },
                    leadingContent = {
                        Icon(Icons.Default.VolumeUp, contentDescription = null)
                    },
                    trailingContent = {
                        Switch(
                            checked = uiState.volumeNormalization,
                            onCheckedChange = { viewModel.setVolumeNormalization(it) }
                        )
                    }
                )
            }

            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.playback_speed)) },
                    supportingContent = { Text("%.1fx".format(uiState.playbackSpeed)) },
                    leadingContent = {
                        Icon(Icons.Default.Speed, contentDescription = null)
                    },
                    modifier = Modifier.clickable { showPlaybackSpeedSlider = true }
                )
            }

            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.skip_silence)) },
                    supportingContent = { Text(stringResource(R.string.skip_silence_description)) },
                    leadingContent = {
                        Icon(Icons.Default.FastForward, contentDescription = null)
                    },
                    trailingContent = {
                        Switch(
                            checked = uiState.skipSilence,
                            onCheckedChange = { viewModel.setSkipSilence(it) }
                        )
                    }
                )
            }

            // Notification section
            item {
                Text(
                    text = stringResource(R.string.notification_settings),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.show_notification)) },
                    leadingContent = {
                        Icon(Icons.Default.Notifications, contentDescription = null)
                    },
                    trailingContent = {
                        Switch(
                            checked = uiState.showNotification,
                            onCheckedChange = { viewModel.setShowNotification(it) }
                        )
                    }
                )
            }

            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.show_on_lock_screen)) },
                    supportingContent = { Text(stringResource(R.string.show_on_lock_screen_description)) },
                    leadingContent = {
                        Icon(Icons.Default.Lock, contentDescription = null)
                    },
                    trailingContent = {
                        Switch(
                            checked = uiState.showOnLockScreen,
                            onCheckedChange = { viewModel.setShowOnLockScreen(it) }
                        )
                    }
                )
            }

            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.floating_lyrics)) },
                    supportingContent = { Text(stringResource(R.string.floating_lyrics_description)) },
                    leadingContent = {
                        Icon(Icons.Default.Layers, contentDescription = null)
                    },
                    trailingContent = {
                        Switch(
                            checked = uiState.floatingLyrics,
                            onCheckedChange = { viewModel.setFloatingLyrics(it) }
                        )
                    }
                )
            }

            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.notification_lyrics)) },
                    supportingContent = { Text(stringResource(R.string.notification_lyrics_description)) },
                    leadingContent = {
                        Icon(Icons.Default.Lyrics, contentDescription = null)
                    },
                    trailingContent = {
                        Switch(
                            checked = uiState.notificationLyrics,
                            onCheckedChange = { viewModel.setNotificationLyrics(it) }
                        )
                    }
                )
            }

            // Audio section
            item {
                Text(
                    text = stringResource(R.string.audio),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.audio_focus)) },
                    supportingContent = { Text(stringResource(R.string.audio_focus_description)) },
                    leadingContent = {
                        Icon(Icons.Default.VolumeUp, contentDescription = null)
                    },
                    trailingContent = {
                        Switch(
                            checked = uiState.handleAudioFocus,
                            onCheckedChange = { viewModel.setAudioFocusHandling(it) }
                        )
                    }
                )
            }

            // Library section
            item {
                Text(
                    text = stringResource(R.string.library),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.scan_music_library)) },
                    supportingContent = { Text(stringResource(R.string.scan_music_library_description)) },
                    leadingContent = {
                        if (uiState.isScanning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                        }
                    },
                    modifier = Modifier.clickable(enabled = !uiState.isScanning) {
                        viewModel.scanMusicLibrary()
                        Toast.makeText(context, context.getString(R.string.scanning_started), Toast.LENGTH_SHORT).show()
                    }
                )
            }

            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.manage_folders)) },
                    supportingContent = { Text(stringResource(R.string.manage_folders_description)) },
                    leadingContent = {
                        Icon(Icons.Default.Folder, contentDescription = null)
                    },
                    trailingContent = {
                        Icon(Icons.Default.ChevronRight, contentDescription = null)
                    },
                    modifier = Modifier.clickable { showFolderManagerDialog = true }
                )
            }

            // Storage section
            item {
                Text(
                    text = stringResource(R.string.storage_settings),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.clear_lyrics_cache)) },
                    supportingContent = { Text(stringResource(R.string.clear_cache_description)) },
                    leadingContent = {
                        Icon(Icons.Default.Delete, contentDescription = null)
                    },
                    modifier = Modifier.clickable { showClearLyricsCacheDialog = true }
                )
            }

            // Data section
            item {
                Text(
                    text = stringResource(R.string.data),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.clear_recent_history)) },
                    leadingContent = {
                        Icon(Icons.Default.History, contentDescription = null)
                    },
                    modifier = Modifier.clickable { showClearHistoryDialog = true }
                )
            }

            // About section
            item {
                Text(
                    text = stringResource(R.string.about),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.version)) },
                    supportingContent = { Text(BuildConfig.VERSION_NAME) },
                    leadingContent = {
                        Icon(Icons.Default.Info, contentDescription = null)
                    }
                )
            }

            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.open_source_licenses)) },
                    leadingContent = {
                        Icon(Icons.Default.Code, contentDescription = null)
                    },
                    modifier = Modifier.clickable { }
                )
            }
        }
    }

    // Theme selection dialog
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text(stringResource(R.string.choose_theme)) },
            text = {
                Column {
                    ThemeOption.entries.forEach { theme ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setTheme(theme)
                                    showThemeDialog = false
                                }
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (uiState.theme == theme) {
                                Icon(Icons.Default.Check, contentDescription = null)
                            } else {
                                Spacer(Modifier.width(24.dp))
                            }
                            Text(theme.displayName)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text("OK")
                }
            }
        )
    }

    // Lyrics font size slider dialog
    if (showLyricsFontSlider) {
        var sliderValue by remember { mutableFloatStateOf(uiState.lyricsFontSize) }
        AlertDialog(
            onDismissRequest = { showLyricsFontSlider = false },
            title = { Text(stringResource(R.string.lyrics_font_size)) },
            text = {
                Column {
                    Slider(
                        value = sliderValue,
                        onValueChange = { sliderValue = it },
                        valueRange = 0.8f..1.5f,
                        steps = 7
                    )
                    Text(
                        text = "%.1fx".format(sliderValue),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.setLyricsFontSize(sliderValue)
                    showLyricsFontSlider = false
                }) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showLyricsFontSlider = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Playback speed slider dialog
    if (showPlaybackSpeedSlider) {
        var sliderValue by remember { mutableFloatStateOf(uiState.playbackSpeed) }
        AlertDialog(
            onDismissRequest = { showPlaybackSpeedSlider = false },
            title = { Text(stringResource(R.string.playback_speed)) },
            text = {
                Column {
                    Slider(
                        value = sliderValue,
                        onValueChange = { sliderValue = it },
                        valueRange = 0.5f..2.0f,
                        steps = 15
                    )
                    Text(
                        text = "%.1fx".format(sliderValue),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.setPlaybackSpeed(sliderValue)
                    showPlaybackSpeedSlider = false
                }) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showPlaybackSpeedSlider = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Clear history confirmation dialog
    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            title = { Text(stringResource(R.string.clear_recent_history)) },
            text = { Text(stringResource(R.string.clear_recent_history_confirmation)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearRecentHistory()
                        showClearHistoryDialog = false
                        Toast.makeText(context, context.getString(R.string.history_cleared), Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text(stringResource(R.string.clear))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Clear lyrics cache confirmation dialog
    if (showClearLyricsCacheDialog) {
        AlertDialog(
            onDismissRequest = { showClearLyricsCacheDialog = false },
            title = { Text(stringResource(R.string.clear_lyrics_cache)) },
            text = { Text(stringResource(R.string.clear_cache_description)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearLyricsCache()
                        showClearLyricsCacheDialog = false
                        Toast.makeText(context, context.getString(R.string.lyrics_cache_cleared), Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text(stringResource(R.string.clear))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearLyricsCacheDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Folder manager dialog
    if (showFolderManagerDialog) {
        FolderManagerDialog(
            folders = uiState.folders,
            excludedFolders = uiState.excludedFolders,
            onToggleFolder = { folderPath, exclude ->
                if (exclude) {
                    viewModel.excludeFolder(folderPath)
                } else {
                    viewModel.includeFolder(folderPath)
                }
            },
            onDismiss = { showFolderManagerDialog = false }
        )
    }
}

@Composable
private fun FolderManagerDialog(
    folders: List<Folder>,
    excludedFolders: Set<String>,
    onToggleFolder: (String, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.manage_folders)) },
        text = {
            if (folders.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.no_folders_found),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                ) {
                    Text(
                        text = stringResource(R.string.folder_manager_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(folders) { folder ->
                            val isExcluded = excludedFolders.contains(folder.path)
                            ListItem(
                                headlineContent = { 
                                    Text(
                                        text = folder.name,
                                        maxLines = 1
                                    ) 
                                },
                                supportingContent = { 
                                    Text("${folder.numberOfSongs} ${stringResource(R.string.songs)}") 
                                },
                                leadingContent = {
                                    Icon(
                                        imageVector = if (isExcluded) Icons.Default.FolderOff else Icons.Default.Folder,
                                        contentDescription = null,
                                        tint = if (isExcluded) 
                                            MaterialTheme.colorScheme.error 
                                        else 
                                            MaterialTheme.colorScheme.primary
                                    )
                                },
                                trailingContent = {
                                    Switch(
                                        checked = !isExcluded,
                                        onCheckedChange = { enabled ->
                                            onToggleFolder(folder.path, !enabled)
                                        }
                                    )
                                },
                                colors = ListItemDefaults.colors(
                                    containerColor = if (isExcluded) 
                                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f) 
                                    else 
                                        MaterialTheme.colorScheme.surface
                                )
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.done))
            }
        }
    )
}

enum class ThemeOption(val displayName: String) {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark")
}

data class SettingsUiState(
    // Appearance
    val theme: ThemeOption = ThemeOption.SYSTEM,
    val themeDisplayName: String = ThemeOption.SYSTEM.displayName,
    val useDynamicColors: Boolean = true,
    // Lyrics
    val lyricsFontSize: Float = 1.0f,
    val showTranslation: Boolean = true,
    val autoScroll: Boolean = true,
    val fetchOnlineLyrics: Boolean = true,
    val lyricsDisplayStyle: Int = 0,
    // Playback
    val fadeInOut: Boolean = false,
    val gaplessPlayback: Boolean = true,
    val volumeNormalization: Boolean = false,
    val playbackSpeed: Float = 1.0f,
    val skipSilence: Boolean = false,
    // Notification
    val showNotification: Boolean = true,
    val showOnLockScreen: Boolean = true,
    val floatingLyrics: Boolean = false,
    val notificationLyrics: Boolean = false,
    // Audio focus
    val handleAudioFocus: Boolean = true,
    // Library
    val isScanning: Boolean = false,
    val folders: List<Folder> = emptyList(),
    val excludedFolders: Set<String> = emptySet()
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: MusicRepository,
    private val lyricsPreferences: LyricsPreferences,
    private val playerPreferences: PlayerPreferences,
    private val notificationPreferences: NotificationPreferences,
    private val lyricRepository: LyricRepository,
    private val themePreferences: ThemePreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadFolders()
        loadExcludedFolders()
        loadLyricsSettings()
        loadPlaybackSettings()
        loadNotificationSettings()
        loadThemeSettings()
    }

    private fun loadFolders() {
        viewModelScope.launch {
            val folders = repository.getFolders()
            _uiState.value = _uiState.value.copy(folders = folders)
        }
    }

    private fun loadExcludedFolders() {
        viewModelScope.launch {
            repository.getExcludedFolders().collect { excluded ->
                _uiState.value = _uiState.value.copy(excludedFolders = excluded)
            }
        }
    }

    private fun loadThemeSettings() {
        viewModelScope.launch {
            themePreferences.themeMode.collect { mode ->
                val themeOption = when (mode) {
                    ThemeMode.LIGHT -> ThemeOption.LIGHT
                    ThemeMode.DARK -> ThemeOption.DARK
                    ThemeMode.SYSTEM -> ThemeOption.SYSTEM
                }
                _uiState.value = _uiState.value.copy(
                    theme = themeOption,
                    themeDisplayName = themeOption.displayName
                )
            }
        }
        viewModelScope.launch {
            themePreferences.dynamicColorsEnabled.collect { enabled ->
                _uiState.value = _uiState.value.copy(useDynamicColors = enabled)
            }
        }
    }

    private fun loadLyricsSettings() {
        viewModelScope.launch {
            lyricsPreferences.lyricsFontSize.collect { size ->
                _uiState.value = _uiState.value.copy(lyricsFontSize = size)
            }
        }
        viewModelScope.launch {
            lyricsPreferences.showTranslation.collect { show ->
                _uiState.value = _uiState.value.copy(showTranslation = show)
            }
        }
        viewModelScope.launch {
            lyricsPreferences.autoScroll.collect { auto ->
                _uiState.value = _uiState.value.copy(autoScroll = auto)
            }
        }
        viewModelScope.launch {
            lyricsPreferences.fetchOnlineLyrics.collect { fetch ->
                _uiState.value = _uiState.value.copy(fetchOnlineLyrics = fetch)
            }
        }
        viewModelScope.launch {
            lyricsPreferences.lyricsDisplayStyle.collect { style ->
                _uiState.value = _uiState.value.copy(lyricsDisplayStyle = style)
            }
        }
    }

    private fun loadPlaybackSettings() {
        viewModelScope.launch {
            playerPreferences.fadeInOut.collect { enabled ->
                _uiState.value = _uiState.value.copy(fadeInOut = enabled)
            }
        }
        viewModelScope.launch {
            playerPreferences.gaplessPlayback.collect { enabled ->
                _uiState.value = _uiState.value.copy(gaplessPlayback = enabled)
            }
        }
        viewModelScope.launch {
            playerPreferences.volumeNormalization.collect { enabled ->
                _uiState.value = _uiState.value.copy(volumeNormalization = enabled)
            }
        }
        viewModelScope.launch {
            playerPreferences.playbackSpeed.collect { speed ->
                _uiState.value = _uiState.value.copy(playbackSpeed = speed)
            }
        }
        viewModelScope.launch {
            playerPreferences.skipSilence.collect { enabled ->
                _uiState.value = _uiState.value.copy(skipSilence = enabled)
            }
        }
    }

    private fun loadNotificationSettings() {
        viewModelScope.launch {
            notificationPreferences.showNotification.collect { show ->
                _uiState.value = _uiState.value.copy(showNotification = show)
            }
        }
        viewModelScope.launch {
            notificationPreferences.showOnLockScreen.collect { show ->
                _uiState.value = _uiState.value.copy(showOnLockScreen = show)
            }
        }
        viewModelScope.launch {
            notificationPreferences.floatingLyrics.collect { enabled ->
                _uiState.value = _uiState.value.copy(floatingLyrics = enabled)
            }
        }
        viewModelScope.launch {
            notificationPreferences.notificationLyrics.collect { enabled ->
                _uiState.value = _uiState.value.copy(notificationLyrics = enabled)
            }
        }
    }

    // Theme
    fun setTheme(theme: ThemeOption) {
        viewModelScope.launch {
            val mode = when (theme) {
                ThemeOption.LIGHT -> ThemeMode.LIGHT
                ThemeOption.DARK -> ThemeMode.DARK
                ThemeOption.SYSTEM -> ThemeMode.SYSTEM
            }
            themePreferences.setThemeMode(mode)
        }
    }

    fun setDynamicColors(enabled: Boolean) {
        viewModelScope.launch {
            themePreferences.setDynamicColorsEnabled(enabled)
        }
    }

    // Lyrics settings
    fun setLyricsFontSize(size: Float) {
        viewModelScope.launch {
            lyricsPreferences.setLyricsFontSize(size)
        }
    }

    fun setShowTranslation(show: Boolean) {
        viewModelScope.launch {
            lyricsPreferences.setShowTranslation(show)
        }
    }

    fun setAutoScroll(enabled: Boolean) {
        viewModelScope.launch {
            lyricsPreferences.setAutoScroll(enabled)
        }
    }

    fun setFetchOnlineLyrics(enabled: Boolean) {
        viewModelScope.launch {
            lyricsPreferences.setFetchOnlineLyrics(enabled)
        }
    }

    fun setLyricsDisplayStyle(style: Int) {
        viewModelScope.launch {
            lyricsPreferences.setLyricsDisplayStyle(style)
        }
    }

    fun clearLyricsCache() {
        viewModelScope.launch {
            lyricRepository.clearAllCache()
        }
    }

    // Playback settings
    fun setFadeInOut(enabled: Boolean) {
        viewModelScope.launch {
            playerPreferences.setFadeInOut(enabled)
        }
    }

    fun setGaplessPlayback(enabled: Boolean) {
        viewModelScope.launch {
            playerPreferences.setGaplessPlayback(enabled)
        }
    }

    fun setVolumeNormalization(enabled: Boolean) {
        viewModelScope.launch {
            playerPreferences.setVolumeNormalization(enabled)
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        viewModelScope.launch {
            playerPreferences.setPlaybackSpeed(speed)
        }
    }

    fun setSkipSilence(enabled: Boolean) {
        viewModelScope.launch {
            playerPreferences.setSkipSilence(enabled)
        }
    }

    // Notification settings
    fun setShowNotification(show: Boolean) {
        viewModelScope.launch {
            notificationPreferences.setShowNotification(show)
        }
    }

    fun setShowOnLockScreen(show: Boolean) {
        viewModelScope.launch {
            notificationPreferences.setShowOnLockScreen(show)
        }
    }

    fun setFloatingLyrics(enabled: Boolean) {
        viewModelScope.launch {
            notificationPreferences.setFloatingLyrics(enabled)
        }
    }

    fun setNotificationLyrics(enabled: Boolean) {
        viewModelScope.launch {
            notificationPreferences.setNotificationLyrics(enabled)
        }
    }

    // Audio focus
    fun setAudioFocusHandling(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(handleAudioFocus = enabled)
    }

    // Library
    fun scanMusicLibrary() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isScanning = true)
            repository.getAllSongs()
            repository.getAlbums()
            repository.getArtists()
            val folders = repository.getFolders()
            _uiState.value = _uiState.value.copy(
                isScanning = false,
                folders = folders
            )
        }
    }

    fun excludeFolder(folderPath: String) {
        viewModelScope.launch {
            repository.excludeFolder(folderPath)
        }
    }

    fun includeFolder(folderPath: String) {
        viewModelScope.launch {
            repository.includeFolder(folderPath)
        }
    }

    fun clearRecentHistory() {
        viewModelScope.launch {
            repository.clearRecentSongs()
        }
    }
}