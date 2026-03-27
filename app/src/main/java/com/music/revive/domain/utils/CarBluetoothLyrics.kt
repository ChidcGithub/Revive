package com.music.revive.domain.utils

import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import com.music.revive.domain.model.Lyric
import com.music.revive.domain.model.LyricLine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Car Bluetooth Lyrics Display Utility
 * 
 * Sends synchronized lyrics to car infotainment systems via MediaSession metadata.
 * Uses Android's MediaMetadataRetriever and custom metadata keys to display lyrics
 * on compatible car head units.
 */
class CarBluetoothLyrics(private val context: Context) {
    
    private var mediaSession: MediaSession? = null
    private var isInitialized = false
    
    // Current lyrics to send to car system
    private val _currentLyrics = MutableStateFlow<Lyric>(Lyric.Empty)
    val currentLyrics: StateFlow<Lyric> = _currentLyrics.asStateFlow()
    
    // Whether car mode is enabled
    private val _isCarModeEnabled = MutableStateFlow(false)
    val isCarModeEnabled: StateFlow<Boolean> = _isCarModeEnabled.asStateFlow()
    
    /**
     * Initialize MediaSession for car lyrics display
     */
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun initialize(sessionTag: String = "ReviveCarLyrics") {
        if (isInitialized) return
        
        try {
            mediaSession = MediaSession(context, sessionTag).apply {
                setFlags(MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS)
                isActive = true
                
                // Set initial playback state
                val playbackState = PlaybackState.Builder()
                    .setState(PlaybackState.STATE_PAUSED, 0, 1.0f)
                    .build()
                setPlaybackState(playbackState)
            }
            
            isInitialized = true
        } catch (e: Exception) {
            // MediaSession initialization failed - car lyrics not available
            e.printStackTrace()
        }
    }
    
    /**
     * Update lyrics and send to car system
     */
    fun updateLyrics(lyrics: Lyric) {
        _currentLyrics.value = lyrics
        
        if (_isCarModeEnabled.value && isInitialized) {
            sendLyricsToCar(lyrics)
        }
    }
    
    /**
     * Enable or disable car mode
     */
    fun setCarModeEnabled(enabled: Boolean) {
        _isCarModeEnabled.value = enabled
        
        if (enabled && !isInitialized) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                initialize()
            }
        }
        
        if (!enabled) {
            clearCarLyrics()
        }
    }
    
    /**
     * Send synchronized lyrics to car infotainment system
     */
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun sendLyricsToCar(lyrics: Lyric) {
        if (!lyrics.isSynced || lyrics.lines.isEmpty()) {
            return
        }
        
        try {
            val metadataBuilder = MediaMetadata.Builder()
            
            // Convert lyrics to LRC format string
            val lrcString = buildLrcString(lyrics)
            
            // Set lyrics as metadata
            // Note: Using custom metadata key as there's no standard key for full lyrics
            metadataBuilder.putText(METADATA_KEY_LYRICS, lrcString)
            
            // Also set as display description for compatibility
            metadataBuilder.putText(
                MediaMetadata.METADATA_KEY_DISPLAY_DESCRIPTION,
                lyrics.lines.firstOrNull()?.text ?: ""
            )
            
            // Update the media session metadata
            mediaSession?.setMetadata(metadataBuilder.build())
            
        } catch (e: Exception) {
            // Failed to send lyrics to car - ignore silently
            e.printStackTrace()
        }
    }
    
    /**
     * Build LRC format string from lyrics
     */
    private fun buildLrcString(lyrics: Lyric): String {
        val sb = StringBuilder()
        
        for (line in lyrics.lines) {
            val timestamp = formatTimestamp(line.startTimeMs)
            sb.append("[$timestamp]${line.text}\n")
        }
        
        return sb.toString()
    }
    
    /**
     * Format milliseconds to LRC timestamp format [mm:ss.xx]
     */
    private fun formatTimestamp(timeMs: Long): String {
        val minutes = timeMs / 60000
        val seconds = (timeMs % 60000) / 1000
        val centiseconds = (timeMs % 1000) / 10
        return String.format("%02d:%02d.%02d", minutes, seconds, centiseconds)
    }
    
    /**
     * Update current line for display on car system
     */
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun updateCurrentLine(currentPositionMs: Long) {
        if (!_isCarModeEnabled.value || !isInitialized) return
        
        val lyrics = _currentLyrics.value
        if (!lyrics.isSynced) return
        
        val currentLineIndex = lyrics.findCurrentLineIndex(currentPositionMs)
        if (currentLineIndex >= 0 && currentLineIndex < lyrics.lines.size) {
            val currentLine = lyrics.lines[currentLineIndex]
            
            try {
                val metadataBuilder = MediaMetadata.Builder()
                
                // Update with current line
                metadataBuilder.putText(METADATA_KEY_LYRICS, currentLine.text)
                metadataBuilder.putText(
                    MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE,
                    currentLine.translation ?: ""
                )
                
                mediaSession?.setMetadata(metadataBuilder.build())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Clear lyrics from car display
     */
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun clearCarLyrics() {
        try {
            val metadataBuilder = MediaMetadata.Builder()
            metadataBuilder.putText(METADATA_KEY_LYRICS, "")
            metadataBuilder.putText(MediaMetadata.METADATA_KEY_DISPLAY_DESCRIPTION, "")
            mediaSession?.setMetadata(metadataBuilder.build())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * Release resources
     */
    fun release() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mediaSession?.release()
        }
        mediaSession = null
        isInitialized = false
    }
    
    companion object {
        /**
         * Custom metadata key for full lyrics
         * Some car systems may support this proprietary key
         */
        const val METADATA_KEY_LYRICS = "android.metadata.LYRICS"
    }
}
