package com.example.data

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.db.AppDatabase
import com.example.data.security.SecurePreferences

class GitSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.d("GitSyncWorker", "Executing background Git repository synchronization...")
        
        try {
            val appDatabase = AppDatabase.getDatabase(applicationContext)
            val securePreferences = SecurePreferences(applicationContext)
            val syncEngine = GitSyncEngine(applicationContext, appDatabase, securePreferences)
            
            val vaults = appDatabase.vaultDao().getAllVaults()
            if (vaults.isEmpty()) {
                Log.d("GitSyncWorker", "No vaults found for background sync.")
                return Result.success()
            }

            var successCount = 0
            var failCount = 0

            for (vault in vaults) {
                try {
                    Log.d("GitSyncWorker", "Auto-syncing vault: ${vault.name}")
                    syncEngine.syncVault(vault.id)
                    successCount++
                } catch (e: Exception) {
                    Log.e("GitSyncWorker", "Failed to sync vault ${vault.name}", e)
                    failCount++
                }
            }

            Log.d("GitSyncWorker", "Sync completed. Success: $successCount, Failed: $failCount")
            return if (failCount > 0 && successCount == 0) {
                Result.retry()
            } else {
                Result.success()
            }
        } catch (e: Exception) {
            Log.e("GitSyncWorker", "Critical background sync failure", e)
            return Result.failure()
        }
    }
}
