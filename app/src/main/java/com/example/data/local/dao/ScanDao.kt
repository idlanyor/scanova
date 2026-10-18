package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.ScanEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanDao {

    @Query("SELECT * FROM scan_history ORDER BY timestamp DESC")
    fun observeAllScans(): Flow<List<ScanEntity>>

    @Query("SELECT * FROM scan_history ORDER BY timestamp DESC")
    suspend fun getAllScansSnapshot(): List<ScanEntity>

    @Query("SELECT * FROM scan_history WHERE id = :id LIMIT 1")
    fun observeScanById(id: Long): Flow<ScanEntity?>

    @Query("SELECT * FROM scan_history WHERE id = :id LIMIT 1")
    suspend fun getScanById(id: Long): ScanEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScan(scan: ScanEntity): Long

    @Query("UPDATE scan_history SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: Long, isFavorite: Boolean)

    @Query("DELETE FROM scan_history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM scan_history WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("DELETE FROM scan_history")
    suspend fun clearAll()
}
