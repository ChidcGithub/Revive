package com.music.revive.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.music.revive.data.local.dao.FavoriteDao
import com.music.revive.data.local.dao.PlaylistDao
import com.music.revive.data.local.dao.RecentSongDao
import com.music.revive.data.local.entity.FavoriteEntity
import com.music.revive.data.local.entity.PlaylistEntity
import com.music.revive.data.local.entity.PlaylistSongEntity
import com.music.revive.data.local.entity.RecentSongEntity

@Database(
    entities = [
        PlaylistEntity::class,
        PlaylistSongEntity::class,
        FavoriteEntity::class,
        RecentSongEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class MusicDatabase : RoomDatabase() {
    abstract fun playlistDao(): PlaylistDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun recentSongDao(): RecentSongDao
}
