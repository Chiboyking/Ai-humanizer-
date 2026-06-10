package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Query("SELECT * FROM humanizer_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<HistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(item: HistoryEntity): Long

    @Delete
    suspend fun deleteHistory(item: HistoryEntity)

    @Query("DELETE FROM humanizer_history WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("DELETE FROM humanizer_history")
    suspend fun clearAllHistory()
}
