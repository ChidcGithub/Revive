package com.music.revive.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.support.v4.media.session.MediaSessionCompat
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle
import com.music.revive.MainActivity
import com.music.revive.R
import com.music.revive.domain.model.Song
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val CHANNEL_ID = "music_playback_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_PLAY = "com.music.revive.action.PLAY"
        const val ACTION_PAUSE = "com.music.revive.action.PAUSE"
        const val ACTION_NEXT = "com.music.revive.action.NEXT"
        const val ACTION_PREVIOUS = "com.music.revive.action.PREVIOUS"
        const val ACTION_STOP = "com.music.revive.action.STOP"
    }

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.notification_channel_description)
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun createNotification(song: Song?, isPlaying: Boolean): Notification {
        val mediaStyle = MediaStyle()

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val contentIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(song?.title ?: context.getString(R.string.unknown_title))
            .setContentText(song?.artist ?: context.getString(R.string.unknown_artist))
            .setSubText(song?.album)
            .setSmallIcon(R.drawable.ic_music_note)
            .setContentIntent(contentIntent)
            .setDeleteIntent(createStopIntent())
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(isPlaying)
            .setStyle(
                mediaStyle
                    .setShowActionsInCompactView(0, 1, 2)
            )
            .addAction(createPreviousAction())
            .addAction(if (isPlaying) createPauseAction() else createPlayAction())
            .addAction(createNextAction())
            .build()
    }

    fun updateNotification(song: Song?, isPlaying: Boolean) {
        val notification = createNotification(song, isPlaying)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    fun hideNotification() {
        notificationManager.cancel(NOTIFICATION_ID)
    }

    private fun createPlayAction(): NotificationCompat.Action {
        return NotificationCompat.Action.Builder(
            R.drawable.ic_play,
            context.getString(R.string.play),
            createPlayIntent()
        ).build()
    }

    private fun createPauseAction(): NotificationCompat.Action {
        return NotificationCompat.Action.Builder(
            R.drawable.ic_pause,
            context.getString(R.string.pause),
            createPauseIntent()
        ).build()
    }

    private fun createNextAction(): NotificationCompat.Action {
        return NotificationCompat.Action.Builder(
            R.drawable.ic_skip_next,
            context.getString(R.string.next),
            createNextIntent()
        ).build()
    }

    private fun createPreviousAction(): NotificationCompat.Action {
        return NotificationCompat.Action.Builder(
            R.drawable.ic_skip_previous,
            context.getString(R.string.previous),
            createPreviousIntent()
        ).build()
    }

    private fun createPlayIntent(): PendingIntent {
        val intent = Intent(ACTION_PLAY).setPackage(context.packageName)
        return PendingIntent.getBroadcast(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createPauseIntent(): PendingIntent {
        val intent = Intent(ACTION_PAUSE).setPackage(context.packageName)
        return PendingIntent.getBroadcast(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createNextIntent(): PendingIntent {
        val intent = Intent(ACTION_NEXT).setPackage(context.packageName)
        return PendingIntent.getBroadcast(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createPreviousIntent(): PendingIntent {
        val intent = Intent(ACTION_PREVIOUS).setPackage(context.packageName)
        return PendingIntent.getBroadcast(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createStopIntent(): PendingIntent {
        val intent = Intent(ACTION_STOP).setPackage(context.packageName)
        return PendingIntent.getBroadcast(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
