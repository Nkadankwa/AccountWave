package com.example.accountwave.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "inventory")
data class InventoryItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val description: String,
    val quantity: Int,
    val cost: Double,
    val category: String,
    val tabId: Int,
    val notes: String? = null
)