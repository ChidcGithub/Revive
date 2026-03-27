package com.music.revive.presentation.screen.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import com.music.revive.data.lyric.LyricRepository
import com.music.revive.data.local.LyricsPreferences
import com.music.revive.data.repository.MusicRepository
import com.music.revive.domain.model.Lyric
import com.music.revive.domain.model.PlayerState
import com.music.revive.service.MusicPlayer
import com.music.revive.data.local.PlayerPreferences
import com.music.revive.domain.utils.CarBluetoothLyrics
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    internal val musicPlayer: MusicPlayer,
    private val repository: MusicRepository,
    private val lyricRepository: LyricRepository,
    private val lyricsPreferences: LyricsPreferences,
    private val playerPreferences: PlayerPreferences,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val playerState: StateFlow<PlayerState> = musicPlayer.playerState

    private val _isFavorite = MutableStateFlow(false)
    val isFavorite: StateFlow<Boolean> = _isFavorite.asStateFlow()

    // Lyrics state
    private val _currentLyrics = MutableStateFlow<Lyric>(Lyric.Empty)
    val currentLyrics: StateFlow<Lyric> = _currentLyrics.asStateFlow()

    private val _isLoadingLyrics = MutableStateFlow(false)
    val isLoadingLyrics: StateFlow<Boolean> = _isLoadingLyrics.asStateFlow()

    // Settings
    val lyricsFontSize: StateFlow<Float> = lyricsPreferences.lyricsFontSize
        .stateIn(viewModelScope, SharingStarted.Lazily, 1.0f)

    val showTranslation: StateFlow<Boolean> = lyricsPreferences.showTranslation
        .stateIn(viewModelScope, SharingStarted.Lazily, true)

    val lyricsDisplayStyle: StateFlow<Int> = lyricsPreferences.lyricsDisplayStyle
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)

    val enableGlow: StateFlow<Boolean> = lyricsPreferences.enableGlow
        .stateIn(viewModelScope, SharingStarted.Lazily, true)

    val enableKaraoke: StateFlow<Boolean> = lyricsPreferences.enableKaraoke
        .stateIn(viewModelScope, SharingStarted.Lazily, true)

    val enableHapticFeedback: StateFlow<Boolean> = playerPreferences.enableHapticFeedback
        .stateIn(viewModelScope, SharingStarted.Lazily, true)

    val enableBlur: StateFlow<Boolean> = lyricsPreferences.enableBlur
        .stateIn(viewModelScope, SharingStarted.Lazily, false)
    
    val enableFullScreenLyricsButton: StateFlow<Boolean> = lyricsPreferences.enableFullScreenLyrics
        .stateIn(viewModelScope, SharingStarted.Lazily, true)
    
    val enableShaderEffect: StateFlow<Boolean> = playerPreferences.enableShaderEffect
        .stateIn(viewModelScope, SharingStarted.Lazily, false)
    
    val enableBalancedLines: StateFlow<Boolean> = lyricsPreferences.enableBalancedLines
        .stateIn(viewModelScope, SharingStarted.Lazily, false)
    
    val enableCarBluetoothLyrics: StateFlow<Boolean> = playerPreferences.enableCarBluetoothLyrics
        .stateIn(viewModelScope, SharingStarted.Lazily, false)
    
    // Car Bluetooth Lyrics helper
    private val carBluetoothLyrics = CarBluetoothLyrics(context.applicationContext)

    private var favoriteJob: Job? = null

    init {
        // Initialize car Bluetooth lyrics
        viewModelScope.launch {
            enableCarBluetoothLyrics.collect { enabled ->
                carBluetoothLyrics.setCarModeEnabled(enabled)
            }
        }
        
        viewModelScope.launch {
            musicPlayer.currentSong.collect { song ->
                favoriteJob?.cancel()

                song?.let { currentSong ->
                    // Update favorite status in a separate coroutine
                    // so it doesn't block loadLyrics
                    favoriteJob = launch {
                        repository.isFavorite(currentSong.id).collect { isFav ->
                            _isFavorite.value = isFav
                        }
                    }

                    // Load lyrics for the new song
                    loadLyrics(currentSong)
                } ?: run {
                    _isFavorite.value = false
                    _currentLyrics.value = Lyric.Empty
                }
            }
        }
    }

    private fun loadLyrics(song: com.music.revive.domain.model.Song) {
        viewModelScope.launch {
            _isLoadingLyrics.value = true
            val lyrics = lyricRepository.loadLyrics(song)
            _currentLyrics.value = lyrics
            _isLoadingLyrics.value = false
            
            // Send lyrics to car Bluetooth system
            carBluetoothLyrics.updateLyrics(lyrics)
        }
    }

    fun refreshLyrics() {
        viewModelScope.launch {
            musicPlayer.currentSong.value?.let { song ->
                lyricRepository.clearCache(song.id)
                loadLyrics(song)
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
        
        // Update car Bluetooth lyrics current line
        viewModelScope.launch {
            carBluetoothLyrics.updateCurrentLine(musicPlayer.playerState.value.position)
        }
    }
    
    /**
     * Set car Bluetooth lyrics mode
     */
    fun setCarBluetoothLyricsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            playerPreferences.setEnableCarBluetoothLyrics(enabled)
        }
    }
}
