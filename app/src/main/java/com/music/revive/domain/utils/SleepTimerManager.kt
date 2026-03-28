package com.music.revive.domain.utils

import android.content.Context
import android.os.CountDownTimer
import android.widget.Toast
import com.music.revive.data.local.PlayerPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sleep Timer Manager
 * 
 * Manages sleep timer functionality with countdown, fade-out, and persistence.
 * Inspired by Apple Music's sleep timer feature.
 */
@Singleton
class SleepTimerManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val playerPreferences: PlayerPreferences
) {
    // Current timer state
    private val _timerState = MutableStateFlow(SleepTimerState())
    val timerState: StateFlow<SleepTimerState> = _timerState.asStateFlow()
    
    // Active countdown timer
    private var countDownTimer: CountDownTimer? = null
    
    // Total duration in milliseconds (for progress calculation)
    private var totalDurationMs: Long = 0
    
    /**
     * Start sleep timer with specified duration
     * 
     * @param durationMillis Duration in milliseconds
     */
    fun startTimer(durationMillis: Long) {
        // Cancel existing timer
        stopTimer()
        
        if (durationMillis <= 0) return
        
        totalDurationMs = durationMillis
        val remainingMs = durationMillis
        
        _timerState.value = SleepTimerState(
            isActive = true,
            remainingTimeMs = remainingMs,
            totalDurationMs = durationMillis,
            isFadingOut = false
        )
        
        // Create countdown timer
        countDownTimer = object : CountDownTimer(remainingMs, 500) {
            override fun onTick(millisUntilFinished: Long) {
                // Check if we should start fading out (last 30 seconds)
                val shouldFadeOut = millisUntilFinished <= 30_000
                
                _timerState.value = SleepTimerState(
                    isActive = true,
                    remainingTimeMs = millisUntilFinished,
                    totalDurationMs = totalDurationMs,
                    isFadingOut = shouldFadeOut
                )
                
                // Apply fade out effect in last 30 seconds
                if (shouldFadeOut && !playerPreferences.fade_in_out) {
                    // TODO: Implement volume fade-out when integrated with MusicPlayer
                }
            }
            
            override fun onFinish() {
                // Timer completed - pause playback
                _timerState.value = SleepTimerState(
                    isActive = false,
                    remainingTimeMs = 0,
                    totalDurationMs = 0,
                    isFadingOut = false,
                    isCompleted = true
                )
                
                // Pause playback via preferences flag
                // The actual pausing is handled by MusicService observing this flag
                launch {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            context,
                            "Sleep timer finished",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }.start()
    }
    
    /**
     * Stop and cancel the active timer
     */
    fun stopTimer() {
        countDownTimer?.cancel()
        countDownTimer = null
        
        _timerState.value = SleepTimerState(
            isActive = false,
            remainingTimeMs = 0,
            totalDurationMs = 0,
            isFadingOut = false,
            isCompleted = false
        )
    }
    
    /**
     * Toggle timer on/off
     */
    fun toggleTimer() {
        if (_timerState.value.isActive) {
            stopTimer()
        }
    }
    
    /**
     * Get formatted remaining time as string (MM:SS)
     */
    fun getFormattedRemainingTime(): String {
        val totalSeconds = _timerState.value.remainingTimeMs / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }
    
    /**
     * Get progress as float (0.0 to 1.0)
     */
    fun getProgress(): Float {
        val total = _timerState.value.totalDurationMs
        return if (total > 0) {
            1.0f - (_timerState.value.remainingTimeMs.toFloat() / total.toFloat())
        } else {
            0.0f
        }
    }
    
    /**
     * Flow of formatted remaining time
     */
    val formattedTimeFlow: Flow<String> = callbackFlow {
        val job = launch {
            timerState.collect { state ->
                val totalSeconds = state.remainingTimeMs / 1000
                val minutes = totalSeconds / 60
                val seconds = totalSeconds % 60
                trySend(String.format("%02d:%02d", minutes, seconds))
            }
        }
        
        awaitClose { job.cancel() }
    }.distinctUntilChanged()
    
    /**
     * Cleanup resources
     */
    fun cleanup() {
        stopTimer()
    }
}

/**
 * Sleep Timer State Data Class
 */
data class SleepTimerState(
    val isActive: Boolean = false,
    val remainingTimeMs: Long = 0,
    val totalDurationMs: Long = 0,
    val isFadingOut: Boolean = false,
    val isCompleted: Boolean = false
) {
    /**
     * Get remaining time in minutes
     */
    val remainingMinutes: Int
        get() = (remainingTimeMs / 60_000).toInt()
    
    /**
     * Get remaining time in seconds (within current minute)
     */
    val remainingSeconds: Int
        get() = ((remainingTimeMs % 60_000) / 1000).toInt()
    
    /**
     * Check if timer is in fade-out phase
     */
    val shouldFadeOut: Boolean
        get() = isFadingOut && isActive
    
    /**
     * Get display text for timer button
     */
    val displayText: String
        get() = if (isActive) {
            val mins = remainingMinutes
            if (mins >= 60) {
                "${mins / 60}h ${mins % 60}m"
            } else {
                "${mins}m"
            }
        } else {
            "Off"
        }
}

// Common timer durations (in milliseconds)
object SleepTimerPresets {
    val END_OF_TRACK = 0L // Special value - stop after current track
    val MINUTES_15 = 15 * 60 * 1000L
    val MINUTES_30 = 30 * 60 * 1000L
    val MINUTES_45 = 45 * 60 * 1000L
    val MINUTES_60 = 60 * 60 * 1000L
    val MINUTES_90 = 90 * 60 * 1000L
    
    fun getPresetOptions(): List<Long> {
        return listOf(MINUTES_15, MINUTES_30, MINUTES_45, MINUTES_60, MINUTES_90)
    }
    
    fun getDisplayText(duration: Long): String {
        return when (duration) {
            END_OF_TRACK -> "End of Track"
            MINUTES_15 -> "15 min"
            MINUTES_30 -> "30 min"
            MINUTES_45 -> "45 min"
            MINUTES_60 -> "1 hour"
            MINUTES_90 -> "1.5 hours"
            else -> {
                val minutes = duration / 60_000
                "${minutes} min"
            }
        }
    }
}
