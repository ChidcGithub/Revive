package com.music.revive.di

import android.content.Context
import com.music.revive.service.MusicNotificationManager
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
}
