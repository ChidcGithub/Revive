package com.music.revive.di

import com.music.revive.data.datasource.MediaStoreDataSource
import com.music.revive.data.local.dao.FavoriteDao
import com.music.revive.data.local.dao.PlaylistDao
import com.music.revive.data.local.dao.RecentSongDao
import com.music.revive.data.repository.MusicRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideMusicRepository(
        mediaStoreDataSource: MediaStoreDataSource,
        playlistDao: PlaylistDao,
        favoriteDao: FavoriteDao,
        recentSongDao: RecentSongDao
    ): MusicRepository {
        return MusicRepository(mediaStoreDataSource, playlistDao, favoriteDao, recentSongDao)
    }
}
