package com.example.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface VaultDao {
    @Query("SELECT * FROM vaults ORDER BY name ASC")
    fun getAllVaultsFlow(): Flow<List<Vault>>

    @Query("SELECT * FROM vaults")
    suspend fun getAllVaults(): List<Vault>

    @Query("SELECT * FROM vaults WHERE id = :id")
    suspend fun getVaultById(id: String): Vault?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVault(vault: Vault)

    @Update
    suspend fun updateVault(vault: Vault)

    @Delete
    suspend fun deleteVault(vault: Vault)
}

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes WHERE vaultId = :vaultId ORDER BY updatedAt DESC")
    fun getNotesForVaultFlow(vaultId: String): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE vaultId = :vaultId")
    suspend fun getNotesForVault(vaultId: String): List<Note>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNoteById(id: String): Note?

    @Query("SELECT * FROM notes WHERE vaultId = :vaultId AND filePath = :filePath")
    suspend fun getNoteByPath(vaultId: String, filePath: String): Note?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: Note)

    @Update
    suspend fun updateNote(note: Note)

    @Delete
    suspend fun deleteNote(note: Note)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNoteById(id: String)
}
