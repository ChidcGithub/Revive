package com.music.revive.di

import android.content.Context
import androidx.media3.exoplayer.ExoPlayer
import com.music.revive.service.MusicNotificationManager
import com.music.revive.service.MusicPlayer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ServiceModule {

    @Provides
    @Singleton
    fun provideMusicNotificationManager(
        @ApplicationContext context: Context
    ): MusicNotificationManager {
        return MusicNotificationManager(context)
    }

    @Provides
    @Singleton
    fun provideMusicPlayer(
        @ApplicationContext context: Context,
        notificationManager: MusicNotificationManager
    ): MusicPlayer {
        return MusicPlayer(context, notificationManager)
    }
}
