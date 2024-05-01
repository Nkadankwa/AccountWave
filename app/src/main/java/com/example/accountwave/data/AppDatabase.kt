package com.example.accountwave.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.accountwave.model.Budget
import com.example.accountwave.model.InventoryItem
import com.example.accountwave.model.Transaction
import com.example.accountwave.model.Tab
import com.example.accountwave.model.LogEntry
import com.example.accountwave.utils.DateConverter

@Database(entities = [Budget::class, InventoryItem::class, Transaction::class,  Tab::class, LogEntry::class], version = 2)
@TypeConverters(DateConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun budgetDao(): BudgetDao
    abstract fun tabDao(): TabDao
    abstract fun inventoryItemDao(): InventoryItemDao
    abstract fun transactionDao(): TransactionDao
    abstract fun logEntryDao(): LogEntryDao

    fun budgetDaoImpl(): BudgetDaoImpl {
        return BudgetDaoImpl(context, logEntryDao(), budgetDao())
    }
    fun inventoryItemDaoImpl(): InventoryItemDaoImpl {
        return InventoryItemDaoImpl(context, logEntryDao(), inventoryItemDao())
    }
    fun transactionDaoImpl(): TransactionDaoImpl {
        return TransactionDaoImpl(context, logEntryDao(), transactionDao())
    }

    private lateinit var context: Context
    companion object {
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS `logs` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `timestamp` INTEGER NOT NULL, `entityName` TEXT NOT NULL, `entityId` INTEGER NOT NULL, `operationType` TEXT NOT NULL, `details` TEXT)")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "accountwave_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration()
                    .build().apply {
                        this.context = context
                    }
                INSTANCE = instance
                instance
            }
        }

        fun createTestInstance(context: Context): AppDatabase {
            return Room.inMemoryDatabaseBuilder(
                context.applicationContext,
                AppDatabase::class.java
            ).allowMainThreadQueries().build()
        }
    }

    fun getContext(context: Context) {
        val budgetDao = BudgetDaoImpl(context, logEntryDao(), budgetDao())
        val inventoryItemDao = InventoryItemDaoImpl(context, logEntryDao(), inventoryItemDao())
        val transactionDao = TransactionDaoImpl(context, logEntryDao(), transactionDao())
    }
}