package com.music.revive.data.local.dao

import androidx.room.*
import com.music.revive.data.local.entity.ArtistCacheEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ArtistCacheDao {
    
    @Query("SELECT * FROM artist_cache ORDER BY name ASC")
    fun getAllArtists(): Flow<List<ArtistCacheEntity>>
    
    @Query("SELECT * FROM artist_cache WHERE id = :id")
    suspend fun getArtistById(id: Long): ArtistCacheEntity?
    
    @Query("SELECT COUNT(*) FROM artist_cache")
    suspend fun getArtistCount(): Int
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArtists(artists: List<ArtistCacheEntity>)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArtist(artist: ArtistCacheEntity)
    
    @Query("DELETE FROM artist_cache")
    suspend fun deleteAll()
}
