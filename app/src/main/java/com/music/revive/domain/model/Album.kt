package com.music.revive.domain.model

data class Album(
    val id: Long,
    val name: String,
    val artist: String,
    val albumArtUri: String?,
    val numberOfSongs: Int
)
