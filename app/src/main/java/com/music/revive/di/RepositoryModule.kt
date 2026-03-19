package com.music.revive.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    // MusicRepository and MediaStoreDataSource are provided via @Inject constructor
}
