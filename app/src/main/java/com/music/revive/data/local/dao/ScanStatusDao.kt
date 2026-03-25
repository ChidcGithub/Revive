package com.music.revive.data.local.dao

import androidx.room.*
import com.music.revive.data.local.entity.ScanStatusEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanStatusDao {
    
    @Query("SELECT * FROM scan_status WHERE id = 0")
    suspend fun getScanStatus(): ScanStatusEntity?
    
    @Query("SELECT * FROM scan_status WHERE id = 0")
    fun getScanStatusFlow(): Flow<ScanStatusEntity?>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateScanStatus(status: ScanStatusEntity)
    
    @Query("UPDATE scan_status SET isScanning = :isScanning WHERE id = 0")
    suspend fun setScanning(isScanning: Boolean)
    
    @Query("DELETE FROM scan_status")
    suspend fun deleteAll()
}
