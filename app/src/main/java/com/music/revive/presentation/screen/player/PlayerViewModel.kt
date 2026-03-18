package com.music.revive.presentation.screen.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.music.revive.data.repository.MusicRepository
import com.music.revive.domain.model.PlayerState
import com.music.revive.service.MusicPlayer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val musicPlayer: MusicPlayer,
    private val repository: MusicRepository
) : ViewModel() {

    val playerState: StateFlow<PlayerState> = musicPlayer.playerState

    private val _isFavorite = MutableStateFlow(false)
    val isFavorite: StateFlow<Boolean> = _isFavorite.asStateFlow()

    init {
        viewModelScope.launch {
            musicPlayer.currentSong.collect { song ->
                song?.let { currentSong ->
                    repository.isFavorite(currentSong.id).collect { isFav ->
                        _isFavorite.value = isFav
                    }
                } ?: run {
                    _isFavorite.value = false
                }
            }
        }
    }

    fun playPause() {
        musicPlayer.playPause()
    }

    fun playNext() {
        musicPlayer.playNext()
    }

    fun playPrevious() {
        musicPlayer.playPrevious()
    }

    fun seekTo(position: Long) {
        musicPlayer.seekTo(position)
    }

    fun toggleShuffle() {
        musicPlayer.toggleShuffle()
    }

    fun cycleRepeatMode() {
        musicPlayer.cycleRepeatMode()
    }

    fun toggleFavorite() {
        viewModelScope.launch {
            musicPlayer.currentSong.value?.let { song ->
                if (_isFavorite.value) {
                    repository.removeFromFavorites(song.id)
                } else {
                    repository.addToFavorites(song.id)
                }
                _isFavorite.value = !_isFavorite.value
            }
        }
    }

    fun updatePosition() {
        musicPlayer.updatePosition()
    }
}