package com.music.revive.data.repository

import com.music.revive.data.datasource.MediaStoreDataSource
import com.music.revive.data.local.dao.FavoriteDao
import com.music.revive.data.local.dao.PlaylistDao
import com.music.revive.data.local.dao.RecentSongDao
import com.music.revive.data.local.entity.FavoriteEntity
import com.music.revive.data.local.entity.PlaylistEntity
import com.music.revive.data.local.entity.RecentSongEntity
import com.music.revive.domain.model.Album
import com.music.revive.domain.model.Artist
import com.music.revive.domain.model.Folder
import com.music.revive.domain.model.Playlist
import com.music.revive.domain.model.RecentSong
import com.music.revive.domain.model.Song
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicRepository @Inject constructor(
    private val mediaStoreDataSource: MediaStoreDataSource,
    private val playlistDao: PlaylistDao,
    private val favoriteDao: FavoriteDao,
    private val recentSongDao: RecentSongDao
) {
    // Songs
    suspend fun getAllSongs(): List<Song> = mediaStoreDataSource.getAllSongs()

    suspend fun getSongsByAlbum(albumId: Long): List<Song> =
        mediaStoreDataSource.getSongsByAlbum(albumId)

    suspend fun getSongsByArtist(artistId: Long): List<Song> =
        mediaStoreDataSource.getSongsByArtist(artistId)

    suspend fun getSongsByFolder(folderPath: String): List<Song> =
        mediaStoreDataSource.getSongsByFolder(folderPath)

    suspend fun searchSongs(query: String): List<Song> = mediaStoreDataSource.searchSongs(query)

    // Albums
    suspend fun getAlbums(): List<Album> = mediaStoreDataSource.getAlbums()

    // Artists
    suspend fun getArtists(): List<Artist> = mediaStoreDataSource.getArtists()

    // Folders
    suspend fun getFolders(): List<Folder> = mediaStoreDataSource.getFolders()

    // Playlists
    fun getAllPlaylists(): Flow<List<Playlist>> = playlistDao.getAllPlaylistsWithSongCount().map { entities ->
        entities.map { Playlist(it.id, it.name, it.createdAt, it.songCount) }
    }

    suspend fun createPlaylist(name: String): Long {
        return playlistDao.insertPlaylist(PlaylistEntity(name = name, createdAt = System.currentTimeMillis()))
    }

    suspend fun renamePlaylist(playlistId: Long, newName: String) {
        playlistDao.getPlaylistById(playlistId)?.let { playlist ->
            playlistDao.updatePlaylist(playlist.copy(name = newName))
        }
    }

    suspend fun deletePlaylist(playlistId: Long) {
        playlistDao.deletePlaylistById(playlistId)
    }

    fun getPlaylistSongIds(playlistId: Long): Flow<List<Long>> =
        playlistDao.getSongIdsForPlaylist(playlistId)

    suspend fun addSongToPlaylist(playlistId: Long, songId: Long) {
        val count = playlistDao.getSongCountSync(playlistId)
        playlistDao.insertPlaylistSong(
            com.music.revive.data.local.entity.PlaylistSongEntity(
                playlistId = playlistId,
                songId = songId,
                orderInPlaylist = count
            )
        )
    }

    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long) {
        playlistDao.removeSongFromPlaylist(playlistId, songId)
    }

    suspend fun updatePlaylistOrder(playlistId: Long, songIds: List<Long>) {
        playlistDao.updatePlaylistOrder(playlistId, songIds)
    }

    // Favorites
    fun getFavoriteSongIds(): Flow<List<Long>> = favoriteDao.getAllFavoriteSongIds()

    fun getFavoriteCount(): Flow<Int> = favoriteDao.getFavoriteCount()

    fun isFavorite(songId: Long): Flow<Boolean> = favoriteDao.isFavorite(songId)

    suspend fun addToFavorites(songId: Long) {
        favoriteDao.addFavorite(FavoriteEntity(songId, System.currentTimeMillis()))
    }

    suspend fun removeFromFavorites(songId: Long) {
        favoriteDao.removeFavorite(songId)
    }

    // Recent Songs
    fun getRecentSongIds(limit: Int = 50): Flow<List<RecentSongEntity>> =
        recentSongDao.getRecentSongs(limit)

    fun getRecentSongCount(): Flow<Int> = recentSongDao.getRecentSongCount()

    suspend fun addToRecent(songId: Long) {
        recentSongDao.addToRecent(songId, System.currentTimeMillis())
    }

    suspend fun clearRecentSongs() {
        recentSongDao.clearAllRecentSongs()
    }
}
