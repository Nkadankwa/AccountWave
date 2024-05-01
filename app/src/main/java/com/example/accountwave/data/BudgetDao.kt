package com.example.accountwave.data
import android.content.Context
import androidx.room.*
import com.example.accountwave.model.Budget
import com.example.accountwave.model.LogEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets")
   fun getAll(): Flow<List<Budget>>

    @Query("SELECT * FROM budgets")
    suspend fun getAllOnce(): List<Budget>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(budget: Budget)

    @Delete
    fun delete(budget: Budget)

    @Query("SELECT * FROM budgets WHERE category = :category")
    suspend fun getBudgetsByCategory(category: String): List<Budget>
}

class BudgetDaoImpl(
    private val context: Context,
    private val logEntryDao: LogEntryDao,
    private val budgetDao: BudgetDao
) {
    suspend fun insert(budget: Budget) {
        kotlinx.coroutines.withContext(Dispatchers.IO) {

        val logEntry = LogEntry(
            timestamp = Date(),
            entityName = "Budget",
            entityId = budget.id,
            operationType = "INSERT",
            details = "Category: ${budget.category}, Limit: ${budget.limit}"
        )
        logEntryDao.insert(logEntry)
        budgetDao.insert(budget)
    }}

    suspend fun delete(budget: Budget) {
        kotlinx.coroutines.withContext(Dispatchers.IO) {

        val logEntry = LogEntry(
            timestamp = Date(),
            entityName = "Budget",
            entityId = budget.id,
            operationType = "DELETE",
            details = "Category: ${budget.category}, Limit: ${budget.limit}"
        )
        logEntryDao.insert(logEntry)
        budgetDao.delete(budget)
        }
    }
}