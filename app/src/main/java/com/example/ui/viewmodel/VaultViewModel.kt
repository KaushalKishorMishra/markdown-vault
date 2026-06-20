package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.GitSyncEngine
import com.example.data.SyncState
import com.example.data.db.*
import com.example.data.security.SecurePreferences
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.example.BuildConfig
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class VaultViewModel(application: Application) : AndroidViewModel(application) {
    private val appDatabase = AppDatabase.getDatabase(application)
    private val securePrefs = SecurePreferences(application)
    val syncEngine = GitSyncEngine(application, appDatabase, securePrefs)

    val vaultDao = appDatabase.vaultDao()
    val noteDao = appDatabase.noteDao()

    // Git Configuration Info
    val gitUsername = MutableStateFlow(securePrefs.getGitHubUsername())
    val gitToken = MutableStateFlow(securePrefs.getGitHubToken())
    val gitName = MutableStateFlow(securePrefs.getGitHubName())
    val gitEmail = MutableStateFlow(securePrefs.getGitHubEmail())
    val gitAvatarUrl = MutableStateFlow(securePrefs.getGitHubAvatarUrl())
    val isGitConfigured = MutableStateFlow(securePrefs.getGitHubToken().isNotEmpty())

    // Vaults State
    val allVaults: StateFlow<List<Vault>> = vaultDao.getAllVaultsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _activeVault = MutableStateFlow<Vault?>(null)
    val activeVault: StateFlow<Vault?> = _activeVault

    // Notes State
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    val notesForActiveVault: StateFlow<List<Note>> = combine(
        _activeVault.flatMapLatest { vault ->
            if (vault != null) noteDao.getNotesForVaultFlow(vault.id)
            else flowOf(emptyList())
        },
        _searchQuery
    ) { notes, query ->
        val filtered = notes.filter { it.syncStatus != "DELETED" && it.syncStatus != "TRASHED" }
        if (query.isEmpty()) {
            filtered
        } else {
            filtered.filter {
                it.title.contains(query, ignoreCase = true) || 
                it.content.contains(query, ignoreCase = true)
            }
        }
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val trashedNotesForActiveVault: StateFlow<List<Note>> = _activeVault.flatMapLatest { vault ->
        if (vault != null) {
            noteDao.getNotesForVaultFlow(vault.id).map { notes ->
                notes.filter { it.syncStatus == "TRASHED" }
            }
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Selected Note State
    private val _selectedNote = MutableStateFlow<Note?>(null)
    val selectedNote: StateFlow<Note?> = _selectedNote

    // Edit vs Preview Mode
    private val _isEditMode = MutableStateFlow(false)
    val isEditMode: StateFlow<Boolean> = _isEditMode

    // Sync State
    val syncStatus: StateFlow<SyncState> = syncEngine.syncState

    init {
        // Pre-populate dummy data if flag is set and no vaults exist
        viewModelScope.launch {
            if (BuildConfig.DUMMY_DATA_ENABLED == "true") {
                maybeInsertDummyData()
            }
        }

        // Automatically activate first vault if available
        viewModelScope.launch {
            allVaults.collect { list ->
                if (_activeVault.value == null && list.isNotEmpty()) {
                    _activeVault.value = list.first()
                }
            }
        }

        // Keep active note updated from DB revisions if available
        viewModelScope.launch {
            _selectedNote.collect { active ->
                if (active != null) {
                    notesForActiveVault.collect { list ->
                        val matching = list.find { it.id == active.id }
                        if (matching != null && matching.content != active.content && !_isEditMode.value) {
                            _selectedNote.value = matching
                        }
                    }
                }
            }
        }
    }

    private suspend fun maybeInsertDummyData() {
        val existing = vaultDao.getAllVaults()
        if (existing.isNotEmpty()) return

        val vaultId = UUID.randomUUID().toString()
        val sampleVault = Vault(
            id = vaultId,
            name = "Demo Vault",
            gitRepo = "",
            branch = "main",
            vaultType = "OBSIDIAN",
            lastSynced = 0L,
            localPath = ""
        )
        vaultDao.insertVault(sampleVault)

        val now = System.currentTimeMillis()
        val sampleNotes = listOf(
            Note(
                id = "$vaultId:welcome.md",
                vaultId = vaultId,
                filePath = "welcome.md",
                title = "Welcome to Markdown Vault",
                content = """# Welcome 📝

This is a demo note. You can edit, delete, or organize your notes into folders.

## Features

- **Markdown editing** with live preview
- **Math equations** via LaTeX: ${'$'}E = mc^2${'$'}
- **Mermaid diagrams** below
- **Git sync** with GitHub
- **Trash** with 30-day auto-cleanup

```mermaid
graph LR
    A[Write Notes] --> B[Preview]
    B --> C[Sync to GitHub]
```""",
                syncStatus = "SYNCED",
                updatedAt = now
            ),
            Note(
                id = "$vaultId:meeting-notes.md",
                vaultId = vaultId,
                filePath = "meeting-notes.md",
                title = "Meeting Notes Template",
                content = """# Meeting Notes

**Date:** 
**Attendees:** 
**Project:** 

## Agenda

1. 
2. 
3. 

## Discussion

## Action Items

- [ ] 
- [ ] 
- [ ] 

## Next Steps""",
                syncStatus = "LOCAL_ONLY",
                updatedAt = now - 3600000
            ),
            Note(
                id = "$vaultId:projects/project-alpha.md",
                vaultId = vaultId,
                filePath = "projects/project-alpha.md",
                title = "Project Alpha",
                content = """# Project Alpha

## Status: In Progress

### Milestones
- [x] Research phase
- [ ] Prototype development
- [ ] Testing
- [ ] Deployment

### Notes
The prototype should be ready by next sprint.

### Math
The quadratic formula: ${'$'}x = \\frac{-b \\pm \\sqrt{b^2 - 4ac}}{2a}${'$'}""",
                syncStatus = "SYNCED",
                updatedAt = now - 7200000
            ),
            Note(
                id = "$vaultId:projects/project-beta.md",
                vaultId = vaultId,
                filePath = "projects/project-beta.md",
                title = "Project Beta",
                content = """# Project Beta

## Overview
A secondary project running in parallel.

## Tasks
- [ ] Set up repository
- [ ] Configure CI/CD
- [ ] Write documentation""",
                syncStatus = "LOCAL_ONLY",
                updatedAt = now - 10800000
            ),
            Note(
                id = "$vaultId:daily/journal-2024-01.md",
                vaultId = vaultId,
                filePath = "daily/journal-2024-01.md",
                title = "January Journal",
                content = """# January Journal

## Week 1
Started the new year with a fresh vault setup.

## Week 2
Exploring the math rendering capabilities.

Inline: ${'$'}\\sum_{n=1}^{\\infty} \\frac{1}{n^2} = \\frac{\\pi^2}{6}${'$'}

Display:
${'$'}${'$'}\\int_{-\\infty}^{\\infty} e^{-x^2} dx = \\sqrt{\\pi}${'$'}${'$'}""",
                syncStatus = "LOCAL_ONLY",
                updatedAt = now - 86400000
            ),
            Note(
                id = "$vaultId:recipes/pasta.md",
                vaultId = vaultId,
                filePath = "recipes/pasta.md",
                title = "Pasta Recipe",
                content = """# Classic Pasta

## Ingredients
- 200g pasta
- 2 tbsp olive oil
- 3 cloves garlic
- Salt and pepper to taste

## Instructions
1. Boil water and cook pasta
2. Sauté garlic in olive oil
3. Mix together and serve""",
                syncStatus = "LOCAL_ONLY",
                updatedAt = now - 172800000
            ),
            Note(
                id = "$vaultId:archive/old-notes.md",
                vaultId = vaultId,
                filePath = "archive/old-notes.md",
                title = "Archived Thoughts",
                content = """# Archived Notes

## Random Thoughts
- Remember to back up regularly
- Check the sync status
- Review the trash policy""",
                syncStatus = "SYNCED",
                updatedAt = now - 259200000
            )
        )

        sampleNotes.forEach { noteDao.insertNote(it) }
        _activeVault.value = sampleVault
        _selectedNote.value = sampleNotes.first()
    }

    fun selectVault(vault: Vault) {
        _activeVault.value = vault
        _selectedNote.value = null
        _isEditMode.value = false
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setEditMode(edit: Boolean) {
        _isEditMode.value = edit
    }

    fun selectNote(note: Note?) {
        _selectedNote.value = note
        _isEditMode.value = false
    }

    // --- Folder and Note Management Extras (Google Files Style) ---

    fun renameNote(note: Note, newTitle: String, newFileName: String) {
        val vault = _activeVault.value ?: return
        val cleanName = newFileName.trim().removeSuffix(".md").removeSuffix(".txt") + ".md"
        val parentDir = if (note.filePath.contains("/")) note.filePath.substringBeforeLast("/") + "/" else ""
        val newFilePath = "$parentDir$cleanName"
        
        if (newFilePath == note.filePath) {
            // Only update title if filename has not changed
            viewModelScope.launch {
                val updatedStatus = if (note.syncStatus == "LOCAL_ONLY") "LOCAL_ONLY" else "MODIFIED"
                noteDao.insertNote(note.copy(title = newTitle, syncStatus = updatedStatus, updatedAt = System.currentTimeMillis()))
                if (_selectedNote.value?.id == note.id) {
                    _selectedNote.value = noteDao.getNoteById(note.id)
                }
            }
            return
        }
        
        viewModelScope.launch {
            val newNote = note.copy(
                id = "${vault.id}:$newFilePath",
                filePath = newFilePath,
                title = newTitle,
                syncStatus = if (note.syncStatus == "LOCAL_ONLY") "LOCAL_ONLY" else "MODIFIED",
                updatedAt = System.currentTimeMillis()
            )
            noteDao.insertNote(newNote)
            
            if (note.syncStatus == "LOCAL_ONLY") {
                noteDao.deleteNote(note)
            } else {
                noteDao.insertNote(note.copy(syncStatus = "DELETED"))
            }
            
            if (_selectedNote.value?.id == note.id) {
                _selectedNote.value = newNote
            }
        }
    }

    fun deleteFolder(folderPath: String) {
        val vault = _activeVault.value ?: return
        viewModelScope.launch {
            val prefix = if (folderPath.endsWith("/")) folderPath else "$folderPath/"
            val allNotes = noteDao.getNotesForVault(vault.id)
            val notesInFolder = allNotes.filter { it.filePath.startsWith(prefix) && it.syncStatus != "DELETED" && it.syncStatus != "TRASHED" }
            
            notesInFolder.forEach { note ->
                moveFileToTrashDisk(vault, note.filePath)
                noteDao.insertNote(note.copy(syncStatus = "TRASHED", updatedAt = System.currentTimeMillis()))
            }
            
            val activeNote = _selectedNote.value
            if (activeNote != null && activeNote.filePath.startsWith(prefix)) {
                _selectedNote.value = null
            }
        }
    }

    fun renameFolder(oldFolderPath: String, newFolderPath: String) {
        val vault = _activeVault.value ?: return
        val oldClean = oldFolderPath.trim().removeSuffix("/")
        val newClean = newFolderPath.trim().removeSuffix("/")
        if (oldClean == newClean || newClean.isEmpty()) return
        
        viewModelScope.launch {
            val oldPrefix = "$oldClean/"
            val newPrefix = "$newClean/"
            val allNotes = noteDao.getNotesForVault(vault.id)
            val notesInFolder = allNotes.filter { it.filePath.startsWith(oldPrefix) }
            
            notesInFolder.forEach { note ->
                val newFilePath = note.filePath.replaceFirst(oldPrefix, newPrefix)
                val newNote = note.copy(
                    id = "${vault.id}:$newFilePath",
                    filePath = newFilePath,
                    syncStatus = if (note.syncStatus == "LOCAL_ONLY") "LOCAL_ONLY" else "MODIFIED",
                    updatedAt = System.currentTimeMillis()
                )
                noteDao.insertNote(newNote)
                
                if (note.syncStatus == "LOCAL_ONLY") {
                    noteDao.deleteNote(note)
                } else {
                    noteDao.insertNote(note.copy(syncStatus = "DELETED"))
                }
            }
            
            val activeNote = _selectedNote.value
            if (activeNote != null && activeNote.filePath.startsWith(oldPrefix)) {
                val newActiveFilePath = activeNote.filePath.replaceFirst(oldPrefix, newPrefix)
                val matching = noteDao.getNoteById("${vault.id}:$newActiveFilePath")
                if (matching != null) {
                    _selectedNote.value = matching
                }
            }
        }
    }

    // --- Vault Management Commands ---

    fun createVault(name: String, repository: String, branch: String, layoutType: String) {
        viewModelScope.launch {
            val uuid = UUID.randomUUID().toString()
            val cleanRepo = repository.trim().removePrefix("https://github.com/").removeSuffix(".git")
            val newVault = Vault(
                id = uuid,
                name = name,
                gitRepo = cleanRepo,
                branch = branch.ifEmpty { "main" },
                vaultType = layoutType
            )
            vaultDao.insertVault(newVault)
            _activeVault.value = newVault

            // Populating visual template note
            val samplePath = when (layoutType) {
                "LOGSEQ" -> "pages/welcome.md"
                else -> "welcome.md"
            }
            val welcomeTemplate = """
            # Welcome to Markdown Vault 🚀
            
            This is a secure, local-first markdown note-taking workspace synced perfectly with GitHub. It supports regular markdown, **LaTeX Math equations**, and **Mermaid.js charts**!
            
            ---
            
            ## 1. LaTeX Math Expression Example
            
            Our high-performance rendering engine supports both ${'$'}inline${'$'} and ${'$'}${'$'}display${'$'}${'$'} equations!
            
            ### Inline Formulas
            The beautiful Euler's identity is defined as: ${'$'}e^{i\pi} + 1 = 0${'$'}.
            
            ### Display Physics Equations
            Schrödinger's Equation for a quantum particle is:
            ${'$'}${'$'} i\hbar \frac{\partial}{\partial t}\Psi(\mathbf{r},t) = \hat{H}\Psi(\mathbf{r},t) ${'$'}${'$'}
            
            And Maxwell's elegant Gauss equation is:
            ${'$'}${'$'} \nabla \cdot \mathbf{E} = \frac{\rho}{\varepsilon_0} ${'$'}${'$'}
            
            ---
            
            ## 2. Mermaid.js Flowchart Example
            
            Visualize complex engineering workflows or class dependencies natively in your vault:
            
            ```mermaid
            graph TD;
                LocalVault[Local Vault SQLite] -->|Offline Read/Write| Database[(Local Room DB)]
                Database -->|Sync Engine Check| GitSyncEngine[Git Rebase/Merge Sync Tool]
                GitSyncEngine -->|Automatic Rebase/Push| GitHubRemote[GitHub Cloud Repository]
                GitHubRemote -->|Visual Merge Conflict| ConflictResolverUI[Inter-App Resolver Screen]
            ```
            
            ---
            
            ## 3. Keyboard Toolbar Options
            Click **Edit Mode** below to start modifying this page! Use the context formatting toolbar above the keyboard to instantly add markdown syntax tags.
            """.trimIndent()

            val welcomeNote = Note(
                id = "$uuid:$samplePath",
                vaultId = uuid,
                filePath = samplePath,
                title = "Welcome Note",
                content = welcomeTemplate,
                syncStatus = "LOCAL_ONLY",
                updatedAt = System.currentTimeMillis()
            )
            noteDao.insertNote(welcomeNote)
            _selectedNote.value = welcomeNote
        }
    }

    fun deleteVault(vault: Vault) {
        viewModelScope.launch {
            vaultDao.deleteVault(vault)
            if (_activeVault.value?.id == vault.id) {
                _activeVault.value = null
                _selectedNote.value = null
            }
        }
    }

    // --- Note Management Commands ---

    fun createNote(title: String, customPath: String = "") {
        val vault = _activeVault.value ?: return
        viewModelScope.launch {
            val prefix = when (vault.vaultType) {
                "LOGSEQ" -> "pages/"
                else -> ""
            }

            val cleanTitle = title.trim().ifEmpty { "New Note" }
            val formattedFileName = cleanTitle.lowercase().replace(" ", "_").replace("/", "_") + ".md"
            val relativePath = customPath.trim().ifEmpty { "$prefix$formattedFileName" }

            val newNote = Note(
                id = "${vault.id}:$relativePath",
                vaultId = vault.id,
                filePath = relativePath,
                title = cleanTitle,
                content = "# $cleanTitle\n\nStart typing here...",
                syncStatus = "LOCAL_ONLY",
                updatedAt = System.currentTimeMillis()
            )
            noteDao.insertNote(newNote)
            _selectedNote.value = newNote
        }
    }

    fun updateSelectedNoteContent(newContent: String) {
        val activeNote = _selectedNote.value ?: return
        viewModelScope.launch {
            val updatedStatus = if (activeNote.syncStatus == "LOCAL_ONLY") "LOCAL_ONLY" else "MODIFIED"
            val revised = activeNote.copy(
                content = newContent,
                syncStatus = updatedStatus,
                updatedAt = System.currentTimeMillis()
            )
            noteDao.insertNote(revised)
            _selectedNote.value = revised
        }
    }

    fun deleteSelectedNote() {
        val note = _selectedNote.value ?: return
        val vault = _activeVault.value ?: return
        viewModelScope.launch {
            moveFileToTrashDisk(vault, note.filePath)
            noteDao.insertNote(note.copy(syncStatus = "TRASHED", updatedAt = System.currentTimeMillis()))
            _selectedNote.value = null
        }
    }

    fun deleteNote(note: Note) {
        val vault = _activeVault.value ?: return
        viewModelScope.launch {
            moveFileToTrashDisk(vault, note.filePath)
            noteDao.insertNote(note.copy(syncStatus = "TRASHED", updatedAt = System.currentTimeMillis()))
            if (_selectedNote.value?.id == note.id) {
                _selectedNote.value = null
            }
        }
    }

    // --- Git Integration & Synchronization Commands ---

    fun updateGitCredentials(username: String, token: String) {
        viewModelScope.launch {
            val success = syncEngine.testConnection(username, token)
            if (success) {
                gitUsername.value = securePrefs.getGitHubUsername()
                gitToken.value = token
                gitName.value = securePrefs.getGitHubName()
                gitEmail.value = securePrefs.getGitHubEmail()
                gitAvatarUrl.value = securePrefs.getGitHubAvatarUrl()
                isGitConfigured.value = true
            }
        }
    }

    fun clearGitCredentials() {
        securePrefs.clear()
        gitUsername.value = ""
        gitToken.value = ""
        gitName.value = ""
        gitEmail.value = ""
        gitAvatarUrl.value = ""
        isGitConfigured.value = false
    }

    val selectedTheme = MutableStateFlow(securePrefs.getSelectedTheme())

    fun selectTheme(theme: String) {
        securePrefs.saveSelectedTheme(theme)
        selectedTheme.value = theme
    }

    fun triggerSync() {
        val vault = _activeVault.value ?: return
        viewModelScope.launch {
            syncEngine.syncVault(vault.id)
        }
    }

    val isBackgroundSyncEnabled: StateFlow<Boolean> = com.example.data.GitSyncScheduler.observeSyncStatus(getApplication())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun toggleBackgroundSync(enabled: Boolean) {
        val app = getApplication<Application>()
        if (enabled) {
            com.example.data.GitSyncScheduler.schedulePeriodicSync(app)
        } else {
            com.example.data.GitSyncScheduler.cancelPeriodicSync(app)
        }
    }

    fun resolveMergeConflict(resolution: String, mergedText: String = "") {
        val note = _selectedNote.value ?: return
        viewModelScope.launch {
            syncEngine.resolveConflict(note.id, resolution, mergedText)
        }
    }

    private fun getVaultDirectory(vault: Vault): File {
        val baseDir = getApplication<Application>().getExternalFilesDir(null) ?: getApplication<Application>().filesDir
        val vaultDirName = vault.name.filter { it.isLetterOrDigit() || it == '_' || it == '-' }
        val vaultDir = File(baseDir, vaultDirName)
        if (!vaultDir.exists()) vaultDir.mkdirs()
        return vaultDir
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
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun restoreFileFromTrashDisk(vault: Vault, filePath: String) {
        try {
            val vaultDir = getVaultDirectory(vault)
            val trashFile = File(vaultDir, ".trash/$filePath")
            if (trashFile.exists()) {
                val destFile = File(vaultDir, filePath)
                destFile.parentFile?.mkdirs()
                trashFile.renameTo(destFile)
                if (!destFile.exists()) {
                    trashFile.copyTo(destFile, overwrite = true)
                    trashFile.delete()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun deleteFilePermanentlyDisk(vault: Vault, filePath: String) {
        try {
            val vaultDir = getVaultDirectory(vault)
            val sourceFile = File(vaultDir, filePath)
            if (sourceFile.exists()) sourceFile.delete()
            val trashFile = File(vaultDir, ".trash/$filePath")
            if (trashFile.exists()) trashFile.delete()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun restoreNote(note: Note) {
        val vault = _activeVault.value ?: return
        viewModelScope.launch {
            restoreFileFromTrashDisk(vault, note.filePath)
            val originalStatus = if (note.remoteSha.isEmpty()) "LOCAL_ONLY" else "MODIFIED"
            val restoredNote = note.copy(
                syncStatus = originalStatus,
                updatedAt = System.currentTimeMillis()
            )
            noteDao.insertNote(restoredNote)
        }
    }

    fun deleteNotePermanently(note: Note) {
        val vault = _activeVault.value ?: return
        viewModelScope.launch {
            deleteFilePermanentlyDisk(vault, note.filePath)
            if (note.remoteSha.isEmpty() || note.syncStatus == "LOCAL_ONLY") {
                noteDao.deleteNote(note)
            } else {
                noteDao.insertNote(note.copy(syncStatus = "DELETED", updatedAt = System.currentTimeMillis()))
            }
        }
    }
}

class VaultViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(VaultViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return VaultViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
