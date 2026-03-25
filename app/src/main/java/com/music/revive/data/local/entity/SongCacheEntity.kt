package com.music.revive.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Cached song entity for fast loading
 * Stores pre-extracted song metadata from MediaStore
 */
@Entity(
    tableName = "song_cache",
    indices = [
        Index(value = ["title"]),
        Index(value = ["artist"]),
        Index(value = ["album"]),
        Index(value = ["albumId"]),
        Index(value = ["artistId"]),
        Index(value = ["path"])
    ]
)
data class SongCacheEntity(
    @PrimaryKey
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long?,
    val artistId: Long?,
    val duration: Long,
    val path: String,
    val dateAdded: Long,
    val albumArtUri: String?,
    val bitrate: Int = 0,
    val sampleRate: Int = 0,
    val fileSize: Long = 0
)
