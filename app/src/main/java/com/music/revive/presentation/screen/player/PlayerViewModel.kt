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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val musicPlayer: MusicPlayer,
    private val repository: MusicRepository
) : ViewModel() {

    val playerState: StateFlow<PlayerState> = musicPlayer.playerState

    val isFavorite: StateFlow<Boolean> = musicPlayer.currentSong.map { song ->
        song?.let { repository.isFavorite(it.id) } ?: false
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)

    private val _currentSong = musicPlayer.currentSong

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
            _currentSong.value?.let { song ->
                if (isFavorite.value) {
                    repository.removeFromFavorites(song.id)
                } else {
                    repository.addToFavorites(song.id)
                }
            }
        }
    }

    fun updatePosition() {
        musicPlayer.updatePosition()
    }
}
