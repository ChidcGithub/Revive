package com.music.revive.presentation.screen.album

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.music.revive.R
import com.music.revive.domain.model.Album
import com.music.revive.domain.model.Song
import com.music.revive.presentation.components.SongItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumDetailScreen(
    album: Album,
    onNavigateBack: () -> Unit,
    onSongClick: (Song, List<Song>) -> Unit,
    onAddToPlaylist: (Long) -> Unit,
    viewModel: AlbumDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(album.id) {
        viewModel.loadAlbumSongs(album.id)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.playAll() }) {
                        Icon(Icons.Default.PlayArrow, contentDescription = stringResource(R.string.play_all))
                    }
                    IconButton(onClick = { viewModel.shuffleAll() }) {
                        Icon(Icons.Default.Shuffle, contentDescription = stringResource(R.string.shuffle))
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Album header
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        modifier = Modifier
                            .size(200.dp)
                            .clip(MaterialTheme.shapes.large),
                        tonalElevation = 4.dp
                    ) {
                        AsyncImage(
                            model = album.albumArtUri,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = album.name,
                        style = MaterialTheme.typography.headlineSmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = album.artist,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${album.numberOfSongs} ${stringResource(R.string.songs)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Button(onClick = { viewModel.playAll() }) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.play_all))
                        }
                        OutlinedButton(onClick = { viewModel.shuffleAll() }) {
                            Icon(Icons.Default.Shuffle, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.shuffle))
                        }
                    }
                }
            }

            // Songs
            items(uiState.songs) { song ->
                SongItem(
                    song = song,
                    onPlayClick = { onSongClick(song, uiState.songs) },
                    onMenuClick = { /* Show menu */ }
                )
            }
        }
    }
}

data class AlbumDetailUiState(
    val songs: List<Song> = emptyList(),
    val isLoading: Boolean = false
)

class AlbumDetailViewModel @javax.inject.Inject constructor(
    private val repository: com.music.revive.data.repository.MusicRepository
) : androidx.lifecycle.ViewModel() {

    private val _uiState = MutableStateFlow(AlbumDetailUiState())
    val uiState: kotlinx.coroutines.flow.StateFlow<AlbumDetailUiState> = _uiState.asStateFlow()

    fun loadAlbumSongs(albumId: Long) {
        kotlinx.coroutines.GlobalScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val songs = repository.getSongsByAlbum(albumId)
            _uiState.value = _uiState.value.copy(songs = songs, isLoading = false)
        }
    }

    fun playAll() {
        // Play all songs in album
    }

    fun shuffleAll() {
        // Shuffle and play all songs
    }
}

private val MutableStateFlow = kotlinx.coroutines.flow.MutableStateFlow
private fun <T> MutableStateFlow<T>.asStateFlow() = kotlinx.coroutines.flow.StateFlow(this)
