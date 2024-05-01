package com.example.accountwave.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.accountwave.model.LogEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface LogEntryDao {
    @Insert
    suspend fun insert(logEntry: LogEntry)

    @Query("SELECT * FROM logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<LogEntry>>
}