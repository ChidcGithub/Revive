package com.music.revive.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.music.revive.data.local.dao.ArtistCacheDao
import com.music.revive.data.local.dao.FavoriteDao
import com.music.revive.data.local.dao.PlaylistDao
import com.music.revive.data.local.dao.RecentSongDao
import com.music.revive.data.local.dao.ScanStatusDao
import com.music.revive.data.local.dao.SongCacheDao
import com.music.revive.data.local.entity.ArtistCacheEntity
import com.music.revive.data.local.entity.FavoriteEntity
import com.music.revive.data.local.entity.PlaylistEntity
import com.music.revive.data.local.entity.PlaylistSongEntity
import com.music.revive.data.local.entity.RecentSongEntity
import com.music.revive.data.local.entity.ScanStatusEntity
import com.music.revive.data.local.entity.SongCacheEntity

@Database(
    entities = [
        PlaylistEntity::class,
        PlaylistSongEntity::class,
        FavoriteEntity::class,
        RecentSongEntity::class,
        SongCacheEntity::class,
        ArtistCacheEntity::class,
        ScanStatusEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class MusicDatabase : RoomDatabase() {
    abstract fun playlistDao(): PlaylistDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun recentSongDao(): RecentSongDao
    abstract fun songCacheDao(): SongCacheDao
    abstract fun artistCacheDao(): ArtistCacheDao
    abstract fun scanStatusDao(): ScanStatusDao
}