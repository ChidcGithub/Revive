package com.music.revive.service

import android.app.Notification
import android.content.Intent
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import com.music.revive.domain.model.RepeatMode
import com.music.revive.domain.model.Song
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MusicService : MediaSessionService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var mediaSession: MediaSession? = null

    @Inject
    lateinit var musicPlayer: MusicPlayer

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()

        mediaSession = MediaSession.Builder(this, musicPlayer.player)
            .setCallback(object : MediaSession.Callback {
                override fun onConnect(
                    session: MediaSession,
                    controller: MediaSession.ControllerInfo
                ): MediaSession.ConnectionResult {
                    return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                        .setAvailableSessionCommands(MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS)
                        .build()
                }

                override fun onCustomCommand(
                    session: MediaSession,
                    controller: MediaSession.ControllerInfo,
                    customCommand: SessionCommand,
                    args: Bundle
                ): androidx.media3.session.MediaSession.ConnectionResult {
                    when (customCommand.customAction) {
                        ACTION_SHUFFLE -> musicPlayer.toggleShuffle()
                        ACTION_REPEAT -> musicPlayer.cycleRepeatMode()
                    }
                    return MediaSession.ConnectionResult.AcceptedResultBuilder(session).build()
                }
            })
            .build()

        musicPlayer.start()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        serviceScope.cancel()
        mediaSession?.release()
        mediaSession = null
        musicPlayer.release()
        super.onDestroy()
    }

    companion object {
        const val ACTION_SHUFFLE = "com.music.revive.action.SHUFFLE"
        const val ACTION_REPEAT = "com.music.revive.action.REPEAT"
    }
}

@OptIn(UnstableApi::class)
class MusicPlayer @Inject constructor(
    private val context: android.content.Context,
    private val notificationManager: MusicNotificationManager
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _playerState = MutableStateFlow(com.music.revive.domain.model.PlayerState())
    val playerState: StateFlow<com.music.revive.domain.model.PlayerState> = _playerState.asStateFlow()

    private val _queue = MutableStateFlow<List<Song>>(emptyList())
    val queue: StateFlow<List<Song>> = _queue.asStateFlow()

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _repeatMode = MutableStateFlow(RepeatMode.OFF)
    val repeatMode: StateFlow<RepeatMode> = _repeatMode.asStateFlow()

    private val _isShuffleEnabled = MutableStateFlow(false)
    val isShuffleEnabled: StateFlow<Boolean> = _isShuffleEnabled.asStateFlow()

    private val _position = MutableStateFlow(0L)
    val position: StateFlow<Long> = _position.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    val player: ExoPlayer by lazy {
        ExoPlayer.Builder(context).build().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true
            )
            setHandleAudioBecomingNoisy(true)
            addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _isPlaying.value = isPlaying
                    updatePlayerState()
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_ENDED) {
                        onSongEnded()
                    }
                    updatePlayerState()
                }

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    updateCurrentSong()
                    updatePlayerState()
                }
            })
        }
    }

    private var currentIndex = 0

    fun start() {}

    fun playSong(song: Song, songList: List<Song> = emptyList()) {
        val songs = if (songList.isEmpty()) listOf(song) else songList
        _queue.value = songs
        currentIndex = songs.indexOf(song).takeIf { it >= 0 } ?: 0

        _currentSong.value = song
        playAtIndex(currentIndex)
    }

    private fun playAtIndex(index: Int) {
        val songs = _queue.value
        if (index !in songs.indices) return

        currentIndex = index
        val song = songs[index]
        _currentSong.value = song

        val mediaItem = MediaItem.fromUri(song.path)
        player.setMediaItem(mediaItem)
        player.prepare()
        player.playWhenReady = true

        notificationManager.updateNotification(song, true)
    }

    fun playPause() {
        if (player.isPlaying) {
            player.pause()
        } else {
            player.play()
        }
        notificationManager.updateNotification(_currentSong.value, player.isPlaying)
    }

    fun playNext() {
        val songs = _queue.value
        if (songs.isEmpty()) return

        when (_repeatMode.value) {
            RepeatMode.ONE -> {
                player.seekTo(0)
                player.play()
            }
            else -> {
                val nextIndex = if (_isShuffleEnabled.value) {
                    (0 until songs.size).random()
                } else {
                    (currentIndex + 1) % songs.size
                }
                playAtIndex(nextIndex)
            }
        }
    }

    fun playPrevious() {
        val songs = _queue.value
        if (songs.isEmpty()) return

        if (player.currentPosition > 3000) {
            player.seekTo(0)
            return
        }

        val prevIndex = if (_isShuffleEnabled.value) {
            (0 until songs.size).random()
        } else {
            (currentIndex - 1).let { if (it < 0) songs.size - 1 else it }
        }
        playAtIndex(prevIndex)
    }

    fun seekTo(position: Long) {
        player.seekTo(position)
        _position.value = position
    }

    fun toggleShuffle() {
        _isShuffleEnabled.value = !_isShuffleEnabled.value
        updatePlayerState()
    }

    fun cycleRepeatMode() {
        _repeatMode.value = when (_repeatMode.value) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        player.repeatMode = when (_repeatMode.value) {
            RepeatMode.OFF -> Player.REPEAT_MODE_OFF
            RepeatMode.ONE -> Player.REPEAT_MODE_ONE
            RepeatMode.ALL -> Player.REPEAT_MODE_ALL
        }
        updatePlayerState()
    }

    fun addToQueue(song: Song) {
        _queue.value = _queue.value + song
    }

    fun removeFromQueue(index: Int) {
        val currentQueue = _queue.value.toMutableList()
        if (index in currentQueue.indices) {
            currentQueue.removeAt(index)
            _queue.value = currentQueue
            if (index < currentIndex) {
                currentIndex--
            }
        }
    }

    private fun onSongEnded() {
        when (_repeatMode.value) {
            RepeatMode.ONE -> {
                player.seekTo(0)
                player.play()
            }
            RepeatMode.ALL -> playNext()
            RepeatMode.OFF -> {
                if (currentIndex < _queue.value.size - 1) {
                    playNext()
                }
            }
        }
    }

    private fun updateCurrentSong() {
        val songs = _queue.value
        if (currentIndex in songs.indices) {
            _currentSong.value = songs[currentIndex]
        }
    }

    private fun updatePlayerState() {
        _position.value = player.currentPosition
        _duration.value = player.duration.takeIf { it > 0 } ?: 0

        _playerState.value = com.music.revive.domain.model.PlayerState(
            currentSong = _currentSong.value,
            isPlaying = _isPlaying.value,
            position = _position.value,
            duration = _duration.value,
            repeatMode = _repeatMode.value,
            isShuffleEnabled = _isShuffleEnabled.value,
            queue = _queue.value,
            queueIndex = currentIndex
        )
    }

    fun release() {
        player.release()
    }

    fun updatePosition() {
        _position.value = player.currentPosition
        updatePlayerState()
    }
}