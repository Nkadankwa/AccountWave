package com.example.accountwave.data

import android.content.Context
import androidx.room.*
import com.example.accountwave.model.Transaction
import com.example.accountwave.model.LogEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions")
    fun getAll(): Flow<List<Transaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: Transaction)

    @Delete
    fun delete(transaction: Transaction)

    @Query("SELECT * FROM transactions WHERE type = :type")
    fun getTransactionsByType(type: String): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE category = :category")
    fun getTransactionsByCategory(category: String): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE tabName = :tabName")
    fun getTransactionsByTab(tabName: String): Flow<List<Transaction>>
}

class TransactionDaoImpl(
    private val context: Context,
    private val logEntryDao: LogEntryDao,
    private val transactionDao: TransactionDao
) {
    suspend fun insert(transaction: Transaction) {
        kotlinx.coroutines.withContext(Dispatchers.IO) {
        val logEntry = LogEntry(
            timestamp = Date(),
            entityName = "Transaction",
            entityId = transaction.id,
            operationType = "INSERT",
            details = "Title: ${transaction.title}, Amount: ${transaction.amount}"
        )
        logEntryDao.insert(logEntry)
        transactionDao.insert(transaction)
    }}

    suspend fun delete(transaction: Transaction) {
        kotlinx.coroutines.withContext(Dispatchers.IO) {
        val logEntry = LogEntry(
            timestamp = Date(),
            entityName = "Transaction",
            entityId = transaction.id,
            operationType = "DELETE",
            details = "Title: ${transaction.title}, Amount: ${transaction.amount}"
        )
        logEntryDao.insert(logEntry)
        transactionDao.delete(transaction)
    }}
}