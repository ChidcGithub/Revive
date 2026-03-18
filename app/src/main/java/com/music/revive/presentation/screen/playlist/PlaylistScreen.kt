package com.music.revive.presentation.screen.playlist

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.music.revive.R
import com.music.revive.domain.model.Playlist
import com.music.revive.domain.model.Song
import com.music.revive.presentation.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistScreen(
    onPlaylistClick: (Long) -> Unit,
    onSongClick: (Song, List<Song>) -> Unit,
    onFavoriteClick: (Long) -> Unit,
    viewModel: PlaylistViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    var playlistToDelete by remember { mutableStateOf<Playlist?>(null) }
    var expandedPlaylistMenu by remember { mutableStateOf<Long?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.playlists)) }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.create_playlist))
            }
        }
    ) { paddingValues ->
        if (uiState.playlists.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.PlaylistPlay,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.no_playlists),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = { showCreateDialog = true }) {
                        Text(stringResource(R.string.create_first_playlist))
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Favorites playlist
                item {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.favorites)) },
                        supportingContent = { Text("${uiState.favoriteCount} ${stringResource(R.string.songs)}") },
                        leadingContent = {
                            Surface(
                                modifier = Modifier.size(48.dp),
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.errorContainer
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Favorite,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        },
                        modifier = Modifier.clickable { /* Navigate to favorites */ }
                    )
                }

                // Recent played
                item {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.recently_played)) },
                        supportingContent = { Text("${uiState.recentCount} ${stringResource(R.string.songs)}") },
                        leadingContent = {
                            Surface(
                                modifier = Modifier.size(48.dp),
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.tertiaryContainer
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.History,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                }
                            }
                        },
                        modifier = Modifier.clickable { /* Navigate to recent */ }
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // User playlists
                items(uiState.playlists) { playlist ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PlaylistItem(
                            playlist = playlist,
                            onClick = { onPlaylistClick(playlist.id) },
                            modifier = Modifier.weight(1f)
                        )
                        Box {
                            IconButton(onClick = { expandedPlaylistMenu = playlist.id }) {
                                Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.more_options))
                            }

                            DropdownMenu(
                                expanded = expandedPlaylistMenu == playlist.id,
                                onDismissRequest = { expandedPlaylistMenu = null }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.rename)) },
                                    onClick = { expandedPlaylistMenu = null },
                                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.delete)) },
                                    onClick = {
                                        playlistToDelete = playlist
                                        expandedPlaylistMenu = null
                                    },
                                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { name ->
                viewModel.createPlaylist(name)
                showCreateDialog = false
            }
        )
    }

    playlistToDelete?.let { playlist ->
        DeletePlaylistDialog(
            playlistName = playlist.name,
            onDismiss = { playlistToDelete = null },
            onConfirm = {
                viewModel.deletePlaylist(playlist.id)
                playlistToDelete = null
            }
        )
    }
}

data class PlaylistUiState(
    val playlists: List<Playlist> = emptyList(),
    val favoriteCount: Int = 0,
    val recentCount: Int = 0
)

class PlaylistViewModel @javax.inject.Inject constructor(
    private val repository: com.music.revive.data.repository.MusicRepository
) : androidx.lifecycle.ViewModel() {

    private val _uiState = MutableStateFlow(PlaylistUiState())
    val uiState: kotlinx.coroutines.flow.StateFlow<PlaylistUiState> = _uiState.asStateFlow()

    init {
        loadPlaylists()
    }

    private fun loadPlaylists() {
        kotlinx.coroutines.GlobalScope.launch {
            repository.getAllPlaylists().collect { playlists ->
                _uiState.value = _uiState.value.copy(playlists = playlists)
            }
        }
        kotlinx.coroutines.GlobalScope.launch {
            repository.getFavoriteSongIds().collect { favorites ->
                _uiState.value = _uiState.value.copy(favoriteCount = favorites.size)
            }
        }
    }

    fun createPlaylist(name: String) {
        kotlinx.coroutines.GlobalScope.launch {
            repository.createPlaylist(name)
        }
    }

    fun deletePlaylist(playlistId: Long) {
        kotlinx.coroutines.GlobalScope.launch {
            repository.deletePlaylist(playlistId)
        }
    }
}

private val MutableStateFlow = kotlinx.coroutines.flow.MutableStateFlow
private fun <T> MutableStateFlow<T>.asStateFlow() = kotlinx.coroutines.flow.StateFlow(this)
private val androidx.compose.foundation.layout.Arrangement.spacedBy
    get() = androidx.compose.foundation.layout.Arrangement
