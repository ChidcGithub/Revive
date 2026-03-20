package com.music.revive.presentation.screen.settings

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.music.revive.R
import com.music.revive.BuildConfig
import com.music.revive.data.repository.MusicRepository
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
                    headlineContent = { Text(stringResource(R.string.show_folders)) },
                    supportingContent = { Text(stringResource(R.string.show_folders_description)) },
                    leadingContent = {
                        Icon(Icons.Default.Folder, contentDescription = null)
                    },
                    trailingContent = {
                        Switch(
                            checked = uiState.showFolders,
                            onCheckedChange = { viewModel.setShowFolders(it) }
                        )
                    }
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
}

enum class ThemeOption(val displayName: String) {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark")
}

data class SettingsUiState(
    val theme: ThemeOption = ThemeOption.SYSTEM,
    val themeDisplayName: String = ThemeOption.SYSTEM.displayName,
    val useDynamicColors: Boolean = true,
    val handleAudioFocus: Boolean = true,
    val showFolders: Boolean = false,
    val isScanning: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: MusicRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun setTheme(theme: ThemeOption) {
        _uiState.value = _uiState.value.copy(
            theme = theme,
            themeDisplayName = theme.displayName
        )
    }

    fun setDynamicColors(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(useDynamicColors = enabled)
    }

    fun setAudioFocusHandling(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(handleAudioFocus = enabled)
    }

    fun setShowFolders(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(showFolders = enabled)
    }

    fun scanMusicLibrary() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isScanning = true)
            // Trigger a rescan by calling getAllSongs which reads from MediaStore
            repository.getAllSongs()
            repository.getAlbums()
            repository.getArtists()
            _uiState.value = _uiState.value.copy(isScanning = false)
        }
    }

    fun clearRecentHistory() {
        viewModelScope.launch {
            repository.clearRecentSongs()
        }
    }
}