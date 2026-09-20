package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FocusLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: FocusLog): Long

    @Query("SELECT * FROM focus_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<FocusLog>>

    @Query("SELECT * FROM focus_logs WHERE timestamp >= :sinceTimestamp ORDER BY timestamp DESC")
    fun getLogsSince(sinceTimestamp: Long): Flow<List<FocusLog>>

    @Query("SELECT SUM(blockedFutureCount) FROM focus_logs")
    fun getTotalBlockedCount(): Flow<Int?>

    @Query("SELECT SUM(durationSeconds) FROM focus_logs")
    fun getTotalSecondsWatched(): Flow<Int?>

    @Query("DELETE FROM focus_logs")
    suspend fun clearAll()
}
