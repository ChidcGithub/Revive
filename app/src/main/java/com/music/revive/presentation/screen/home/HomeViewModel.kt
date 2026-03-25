package com.music.revive.presentation.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.music.revive.data.repository.MusicRepository
import com.music.revive.domain.model.Album
import com.music.revive.domain.model.Artist
import com.music.revive.domain.model.Folder
import com.music.revive.domain.model.Playlist
import com.music.revive.domain.model.RecentSong
import com.music.revive.domain.model.Song
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val isLoading: Boolean = true,
    val isScanning: Boolean = false,
    val songs: List<Song> = emptyList(),
    val albums: List<Album> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val folders: List<Folder> = emptyList(),
    val recentSongs: List<RecentSong> = emptyList(),
    val favoriteSongIds: Set<Long> = emptySet(),
    val playlists: List<Playlist> = emptyList(),
    val searchQuery: String = "",
    val searchResults: List<Song> = emptyList(),
    val isSearching: Boolean = false,
    val cacheInfo: MusicRepository.CacheInfo? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: MusicRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    // Flow-based data from cache (fast loading)
    private val songsFlow = repository.getAllSongsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    private val artistsFlow = repository.getArtistsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    private val playlistsFlow = repository.getAllPlaylists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    private val favoriteIdsFlow = repository.getFavoriteSongIds()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadAllData()
    }

    private fun loadAllData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            // Check if cache is valid, if not scan first
            val isCacheValid = repository.isCacheValid()
            if (!isCacheValid) {
                _uiState.value = _uiState.value.copy(isScanning = true)
                repository.scanMusicLibrary()
                _uiState.value = _uiState.value.copy(isScanning = false)
            }

            // Collect flow-based data from cache
            launch {
                songsFlow.collect { songs ->
                    _uiState.update { it.copy(songs = songs, isLoading = false) }
                }
            }

            launch {
                artistsFlow.collect { artists ->
                    _uiState.update { it.copy(artists = artists) }
                }
            }

            launch {
                playlistsFlow.collect { playlists ->
                    _uiState.update { it.copy(playlists = playlists) }
                }
            }

            launch {
                favoriteIdsFlow.collect { ids ->
                    _uiState.update { it.copy(favoriteSongIds = ids.toSet()) }
                }
            }

            // Load albums from MediaStore (not cached yet)
            launch {
                val albums = repository.getAlbums()
                _uiState.update { it.copy(albums = albums) }
            }

            // Load cache info
            launch {
                val cacheInfo = repository.getCacheInfo()
                _uiState.update { it.copy(cacheInfo = cacheInfo) }
            }
        }
    }

    fun search(query: String) {
        _uiState.value = _uiState.value.copy(
            searchQuery = query,
            isSearching = true
        )

        viewModelScope.launch {
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

    fun toggleFavorite(songId: Long) {
        viewModelScope.launch {
            if (_uiState.value.favoriteSongIds.contains(songId)) {
                repository.removeFromFavorites(songId)
            } else {
                repository.addToFavorites(songId)
            }
        }
    }

    fun createPlaylist(name: String) {
        viewModelScope.launch {
            repository.createPlaylist(name)
        }
    }

    fun addToPlaylist(playlistId: Long, songId: Long) {
        viewModelScope.launch {
            repository.addSongToPlaylist(playlistId, songId)
        }
    }

    fun addToRecent(songId: Long) {
        viewModelScope.launch {
            repository.addToRecent(songId)
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isScanning = true)
            repository.scanMusicLibrary()
            _uiState.value = _uiState.value.copy(isScanning = false)
            
            // Reload albums
            val albums = repository.getAlbums()
            _uiState.update { it.copy(albums = albums) }
            
            // Update cache info
            val cacheInfo = repository.getCacheInfo()
            _uiState.update { it.copy(cacheInfo = cacheInfo) }
        }
    }
}