package com.music.revive.domain.model

data class Song(
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
    val isFavorite: Boolean = false
) {
    val formattedDuration: String
        get() {
            val totalSeconds = duration / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return String.format("%02d:%02d", minutes, seconds)
        }
}
