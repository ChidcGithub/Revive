package com.music.revive.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.music.revive.data.datasource.MediaStoreDataSource
import com.music.revive.data.local.FolderPreferences
import com.music.revive.data.local.dao.FavoriteDao
import com.music.revive.data.local.dao.PlaylistDao
import com.music.revive.data.local.dao.RecentSongDao
import com.music.revive.data.local.dao.SongCacheDao
import com.music.revive.data.local.dao.ArtistCacheDao
import com.music.revive.data.local.HistoryPreferences
import com.music.revive.data.local.entity.ArtistCacheEntity
import com.music.revive.data.local.entity.FavoriteEntity
import com.music.revive.data.local.entity.PlaylistEntity
import com.music.revive.data.local.entity.RecentSongEntity
import com.music.revive.data.local.entity.SongCacheEntity
import com.music.revive.domain.model.Album
import com.music.revive.domain.model.Artist
import com.music.revive.domain.model.Folder
import com.music.revive.domain.model.Playlist
import com.music.revive.domain.model.Song
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mediaStoreDataSource: MediaStoreDataSource,
    private val playlistDao: PlaylistDao,
    private val favoriteDao: FavoriteDao,
    private val recentSongDao: RecentSongDao,
    private val folderPreferences: FolderPreferences,
    private val songCacheDao: SongCacheDao,
    private val artistCacheDao: ArtistCacheDao,
    private val historyPreferences: HistoryPreferences
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("music_repo", Context.MODE_PRIVATE)
    
    companion object {
        private const val KEY_LAST_SCAN = "last_scan_time"
        private const val KEY_CACHE_VALID = "cache_valid"
        private const val SCAN_CACHE_DURATION = 24 * 60 * 60 * 1000L // 24 hours
    }
    
    // ==================== Cache Management ====================
    
    /**
     * Check if cache is valid (has data and not too old)
     */
    suspend fun isCacheValid(): Boolean {
        val lastScan = prefs.getLong(KEY_LAST_SCAN, 0)
        val cacheValid = prefs.getBoolean(KEY_CACHE_VALID, false)
        
        if (!cacheValid) return false
        if (lastScan == 0L) return false
        
        val songCount = songCacheDao.getSongCount()
        if (songCount == 0) return false
        
        return System.currentTimeMillis() - lastScan < SCAN_CACHE_DURATION
    }
    
    /**
     * Scan music library from MediaStore and update cache
     */
    suspend fun scanMusicLibrary(): ScanResult {
        val startTime = System.currentTimeMillis()
        
        // Scan songs from MediaStore
        val songs = mediaStoreDataSource.getAllSongs()
        val excludedFolders = folderPreferences.excludedFolders.first()
        val filteredSongs = filterExcludedFolders(songs, excludedFolders)
        
        // Convert to cache entities
        val songEntities = filteredSongs.map { song ->
            SongCacheEntity(
                id = song.id,
                title = song.title,
                artist = song.artist,
                album = song.album,
                albumId = song.albumId,
                artistId = song.artistId,
                duration = song.duration,
                path = song.path,
                dateAdded = song.dateAdded,
                albumArtUri = song.albumArtUri,
                bitrate = song.bitrate,
                sampleRate = song.sampleRate,
                fileSize = song.fileSize
            )
        }
        
        // Clear and insert new cache
        songCacheDao.deleteAll()
        songCacheDao.insertSongs(songEntities)
        
        // Scan artists
        val artists = mediaStoreDataSource.getArtists()
        val artistEntities = artists.map { artist ->
            ArtistCacheEntity(
                id = artist.id,
                name = artist.name,
                numberOfAlbums = artist.numberOfAlbums,
                numberOfSongs = artist.numberOfSongs
            )
        }
        artistCacheDao.deleteAll()
        artistCacheDao.insertArtists(artistEntities)
        
        // Update scan time
        prefs.edit()
            .putLong(KEY_LAST_SCAN, System.currentTimeMillis())
            .putBoolean(KEY_CACHE_VALID, true)
            .apply()
        
        return ScanResult(
            songCount = filteredSongs.size,
            albumCount = artists.sumOf { it.numberOfAlbums },
            artistCount = artists.size,
            duration = System.currentTimeMillis() - startTime
        )
    }
    
    data class ScanResult(
        val songCount: Int,
        val albumCount: Int,
        val artistCount: Int,
        val duration: Long
    )
    
    // ==================== Songs (with cache) ====================
    
    /**
     * Get all songs from cache (fast)
     */
    fun getAllSongsFlow(): Flow<List<Song>> = songCacheDao.getAllSongs()
        .map { entities -> entities.map { it.toSong() } }
    
    /**
     * Get all songs from cache synchronously
     */
    suspend fun getAllSongs(): List<Song> {
        // Check if cache is valid
        if (!isCacheValid()) {
            // Perform scan if cache invalid
            scanMusicLibrary()
        }
        
        val entities = songCacheDao.getAllSongs().first()
        return entities.map { it.toSong() }
    }
    
    /**
     * Get songs by album from cache
     */
    fun getSongsByAlbumFlow(albumId: Long): Flow<List<Song>> = songCacheDao.getSongsByAlbum(albumId)
        .map { entities -> entities.map { it.toSong() } }
    
    /**
     * Get songs by artist from cache
     */
    fun getSongsByArtistFlow(artistId: Long): Flow<List<Song>> = songCacheDao.getSongsByArtist(artistId)
        .map { entities -> entities.map { it.toSong() } }
    
    /**
     * Get songs by folder from cache
     */
    fun getSongsByFolderFlow(folderPath: String): Flow<List<Song>> = songCacheDao.getSongsByFolder(folderPath)
        .map { entities -> entities.map { it.toSong() } }
    
    /**
     * Search songs from cache
     */
    fun searchSongsFlow(query: String): Flow<List<Song>> = songCacheDao.searchSongs(query)
        .map { entities -> entities.map { it.toSong() } }
    
    // Legacy methods for backward compatibility
    suspend fun getSongsByAlbum(albumId: Long): List<Song> {
        val entities = songCacheDao.getSongsByAlbum(albumId).first()
        return entities.map { it.toSong() }
    }
    
    suspend fun getSongsByArtist(artistId: Long): List<Song> {
        val entities = songCacheDao.getSongsByArtist(artistId).first()
        return entities.map { it.toSong() }
    }
    
    suspend fun getSongsByFolder(folderPath: String): List<Song> {
        val entities = songCacheDao.getSongsByFolder(folderPath).first()
        return entities.map { it.toSong() }
    }
    
    suspend fun searchSongs(query: String): List<Song> {
        val entities = songCacheDao.searchSongs(query).first()
        return entities.map { it.toSong() }
    }
    
    // ==================== Albums ====================
    
    suspend fun getAlbums(): List<Album> = mediaStoreDataSource.getAlbums()

    // ==================== Artists ====================
    
    fun getArtistsFlow(): Flow<List<Artist>> = artistCacheDao.getAllArtists()
        .map { entities -> entities.map { Artist(it.id, it.name, it.numberOfAlbums, it.numberOfSongs) } }
    
    suspend fun getArtists(): List<Artist> {
        if (!isCacheValid()) {
            scanMusicLibrary()
        }
        val entities = artistCacheDao.getAllArtists().first()
        return entities.map { Artist(it.id, it.name, it.numberOfAlbums, it.numberOfSongs) }
    }

    // ==================== Folders ====================
    
    suspend fun getFolders(): List<Folder> = mediaStoreDataSource.getFolders()

    // ==================== Playlists ====================
    
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

    // ==================== Favorites ====================
    
    fun getFavoriteSongIds(): Flow<List<Long>> = favoriteDao.getAllFavoriteSongIds()

    fun getFavoriteCount(): Flow<Int> = favoriteDao.getFavoriteCount()

    fun isFavorite(songId: Long): Flow<Boolean> = favoriteDao.isFavorite(songId)

    suspend fun addToFavorites(songId: Long) {
        favoriteDao.addFavorite(FavoriteEntity(songId, System.currentTimeMillis()))
    }

    suspend fun removeFromFavorites(songId: Long) {
        favoriteDao.removeFavorite(songId)
    }

    // ==================== Recent Songs ====================
    
    fun getRecentSongIds(limit: Int = 50): Flow<List<RecentSongEntity>> =
        recentSongDao.getRecentSongs(limit)

    fun getRecentSongCount(): Flow<Int> = recentSongDao.getRecentSongCount()

    suspend fun addToRecent(songId: Long) {
        recentSongDao.addToRecent(songId, System.currentTimeMillis())
    }

    suspend fun clearRecentSongs() {
        recentSongDao.clearAllRecentSongs()
    }

    // ==================== Folder Preferences ====================
    
    fun getExcludedFolders(): Flow<Set<String>> = folderPreferences.excludedFolders

    suspend fun excludeFolder(folderPath: String) {
        folderPreferences.excludeFolder(folderPath)
        // Invalidate cache when folders change
        invalidateCache()
    }

    suspend fun includeFolder(folderPath: String) {
        folderPreferences.includeFolder(folderPath)
        // Invalidate cache when folders change
        invalidateCache()
    }

    suspend fun setExcludedFolders(folders: Set<String>) {
        folderPreferences.setExcludedFolders(folders)
        invalidateCache()
    }
    
    /**
     * Invalidate cache to force rescan
     */
    fun invalidateCache() {
        prefs.edit()
            .putBoolean(KEY_CACHE_VALID, false)
            .apply()
    }
    
    /**
     * Get cache info
     */
    suspend fun getCacheInfo(): CacheInfo {
        val lastScan = prefs.getLong(KEY_LAST_SCAN, 0)
        val songCount = songCacheDao.getSongCount()
        val artistCount = artistCacheDao.getArtistCount()
        
        return CacheInfo(
            lastScanTime = lastScan,
            songCount = songCount,
            artistCount = artistCount,
            isValid = isCacheValid()
        )
    }
    
    data class CacheInfo(
        val lastScanTime: Long,
        val songCount: Int,
        val artistCount: Int,
        val isValid: Boolean
    )
    
    // ==================== Listening History ====================
    
    /**
     * Add song to listening history
     */
    suspend fun addToListeningHistory(song: Song) {
        historyPreferences.addToHistory(
            songId = song.id,
            title = song.title,
            artist = song.artist,
            album = song.album,
            albumArtUri = song.albumArtUri
        )
    }
    
    /**
     * Update play count for song in history
     */
    suspend fun updateHistoryPlayCount(songId: Long) {
        historyPreferences.updatePlayCount(songId)
    }
    
    /**
     * Get listening history flow
     */
    fun getListeningHistory(): Flow<List<com.music.revive.data.local.HistoryItem>> {
        return historyPreferences.historyItems
    }
    
    /**
     * Clear all listening history
     */
    suspend fun clearListeningHistory() {
        historyPreferences.clearAllHistory()
    }
    
    /**
     * Remove song from history
     */
    suspend fun removeFromListeningHistory(songId: Long) {
        historyPreferences.removeFromHistory(songId)
    }
    
    // ==================== Private Helpers ====================
    
    private fun filterExcludedFolders(songs: List<Song>, excludedFolders: Set<String>): List<Song> {
        if (excludedFolders.isEmpty()) return songs
        return songs.filter { song ->
            excludedFolders.none { excludedPath ->
                song.path.startsWith(excludedPath)
            }
        }
    }
}

// Extension function to convert SongCacheEntity to Song
fun SongCacheEntity.toSong(): Song {
    return Song(
        id = id,
        title = title,
        artist = artist,
        album = album,
        albumId = albumId,
        artistId = artistId,
        duration = duration,
        path = path,
        dateAdded = dateAdded,
        albumArtUri = albumArtUri,
        bitrate = bitrate,
        sampleRate = sampleRate,
        fileSize = fileSize
    )
}
