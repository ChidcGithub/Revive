package com.music.revive.data.repository

import com.music.revive.data.datasource.MediaStoreDataSource
import com.music.revive.data.local.FolderPreferences
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicRepository @Inject constructor(
    private val mediaStoreDataSource: MediaStoreDataSource,
    private val playlistDao: PlaylistDao,
    private val favoriteDao: FavoriteDao,
    private val recentSongDao: RecentSongDao,
    private val folderPreferences: FolderPreferences
) {
    // Songs
    suspend fun getAllSongs(): List<Song> {
        val allSongs = mediaStoreDataSource.getAllSongs()
        val excludedFolders = folderPreferences.excludedFolders.first()
        return filterExcludedFolders(allSongs, excludedFolders)
    }

    suspend fun getSongsByAlbum(albumId: Long): List<Song> {
        val songs = mediaStoreDataSource.getSongsByAlbum(albumId)
        val excludedFolders = folderPreferences.excludedFolders.first()
        return filterExcludedFolders(songs, excludedFolders)
    }

    suspend fun getSongsByArtist(artistId: Long): List<Song> {
        val songs = mediaStoreDataSource.getSongsByArtist(artistId)
        val excludedFolders = folderPreferences.excludedFolders.first()
        return filterExcludedFolders(songs, excludedFolders)
    }

    suspend fun getSongsByFolder(folderPath: String): List<Song> =
        mediaStoreDataSource.getSongsByFolder(folderPath)

    suspend fun searchSongs(query: String): List<Song> {
        val songs = mediaStoreDataSource.searchSongs(query)
        val excludedFolders = folderPreferences.excludedFolders.first()
        return filterExcludedFolders(songs, excludedFolders)
    }

    /**
     * Filter out songs that are in excluded folders
     */
    private fun filterExcludedFolders(songs: List<Song>, excludedFolders: Set<String>): List<Song> {
        if (excludedFolders.isEmpty()) return songs
        return songs.filter { song ->
            excludedFolders.none { excludedPath ->
                song.path.startsWith(excludedPath)
            }
        }
    }

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

    // Folder Preferences
    fun getExcludedFolders(): Flow<Set<String>> = folderPreferences.excludedFolders

    suspend fun excludeFolder(folderPath: String) {
        folderPreferences.excludeFolder(folderPath)
    }

    suspend fun includeFolder(folderPath: String) {
        folderPreferences.includeFolder(folderPath)
    }

    suspend fun setExcludedFolders(folders: Set<String>) {
        folderPreferences.setExcludedFolders(folders)
    }
}
