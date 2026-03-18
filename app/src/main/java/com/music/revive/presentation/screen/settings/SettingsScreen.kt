package com.music.revive.presentation.screen.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.music.revive.R
import com.music.revive.BuildConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showThemeDialog by remember { mutableStateOf(false) }

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
                    modifier = Modifier.clickable { viewModel.clearRecentHistory() }
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
                    modifier = Modifier.clickable { /* Open licenses */ }
                )
            }
        }
    }

    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text(stringResource(R.string.choose_theme)) }
        ) {
            Column {
                ThemeOption.values().forEach { theme ->
                    DropdownMenuItem(
                        text = { Text(theme.displayName) },
                        onClick = {
                            viewModel.setTheme(theme)
                            showThemeDialog = false
                        },
                        leadingIcon = {
                            if (uiState.theme == theme) {
                                Icon(Icons.Default.Check, contentDescription = null)
                            }
                        }
                    )
                }
            }
        }
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
    val handleAudioFocus: Boolean = true
)

class SettingsViewModel @javax.inject.Inject constructor(
    private val repository: com.music.revive.data.repository.MusicRepository
) : androidx.lifecycle.ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: kotlinx.coroutines.flow.StateFlow<SettingsUiState> = _uiState.asStateFlow()

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

    fun clearRecentHistory() {
        kotlinx.coroutines.GlobalScope.launch {
            repository.clearRecentSongs()
        }
    }
}

private val MutableStateFlow = kotlinx.coroutines.flow.MutableStateFlow
private fun <T> MutableStateFlow<T>.asStateFlow() = kotlinx.coroutines.flow.StateFlow(this)
