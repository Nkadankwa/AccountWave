package com.example.accountwave.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "logs")
data class LogEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Date,
    val entityName: String,
    val entityId: Int,
    val operationType: String,
    val details: String? = null
)