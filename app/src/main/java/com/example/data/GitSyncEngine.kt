package com.example.data

import android.content.Context
import android.util.Base64
import android.util.Log
import com.example.data.api.*
import com.example.data.db.*
import com.example.data.security.SecurePreferences
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

sealed class SyncState {
    object Idle : SyncState()
    object Syncing : SyncState()
    data class Success(val message: String) : SyncState()
    data class Error(val error: String) : SyncState()
}

class GitSyncEngine(
    private val context: Context,
    private val appDatabase: AppDatabase,
    private val securePreferences: SecurePreferences
) {
    private val TAG = "GitSyncEngine"

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(loggingInterceptor)
        .build()

    private val gitHubApi: GitHubApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GitHubApi::class.java)
    }

    private fun getAuthHeader(): String {
        val token = securePreferences.getGitHubToken()
        return if (token.isNotEmpty()) "Bearer $token" else ""
    }

    /**
     * Authenticates connection and returns logged in user info.
     */
    suspend fun testConnection(username: String, token: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val header = "Bearer $token"
                val response = gitHubApi.getUser(header)
                if (response.isSuccessful && response.body() != null) {
                    val user = response.body()!!
                    Log.d(TAG, "Test connection success: ${user.login}")
                    // Ensure usernames match or save what's in github
                    securePreferences.saveGitHubUsername(username.ifEmpty { user.login })
                    securePreferences.saveGitHubToken(token)
                    securePreferences.saveGitHubName(user.name ?: "")
                    securePreferences.saveGitHubEmail(user.email ?: "")
                    securePreferences.saveGitHubAvatarUrl(user.avatarUrl ?: "")
                    true
                } else {
                    Log.e(TAG, "Authentication failed with code: ${response.code()}")
                    false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Connection exception", e)
                false
            }
        }
    }

    /**
     * Performs a full bi-directional synchronization cycle for a specific vault.
     */
    suspend fun syncVault(vaultId: String) {
        withContext(Dispatchers.IO) {
            _syncState.value = SyncState.Syncing
            val vaultDao = appDatabase.vaultDao()
            val noteDao = appDatabase.noteDao()

            val vault = vaultDao.getVaultById(vaultId)
            if (vault == null) {
                _syncState.value = SyncState.Error("Vault not found")
                return@withContext
            }

            val token = securePreferences.getGitHubToken()
            if (token.isEmpty()) {
                _syncState.value = SyncState.Error("GitHub token/PAT is missing in Secure Storage")
                return@withContext
            }

            val parts = vault.gitRepo.split("/")
            if (parts.size != 2) {
                _syncState.value = SyncState.Error("Invalid GitHub repository format. Expected 'owner/repo'")
                return@withContext
            }
            val owner = parts[0].trim()
            val repo = parts[1].trim()
            val authHeader = getAuthHeader()

            try {
                Log.d(TAG, "Starting sync for vault: ${vault.name} of repo: $owner/$repo branch: ${vault.branch}")

                // 1. Fetch Remote Tree
                val treeResponse = gitHubApi.getTree(authHeader, owner, repo, vault.branch, recursive = 1)
                if (!treeResponse.isSuccessful) {
                    _syncState.value = SyncState.Error("Failed to fetch repository tree: ${treeResponse.message()}")
                    return@withContext
                }

                val remoteTree = treeResponse.body()?.tree ?: emptyList()
                val isTruncated = treeResponse.body()?.truncated ?: false
                val remoteFilesMap = remoteTree.filter { it.type == "blob" && (it.path.endsWith(".md") || it.path.endsWith(".txt")) }
                    .associateBy { it.path }
                val isRemoteTreeEmpty = remoteFilesMap.isEmpty()

                // 2. Fetch existing local notes
                val localNotes = noteDao.getNotesForVault(vaultId)
                val localNotesMap = localNotes.associateBy { it.filePath }

                var pullCount = 0
                var pushCount = 0
                var conflictCount = 0

                // 3. Bidirectional logic: Loop through remote files to reconcile pulls, updates, and detect conflicts
                for ((remotePath, remoteEntry) in remoteFilesMap) {
                    val localNote = localNotesMap[remotePath]

                    if (localNote != null && localNote.syncStatus == "TRASHED") {
                        Log.d(TAG, "Skipping remote sync for trashed note: $remotePath")
                        continue
                    }

                    if (localNote == null) {
                        // REMOTE-ONLY file: Download it!
                        Log.d(TAG, "Pulling new remote file: $remotePath")
                        val contentResponse = gitHubApi.getFileContent(authHeader, owner, repo, remotePath, vault.branch)
                        if (contentResponse.isSuccessful && contentResponse.body() != null) {
                            val remoteFile = contentResponse.body()!!
                            val base64Content = remoteFile.content?.replace("\n", "")?.replace("\r", "") ?: ""
                            val plainText = String(Base64.decode(base64Content, Base64.DEFAULT), Charsets.UTF_8)
                            
                            val title = getCleanTitle(remotePath)
                            val decodedNewNote = Note(
                                id = "${vaultId}:${remotePath}",
                                vaultId = vaultId,
                                filePath = remotePath,
                                title = title,
                                content = plainText,
                                localSha = remoteFile.sha,
                                remoteSha = remoteFile.sha,
                                syncStatus = "SYNCED",
                                updatedAt = System.currentTimeMillis()
                            )
                            noteDao.insertNote(decodedNewNote)
                            writeLocalStorageFile(vault, remotePath, plainText)
                            pullCount++
                        }
                    } else {
                        // Compare SHAs
                        if (localNote.remoteSha == remoteEntry.sha) {
                            // Remote hasn't changed. Is the local file modified?
                            if (localNote.syncStatus == "MODIFIED") {
                                // Real LOCAL modified! Push it to GitHub
                                Log.d(TAG, "Pushing modifications for: $remotePath")
                                val base64Content = Base64.encodeToString(localNote.content.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
                                val updateRequest = UpdateFileRequest(
                                    message = "Sync update: ${localNote.title}",
                                    content = base64Content,
                                    sha = localNote.remoteSha,
                                    branch = vault.branch
                                )
                                val updateResponse = gitHubApi.createOrUpdateFile(authHeader, owner, repo, remotePath, updateRequest)
                                if (updateResponse.isSuccessful && updateResponse.body() != null) {
                                    val responseBody = updateResponse.body()!!
                                    val newSha = responseBody.content?.sha ?: remoteEntry.sha
                                    val syncedNote = localNote.copy(
                                        localSha = newSha,
                                        remoteSha = newSha,
                                        syncStatus = "SYNCED",
                                        updatedAt = System.currentTimeMillis()
                                    )
                                    noteDao.insertNote(syncedNote)
                                    pushCount++
                                }
                            }
                        } else {
                            // Remote HAS changed!
                            if (localNote.syncStatus == "MODIFIED") {
                                // Out of sync: Local edit AND remote edit! CONFLICT
                                Log.w(TAG, "Conflict detected for Note: $remotePath")
                                val conflictNote = localNote.copy(
                                    remoteSha = remoteEntry.sha,
                                    syncStatus = "CONFLICT"
                                )
                                noteDao.insertNote(conflictNote)
                                conflictCount++
                            } else {
                                // Local in-sync, Remote edited: Pull latest update automatically!
                                Log.d(TAG, "Pulling updated remote file: $remotePath (fast-forward)")
                                val contentResponse = gitHubApi.getFileContent(authHeader, owner, repo, remotePath, vault.branch)
                                if (contentResponse.isSuccessful && contentResponse.body() != null) {
                                    val remoteFile = contentResponse.body()!!
                                    val base64Content = remoteFile.content?.replace("\n", "")?.replace("\r", "") ?: ""
                                    val plainText = String(Base64.decode(base64Content, Base64.DEFAULT), Charsets.UTF_8)

                                    val updatedSyncedNote = localNote.copy(
                                        content = plainText,
                                        localSha = remoteFile.sha,
                                        remoteSha = remoteFile.sha,
                                        syncStatus = "SYNCED",
                                        updatedAt = System.currentTimeMillis()
                                    )
                                    noteDao.insertNote(updatedSyncedNote)
                                    writeLocalStorageFile(vault, remotePath, plainText)
                                    pullCount++
                                }
                            }
                        }
                    }
                }

                // 4. Handle LOCAL ONLY or DELETED files (present locally but missing on GitHub)
                for (localNote in localNotes) {
                    val remoteEntryStatus = remoteFilesMap[localNote.filePath]

                    if (remoteEntryStatus == null) {
                        // Safety protection: If the remote tree is completely empty, it means the repository is empty
                        // or newly configured. To prevent data loss, treat previously SYNCED notes as LOCAL_ONLY so they
                        // populate the repository instead of getting deleted.
                        val adjustedNote = if (isRemoteTreeEmpty && localNote.syncStatus == "SYNCED") {
                            localNote.copy(syncStatus = "LOCAL_ONLY", remoteSha = "")
                        } else {
                            localNote
                        }

                        if (adjustedNote.syncStatus == "LOCAL_ONLY" || adjustedNote.remoteSha.isEmpty()) {
                            if (adjustedNote.syncStatus == "TRASHED") {
                                val timeInTrash = System.currentTimeMillis() - adjustedNote.updatedAt
                                val thirtyDaysMs = 30L * 24 * 60 * 60 * 1000
                                if (timeInTrash >= thirtyDaysMs) {
                                    Log.d(TAG, "Purging local-only trashed note: ${adjustedNote.filePath}")
                                    noteDao.deleteNote(adjustedNote)
                                    deleteLocalFile(vault, adjustedNote.filePath)
                                    deleteLocalFile(vault, ".trash/${adjustedNote.filePath}")
                                }
                            } else {
                                // LOCAL ONLY: Push create!
                                Log.d(TAG, "Uploading new local file: ${adjustedNote.filePath}")
                                val base64Content = Base64.encodeToString(adjustedNote.content.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
                                val updateRequest = UpdateFileRequest(
                                    message = "Sync create: ${adjustedNote.title}",
                                    content = base64Content,
                                    sha = null,
                                    branch = vault.branch
                                )
                                val updateResponse = gitHubApi.createOrUpdateFile(authHeader, owner, repo, adjustedNote.filePath, updateRequest)
                                if (updateResponse.isSuccessful && updateResponse.body() != null) {
                                    val responseBody = updateResponse.body()!!
                                    val newSha = responseBody.content?.sha ?: ""
                                    val syncedNote = adjustedNote.copy(
                                        localSha = newSha,
                                        remoteSha = newSha,
                                        syncStatus = "SYNCED",
                                        updatedAt = System.currentTimeMillis()
                                    )
                                    noteDao.insertNote(syncedNote)
                                    pushCount++
                                }
                            }
                        } else if (adjustedNote.syncStatus == "DELETED" || adjustedNote.syncStatus == "TRASHED") {
                            // If it's deleted/trashed locally but already missing on GitHub, we can just purge it from our local database!
                            Log.d(TAG, "Purging note missing from remote: ${adjustedNote.filePath}")
                            noteDao.deleteNote(adjustedNote)
                            deleteLocalFile(vault, adjustedNote.filePath)
                            deleteLocalFile(vault, ".trash/${adjustedNote.filePath}")
                        } else {
                            // Previously synced in GitHub but deleted from GitHub remote. Discard locally or mark conflict?
                            // Skip if the remote tree fetch was truncated (safety net).
                            // Skip if the note was updated/synced recently (less than 5 mins ago) to protect against replication lag.
                            val timeSinceUpdate = System.currentTimeMillis() - adjustedNote.updatedAt
                            val isRecent = timeSinceUpdate < 5L * 60 * 1000 // 5 minutes
                            
                            if (isTruncated) {
                                Log.w(TAG, "Remote tree was truncated; skipping local deletion for safety: ${adjustedNote.filePath}")
                            } else if (isRecent) {
                                Log.d(TAG, "Recent note missing from remote tree (likely replication lag); skipping local deletion: ${adjustedNote.filePath}")
                            } else if (adjustedNote.syncStatus == "MODIFIED") {
                                Log.w(TAG, "File was deleted on remote but edited locally: ${adjustedNote.filePath}")
                                val conflictNote = adjustedNote.copy(
                                    syncStatus = "CONFLICT",
                                    remoteSha = ""
                                )
                                noteDao.insertNote(conflictNote)
                                conflictCount++
                            } else {
                                Log.d(TAG, "File deleted from remote, moving to local trash: ${adjustedNote.filePath}")
                                moveFileToTrashDisk(vault, adjustedNote.filePath)
                                val trashedNote = adjustedNote.copy(
                                    syncStatus = "TRASHED",
                                    updatedAt = System.currentTimeMillis()
                                )
                                noteDao.insertNote(trashedNote)
                                pullCount++
                            }
                        }
                    } else if (localNote.syncStatus == "DELETED") {
                        // DELETED LOCALLY: Sync DELETE to remote
                        Log.d(TAG, "Deleting remote file: ${localNote.filePath}")
                        val deleteRequest = DeleteFileRequest(
                            message = "Sync delete: ${localNote.title}",
                            sha = localNote.remoteSha,
                            branch = vault.branch
                        )
                        val deleteResponse = gitHubApi.deleteFile(authHeader, owner, repo, localNote.filePath, deleteRequest)
                        if (deleteResponse.isSuccessful) {
                            noteDao.deleteNote(localNote)
                            deleteLocalFile(vault, localNote.filePath)
                            deleteLocalFile(vault, ".trash/${localNote.filePath}")
                            pushCount++
                        }
                    } else if (localNote.syncStatus == "TRASHED") {
                        // TRASHED LOCALLY (but exists on GitHub): check if 30 days have expired
                        val timeInTrash = System.currentTimeMillis() - localNote.updatedAt
                        val thirtyDaysMs = 30L * 24 * 60 * 60 * 1000
                        if (timeInTrash >= thirtyDaysMs) {
                            Log.d(TAG, "Deleting remote file for expired trashed note: ${localNote.filePath}")
                            val deleteRequest = DeleteFileRequest(
                                message = "Sync delete (Trash Expiry): ${localNote.title}",
                                sha = localNote.remoteSha,
                                branch = vault.branch
                            )
                            val deleteResponse = gitHubApi.deleteFile(authHeader, owner, repo, localNote.filePath, deleteRequest)
                            if (deleteResponse.isSuccessful) {
                                noteDao.deleteNote(localNote)
                                deleteLocalFile(vault, localNote.filePath)
                                deleteLocalFile(vault, ".trash/${localNote.filePath}")
                                pushCount++
                            }
                        }
                    }
                }

                vaultDao.updateVault(vault.copy(lastSynced = System.currentTimeMillis()))

                // 5. Build summary message
                val summary = buildString {
                    append("Sync Successful. ")
                    if (pullCount > 0) append("Pulled $pullCount changes. ")
                    if (pushCount > 0) append("Pushed $pushCount updates. ")
                    if (conflictCount > 0) append("Found $conflictCount conflicts.")
                    if (pullCount == 0 && pushCount == 0 && conflictCount == 0) append("All files are up to date.")
                }
                _syncState.value = SyncState.Success(summary)
            } catch (e: Exception) {
                Log.e(TAG, "Sync process failed", e)
                _syncState.value = SyncState.Error("Sync failed: ${e.localizedMessage ?: "Unknown network error"}")
            }
        }
    }

    /**
     * Resolves a merge conflict either by force-keeping local, force-keeping remote, or merging.
     */
    suspend fun resolveConflict(noteId: String, resolution: String, mergedContent: String = "") {
        withContext(Dispatchers.IO) {
            val noteDao = appDatabase.noteDao()
            val vaultDao = appDatabase.vaultDao()
            val note = noteDao.getNoteById(noteId) ?: return@withContext
            val vault = vaultDao.getVaultById(note.vaultId) ?: return@withContext

            val parts = vault.gitRepo.split("/")
            if (parts.size != 2) return@withContext
            val owner = parts[0].trim()
            val repo = parts[1].trim()
            val authHeader = getAuthHeader()

            when (resolution) {
                "LOCAL" -> {
                    // Update note status to MODIFIED with the updated remote sha from conflict state
                    // This creates an updated push
                    val updatedNote = note.copy(
                        syncStatus = "MODIFIED",
                        updatedAt = System.currentTimeMillis()
                    )
                    noteDao.insertNote(updatedNote)
                    writeLocalStorageFile(vault, note.filePath, note.content)
                    syncVault(vault.id) // Re-sync to force push!
                }
                "REMOTE" -> {
                    // Download latest content from remote and throw away local
                    val contentResponse = gitHubApi.getFileContent(authHeader, owner, repo, note.filePath, vault.branch)
                    if (contentResponse.isSuccessful && contentResponse.body() != null) {
                        val remoteFile = contentResponse.body()!!
                        val base64Content = remoteFile.content?.replace("\n", "")?.replace("\r", "") ?: ""
                        val plainText = String(Base64.decode(base64Content, Base64.DEFAULT), Charsets.UTF_8)
                        
                        val resolvedNote = note.copy(
                            content = plainText,
                            localSha = remoteFile.sha,
                            remoteSha = remoteFile.sha,
                            syncStatus = "SYNCED",
                            updatedAt = System.currentTimeMillis()
                        )
                        noteDao.insertNote(resolvedNote)
                        writeLocalStorageFile(vault, note.filePath, plainText)
                    }
                }
                "MERGED" -> {
                    // Save combined content to local DB and flag as MODIFIED to sync to remote
                    val resolvedNote = note.copy(
                        content = mergedContent,
                        syncStatus = "MODIFIED",
                        updatedAt = System.currentTimeMillis()
                    )
                    noteDao.insertNote(resolvedNote)
                    writeLocalStorageFile(vault, note.filePath, mergedContent)
                    syncVault(vault.id) // Re-sync to push!
                }
            }
        }
    }

    private fun getCleanTitle(filePath: String): String {
        val fileName = File(filePath).name
        return fileName.removeSuffix(".md").removeSuffix(".txt")
            .split("_", "-")
            .joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
    }

    // --- Physical file storage helpers to keep desktop/external apps (Obsidian, Logseq) compatible ---

    private fun getVaultDirectory(vault: Vault): File {
        val baseDir = File(context.filesDir, "vaults")
        if (!baseDir.exists()) baseDir.mkdirs()
        
        val vaultDirName = vault.name.filter { it.isLetterOrDigit() || it == '_' || it == '-' }
        val vaultDir = File(baseDir, vaultDirName)
        if (!vaultDir.exists()) vaultDir.mkdirs()
        return vaultDir
    }

    private fun writeLocalStorageFile(vault: Vault, filePath: String, content: String) {
        try {
            val vaultDir = getVaultDirectory(vault)
            val destinationFile = File(vaultDir, filePath)
            
            // Create parent directories if any (e.g. pages/journals)
            destinationFile.parentFile?.mkdirs()
            destinationFile.writeText(content, Charsets.UTF_8)
            Log.d(TAG, "Wrote physically to: ${destinationFile.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed writing physical file", e)
        }
    }

    private fun deleteLocalFile(vault: Vault, filePath: String) {
        try {
            val vaultDir = getVaultDirectory(vault)
            val fileToDelete = File(vaultDir, filePath)
            if (fileToDelete.exists()) {
                fileToDelete.delete()
                Log.d(TAG, "Deleted physical file: ${fileToDelete.absolutePath}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed deleting physical file", e)
        }
    }

    private fun moveFileToTrashDisk(vault: Vault, filePath: String) {
        try {
            val vaultDir = getVaultDirectory(vault)
            val sourceFile = File(vaultDir, filePath)
            if (sourceFile.exists()) {
                val trashFile = File(vaultDir, ".trash/$filePath")
                trashFile.parentFile?.mkdirs()
                sourceFile.renameTo(trashFile)
                if (!trashFile.exists()) {
                    sourceFile.copyTo(trashFile, overwrite = true)
                    sourceFile.delete()
                }
                Log.d(TAG, "Moved file to trash: ${trashFile.absolutePath}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to move file to trash", e)
        }
    }
}
