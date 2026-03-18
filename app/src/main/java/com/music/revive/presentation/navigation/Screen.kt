package com.music.revive.presentation.navigation

import kotlinx.serialization.Serializable

sealed class Screen {
    @Serializable
    object Home : Screen()

    @Serializable
    object Playlists : Screen()

    @Serializable
    object Settings : Screen()

    @Serializable
    object Search : Screen()

    @Serializable
    object Player : Screen()

    @Serializable
    object Queue : Screen()

    @Serializable
    data class AlbumDetail(val albumId: Long) : Screen()

    @Serializable
    data class ArtistDetail(val artistId: Long) : Screen()

    @Serializable
    data class FolderDetail(val folderPath: String) : Screen()

    @Serializable
    data class PlaylistDetail(val playlistId: Long) : Screen()

    @Serializable
    object Favorites : Screen()

    @Serializable
    object Recent : Screen()
}
