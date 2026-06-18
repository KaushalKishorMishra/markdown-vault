package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.*
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GitVaultUnitTest {

    private lateinit var db: AppDatabase
    private lateinit var vaultDao: VaultDao
    private lateinit var noteDao: NoteDao
    private lateinit var context: Context

    @Before
    fun createDb() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        vaultDao = db.vaultDao()
        noteDao = db.noteDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun testCreateVault() = runBlocking {
        val vaultId = UUID.randomUUID().toString()
        val vault = Vault(
            id = vaultId,
            name = "Test Obsidian",
            gitRepo = "testuser/testrepo",
            branch = "main",
            vaultType = "OBSIDIAN"
        )
        vaultDao.insertVault(vault)
        
        val retrieved = vaultDao.getVaultById(vaultId)
        assertNotNull(retrieved)
        assertEquals("Test Obsidian", retrieved?.name)
        assertEquals("testuser/testrepo", retrieved?.gitRepo)
        assertEquals("main", retrieved?.branch)
        assertEquals("OBSIDIAN", retrieved?.vaultType)
    }

    @Test
    fun testCreateNote() = runBlocking {
        val vaultId = UUID.randomUUID().toString()
        val vault = Vault(
            id = vaultId,
            name = "Test Flat",
            gitRepo = "testuser/flat",
            branch = "main",
            vaultType = "BASIC"
        )
        vaultDao.insertVault(vault)

        val note = Note(
            id = "$vaultId:welcome.md",
            vaultId = vaultId,
            filePath = "welcome.md",
            title = "Welcome",
            content = "# Welcome\nHello World",
            syncStatus = "LOCAL_ONLY"
        )
        noteDao.insertNote(note)

        val retrieved = noteDao.getNoteById("$vaultId:welcome.md")
        assertNotNull(retrieved)
        assertEquals("Welcome", retrieved?.title)
        assertEquals("# Welcome\nHello World", retrieved?.content)
        assertEquals("LOCAL_ONLY", retrieved?.syncStatus)
    }

    @Test
    fun testSoftDeleteToTrash() = runBlocking {
        val vaultId = UUID.randomUUID().toString()
        val vault = Vault(id = vaultId, name = "V", gitRepo = "r", branch = "b", vaultType = "BASIC")
        vaultDao.insertVault(vault)

        val note = Note(
            id = "$vaultId:note.md",
            vaultId = vaultId,
            filePath = "note.md",
            title = "Note",
            content = "Hello",
            syncStatus = "SYNCED",
            updatedAt = System.currentTimeMillis()
        )
        noteDao.insertNote(note)

        // Soft Delete simulation: status set to "TRASHED"
        val trashedNote = note.copy(
            syncStatus = "TRASHED",
            updatedAt = System.currentTimeMillis()
        )
        noteDao.insertNote(trashedNote)

        val retrieved = noteDao.getNoteById("$vaultId:note.md")
        assertNotNull(retrieved)
        assertEquals("TRASHED", retrieved?.syncStatus)
    }

    @Test
    fun testRestoreFromTrash() = runBlocking {
        val vaultId = UUID.randomUUID().toString()
        val vault = Vault(id = vaultId, name = "V", gitRepo = "r", branch = "b", vaultType = "BASIC")
        vaultDao.insertVault(vault)

        val note = Note(
            id = "$vaultId:note.md",
            vaultId = vaultId,
            filePath = "note.md",
            title = "Note",
            content = "Hello",
            remoteSha = "sha123", // previously synced note
            syncStatus = "TRASHED",
            updatedAt = System.currentTimeMillis()
        )
        noteDao.insertNote(note)

        // Restore simulation
        val originalStatus = if (note.remoteSha.isEmpty()) "LOCAL_ONLY" else "MODIFIED"
        val restoredNote = note.copy(
            syncStatus = originalStatus,
            updatedAt = System.currentTimeMillis()
        )
        noteDao.insertNote(restoredNote)

        val retrieved = noteDao.getNoteById("$vaultId:note.md")
        assertNotNull(retrieved)
        assertEquals("MODIFIED", retrieved?.syncStatus)
    }

    @Test
    fun testPermanentDelete() = runBlocking {
        val vaultId = UUID.randomUUID().toString()
        val vault = Vault(id = vaultId, name = "V", gitRepo = "r", branch = "b", vaultType = "BASIC")
        vaultDao.insertVault(vault)

        val note = Note(
            id = "$vaultId:note.md",
            vaultId = vaultId,
            filePath = "note.md",
            title = "Note",
            content = "Hello",
            remoteSha = "", // local-only note
            syncStatus = "TRASHED",
            updatedAt = System.currentTimeMillis()
        )
        noteDao.insertNote(note)

        // Local-only permanently delete simulation (directly deletes from DB)
        if (note.remoteSha.isEmpty()) {
            noteDao.deleteNote(note)
        } else {
            noteDao.insertNote(note.copy(syncStatus = "DELETED"))
        }

        val retrieved = noteDao.getNoteById("$vaultId:note.md")
        assertNull(retrieved)
    }

    @Test
    fun testTrashExpiryDaysCalculation() {
        val now = System.currentTimeMillis()
        
        // 1. Trashed just now
        val timeDiff1 = now - now
        val daysLeft1 = 30 - (timeDiff1 / (24L * 60 * 60 * 1000)).toInt()
        assertEquals(30, daysLeft1)

        // 2. Trashed 10 days ago
        val tenDaysMs = 10L * 24 * 60 * 60 * 1000
        val timeDiff2 = now - (now - tenDaysMs)
        val daysLeft2 = 30 - (timeDiff2 / (24L * 60 * 60 * 1000)).toInt()
        assertEquals(20, daysLeft2)

        // 3. Trashed 30 days ago
        val thirtyDaysMs = 30L * 24 * 60 * 60 * 1000
        val timeDiff3 = now - (now - thirtyDaysMs)
        val daysLeft3 = 30 - (timeDiff3 / (24L * 60 * 60 * 1000)).toInt()
        assertEquals(0, daysLeft3)
    }
}
