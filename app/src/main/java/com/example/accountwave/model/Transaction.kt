package com.example.accountwave.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: Date,
    val amount: Double,
    val category: String,
    val title: String,
    val type: String,
    val tabName: String
)