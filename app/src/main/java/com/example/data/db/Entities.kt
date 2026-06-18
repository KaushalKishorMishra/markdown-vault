package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "vaults"
)
data class Vault(
    @PrimaryKey val id: String, // UUID
    val name: String,
    val gitRepo: String, // format "username/repo"
    val branch: String, // "main" / "master"
    val vaultType: String, // "OBSIDIAN" (standard folders), "LOGSEQ" (pages/ journals/), "BASIC" (flat)
    val lastSynced: Long = 0L,
    val localPath: String = ""
)

@Entity(
    tableName = "notes",
    foreignKeys = [
        ForeignKey(
            entity = Vault::class,
            parentColumns = ["id"],
            childColumns = ["vaultId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["vaultId"])]
)
data class Note(
    @PrimaryKey val id: String, // e.g., vaultId + ":" + filePath
    val vaultId: String,
    val filePath: String, // relative path e.g. "pages/notes.md"
    val title: String,
    val content: String,
    val localSha: String = "",       // Git SHA of local state
    val remoteSha: String = "",      // Git SHA of remote state
    val syncStatus: String = "SYNCED", // "SYNCED", "MODIFIED", "CONFLICT", "LOCAL_ONLY", "DELETED"
    val updatedAt: Long = System.currentTimeMillis()
)
