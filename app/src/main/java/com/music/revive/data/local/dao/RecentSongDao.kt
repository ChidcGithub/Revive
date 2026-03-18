package com.music.revive.data.local.dao

import androidx.room.*
import com.music.revive.data.local.entity.RecentSongEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecentSongDao {

    @Query("SELECT * FROM recent_songs ORDER BY playedAt DESC LIMIT :limit")
    fun getRecentSongs(limit: Int = 50): Flow<List<RecentSongEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecentSong(recentSong: RecentSongEntity)

    @Query("DELETE FROM recent_songs WHERE songId = :songId")
    suspend fun removeRecentSong(songId: Long)

    @Query("DELETE FROM recent_songs")
    suspend fun clearAllRecentSongs()

    @Transaction
    suspend fun addToRecent(songId: Long, playedAt: Long) {
        removeRecentSong(songId)
        insertRecentSong(RecentSongEntity(songId, playedAt))
    }

    @Query("SELECT COUNT(*) FROM recent_songs")
    fun getRecentSongCount(): Flow<Int>
}
