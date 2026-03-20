package com.music.revive.data.local.dao

import androidx.room.*
import com.music.revive.data.local.entity.PlaylistEntity
import com.music.revive.data.local.entity.PlaylistSongEntity
import kotlinx.coroutines.flow.Flow

// Data class for playlist with song count
data class PlaylistWithSongCount(
    val id: Long = 0,
    val name: String,
    val createdAt: Long,
    val songCount: Int = 0
)

@Dao
interface PlaylistDao {

    // Playlist operations
    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun getAllPlaylists(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists WHERE id = :playlistId")
    suspend fun getPlaylistById(playlistId: Long): PlaylistEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Update
    suspend fun updatePlaylist(playlist: PlaylistEntity)

    @Delete
    suspend fun deletePlaylist(playlist: PlaylistEntity)

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylistById(playlistId: Long)

    // Playlist Song operations
    @Query("SELECT songId FROM playlist_songs WHERE playlistId = :playlistId ORDER BY orderInPlaylist")
    fun getSongIdsForPlaylist(playlistId: Long): Flow<List<Long>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistSong(playlistSong: PlaylistSongEntity)

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long)

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId")
    suspend fun clearPlaylist(playlistId: Long)

    @Query("SELECT COUNT(*) FROM playlist_songs WHERE playlistId = :playlistId")
    fun getSongCount(playlistId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM playlist_songs WHERE playlistId = :playlistId")
    suspend fun getSongCountSync(playlistId: Long): Int

    // Get playlist with song count
    @Query("""
        SELECT p.*, COUNT(ps.songId) as songCount 
        FROM playlists p 
        LEFT JOIN playlist_songs ps ON p.id = ps.playlistId 
        GROUP BY p.id 
        ORDER BY p.createdAt DESC
    """)
    fun getAllPlaylistsWithSongCount(): Flow<List<PlaylistWithSongCount>>

    @Transaction
    suspend fun updatePlaylistOrder(playlistId: Long, songIds: List<Long>) {
        clearPlaylist(playlistId)
        songIds.forEachIndexed { index, songId ->
            insertPlaylistSong(
                PlaylistSongEntity(
                    playlistId = playlistId,
                    songId = songId,
                    orderInPlaylist = index
                )
            )
        }
    }
}
