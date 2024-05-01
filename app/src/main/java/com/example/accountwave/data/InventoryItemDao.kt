package com.example.accountwave.data

import android.content.Context
import androidx.room.*
import com.example.accountwave.data.LogEntryDao
import com.example.accountwave.model.InventoryItem
import com.example.accountwave.model.LogEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface InventoryItemDao {
    @Query("SELECT * FROM inventory")
    fun getAll(): Flow<List<InventoryItem>>

    @Query("SELECT * FROM inventory")
    suspend fun getAllOnce(): List<InventoryItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: InventoryItem)

    @Update
    suspend fun update(item: InventoryItem)

    @Delete
    fun delete(item: InventoryItem)

    @Query("SELECT * FROM inventory WHERE name LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%'")
    fun searchInventory(query: String): Flow<List<InventoryItem>>

    @Query("SELECT * FROM inventory WHERE category = :category")
    suspend fun getInventoryByCategory(category: String): List<InventoryItem>

    @Query("SELECT * FROM inventory WHERE name = :name")
    suspend fun getInventoryByName(name: String): InventoryItem?
}

class InventoryItemDaoImpl(
    private val context: Context,
    private val logEntryDao: LogEntryDao,
    private val inventoryItemDao: InventoryItemDao
) {
    suspend fun insert(item: InventoryItem) {
        kotlinx.coroutines.withContext(Dispatchers.IO) {
        val logEntry = LogEntry(
            timestamp = Date(),
            entityName = "InventoryItem",
            entityId = item.id,
            operationType = "INSERT",
            details = "Item name: ${item.name}, quantity: ${item.quantity}"
        )
        logEntryDao.insert(logEntry)
        inventoryItemDao.insert(item)
        }
    }

    suspend fun delete(item: InventoryItem) {
        kotlinx.coroutines.withContext(Dispatchers.IO) {
        val logEntry = LogEntry(
            timestamp = Date(),
            entityName = "InventoryItem",
            entityId = item.id,
            operationType = "DELETE",
            details = "Item name: ${item.name}, quantity: ${item.quantity}"
        )
        logEntryDao.insert(logEntry)
        inventoryItemDao.delete(item)
    }}
}