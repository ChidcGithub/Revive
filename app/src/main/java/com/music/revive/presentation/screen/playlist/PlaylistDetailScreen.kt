package com.music.revive.presentation.screen.playlist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.music.revive.R
import com.music.revive.data.repository.MusicRepository
import com.music.revive.domain.model.Song
import com.music.revive.presentation.components.AlbumArtWithFallback
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlaylistDetailUiState(
    val playlistName: String = "",
    val songs: List<Song> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class PlaylistDetailViewModel @Inject constructor(
    private val repository: MusicRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlaylistDetailUiState())
    val uiState: StateFlow<PlaylistDetailUiState> = _uiState.asStateFlow()

    private var currentPlaylistId: Long = -1

    fun loadPlaylist(playlistId: Long) {
        if (currentPlaylistId == playlistId) return
        currentPlaylistId = playlistId

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            // Get playlist name
            repository.getAllPlaylists().first().find { it.id == playlistId }?.let { playlist ->
                _uiState.value = _uiState.value.copy(playlistName = playlist.name)
            }

            // Get all songs and filter by playlist song IDs
            val allSongs = repository.getAllSongs()
            repository.getPlaylistSongIds(playlistId).collect { songIds ->
                val playlistSongs = allSongs.filter { song -> songIds.contains(song.id) }
                _uiState.value = _uiState.value.copy(
                    songs = playlistSongs,
                    isLoading = false
                )
            }
        }
    }

    fun removeSongFromPlaylist(songId: Long) {
        viewModelScope.launch {
            repository.removeSongFromPlaylist(currentPlaylistId, songId)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    playlistId: Long,
    onNavigateBack: () -> Unit,
    onSongClick: (Song, List<Song>) -> Unit,
    viewModel: PlaylistDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(playlistId) {
        viewModel.loadPlaylist(playlistId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.playlistName,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    if (uiState.songs.isNotEmpty()) {
                        IconButton(onClick = { /* Shuffle all */ }) {
                            Icon(Icons.Rounded.Shuffle, contentDescription = stringResource(R.string.shuffle))
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.songs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Rounded.MusicNote,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.playlist_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                items(uiState.songs, key = { it.id }) { song ->
                    PlaylistSongItem(
                        song = song,
                        onClick = { onSongClick(song, uiState.songs) },
                        onRemove = { viewModel.removeSongFromPlaylist(song.id) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaylistSongItem(
    song: Song,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    ListItem(
        headlineContent = {
            Text(
                text = song.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            Text(
                text = song.artist,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        leadingContent = {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = MaterialTheme.shapes.small
            ) {
                AlbumArtWithFallback(
                    albumArtUri = song.albumArtUri,
                    modifier = Modifier.fillMaxSize()
                )
            }
        },
        trailingContent = {
            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = Icons.Rounded.Remove,
                    contentDescription = stringResource(R.string.remove_from_playlist)
                )
            }
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
}