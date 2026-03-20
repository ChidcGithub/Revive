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
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val isLoading: Boolean = true,
    val songs: List<Song> = emptyList(),
    val albums: List<Album> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val folders: List<Folder> = emptyList(),
    val recentSongs: List<RecentSong> = emptyList(),
    val favoriteSongIds: Set<Long> = emptySet(),
    val playlists: List<Playlist> = emptyList(),
    val searchQuery: String = "",
    val searchResults: List<Song> = emptyList(),
    val isSearching: Boolean = false
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: MusicRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadAllData()
    }

    private fun loadAllData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            launch {
                val songs = repository.getAllSongs()
                _uiState.value = _uiState.value.copy(songs = songs)
            }

            launch {
                val albums = repository.getAlbums()
                _uiState.value = _uiState.value.copy(albums = albums)
            }

            launch {
                val artists = repository.getArtists()
                _uiState.value = _uiState.value.copy(artists = artists)
            }

            launch {
                repository.getAllPlaylists().collect { playlists ->
                    _uiState.value = _uiState.value.copy(playlists = playlists)
                }
            }

            launch {
                repository.getFavoriteSongIds().collect { ids ->
                    _uiState.value = _uiState.value.copy(favoriteSongIds = ids.toSet())
                }
            }

            _uiState.value = _uiState.value.copy(isLoading = false)
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
        loadAllData()
    }
}
