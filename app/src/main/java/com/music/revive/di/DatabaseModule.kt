package com.music.revive.di

import android.content.Context
import androidx.room.Room
import com.music.revive.data.local.MusicDatabase
import com.music.revive.data.local.dao.ArtistCacheDao
import com.music.revive.data.local.dao.FavoriteDao
import com.music.revive.data.local.dao.PlaylistDao
import com.music.revive.data.local.dao.RecentSongDao
import com.music.revive.data.local.dao.ScanStatusDao
import com.music.revive.data.local.dao.SongCacheDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideMusicDatabase(
        @ApplicationContext context: Context
    ): MusicDatabase {
        return Room.databaseBuilder(
            context,
            MusicDatabase::class.java,
            "music_database"
        ).build()
    }

    @Provides
    fun providePlaylistDao(database: MusicDatabase): PlaylistDao {
        return database.playlistDao()
    }

    @Provides
    fun provideFavoriteDao(database: MusicDatabase): FavoriteDao {
        return database.favoriteDao()
    }

    @Provides
    fun provideRecentSongDao(database: MusicDatabase): RecentSongDao {
        return database.recentSongDao()
    }

    @Provides
    fun provideSongCacheDao(database: MusicDatabase): SongCacheDao {
        return database.songCacheDao()
    }

    @Provides
    fun provideArtistCacheDao(database: MusicDatabase): ArtistCacheDao {
        return database.artistCacheDao()
    }

    @Provides
    fun provideScanStatusDao(database: MusicDatabase): ScanStatusDao {
        return database.scanStatusDao()
    }
}