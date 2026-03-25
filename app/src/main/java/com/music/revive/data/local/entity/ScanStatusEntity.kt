package com.music.revive.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Scan status entity to track library scan state
 */
@Entity(tableName = "scan_status")
data class ScanStatusEntity(
    @PrimaryKey
    val id: Int = 0,  // Single row, always id = 0
    val lastScanTime: Long = 0,
    val isScanning: Boolean = false,
    val songCount: Int = 0,
    val albumCount: Int = 0,
    val artistCount: Int = 0
)
