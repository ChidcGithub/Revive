package com.music.revive.service

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import com.music.revive.domain.model.PlayerState
import com.music.revive.domain.model.RepeatMode
import com.music.revive.domain.model.Song
import com.music.revive.data.repository.MusicRepository
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "MusicService"

@AndroidEntryPoint
class MusicService : MediaSessionService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var mediaSession: MediaSession? = null

    @Inject
    lateinit var musicPlayer: MusicPlayer

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()

        try {
            mediaSession = MediaSession.Builder(this, musicPlayer.player)
                .setCallback(object : MediaSession.Callback {
                    override fun onConnect(
                        session: MediaSession,
                        controller: MediaSession.ControllerInfo
                    ): MediaSession.ConnectionResult {
                        val sessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS
                            .buildUpon()
                            .add(SessionCommand(ACTION_SHUFFLE, android.os.Bundle()))
                            .add(SessionCommand(ACTION_REPEAT, android.os.Bundle()))
                            .build()
                        return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                            .setAvailableSessionCommands(sessionCommands)
                            .build()
                    }

                    override fun onCustomCommand(
                        session: MediaSession,
                        controller: MediaSession.ControllerInfo,
                        customCommand: androidx.media3.session.SessionCommand,
                        args: android.os.Bundle
                    ): com.google.common.util.concurrent.ListenableFuture<androidx.media3.session.SessionResult> {
                        when (customCommand.customAction) {
                            ACTION_SHUFFLE -> musicPlayer.toggleShuffle()
                            ACTION_REPEAT -> musicPlayer.cycleRepeatMode()
                        }
                        return com.google.common.util.concurrent.Futures.immediateFuture(
                            androidx.media3.session.SessionResult(androidx.media3.session.SessionResult.RESULT_SUCCESS)
                        )
                    }
                })
                .build()

            musicPlayer.start()
            Log.d(TAG, "MusicService created successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing MusicService", e)
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        Log.d(TAG, "MusicService destroying")
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

/**
 * Singleton class that manages music playback using ExoPlayer
 */
@OptIn(UnstableApi::class)
@Singleton
class MusicPlayer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val notificationManager: MusicNotificationManager,
    private val musicRepository: MusicRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    companion object {
        private const val TAG = "MusicPlayer"
        private const val POSITION_UPDATE_INTERVAL = 500L
        private const val MAX_RETRY_COUNT = 3
        private const val RETRY_DELAY_MS = 1000L
    }

    // State flows
    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

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

    private val _playbackError = MutableStateFlow<String?>(null)
    val playbackError: StateFlow<String?> = _playbackError.asStateFlow()

    // Player instance
    private var _player: ExoPlayer? = null
    private var currentIndex = 0
    private var retryCount = 0
    private var positionUpdateJob: kotlinx.coroutines.Job? = null

    val player: ExoPlayer
        get() = _player ?: createPlayer()

    private fun createPlayer(): ExoPlayer {
        return ExoPlayer.Builder(context)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true
            )
            .setHandleAudioBecomingNoisy(true)
            .setUseLazyPreparation(true)
            .build()
            .also { exoPlayer ->
                _player = exoPlayer
                setupPlayer(exoPlayer)
                Log.d(TAG, "ExoPlayer created successfully")
            }
    }

    private fun setupPlayer(exoPlayer: ExoPlayer) {
        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
                updatePlayerState()
                
                if (isPlaying) {
                    startPositionUpdates()
                } else {
                    stopPositionUpdates()
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_ENDED -> onSongEnded()
                    Player.STATE_READY -> {
                        retryCount = 0
                        _playbackError.value = null
                    }
                    Player.STATE_IDLE -> {
                        // Player is idle, may have encountered error
                    }
                }
                updatePlayerState()
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                updateCurrentSong()
                updatePlayerState()
                _playbackError.value = null
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.e(TAG, "Playback error: ${error.message}", error)
                handlePlaybackError(error)
            }

            override fun onAudioAttributesChanged(audioAttributes: AudioAttributes) {
                // Handle audio attribute changes
                Log.d(TAG, "Audio attributes changed")
            }
        })
    }

    private fun handlePlaybackError(error: PlaybackException) {
        val errorMessage = when (error.errorCode) {
            PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND -> "文件未找到"
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED -> "网络连接失败"
            PlaybackException.ERROR_CODE_DECODING_FAILED -> "解码失败"
            PlaybackException.ERROR_CODE_AUDIO_TRACK_INIT_FAILED -> "音频轨道初始化失败"
            PlaybackException.ERROR_CODE_AUDIO_TRACK_WRITE_FAILED -> "音频写入失败"
            PlaybackException.ERROR_CODE_TIMEOUT -> "播放超时"
            PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW -> "直播窗口超时"
            else -> "播放错误: ${error.errorCodeName}"
        }
        
        _playbackError.value = errorMessage
        
        // Retry logic
        if (retryCount < MAX_RETRY_COUNT) {
            retryCount++
            Log.d(TAG, "Retrying playback ($retryCount/$MAX_RETRY_COUNT)")
            scope.launch {
                delay(RETRY_DELAY_MS)
                retryPlayback()
            }
        } else {
            Log.e(TAG, "Max retry count reached, skipping to next song")
            retryCount = 0
            playNext()
        }
    }

    private fun retryPlayback() {
        try {
            player.prepare()
            player.playWhenReady = true
        } catch (e: Exception) {
            Log.e(TAG, "Retry failed", e)
        }
    }

    fun start() {
        Log.d(TAG, "MusicPlayer started")
        // Initialize player lazily when needed
    }

    fun playSong(song: Song, songList: List<Song> = emptyList()) {
        Log.d(TAG, "Playing song: ${song.title}")
        
        val songs = if (songList.isEmpty()) listOf(song) else songList
        _queue.value = songs
        currentIndex = songs.indexOf(song).takeIf { it >= 0 } ?: 0

        _currentSong.value = song
        _playbackError.value = null
        retryCount = 0
        
        playAtIndex(currentIndex)
    }

    private fun playAtIndex(index: Int) {
        val songs = _queue.value
        if (index !in songs.indices) {
            Log.w(TAG, "Invalid index: $index, queue size: ${songs.size}")
            return
        }

        currentIndex = index
        val song = songs[index]
        _currentSong.value = song

        try {
            val mediaItem = MediaItem.Builder()
                .setUri(song.path)
                .setMediaId(song.id.toString())
                .setMediaMetadata(
                    androidx.media3.common.MediaMetadata.Builder()
                        .setTitle(song.title)
                        .setArtist(song.artist)
                        .setAlbumTitle(song.album)
                        .build()
                )
                .build()

            player.setMediaItem(mediaItem)
            player.prepare()
            player.playWhenReady = true

            notificationManager.updateNotification(song, true)
            Log.d(TAG, "Started playing: ${song.title}")
        } catch (e: Exception) {
            Log.e(TAG, "Error playing song: ${song.title}", e)
            _playbackError.value = "无法播放此歌曲"
        }
    }

    fun playPause() {
        try {
            if (player.isPlaying) {
                player.pause()
            } else {
                player.play()
            }
            notificationManager.updateNotification(_currentSong.value, player.isPlaying)
        } catch (e: Exception) {
            Log.e(TAG, "Error in playPause", e)
        }
    }

    fun playNext() {
        val songs = _queue.value
        if (songs.isEmpty()) {
            Log.w(TAG, "Queue is empty, cannot play next")
            return
        }

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
        if (songs.isEmpty()) {
            Log.w(TAG, "Queue is empty, cannot play previous")
            return
        }

        // If played more than 3 seconds, restart current song
        if (player.currentPosition > 3000) {
            player.seekTo(0)
            return
        }

        val prevIndex = if (_isShuffleEnabled.value) {
            (0 until songs.size).random()
        } else {
            if (currentIndex <= 0) songs.size - 1 else currentIndex - 1
        }
        playAtIndex(prevIndex)
    }

    fun seekTo(position: Long) {
        try {
            player.seekTo(position)
            _position.value = position
        } catch (e: Exception) {
            Log.e(TAG, "Error seeking", e)
        }
    }

    fun toggleShuffle() {
        _isShuffleEnabled.value = !_isShuffleEnabled.value
        updatePlayerState()
        Log.d(TAG, "Shuffle: ${_isShuffleEnabled.value}")
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
        Log.d(TAG, "Repeat mode: ${_repeatMode.value}")
    }

    fun addToQueue(song: Song) {
        _queue.value = _queue.value + song
        Log.d(TAG, "Added to queue: ${song.title}")
    }

    fun removeFromQueue(index: Int) {
        val currentQueue = _queue.value.toMutableList()
        if (index in currentQueue.indices) {
            currentQueue.removeAt(index)
            _queue.value = currentQueue
            if (index < currentIndex) {
                currentIndex--
            }
            Log.d(TAG, "Removed from queue at index: $index")
        }
    }

    fun clearQueue() {
        _queue.value = emptyList()
        currentIndex = 0
        Log.d(TAG, "Queue cleared")
    }

    fun moveInQueue(fromIndex: Int, toIndex: Int) {
        val currentQueue = _queue.value.toMutableList()
        if (fromIndex in currentQueue.indices && toIndex in currentQueue.indices) {
            val song = currentQueue.removeAt(fromIndex)
            currentQueue.add(toIndex, song)
            _queue.value = currentQueue
            
            // Update current index if necessary
            when {
                currentIndex == fromIndex -> currentIndex = toIndex
                fromIndex < currentIndex && toIndex >= currentIndex -> currentIndex--
                fromIndex > currentIndex && toIndex <= currentIndex -> currentIndex++
            }
            Log.d(TAG, "Moved in queue: $fromIndex -> $toIndex")
        }
    }

    private fun onSongEnded() {
        Log.d(TAG, "Song ended")
        
        when (_repeatMode.value) {
            RepeatMode.ONE -> {
                player.seekTo(0)
                player.play()
            }
            RepeatMode.ALL -> playNext()
            RepeatMode.OFF -> {
                if (currentIndex < _queue.value.size - 1) {
                    playNext()
                } else {
                    // End of queue
                    _isPlaying.value = false
                    notificationManager.updateNotification(_currentSong.value, false)
                }
            }
        }
    }

    private fun updateCurrentSong() {
        val songs = _queue.value
        if (currentIndex in songs.indices) {
            _currentSong.value = songs[currentIndex]
            
            // Add to listening history
            scope.launch(Dispatchers.IO) {
                songs[currentIndex].let { song ->
                    musicRepository.addToListeningHistory(song)
                }
            }
        }
    }

    private fun updatePlayerState() {
        try {
            _position.value = player.currentPosition
            _duration.value = player.duration.takeIf { it > 0 } ?: 0

            _playerState.value = PlayerState(
                currentSong = _currentSong.value,
                isPlaying = _isPlaying.value,
                position = _position.value,
                duration = _duration.value,
                repeatMode = _repeatMode.value,
                isShuffleEnabled = _isShuffleEnabled.value,
                queue = _queue.value,
                queueIndex = currentIndex
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error updating player state", e)
        }
    }

    private fun startPositionUpdates() {
        positionUpdateJob?.cancel()
        positionUpdateJob = scope.launch {
            while (isActive) {
                updatePlayerState()
                delay(POSITION_UPDATE_INTERVAL)
            }
        }
    }

    private fun stopPositionUpdates() {
        positionUpdateJob?.cancel()
        positionUpdateJob = null
    }

    fun updatePosition() {
        try {
            _position.value = player.currentPosition
            updatePlayerState()
        } catch (e: Exception) {
            Log.e(TAG, "Error updating position", e)
        }
    }

    fun release() {
        Log.d(TAG, "Releasing MusicPlayer")
        stopPositionUpdates()
        scope.cancel()
        
        try {
            player.stop()
            player.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing player", e)
        }
        
        _player = null
    }
}
