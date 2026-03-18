package com.music.revive.presentation.screen.search

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.music.revive.R
import com.music.revive.domain.model.Song
import com.music.revive.presentation.components.SongItemWithFavorite

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onNavigateBack: () -> Unit,
    onSongClick: (Song, List<Song>) -> Unit,
    onFavoriteClick: (Long) -> Unit,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(searchQuery) {
        viewModel.search(searchQuery)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text(stringResource(R.string.search_songs)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.clear))
                                }
                            }
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { paddingValues ->
        if (uiState.isSearching) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.searchResults.isEmpty()) {
            if (searchQuery.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.search_hint),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.no_results_found, searchQuery),
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
                items(uiState.searchResults) { song ->
                    SongItemWithFavorite(
                        song = song,
                        isFavorite = uiState.favoriteSongIds.contains(song.id),
                        onPlayClick = { onSongClick(song, uiState.searchResults) },
                        onFavoriteClick = { onFavoriteClick(song.id) }
                    )
                }
            }
        }
    }
}

data class SearchUiState(
    val searchQuery: String = "",
    val searchResults: List<Song> = emptyList(),
    val isSearching: Boolean = false,
    val favoriteSongIds: Set<Long> = emptySet()
)

class SearchViewModel @javax.inject.Inject constructor(
    private val repository: com.music.revive.data.repository.MusicRepository
) : androidx.lifecycle.ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: kotlinx.coroutines.flow.StateFlow<SearchUiState> = _uiState.asStateFlow()

    init {
        loadFavorites()
    }

    private fun loadFavorites() {
        kotlinx.coroutines.GlobalScope.launch {
            repository.getFavoriteSongIds().collect { ids ->
                _uiState.value = _uiState.value.copy(favoriteSongIds = ids.toSet())
            }
        }
    }

    fun search(query: String) {
        _uiState.value = _uiState.value.copy(
            searchQuery = query,
            isSearching = true
        )

        kotlinx.coroutines.GlobalScope.launch {
            if (query.isBlank()) {
                _uiState.value = _uiState.value.copy(
                    searchResults = emptyList(),
                    isSearching = false
                )
            } else {
                val results = repository.searchSongs(query)
                _uiState.value = _uiState.value.copy(
                    searchResults = results,
                    isSearching = false
                )
            }
        }
    }
}

private val MutableStateFlow = kotlinx.coroutines.flow.MutableStateFlow
private fun <T> MutableStateFlow<T>.asStateFlow() = kotlinx.coroutines.flow.StateFlow(this)
