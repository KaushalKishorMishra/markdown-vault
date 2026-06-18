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
        val filtered = notes.filter { it.syncStatus != "DELETED" }
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

    // Selected Note State
    private val _selectedNote = MutableStateFlow<Note?>(null)
    val selectedNote: StateFlow<Note?> = _selectedNote

    // Edit vs Preview Mode
    private val _isEditMode = MutableStateFlow(false)
    val isEditMode: StateFlow<Boolean> = _isEditMode

    // Sync State
    val syncStatus: StateFlow<SyncState> = syncEngine.syncState

    init {
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
            val notesInFolder = allNotes.filter { it.filePath.startsWith(prefix) }
            
            notesInFolder.forEach { note ->
                if (note.syncStatus == "LOCAL_ONLY") {
                    noteDao.deleteNote(note)
                } else {
                    noteDao.insertNote(note.copy(syncStatus = "DELETED"))
                }
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
        viewModelScope.launch {
            if (note.syncStatus == "LOCAL_ONLY") {
                noteDao.deleteNote(note)
            } else {
                // Soft delete to let GitHub Sync handle remote removal
                noteDao.insertNote(note.copy(syncStatus = "DELETED"))
            }
            _selectedNote.value = null
        }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch {
            if (note.syncStatus == "LOCAL_ONLY") {
                noteDao.deleteNote(note)
            } else {
                noteDao.insertNote(note.copy(syncStatus = "DELETED"))
            }
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
