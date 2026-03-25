package com.music.revive.data.local.dao

import androidx.room.*
import com.music.revive.data.local.entity.SongCacheEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SongCacheDao {
    
    @Query("SELECT * FROM song_cache ORDER BY title ASC")
    fun getAllSongs(): Flow<List<SongCacheEntity>>
    
    @Query("SELECT * FROM song_cache WHERE albumId = :albumId ORDER BY title ASC")
    fun getSongsByAlbum(albumId: Long): Flow<List<SongCacheEntity>>
    
    @Query("SELECT * FROM song_cache WHERE artistId = :artistId ORDER BY album ASC, title ASC")
    fun getSongsByArtist(artistId: Long): Flow<List<SongCacheEntity>>
    
    @Query("SELECT * FROM song_cache WHERE path LIKE :folderPath || '%' ORDER BY title ASC")
    fun getSongsByFolder(folderPath: String): Flow<List<SongCacheEntity>>
    
    @Query("SELECT * FROM song_cache WHERE title LIKE '%' || :query || '%' OR artist LIKE '%' || :query || '%' OR album LIKE '%' || :query || '%' ORDER BY title ASC")
    fun searchSongs(query: String): Flow<List<SongCacheEntity>>
    
    @Query("SELECT * FROM song_cache WHERE id = :id")
    suspend fun getSongById(id: Long): SongCacheEntity?
    
    @Query("SELECT COUNT(*) FROM song_cache")
    suspend fun getSongCount(): Int
    
    @Query("SELECT COUNT(*) FROM song_cache")
    fun getSongCountFlow(): Flow<Int>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSongs(songs: List<SongCacheEntity>)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSong(song: SongCacheEntity)
    
    @Update
    suspend fun updateSong(song: SongCacheEntity)
    
    @Delete
    suspend fun deleteSong(song: SongCacheEntity)
    
    @Query("DELETE FROM song_cache WHERE path = :path")
    suspend fun deleteSongByPath(path: String)
    
    @Query("DELETE FROM song_cache")
    suspend fun deleteAll()
    
    @Query("DELETE FROM song_cache WHERE id NOT IN (:ids)")
    suspend fun deleteSongsNotIn(ids: List<Long>)
    
    @Transaction
    suspend fun replaceAll(songs: List<SongCacheEntity>) {
        deleteAll()
        insertSongs(songs)
    }
}
