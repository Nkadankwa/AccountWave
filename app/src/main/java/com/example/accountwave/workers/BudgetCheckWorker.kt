package com.example.accountwave.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.accountwave.data.AppDatabase
import com.example.accountwave.utils.NotificationHelper
import kotlinx.coroutines.flow.first

class BudgetCheckWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val database = AppDatabase.getDatabase(applicationContext)

        val budgets = database.budgetDao().getAll().first()
        val transactions = database.transactionDao().getAll().first()

        val categorySpending = mutableMapOf<String, Double>()
        transactions.forEach { transaction: com.example.accountwave.model.Transaction ->
            categorySpending[transaction.category] =
                (categorySpending[transaction.category] ?: 0.0) + transaction.amount
        }

        budgets.forEach { budget: com.example.accountwave.model.Budget ->
            val spent = categorySpending[budget.category] ?: 0.0
            val limit = budget.limit

            if (spent >= limit * 0.8 && spent <= limit) {
                NotificationHelper.showNotification(
                    applicationContext,
                    "Budget Alert",
                    "You are approaching your budget limit for ${budget.category}"
                )
            }
            if (spent > limit) {
                NotificationHelper.showNotification(
                    applicationContext,
                    "Budget Alert",
                    "You have exceeded your budget limit for ${budget.category}"
                )
            }
        }

        return Result.success()
    }
}