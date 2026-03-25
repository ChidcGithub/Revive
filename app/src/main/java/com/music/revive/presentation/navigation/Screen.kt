package com.music.revive.presentation.navigation

import kotlinx.serialization.Serializable

@Serializable
object Home

@Serializable
object Songs

@Serializable
object Playlists

@Serializable
object Settings

@Serializable
object Search

@Serializable
object Player

@Serializable
object Queue

@Serializable
data class AlbumDetail(val albumId: Long)

@Serializable
data class ArtistDetail(val artistId: Long)

@Serializable
data class FolderDetail(val folderPath: String)

@Serializable
data class PlaylistDetail(val playlistId: Long)

@Serializable
object Favorites

@Serializable
object Recent

@Serializable
data class SongDetail(val songId: Long)