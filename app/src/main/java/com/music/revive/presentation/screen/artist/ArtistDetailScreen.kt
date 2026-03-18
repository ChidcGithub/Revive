package com.music.revive.presentation.screen.artist

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
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import coil.compose.AsyncImage
import com.music.revive.R
import com.music.revive.data.repository.MusicRepository
import com.music.revive.domain.model.Album
import com.music.revive.domain.model.Song
import com.music.revive.presentation.components.AlbumListItem
import com.music.revive.presentation.components.SongItem
import com.music.revive.presentation.navigation.ArtistDetail
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistDetailScreen(
    onNavigateBack: () -> Unit,
    onSongClick: (Song, List<Song>) -> Unit,
    viewModel: ArtistDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        viewModel.loadArtistData()
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
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        modifier = Modifier
                            .size(180.dp)
                            .clip(MaterialTheme.shapes.extraLarge),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(80.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = uiState.artistName,
                        style = MaterialTheme.typography.headlineSmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${uiState.albums.size} ${stringResource(R.string.albums)} - ${uiState.songs.size} ${stringResource(R.string.songs)}",
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

            item {
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text(stringResource(R.string.songs)) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text(stringResource(R.string.albums)) }
                    )
                }
            }

            if (selectedTab == 0) {
                items(uiState.songs) { song ->
                    SongItem(
                        song = song,
                        onPlayClick = { onSongClick(song, uiState.songs) },
                        onMenuClick = { }
                    )
                }
            } else {
                items(uiState.albums) { album ->
                    AlbumListItem(
                        album = album,
                        onClick = { }
                    )
                }
            }
        }
    }
}

data class ArtistDetailUiState(
    val artistId: Long = 0,
    val artistName: String = "Unknown Artist",
    val songs: List<Song> = emptyList(),
    val albums: List<Album> = emptyList(),
    val isLoading: Boolean = false
)

@HiltViewModel
class ArtistDetailViewModel @Inject constructor(
    private val repository: MusicRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val artistDetail: ArtistDetail = savedStateHandle.toRoute<ArtistDetail>()

    private val _uiState = MutableStateFlow(ArtistDetailUiState(artistId = artistDetail.artistId))
    val uiState: StateFlow<ArtistDetailUiState> = _uiState.asStateFlow()

    fun loadArtistData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val songs = repository.getSongsByArtist(artistDetail.artistId)
            _uiState.value = _uiState.value.copy(
                songs = songs,
                isLoading = false,
                artistName = songs.firstOrNull()?.artist ?: "Unknown Artist"
            )
        }
    }

    fun playAll() {}

    fun shuffleAll() {}
}