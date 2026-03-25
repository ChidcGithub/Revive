package com.music.revive.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Cached artist entity for fast loading
 */
@Entity(
    tableName = "artist_cache",
    indices = [
        Index(value = ["name"])
    ]
)
data class ArtistCacheEntity(
    @PrimaryKey
    val id: Long,
    val name: String,
    val numberOfAlbums: Int = 0,
    val numberOfSongs: Int = 0
)
