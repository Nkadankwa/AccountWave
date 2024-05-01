package com.example.accountwave.data

import androidx.room.*
import com.example.accountwave.model.Tab
import kotlinx.coroutines.flow.Flow

@Dao
interface TabDao {
    @Query("SELECT * FROM tabs")
    fun getAll(): Flow<List<Tab>>

    @Query("SELECT * FROM tabs")
    suspend fun getAllOnce(): List<Tab>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(tab: Tab)

    @Delete
    suspend fun delete(tab: Tab)

    @Query("SELECT * FROM tabs WHERE id = :id LIMIT 1")
    suspend fun getTabById(id: Int): Tab?
}