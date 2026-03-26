package com.music.revive.presentation.screen.settings

import android.os.Build
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.music.revive.R
import com.music.revive.BuildConfig
import com.music.revive.data.local.ColorSource
import com.music.revive.data.local.LyricsPreferences
import com.music.revive.data.local.NotificationPreferences
import com.music.revive.data.local.PlayerPreferences
import com.music.revive.data.local.ThemePreferences
import com.music.revive.data.repository.MusicRepository
import com.music.revive.data.lyric.LyricRepository
import com.music.revive.domain.model.Folder
import com.music.revive.presentation.components.AboutDialog
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
    var showColorSourceDialog by remember { mutableStateOf(false) }
    var showClearHistoryDialog by remember { mutableStateOf(false) }
    var showFolderManagerDialog by remember { mutableStateOf(false) }
    var showLyricsFontSlider by remember { mutableStateOf(false) }
    var showPlaybackSpeedSlider by remember { mutableStateOf(false) }
    var showClearLyricsCacheDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { 
                    Text(
                        text = stringResource(R.string.settings),
                        fontWeight = FontWeight.Black,
                        fontSize = 28.sp
                    ) 
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                modifier = Modifier.background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding(),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            // Appearance section (collapsible)
            item {
                CollapsibleSettingsSection(
                    title = stringResource(R.string.appearance),
                    icon = Icons.Rounded.Palette,
                    initiallyExpanded = true
                ) {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.theme)) },
                        supportingContent = { Text(uiState.themeDisplayName) },
                        leadingContent = {
                            Icon(Icons.Rounded.DarkMode, contentDescription = null)
                        },
                        trailingContent = {
                            Icon(Icons.Rounded.ChevronRight, contentDescription = null)
                        },
                        modifier = Modifier.clickable { showThemeDialog = true }
                    )
                    
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.color_source)) },
                        supportingContent = { Text(uiState.colorSourceDisplayName) },
                        leadingContent = {
                            Icon(Icons.Rounded.ColorLens, contentDescription = null)
                        },
                        trailingContent = {
                            Icon(Icons.Rounded.ChevronRight, contentDescription = null)
                        },
                        modifier = Modifier.clickable { showColorSourceDialog = true }
                    )
                }
            }

            // Lyrics section (collapsible)
            item {
                CollapsibleSettingsSection(
                    title = stringResource(R.string.lyrics_settings),
                    icon = Icons.Rounded.Lyrics,
                    initiallyExpanded = true
                ) {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.lyrics_font_size)) },
                        supportingContent = { Text("%.1fx".format(uiState.lyricsFontSize)) },
                        leadingContent = {
                            Icon(Icons.Rounded.TextFields, contentDescription = null)
                        },
                        modifier = Modifier.clickable { showLyricsFontSlider = true }
                    )
                    
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.show_translation)) },
                        leadingContent = {
                            Icon(Icons.Rounded.Translate, contentDescription = null)
                        },
                        trailingContent = {
                            Switch(
                                checked = uiState.showTranslation,
                                onCheckedChange = { viewModel.setShowTranslation(it) }
                            )
                        }
                    )
                    
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.auto_scroll)) },
                        leadingContent = {
                            Icon(Icons.Rounded.List, contentDescription = null)
                        },
                        trailingContent = {
                            Switch(
                                checked = uiState.autoScroll,
                                onCheckedChange = { viewModel.setAutoScroll(it) }
                            )
                        }
                    )
                    
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.lyrics_display_style)) },
                        supportingContent = { 
                            Text(if (uiState.lyricsDisplayStyle == 0) 
                                stringResource(R.string.lyrics_centered) 
                            else 
                                stringResource(R.string.lyrics_left_aligned)) 
                        },
                        leadingContent = {
                            Icon(Icons.Rounded.FormatAlignCenter, contentDescription = null)
                        },
                        modifier = Modifier.clickable { 
                            viewModel.setLyricsDisplayStyle(if (uiState.lyricsDisplayStyle == 0) 1 else 0) 
                        }
                    )
                    
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.enable_glow_effect)) },
                        supportingContent = { Text(stringResource(R.string.glow_effect_description)) },
                        leadingContent = {
                            Icon(Icons.Rounded.AutoFixHigh, contentDescription = null)
                        },
                        trailingContent = {
                            Switch(
                                checked = uiState.enableGlow,
                                onCheckedChange = { viewModel.setEnableGlow(it) }
                            )
                        }
                    )
                    
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.enable_karaoke_effect)) },
                        supportingContent = { Text(stringResource(R.string.karaoke_effect_description)) },
                        leadingContent = {
                            Icon(Icons.Rounded.MusicNote, contentDescription = null)
                        },
                        trailingContent = {
                            Switch(
                                checked = uiState.enableKaraoke,
                                onCheckedChange = { viewModel.setEnableKaraoke(it) }
                            )
                        }
                    )
                }
            }

            // Playback section (collapsible)
            item {
                CollapsibleSettingsSection(
                    title = stringResource(R.string.playback_settings),
                    icon = Icons.Rounded.PlayCircle,
                    initiallyExpanded = false
                ) {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.fade_in_out)) },
                        supportingContent = { Text(stringResource(R.string.fade_in_out_description)) },
                        leadingContent = {
                            Icon(Icons.Rounded.GraphicEq, contentDescription = null)
                        },
                        trailingContent = {
                            Switch(
                                checked = uiState.fadeInOut,
                                onCheckedChange = { viewModel.setFadeInOut(it) }
                            )
                        }
                    )
                    
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.gapless_playback)) },
                        supportingContent = { Text(stringResource(R.string.gapless_playback_description)) },
                        leadingContent = {
                            Icon(Icons.Rounded.SkipNext, contentDescription = null)
                        },
                        trailingContent = {
                            Switch(
                                checked = uiState.gaplessPlayback,
                                onCheckedChange = { viewModel.setGaplessPlayback(it) }
                            )
                        }
                    )
                    
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.volume_normalization)) },
                        supportingContent = { Text(stringResource(R.string.volume_normalization_description)) },
                        leadingContent = {
                            Icon(Icons.Rounded.VolumeUp, contentDescription = null)
                        },
                        trailingContent = {
                            Switch(
                                checked = uiState.volumeNormalization,
                                onCheckedChange = { viewModel.setVolumeNormalization(it) }
                            )
                        }
                    )
                    
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.playback_speed)) },
                        supportingContent = { Text("%.1fx".format(uiState.playbackSpeed)) },
                        leadingContent = {
                            Icon(Icons.Rounded.Speed, contentDescription = null)
                        },
                        modifier = Modifier.clickable { showPlaybackSpeedSlider = true }
                    )
                    
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.skip_silence)) },
                        supportingContent = { Text(stringResource(R.string.skip_silence_description)) },
                        leadingContent = {
                            Icon(Icons.Rounded.FastForward, contentDescription = null)
                        },
                        trailingContent = {
                            Switch(
                                checked = uiState.skipSilence,
                                onCheckedChange = { viewModel.setSkipSilence(it) }
                            )
                        }
                    )
                }
            }

            // Notification section (collapsible)
            item {
                CollapsibleSettingsSection(
                    title = stringResource(R.string.notification_settings),
                    icon = Icons.Rounded.Notifications,
                    initiallyExpanded = false
                ) {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.show_notification)) },
                        leadingContent = {
                            Icon(Icons.Rounded.Notifications, contentDescription = null)
                        },
                        trailingContent = {
                            Switch(
                                checked = uiState.showNotification,
                                onCheckedChange = { viewModel.setShowNotification(it) }
                            )
                        }
                    )
                    
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.show_on_lock_screen)) },
                        supportingContent = { Text(stringResource(R.string.show_on_lock_screen_description)) },
                        leadingContent = {
                            Icon(Icons.Rounded.Lock, contentDescription = null)
                        },
                        trailingContent = {
                            Switch(
                                checked = uiState.showOnLockScreen,
                                onCheckedChange = { viewModel.setShowOnLockScreen(it) }
                            )
                        }
                    )
                    
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.floating_lyrics)) },
                        supportingContent = { Text(stringResource(R.string.floating_lyrics_description)) },
                        leadingContent = {
                            Icon(Icons.Rounded.Layers, contentDescription = null)
                        },
                        trailingContent = {
                            Switch(
                                checked = uiState.floatingLyrics,
                                onCheckedChange = { viewModel.setFloatingLyrics(it) }
                            )
                        }
                    )
                    
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.notification_lyrics)) },
                        supportingContent = { Text(stringResource(R.string.notification_lyrics_description)) },
                        leadingContent = {
                            Icon(Icons.Rounded.Lyrics, contentDescription = null)
                        },
                        trailingContent = {
                            Switch(
                                checked = uiState.notificationLyrics,
                                onCheckedChange = { viewModel.setNotificationLyrics(it) }
                            )
                        }
                    )
                }
            }

            // Audio section (collapsible)
            item {
                CollapsibleSettingsSection(
                    title = stringResource(R.string.audio),
                    icon = Icons.Rounded.VolumeUp,
                    initiallyExpanded = false
                ) {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.audio_focus)) },
                        supportingContent = { Text(stringResource(R.string.audio_focus_description)) },
                        leadingContent = {
                            Icon(Icons.Rounded.VolumeUp, contentDescription = null)
                        },
                        trailingContent = {
                            Switch(
                                checked = uiState.handleAudioFocus,
                                onCheckedChange = { viewModel.setAudioFocusHandling(it) }
                            )
                        }
                    )
                }
            }

            // Library section (collapsible)
            item {
                CollapsibleSettingsSection(
                    title = stringResource(R.string.library),
                    icon = Icons.Rounded.LibraryMusic,
                    initiallyExpanded = true
                ) {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.scan_music_library)) },
                        supportingContent = { 
                            if (uiState.isScanning) {
                                Text(stringResource(R.string.scanning))
                            } else {
                                Text(stringResource(R.string.scan_music_library_description))
                            }
                        },
                        leadingContent = {
                            if (uiState.isScanning) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Rounded.Refresh, contentDescription = null)
                            }
                        },
                        modifier = Modifier.clickable(enabled = !uiState.isScanning) {
                            viewModel.scanMusicLibrary()
                            Toast.makeText(context, context.getString(R.string.scanning_started), Toast.LENGTH_SHORT).show()
                        }
                    )
                    
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.manage_folders)) },
                        supportingContent = { Text(stringResource(R.string.manage_folders_description)) },
                        leadingContent = {
                            Icon(Icons.Rounded.Folder, contentDescription = null)
                        },
                        trailingContent = {
                            Icon(Icons.Rounded.ChevronRight, contentDescription = null)
                        },
                        modifier = Modifier.clickable { showFolderManagerDialog = true }
                    )
                }
            }

            // Storage section (collapsible)
            item {
                CollapsibleSettingsSection(
                    title = stringResource(R.string.storage_settings),
                    icon = Icons.Rounded.Storage,
                    initiallyExpanded = false
                ) {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.clear_lyrics_cache)) },
                        supportingContent = { Text(stringResource(R.string.clear_cache_description)) },
                        leadingContent = {
                            Icon(Icons.Rounded.Delete, contentDescription = null)
                        },
                        modifier = Modifier.clickable { showClearLyricsCacheDialog = true }
                    )
                }
            }

            // Data section (collapsible)
            item {
                CollapsibleSettingsSection(
                    title = stringResource(R.string.data),
                    icon = Icons.Rounded.DataObject,
                    initiallyExpanded = false
                ) {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.clear_recent_history)) },
                        leadingContent = {
                            Icon(Icons.Rounded.History, contentDescription = null)
                        },
                        modifier = Modifier.clickable { showClearHistoryDialog = true }
                    )
                }
            }

            // About section (collapsible)
            item {
                CollapsibleSettingsSection(
                    title = stringResource(R.string.about),
                    icon = Icons.Rounded.Info,
                    initiallyExpanded = false
                ) {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.version)) },
                        supportingContent = { Text(BuildConfig.VERSION_NAME) },
                        leadingContent = {
                            Icon(Icons.Rounded.Info, contentDescription = null)
                        },
                        modifier = Modifier.clickable { showAboutDialog = true }
                    )
                    
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.open_source_licenses)) },
                        leadingContent = {
                            Icon(Icons.Rounded.Code, contentDescription = null)
                        },
                        modifier = Modifier.clickable { }
                    )
                }
            }
            
            // Bottom padding
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Theme selection dialog
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text(stringResource(R.string.choose_theme)) },
            text = {
                Column(modifier = Modifier.selectableGroup()) {
                    ThemeOption.entries.forEach { theme ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = uiState.theme == theme,
                                    onClick = {
                                        viewModel.setTheme(theme)
                                        showThemeDialog = false
                                    },
                                    role = Role.RadioButton
                                )
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            RadioButton(
                                selected = uiState.theme == theme,
                                onClick = null
                            )
                            Text(theme.displayName)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text(stringResource(R.string.done))
                }
            }
        )
    }

    // Color source selection dialog
    if (showColorSourceDialog) {
        AlertDialog(
            onDismissRequest = { showColorSourceDialog = false },
            title = { Text(stringResource(R.string.color_source)) },
            text = {
                Column(modifier = Modifier.selectableGroup()) {
                    // Static colors
                    ColorSourceOption(
                        title = stringResource(R.string.color_source_static),
                        description = stringResource(R.string.color_source_static_desc),
                        selected = uiState.colorSource == ColorSource.STATIC,
                        onClick = {
                            viewModel.setColorSource(ColorSource.STATIC)
                            showColorSourceDialog = false
                        },
                        previewColor = MaterialTheme.colorScheme.primary
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Dynamic colors (Material You)
                    ColorSourceOption(
                        title = stringResource(R.string.color_source_dynamic),
                        description = stringResource(R.string.color_source_dynamic_desc),
                        selected = uiState.colorSource == ColorSource.DYNAMIC,
                        onClick = {
                            viewModel.setColorSource(ColorSource.DYNAMIC)
                            showColorSourceDialog = false
                        },
                        previewColor = MaterialTheme.colorScheme.tertiary,
                        enabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Album colors
                    ColorSourceOption(
                        title = stringResource(R.string.color_source_album),
                        description = stringResource(R.string.color_source_album_desc),
                        selected = uiState.colorSource == ColorSource.ALBUM,
                        onClick = {
                            viewModel.setColorSource(ColorSource.ALBUM)
                            showColorSourceDialog = false
                        },
                        previewColor = MaterialTheme.colorScheme.secondary
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showColorSourceDialog = false }) {
                    Text(stringResource(R.string.done))
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
    
    // About dialog
    if (showAboutDialog) {
        AboutDialog(onDismiss = { showAboutDialog = false })
    }
}

/**
 * Color source option item for selection
 */
@Composable
private fun ColorSourceOption(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
    previewColor: androidx.compose.ui.graphics.Color,
    enabled: Boolean = true
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .selectable(
                selected = selected,
                enabled = enabled,
                onClick = onClick,
                role = Role.RadioButton
            ),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
               else MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Color preview circle
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(previewColor.copy(alpha = if (enabled) 1f else 0.5f))
                    .then(
                        if (selected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                        else Modifier
                    )
            )
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface
                           else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant
                           else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
            
            RadioButton(
                selected = selected,
                onClick = null,
                enabled = enabled
            )
        }
    }
}

/**
 * Collapsible settings section component with enhanced design
 */
@Composable
private fun CollapsibleSettingsSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    initiallyExpanded: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    var isExpanded by remember { mutableStateOf(initiallyExpanded) }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        // Header - clickable to toggle expansion
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded },
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = MaterialTheme.shapes.large
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Icon container with gradient
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                
                AnimatedVisibility(
                    visible = isExpanded,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ExpandLess,
                        contentDescription = "Collapse",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                AnimatedVisibility(
                    visible = !isExpanded,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowDown,
                        contentDescription = "Expand",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
        
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, start = 8.dp, end = 8.dp),
                content = content
            )
        }
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
    val colorSource: ColorSource = ColorSource.DYNAMIC,
    val colorSourceDisplayName: String = "Dynamic",
    // Lyrics
    val lyricsFontSize: Float = 1.0f,
    val showTranslation: Boolean = true,
    val autoScroll: Boolean = true,
    val lyricsDisplayStyle: Int = 0,
    val enableGlow: Boolean = true,
    val enableKaraoke: Boolean = true,
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
) {
    // Legacy compatibility
    val useDynamicColors: Boolean get() = colorSource == ColorSource.DYNAMIC
}

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
            themePreferences.colorSource.collect { source ->
                val displayName = when (source) {
                    ColorSource.STATIC -> "Static"
                    ColorSource.DYNAMIC -> "Dynamic"
                    ColorSource.ALBUM -> "Album"
                }
                _uiState.value = _uiState.value.copy(
                    colorSource = source,
                    colorSourceDisplayName = displayName
                )
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
            lyricsPreferences.lyricsDisplayStyle.collect { style ->
                _uiState.value = _uiState.value.copy(lyricsDisplayStyle = style)
            }
        }
        viewModelScope.launch {
            lyricsPreferences.enableGlow.collect { enabled ->
                _uiState.value = _uiState.value.copy(enableGlow = enabled)
            }
        }
        viewModelScope.launch {
            lyricsPreferences.enableKaraoke.collect { enabled ->
                _uiState.value = _uiState.value.copy(enableKaraoke = enabled)
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

    fun setColorSource(source: ColorSource) {
        viewModelScope.launch {
            themePreferences.setColorSource(source)
        }
    }

    fun setDynamicColors(enabled: Boolean) {
        viewModelScope.launch {
            themePreferences.setColorSource(if (enabled) ColorSource.DYNAMIC else ColorSource.STATIC)
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

    fun setLyricsDisplayStyle(style: Int) {
        viewModelScope.launch {
            lyricsPreferences.setLyricsDisplayStyle(style)
        }
    }

    fun setEnableGlow(enabled: Boolean) {
        viewModelScope.launch {
            lyricsPreferences.setEnableGlow(enabled)
        }
    }

    fun setEnableKaraoke(enabled: Boolean) {
        viewModelScope.launch {
            lyricsPreferences.setEnableKaraoke(enabled)
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
            repository.scanMusicLibrary()
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
