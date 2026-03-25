package com.music.revive.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Cached album entity for fast loading
 */
@Entity(
    tableName = "album_cache",
    indices = [
        Index(value = ["name"]),
        Index(value = ["artist"])
    ]
)
data class AlbumCacheEntity(
    @PrimaryKey
    val id: Long,
    val name: String,
    val artist: String,
    val albumArtUri: String?,
    val numberOfSongs: Int = 0
)
