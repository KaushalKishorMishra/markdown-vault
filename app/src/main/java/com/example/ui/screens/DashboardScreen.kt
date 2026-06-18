package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.SyncState
import com.example.data.db.*
import com.example.ui.components.MarkdownPreview
import com.example.ui.components.MathView
import com.example.ui.theme.*
import com.example.ui.viewmodel.VaultViewModel
import kotlinx.coroutines.launch
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: VaultViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600

    val vaults by viewModel.allVaults.collectAsStateWithLifecycle()
    val activeVault by viewModel.activeVault.collectAsStateWithLifecycle()
    val notes by viewModel.notesForActiveVault.collectAsStateWithLifecycle()
    val selectedNote by viewModel.selectedNote.collectAsStateWithLifecycle()
    val isEditMode by viewModel.isEditMode.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val syncState by viewModel.syncStatus.collectAsStateWithLifecycle()

    val gitUser by viewModel.gitUsername.collectAsStateWithLifecycle()
    val gitToken by viewModel.gitToken.collectAsStateWithLifecycle()
    val isGitConfigured by viewModel.isGitConfigured.collectAsStateWithLifecycle()
    val isBackgroundSyncEnabled by viewModel.isBackgroundSyncEnabled.collectAsStateWithLifecycle()

    // Dialog state
    var showAddVaultDialog by remember { mutableStateOf(false) }
    var showAddNoteDialog by remember { mutableStateOf(false) }
    var showGitConfigDialog by remember { mutableStateOf(false) }
    var showVaultSettingsMenu by remember { mutableStateOf(false) }
    var showVaultManagerDialog by remember { mutableStateOf(false) }

    // Drawer state (for mobile)
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    // Main scaffold
    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = !isTablet,
        drawerContent = {
            if (!isTablet) {
                ModalDrawerSheet(
                    drawerContainerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.width(300.dp)
                ) {
                    SidebarContent(
                        vaults = vaults,
                        activeVault = activeVault,
                        notes = notes,
                        selectedNote = selectedNote,
                        searchQuery = searchQuery,
                        gitUser = gitUser,
                        isGitConfigured = isGitConfigured,
                        onVaultSelect = {
                            viewModel.selectVault(it)
                            coroutineScope.launch { drawerState.close() }
                        },
                        onNoteSelect = {
                            viewModel.selectNote(it)
                            coroutineScope.launch { drawerState.close() }
                        },
                        onAddVaultClick = { showAddVaultDialog = true },
                        onManageVaultsClick = { showVaultManagerDialog = true },
                        onAddNoteClick = { showAddNoteDialog = true },
                        onSearchChange = { viewModel.setSearchQuery(it) },
                        onGitConfigClick = { showGitConfigDialog = true }
                    )
                }
            }
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Adaptive Sidebar for Tablets
            if (isTablet) {
                Box(
                    modifier = Modifier
                        .width(320.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    SidebarContent(
                        vaults = vaults,
                        activeVault = activeVault,
                        notes = notes,
                        selectedNote = selectedNote,
                        searchQuery = searchQuery,
                        gitUser = gitUser,
                        isGitConfigured = isGitConfigured,
                        onVaultSelect = { viewModel.selectVault(it) },
                        onNoteSelect = { viewModel.selectNote(it) },
                        onAddVaultClick = { showAddVaultDialog = true },
                        onManageVaultsClick = { showVaultManagerDialog = true },
                        onAddNoteClick = { showAddNoteDialog = true },
                        onSearchChange = { viewModel.setSearchQuery(it) },
                        onGitConfigClick = { showGitConfigDialog = true }
                    )
                }
                Divider(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(1.dp),
                    color = MaterialTheme.colorScheme.outline
                )
            }

            // Main Editor & Workspace Section
            Scaffold(
                topBar = {
                    CenterAlignedTopAppBar(
                        title = {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = activeVault?.name ?: "No Vault Selected",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                if (activeVault != null) {
                                    Text(
                                        text = activeVault!!.gitRepo,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        },
                        navigationIcon = {
                            if (!isTablet) {
                                IconButton(onClick = { coroutineScope.launch { drawerState.open() } }) {
                                    Icon(imageVector = Icons.Default.Menu, contentDescription = "Open Sidebar")
                                }
                            }
                        },
                        actions = {
                            if (activeVault != null) {
                                // Sync indicator and trigger button
                                when (syncState) {
                                    is SyncState.Syncing -> {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                    }
                                    else -> {
                                        IconButton(onClick = { viewModel.triggerSync() }) {
                                            Icon(
                                                imageVector = Icons.Default.Refresh,
                                                contentDescription = "Sync Vault",
                                                tint = if (isGitConfigured) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                                Box {
                                    IconButton(onClick = { showVaultSettingsMenu = true }) {
                                        Icon(imageVector = Icons.Default.MoreVert, contentDescription = "Vault Options")
                                    }
                                    DropdownMenu(
                                        expanded = showVaultSettingsMenu,
                                        onDismissRequest = { showVaultSettingsMenu = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Delete This Vault") },
                                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                            onClick = {
                                                activeVault?.let { viewModel.deleteVault(it) }
                                                showVaultSettingsMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            titleContentColor = MaterialTheme.colorScheme.onSurface,
                            navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                            actionIconContentColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (selectedNote == null) {
                        EmptyWorkspaceArea(
                            activeVault = activeVault,
                            notesCount = notes.size,
                            onAddNoteClick = { showAddNoteDialog = true },
                            onGitConfigClick = { showGitConfigDialog = true },
                            isGitConfigured = isGitConfigured
                        )
                    } else {
                        NoteWorkspace(
                            note = selectedNote!!,
                            isEditMode = isEditMode,
                            isTabletLayout = isTablet,
                            syncState = syncState,
                            onContentChange = { viewModel.updateSelectedNoteContent(it) },
                            onEditToggle = { viewModel.setEditMode(it) },
                            onDeleteClick = { viewModel.deleteSelectedNote() },
                            onResolveConflict = { resolution, mergedText ->
                                viewModel.resolveMergeConflict(resolution, mergedText)
                            }
                        )
                    }
                }
            }
        }
    }

    // --- Dialogs & Overlays ---

    if (showAddVaultDialog) {
        AddVaultDialog(
            onDismiss = { showAddVaultDialog = false },
            onConfirm = { name, repo, branch, style ->
                viewModel.createVault(name, repo, branch, style)
                showAddVaultDialog = false
            }
        )
    }

    if (showAddNoteDialog) {
        val existingFolders = remember(notes) {
            notes.map { note ->
                if (note.filePath.contains("/")) note.filePath.substringBeforeLast("/") else ""
            }.filter { it.isNotEmpty() }.distinct().sorted()
        }
        val defaultPrefix = when (activeVault?.vaultType) {
            "LOGSEQ" -> "pages/"
            else -> ""
        }
        AddNoteDialog(
            existingFolders = existingFolders,
            defaultPrefix = defaultPrefix,
            onDismiss = { showAddNoteDialog = false },
            onConfirm = { title, path ->
                viewModel.createNote(title, path)
                showAddNoteDialog = false
            }
        )
    }

    if (showGitConfigDialog) {
        val gitName by viewModel.gitName.collectAsStateWithLifecycle()
        val gitEmail by viewModel.gitEmail.collectAsStateWithLifecycle()
        val gitAvatarUrl by viewModel.gitAvatarUrl.collectAsStateWithLifecycle()
        val selectedTheme by viewModel.selectedTheme.collectAsStateWithLifecycle()

        GitCredentialsDialog(
            currentUsername = gitUser,
            currentToken = gitToken,
            currentName = gitName,
            currentEmail = gitEmail,
            currentAvatarUrl = gitAvatarUrl,
            isBackgroundSyncEnabled = isBackgroundSyncEnabled,
            onToggleBackgroundSync = { enabled ->
                viewModel.toggleBackgroundSync(enabled)
            },
            selectedTheme = selectedTheme,
            onSelectTheme = { theme ->
                viewModel.selectTheme(theme)
            },
            onDismiss = { showGitConfigDialog = false },
            onConfirm = { user, token ->
                viewModel.updateGitCredentials(user, token)
                showGitConfigDialog = false
            },
            onClear = {
                viewModel.clearGitCredentials()
                showGitConfigDialog = false
            },
            onCheckStatus = { user, token ->
                viewModel.syncEngine.testConnection(user, token)
            }
        )
    }

    if (showVaultManagerDialog) {
        VaultManagerDialog(
            vaults = vaults,
            activeVault = activeVault,
            onDismiss = { showVaultManagerDialog = false },
            onSelect = { v ->
                viewModel.selectVault(v)
                showVaultManagerDialog = false
            },
            onDelete = { v ->
                viewModel.deleteVault(v)
            },
            onAddNewVaultSpace = {
                showVaultManagerDialog = false
                showAddVaultDialog = true
            }
        )
    }
}

// --- Inner Sidebar Section ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SidebarContent(
    vaults: List<Vault>,
    activeVault: Vault?,
    notes: List<Note>,
    selectedNote: Note?,
    searchQuery: String,
    gitUser: String,
    isGitConfigured: Boolean,
    onVaultSelect: (Vault) -> Unit,
    onNoteSelect: (Note) -> Unit,
    onAddVaultClick: () -> Unit,
    onManageVaultsClick: () -> Unit,
    onAddNoteClick: () -> Unit,
    onSearchChange: (String) -> Unit,
    onGitConfigClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isVaultDropdownExpanded by remember { mutableStateOf(false) }

    val groupedNotes = remember(notes) {
        notes.groupBy { note ->
            if (note.filePath.contains("/")) {
                note.filePath.substringBeforeLast("/")
            } else {
                ""
            }
        }
    }
    var expandedFolders by remember { mutableStateOf(setOf<String>()) }
    LaunchedEffect(groupedNotes) {
        expandedFolders = groupedNotes.keys.toSet()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp)
    ) {
        // App Logo Icon & Branding
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 18.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "GitVault",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    letterSpacing = (-0.5).sp
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(Emerald500)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "AES-256 PROTECTED",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Emerald500.copy(alpha = 0.9f),
                        letterSpacing = 1.sp
                    )
                }
            }
        }

        // Active Vaults Section Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Active Vaults".uppercase(),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                letterSpacing = 1.sp
            )
            Text(
                text = "Manage",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { onManageVaultsClick() }
            )
        }

        // Horizontal Vault Cards Row & Selection Menu
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            DropdownMenu(
                expanded = isVaultDropdownExpanded,
                onDismissRequest = { isVaultDropdownExpanded = false },
                modifier = Modifier.fillMaxWidth(0.8f)
            ) {
                if (vaults.isEmpty()) {
                    DropdownMenuItem(
                        text = { Text("No vaults found", fontStyle = androidx.compose.ui.text.font.FontStyle.Italic) },
                        onClick = {}
                    )
                } else {
                    vaults.forEach { v ->
                        DropdownMenuItem(
                            text = { Text(v.name) },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (v.id == activeVault?.id) Icons.Default.CheckCircle else Icons.Default.List,
                                    contentDescription = null,
                                    tint = if (v.id == activeVault?.id) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            onClick = {
                                onVaultSelect(v)
                                isVaultDropdownExpanded = false
                            }
                        )
                    }
                }
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    thickness = 1.dp
                )
                DropdownMenuItem(
                    text = { Text("Create New Vault", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary) },
                    leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    onClick = {
                        onAddVaultClick()
                        isVaultDropdownExpanded = false
                    }
                )
            }

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(vaults) { v ->
                    val isActive = v.id == activeVault?.id
                    Box(
                        modifier = Modifier
                            .width(148.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .border(
                                width = if (isActive) 1.5.dp else 1.dp,
                                color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(20.dp)
                            )
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { onVaultSelect(v) }
                            .padding(14.dp)
                            .then(
                                if (!isActive) Modifier.alpha(0.7f) else Modifier
                            )
                    ) {
                        Column {
                            // Icon Box
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isActive) MaterialTheme.colorScheme.primaryContainer 
                                        else MaterialTheme.colorScheme.surface
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                val icon = when (v.vaultType) {
                                    "LOGSEQ" -> Icons.Default.List
                                    "OBSIDIAN" -> Icons.Default.Lock
                                    else -> Icons.Default.Edit
                                }
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = v.name,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            val cleanDisplayRepo = v.gitRepo.substringAfter("github.com/")
                            Text(
                                text = if (v.vaultType == "LOGSEQ") "Logseq • $cleanDisplayRepo" else if (v.vaultType == "OBSIDIAN") "Obsidian • $cleanDisplayRepo" else "Flat • $cleanDisplayRepo",
                                fontSize = 9.sp,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Append an elegant placeholder card to quickly add more vault spaces
                item {
                    Box(
                        modifier = Modifier
                            .width(110.dp)
                            .height(98.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(20.dp)
                            )
                            .background(Color.Transparent)
                            .clickable { onAddVaultClick() }
                            .padding(14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "New Space",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        if (activeVault != null) {
            // Search Box and Note Count
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("Search workspace...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                maxLines = 1,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            )

            // Header for notes matching search, plus a New Note button
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp, horizontal = 2.dp)
            ) {
                Text(
                    text = "VAULT FILES (${notes.size})",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
                IconButton(
                    onClick = onAddNoteClick,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AddCircle,
                        contentDescription = "Create Note",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // List of Notes
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f)
            ) {
                if (notes.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (searchQuery.isNotEmpty()) "No matching notes found" else "Vault is empty. Create a file!",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }
                } else {
                    groupedNotes.forEach { (folderPath, folderNotes) ->
                        if (folderPath.isNotEmpty()) {
                            val isExpanded = expandedFolders.contains(folderPath)
                            item(key = "dir:$folderPath") {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            expandedFolders = if (isExpanded) {
                                                expandedFolders - folderPath
                                            } else {
                                                expandedFolders + folderPath
                                            }
                                        }
                                        .padding(vertical = 6.dp, horizontal = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.Folder,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = folderPath,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.weight(1f))
                                    Text(
                                        text = "${folderNotes.size}",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                        modifier = Modifier.padding(end = 4.dp)
                                    )
                                }
                            }
                            if (isExpanded) {
                                items(folderNotes, key = { it.id }) { note ->
                                    Row(modifier = Modifier.fillMaxWidth()) {
                                        Spacer(modifier = Modifier.width(20.dp)) // Indent files inside folder
                                        val isSelected = selectedNote?.id == note.id
                                        val syncColor = when (note.syncStatus) {
                                            "SYNCED" -> Emerald500
                                            "MODIFIED" -> Sky400
                                            "CONFLICT" -> Amber500
                                            "LOCAL_ONLY" -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                        Surface(
                                            onClick = { onNoteSelect(note) },
                                            shape = RoundedCornerShape(14.dp),
                                            color = if (isSelected) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent,
                                            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline) else null,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(7.dp)
                                                        .clip(RoundedCornerShape(3.dp))
                                                        .background(syncColor)
                                                )
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = note.title,
                                                        fontSize = 13.sp,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = note.filePath.substringAfterLast("/"),
                                                        fontSize = 10.sp,
                                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            // Root files
                            items(folderNotes, key = { it.id }) { note ->
                                val isSelected = selectedNote?.id == note.id
                                val syncColor = when (note.syncStatus) {
                                    "SYNCED" -> Emerald500
                                    "MODIFIED" -> Sky400
                                    "CONFLICT" -> Amber500
                                    "LOCAL_ONLY" -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                                Surface(
                                    onClick = { onNoteSelect(note) },
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent,
                                    border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline) else null,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(7.dp)
                                                .clip(RoundedCornerShape(3.dp))
                                                .background(syncColor)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = note.title,
                                                fontSize = 13.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = note.filePath,
                                                fontSize = 10.sp,
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Empty state for sidebar
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Create or select a vault\nto begin note-taking.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }

        Divider(modifier = Modifier.padding(vertical = 12.dp))

        // Secure GitHub Credentials Status Footer
        Surface(
            onClick = onGitConfigClick,
            shape = RoundedCornerShape(12.dp),
            color = if (isGitConfigured) Emerald500.copy(alpha = 0.12f) else MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(10.dp)
            ) {
                Icon(
                    imageVector = if (isGitConfigured) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (isGitConfigured) Emerald500 else MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isGitConfigured) "GitHub Authorized" else "GitHub Authorization Pending",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isGitConfigured) Emerald500 else MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = if (isGitConfigured) "@$gitUser" else "Tap here to configure PAT",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

// --- Empty Area Placeholder for main area ---

@Composable
fun EmptyWorkspaceArea(
    activeVault: Vault?,
    notesCount: Int,
    onAddNoteClick: () -> Unit,
    onGitConfigClick: () -> Unit,
    isGitConfigured: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.widthIn(max = 420.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .border(
                        width = 1.5.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            androidx.compose.ui.graphics.Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.secondary
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Welcome to Markdown Vault",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (activeVault == null) {
                    "Get started by creating a new vault cabinet. You can map it to any GitHub repository to back up your notes."
                } else if (notesCount == 0) {
                    "This vault is currently empty. Tap the button below to add your very first Markdown document!"
                } else {
                    "Select a Markdown file from your vault navigation sidebar to start reading, writing, typesetting math, or drawing diagrams."
                },
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (activeVault != null && notesCount == 0) {
                Button(
                    onClick = onAddNoteClick,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add First Note")
                }
            } else if (!isGitConfigured) {
                OutlinedButton(
                    onClick = onGitConfigClick,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Authorize GitHub Repo Sync")
                }
            }
        }
    }
}

// --- Note Workspace Layout with optional side-by-side split screen preview ---

@Composable
fun NoteWorkspace(
    note: Note,
    isEditMode: Boolean,
    isTabletLayout: Boolean,
    syncState: SyncState,
    onContentChange: (String) -> Unit,
    onEditToggle: (Boolean) -> Unit,
    onDeleteClick: () -> Unit,
    onResolveConflict: (String, String) -> Unit
) {
    if (note.syncStatus == "CONFLICT") {
        // Show Conflict Resolution Overlay
        ConflictResolver(
            note = note,
            onResolve = { resolution, mergedText ->
                onResolveConflict(resolution, mergedText)
            }
        )
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            // Note Top Command Bar
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                // File info + status
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val statusText = when (note.syncStatus) {
                        "SYNCED" -> "Synced"
                        "MODIFIED" -> "Modified locally"
                        "LOCAL_ONLY" -> "Local only"
                        else -> note.syncStatus
                    }
                    val statusColor = when (note.syncStatus) {
                        "SYNCED" -> Emerald500
                        "MODIFIED" -> Sky400
                        "LOCAL_ONLY" -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        else -> MaterialTheme.colorScheme.onSurface
                    }

                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(statusColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "$statusText • ${note.filePath}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Actions (Delete)
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Note",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Mode Toggle (Only show on Phone)
                    if (!isTabletLayout) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surface)
                        ) {
                            Text(
                                text = "Preview",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (!isEditMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (!isEditMode) MaterialTheme.colorScheme.primary else Color.Transparent)
                                    .clickable { onEditToggle(false) }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                            Text(
                                text = "Edit",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isEditMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isEditMode) MaterialTheme.colorScheme.primary else Color.Transparent)
                                    .clickable { onEditToggle(true) }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    } else {
                        // On Tablet, it's always in side-by-side mode! Display informative notice
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "Side-by-Side Dual Deck Mode",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // Workspace Panels Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                // If Tablet: side-by-side. Else: swap based on isEditMode
                if (isTabletLayout) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        // Left: Editor
                        Box(modifier = Modifier.weight(1f)) {
                            MarkdownEditorArea(
                                content = note.content,
                                onContentChange = onContentChange,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Divider(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(1.dp),
                            color = MaterialTheme.colorScheme.outline
                        )
                        // Right: Live html Preview
                        Box(modifier = Modifier.weight(1f)) {
                            MarkdownPreview(markdownString = note.content)
                        }
                    }
                } else {
                    if (isEditMode) {
                        MarkdownEditorArea(
                            content = note.content,
                            onContentChange = onContentChange,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        MarkdownPreview(markdownString = note.content)
                    }
                }
            }
        }
    }
}
 // --- LaTeX Equation Structures and Parser Helper ---

data class MathEquation(
    val latex: String,
    val isBlock: Boolean,
    val startChar: Int,
    val endChar: Int
)

fun parseEquationsInText(text: String): List<MathEquation> {
    if (text.isEmpty()) return emptyList()
    val list = mutableListOf<MathEquation>()
    
    // 1. Find block math: $$ ... $$
    val blockRegex = Regex("""\$\$(.*?)\$\$""", RegexOption.DOT_MATCHES_ALL)
    blockRegex.findAll(text).forEach { match ->
        val range = match.range
        val latex = match.groupValues[1].trim()
        if (latex.isNotEmpty()) {
            list.add(MathEquation(latex, true, range.first, range.last + 1))
        }
    }
    
    // 2. Find inline math: $ ... $ (avoiding overlapping with blocks)
    var i = 0
    while (i < text.length) {
        if (text[i] == '$') {
            if (i + 1 < text.length && text[i + 1] == '$') {
                // Skip block math
                val nextBlock = text.indexOf("$$", i + 2)
                if (nextBlock != -1) {
                    i = nextBlock + 2
                } else {
                    i += 2
                }
                continue
            } else {
                val nextInline = text.indexOf('$', i + 1)
                if (nextInline != -1) {
                    val content = text.substring(i + 1, nextInline).trim()
                    // Check that it's single line (GFM standard inline math is single line)
                    if (content.isNotEmpty() && !content.contains('\n')) {
                        // Ensure this range does not overlap with any already parsed block math
                        val start = i
                        val end = nextInline + 1
                        val overlaps = list.any { start >= it.startChar && end <= it.endChar }
                        if (!overlaps) {
                            list.add(MathEquation(content, false, start, end))
                        }
                    }
                    i = nextInline + 1
                    continue
                }
            }
        }
        i++
    }
    
    return list.sortedBy { it.startChar }
}

// --- Mermaid Block Structures and Parser Helper ---

data class MermaidBlock(
    val rawBlock: String,
    val code: String,
    val startChar: Int,
    val endChar: Int
)

fun parseMermaidBlocksInText(text: String): List<MermaidBlock> {
    if (text.isEmpty()) return emptyList()
    val list = mutableListOf<MermaidBlock>()
    
    val regex = Regex("""```mermaid\s*([\s\S]*?)```""", RegexOption.IGNORE_CASE)
    regex.findAll(text).forEach { match ->
        val range = match.range
        val code = match.groupValues[1].trim()
        val raw = match.value
        list.add(MermaidBlock(raw, code, range.first, range.last + 1))
    }
    
    return list.sortedBy { it.startChar }
}

// --- Content Editor Component with Markdown toolbar ---

@Composable
fun MarkdownEditorArea(
    content: String,
    onContentChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var textFieldValueState by remember(content) {
        val sel = content.length
        mutableStateOf(TextFieldValue(text = content, selection = androidx.compose.ui.text.TextRange(sel)))
    }

    var isAssistantPanelExpanded by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableStateOf(0) } // 0: LaTeX Math, 1: Mermaid Diagrams
    
    val cursorIndex = textFieldValueState.selection.start
    
    // 1. Math State
    val equations = remember(content) { parseEquationsInText(content) }
    val activeEquation = remember(equations, cursorIndex) {
        equations.find { cursorIndex >= it.startChar && cursorIndex <= it.endChar }
    }
    val displayedEquation = activeEquation ?: equations.firstOrNull()

    val mathTemplates = listOf(
        Pair("Quadratic Formula", "x = \\frac{-b \\pm \\sqrt{b^2 - 4ac}}{2a}"),
        Pair("Euler's Identity", "e^{i\\pi} + 1 = 0"),
        Pair("Pythagoras", "a^2 + b^2 = c^2"),
        Pair("Normal Distribution", "f(x) = \\frac{1}{\\sigma\\sqrt{2\\pi}} e^{-\\frac{1}{2}\\left(\\frac{x-\\mu}{\\sigma\\right)^2}"),
        Pair("Fourier Transform", "\\hat{f}(\\xi) = \\int_{-\\infty}^{\\infty} f(x) e^{-2\\pi i x \\xi} dx"),
        Pair("Maxwell's Eq", "\\nabla \\cdot \\mathbf{E} = \\frac{\\rho}{\\varepsilon_0}")
    )

    // 2. Mermaid State
    val mermaidBlocks = remember(content) { parseMermaidBlocksInText(content) }
    val activeMermaid = remember(mermaidBlocks, cursorIndex) {
        mermaidBlocks.find { cursorIndex >= it.startChar && cursorIndex <= it.endChar }
    }
    val displayedMermaid = activeMermaid ?: mermaidBlocks.firstOrNull()

    val mermaidTemplates = listOf(
        Pair("Flowchart", "graph TD\n    A[Start] --> B(Verify Connection)\n    B -->|Yes| C[Auto Git Sync]\n    B -->|No| D[Queue Changes]"),
        Pair("Sequence", "sequenceDiagram\n    participant App as Mobile App\n    participant Git as GitHub API\n    App->>Git: Push local commits\n    Git-->>App: Sync successful feedback"),
        Pair("State", "stateDiagram-v2\n    [*] --> Offline\n    Offline --> Syncing : Connection Detected\n    Syncing --> Online : Complete\n    Online --> Offline : Connection Lost"),
        Pair("Gantt Chart", "gantt\n    title Git Sync Roadmap\n    section Dev\n    Design Core   :a1, 2026-06-15, 5d\n    Background Sync:after a1, 7d"),
        Pair("User Journey", "journey\n    title Git Sync App Journey\n    section Access\n      Configure Credentials: 5: User, Dev\n      Clone Repository: 4: User")
    )

    Column(modifier = modifier) {
        // Appending formatting tags toolbar helper
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            val helpers = listOf(
                Pair("B", "**"),
                Pair("I", "*"),
                Pair("Header", "## "),
                Pair("Math Inline", "$"),
                Pair("Math Block", "$$"),
                Pair("Mermaid", "```mermaid\n")
            )

            LazyRowForHelper(helpers) { symbol ->
                val txt = textFieldValueState.text
                val selection = textFieldValueState.selection
                val start = selection.start
                val end = selection.end
                
                val inserted = if (symbol == "```mermaid\n") {
                    txt.substring(0, start) + "\n```mermaid\ngraph TD\n    A[Start] --> B[Goal];\n```\n" + txt.substring(end)
                } else if (symbol.contains("#")) {
                    txt.substring(0, start) + "\n$symbol" + txt.substring(end)
                } else {
                    txt.substring(0, start) + "$symbol$symbol" + txt.substring(end)
                }
                
                onContentChange(inserted)
            }
        }

        TextField(
            value = textFieldValueState,
            onValueChange = {
                textFieldValueState = it
                onContentChange(it.text)
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = ImeAction.Default
            ),
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            ),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )

        // Integrated LaTeX Math & Mermaid Diagram Multi-Tab Composer Assistant Panel
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
                .border(
                    width = 1.dp,
                    color = if (activeEquation != null || activeMermaid != null) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp)
                ),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
            )
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                // Header of Drawer Panel
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f).clickable { isAssistantPanelExpanded = !isAssistantPanelExpanded }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (activeEquation != null || activeMermaid != null) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                    else MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (selectedTab == 0) "∑" else "📊",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = if (activeEquation != null || activeMermaid != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (selectedTab == 0) "LaTeX Mathematical Editor" else "Mermaid Diagram Composer",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Compact Segmented Tab Switcher (only show if panel is expanded)
                    if (isAssistantPanelExpanded) {
                        Row(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                                .padding(2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (selectedTab == 0) MaterialTheme.colorScheme.primary else Color.Transparent)
                                    .clickable { selectedTab = 0 }
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "Math",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedTab == 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (selectedTab == 1) MaterialTheme.colorScheme.primary else Color.Transparent)
                                    .clickable { selectedTab = 1 }
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "Mermaid",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedTab == 1) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    Icon(
                        imageVector = if (isAssistantPanelExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                        contentDescription = "Toggle Panel",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(20.dp)
                            .clickable { isAssistantPanelExpanded = !isAssistantPanelExpanded }
                    )
                }

                if (isAssistantPanelExpanded) {
                    Spacer(modifier = Modifier.height(10.dp))

                    if (selectedTab == 0) {
                        // --- TAB 1: LATEX MATH ---
                        if (displayedEquation != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(95.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            ) {
                                MathView(
                                    latex = displayedEquation.latex,
                                    isBlock = displayedEquation.isBlock,
                                    modifier = Modifier.fillMaxSize(),
                                    backgroundColor = Color.Transparent,
                                    textColor = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(6.dp))
                            
                            Text(
                                text = if (displayedEquation.isBlock) "$$" + displayedEquation.latex + "$$"
                                       else "$" + displayedEquation.latex + "$",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                        } else {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "No math equations detected at current text position. Select a quick mathematical template to insert at cursor:",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                
                                androidx.compose.foundation.lazy.LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    contentPadding = PaddingValues(bottom = 2.dp)
                                ) {
                                    items(mathTemplates) { item ->
                                        val title = item.first
                                        val latexBody = item.second
                                        Surface(
                                            onClick = {
                                                val txt = textFieldValueState.text
                                                val selection = textFieldValueState.selection
                                                val start = selection.start
                                                val end = selection.end
                                                val tag = if (title == "Pythagoras") "$" else "$$"
                                                val inserted = txt.substring(0, start) + "\n$tag\n$latexBody\n$tag\n" + txt.substring(end)
                                                onContentChange(inserted)
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                            border = androidx.compose.foundation.BorderStroke(
                                                width = 1.dp,
                                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                            )
                                        ) {
                                            Text(
                                                text = "+ $title",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // --- TAB 2: MERMAID DIAGRAMS ---
                        if (displayedMermaid != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(130.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            ) {
                                // Render using MarkdownPreview because it is already optimized to parse and render mermaid blocks beautifully!
                                MarkdownPreview(
                                    markdownString = "```mermaid\n" + displayedMermaid.code + "\n```",
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(6.dp))
                            
                            Text(
                                text = "Active Mermaid Code under cursor (" + displayedMermaid.code.substringBefore("\n") + " ...)",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.secondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                        } else {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "No Mermaid codeblock detected. Select a dynamic flowchart, sequence, or timeline template to insert at cursor:",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                
                                androidx.compose.foundation.lazy.LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    contentPadding = PaddingValues(bottom = 2.dp)
                                ) {
                                    items(mermaidTemplates) { item ->
                                        val title = item.first
                                        val codeBody = item.second
                                        Surface(
                                            onClick = {
                                                val txt = textFieldValueState.text
                                                val selection = textFieldValueState.selection
                                                val start = selection.start
                                                val end = selection.end
                                                val inserted = txt.substring(0, start) + "\n```mermaid\n$codeBody\n```\n" + txt.substring(end)
                                                onContentChange(inserted)
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f),
                                            border = androidx.compose.foundation.BorderStroke(
                                                width = 1.dp,
                                                color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)
                                            )
                                        ) {
                                            Text(
                                                text = "+ $title Diagram",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LazyRowForHelper(
    items: List<Pair<String, String>>,
    onClick: (String) -> Unit
) {
    androidx.compose.foundation.lazy.LazyRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
    ) {
        items(items) { helper ->
            Surface(
                onClick = { onClick(helper.second) },
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 2.dp
            ) {
                Text(
                    text = helper.first,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }
    }
}

// --- Side-by-Side Merge Conflict Resolver UI ---

@Composable
fun ConflictResolver(
    note: Note,
    onResolve: (String, String) -> Unit
) {
    var editMergedText by remember { mutableStateOf(note.content) }

    Card(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, Amber500.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Amber500,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Merge Conflict Detected",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "This file has been edited on GitHub and locally. Choose resolution:",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Options Selector Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { onResolve("LOCAL", "") },
                    colors = ButtonDefaults.buttonColors(containerColor = Sky500),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Keep My Local", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = { onResolve("REMOTE", "") },
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald500),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Keep Remote Git", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Divider()
            Spacer(modifier = Modifier.height(12.dp))

            // Interactive Composing Merge section
            Text(
                text = "Alternative: Compose Combined Resolution Merge below:",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            OutlinedTextField(
                value = editMergedText,
                onValueChange = { editMergedText = it },
                textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedContainerColor = MaterialTheme.colorScheme.background
                ),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = { onResolve("MERGED", editMergedText) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Accept Composited Merge & Sync", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// --- Credentials Dialog ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GitCredentialsDialog(
    currentUsername: String,
    currentToken: String,
    currentName: String,
    currentEmail: String,
    currentAvatarUrl: String,
    isBackgroundSyncEnabled: Boolean,
    onToggleBackgroundSync: (Boolean) -> Unit,
    selectedTheme: String,
    onSelectTheme: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit,
    onClear: () -> Unit,
    onCheckStatus: suspend (String, String) -> Boolean
) {
    var user by remember { mutableStateOf(currentUsername) }
    var token by remember { mutableStateOf(currentToken) }

    val coroutineScope = rememberCoroutineScope()
    var connectionStatus by remember { mutableStateOf(if (currentToken.isNotEmpty()) "Configured" else "Unconfigured") }
    var isChecking by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(10.dp))
                Text("Workspace & Git Settings")
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // User Profile Section
                if (currentToken.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(12.dp)
                    ) {
                        if (currentAvatarUrl.isNotEmpty()) {
                            AsyncImage(
                                model = currentAvatarUrl,
                                contentDescription = "GitHub Avatar",
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(androidx.compose.foundation.shape.CircleShape)
                                    .border(2.dp, MaterialTheme.colorScheme.primary, androidx.compose.foundation.shape.CircleShape)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(androidx.compose.foundation.shape.CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                val initials = (currentName.ifEmpty { currentUsername }).take(2).uppercase()
                                Text(
                                    text = initials,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = currentName.ifEmpty { currentUsername },
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (currentEmail.isNotEmpty()) {
                                Text(
                                    text = currentEmail,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "@$currentUsername",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Theme Selection Section
                Text(
                    text = "Color Themes".uppercase(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    letterSpacing = 1.sp
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val themes = listOf(
                        "ARTISTIC" to "Artistic Dark",
                        "CYBERPUNK" to "Neon Cyber",
                        "EMERALD" to "Sage Forest",
                        "CLASSIC" to "Warm Amber",
                        "LIGHT" to "Lavender Light"
                    )
                    items(themes) { (themeKey, themeLabel) ->
                        val isSelected = selectedTheme == themeKey
                        FilterChip(
                            selected = isSelected,
                            onClick = { onSelectTheme(themeKey) },
                            label = { Text(themeLabel, fontSize = 10.sp) },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                // GitHub Credentials Section
                Text(
                    text = "GitHub Authentication".uppercase(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    letterSpacing = 1.sp
                )
                OutlinedTextField(
                    value = user,
                    onValueChange = { user = it },
                    label = { Text("GitHub Username") },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = token,
                    onValueChange = { token = it },
                    label = { Text("Personal Access Token (PAT)") },
                    visualTransformation = PasswordVisualTransformation(),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                isChecking = true
                                val success = onCheckStatus(user, token)
                                connectionStatus = if (success) "Active & Valid" else "Connection Failed"
                                isChecking = false
                            }
                        },
                        enabled = !isChecking && user.isNotEmpty() && token.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        if (isChecking) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 1.5.dp)
                        } else {
                            Text("Check Connection", fontSize = 11.sp)
                        }
                    }

                    // Connection status badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                when (connectionStatus) {
                                    "Active & Valid" -> Emerald500.copy(alpha = 0.2f)
                                    "Connection Failed" -> MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
                                    else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                }
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = connectionStatus.uppercase(),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = when (connectionStatus) {
                                "Active & Valid" -> Emerald500
                                "Connection Failed" -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                // Settings Switch Section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Automatic Background Sync",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Auto-sync vaults periodically on connection",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isBackgroundSyncEnabled,
                        onCheckedChange = onToggleBackgroundSync
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(user, token) },
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            Row {
                if (currentToken.isNotEmpty()) {
                    TextButton(
                        onClick = onClear,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Remove Auth")
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            }
        }
    )
}

// --- Add Vault Dialog ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddVaultDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var repo by remember { mutableStateOf("") }
    var branch by remember { mutableStateOf("main") }
    var style by remember { mutableStateOf("OBSIDIAN") } // "OBSIDIAN", "LOGSEQ", "BASIC"
    var expandedStyleMenu by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create & Connect Note Vault") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Workspace Name") },
                    placeholder = { Text("e.g. Personal Notes") },
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
                )
                OutlinedTextField(
                    value = repo,
                    onValueChange = { repo = it },
                    label = { Text("GitHub Repo (owner/repo)") },
                    placeholder = { Text("e.g. kaushalkmishra/notes-vault") },
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
                )
                OutlinedTextField(
                    value = branch,
                    onValueChange = { branch = it },
                    label = { Text("Default Sync Branch (defaults: main)") },
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
                )

                // Structure Selection Expose
                Box {
                    ExposedDropdownMenuBox(
                        expanded = expandedStyleMenu,
                        onExpandedChange = { expandedStyleMenu = !expandedStyleMenu }
                    ) {
                        OutlinedTextField(
                            value = style,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Subfolder Structured Layout") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedStyleMenu) },
                            modifier = Modifier.menuAnchor(),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
                        )
                        ExposedDropdownMenu(
                            expanded = expandedStyleMenu,
                            onDismissRequest = { expandedStyleMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Obsidian-style (Nested Standard Folder)") },
                                onClick = {
                                    style = "OBSIDIAN"
                                    expandedStyleMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Logseq-style (/pages & /journals folders)") },
                                onClick = {
                                    style = "LOGSEQ"
                                    expandedStyleMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Flat Basic List (flat folders)") },
                                onClick = {
                                    style = "BASIC"
                                    expandedStyleMenu = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotEmpty() && repo.isNotEmpty()) onConfirm(name, repo, branch, style) },
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Create Workspace")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// --- Add Note Dialog ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddNoteDialog(
    existingFolders: List<String>,
    defaultPrefix: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedFolder by remember { mutableStateOf(defaultPrefix) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Note to Vault") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Note Title") },
                    placeholder = { Text("e.g. Project Specs") },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = selectedFolder,
                    onValueChange = { selectedFolder = it },
                    label = { Text("Folder Location") },
                    placeholder = { Text("e.g. pages/ or logs/weekly/") },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                if (existingFolders.isNotEmpty()) {
                    Text(
                        text = "Existing Folders:".uppercase(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        letterSpacing = 0.5.sp
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(existingFolders) { folder ->
                            val cleanFolder = if (folder.endsWith("/")) folder else "$folder/"
                            FilterChip(
                                selected = selectedFolder == cleanFolder,
                                onClick = { selectedFolder = cleanFolder },
                                label = { Text(cleanFolder, fontSize = 10.sp) },
                                shape = RoundedCornerShape(6.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotEmpty()) {
                        val finalFolder = if (selectedFolder.isNotEmpty() && !selectedFolder.endsWith("/")) "$selectedFolder/" else selectedFolder
                        val formattedFileName = title.trim().lowercase().replace(" ", "_").replace("/", "_") + ".md"
                        val finalPath = "$finalFolder$formattedFileName"
                        onConfirm(title, finalPath)
                    }
                },
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Create File")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// --- Vault & Repository Manager Dialog ---

@Composable
fun VaultManagerDialog(
    vaults: List<Vault>,
    activeVault: Vault?,
    onDismiss: () -> Unit,
    onSelect: (Vault) -> Unit,
    onDelete: (Vault) -> Unit,
    onAddNewVaultSpace: () -> Unit
) {
    var vaultToDeleteId by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Vault Workspace Manager",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Add, list, and switch between local Git vaults",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
            ) {
                if (vaults.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No local note vaults connected.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(vaults) { v ->
                            val isActive = v.id == activeVault?.id
                            val isConfirmingDelete = vaultToDeleteId == v.id

                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                ),
                                border = if (isActive) androidx.compose.foundation.BorderStroke(
                                    width = 1.5.dp,
                                    color = MaterialTheme.colorScheme.primary
                                ) else androidx.compose.foundation.BorderStroke(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        // Left brand indicator
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(
                                                    if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                                    else MaterialTheme.colorScheme.surfaceVariant
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            val icon = when (v.vaultType) {
                                                "LOGSEQ" -> Icons.Default.List
                                                "OBSIDIAN" -> Icons.Default.Lock
                                                else -> Icons.Default.Edit
                                            }
                                            Icon(
                                                imageVector = icon,
                                                contentDescription = null,
                                                tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = v.name,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "git: " + v.gitRepo + " (" + v.branch + ")",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        if (isActive) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(Emerald500.copy(alpha = 0.15f))
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    text = "Active",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Emerald500
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Tags for Vault Format Type
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val typeDescription = when(v.vaultType) {
                                            "OBSIDIAN" -> "Obsidian Subfolders"
                                            "LOGSEQ" -> "Logseq Structure"
                                            else -> "Flat Book Structure"
                                        }
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = typeDescription,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    if (isConfirmingDelete) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Confirm delete? Cached files will be erased.",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Row {
                                                TextButton(
                                                    onClick = { vaultToDeleteId = null }
                                                ) {
                                                    Text("Cancel", fontSize = 11.sp)
                                                }
                                                Button(
                                                    onClick = {
                                                        onDelete(v)
                                                        vaultToDeleteId = null
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                                    shape = RoundedCornerShape(6.dp),
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                                ) {
                                                    Text("Confirm Delete", fontSize = 11.sp)
                                                }
                                            }
                                        }
                                    } else {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Switch Workspace Button
                                            if (!isActive) {
                                                Button(
                                                    onClick = { onSelect(v) },
                                                    shape = RoundedCornerShape(8.dp),
                                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                                                ) {
                                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Switch Workspace", fontSize = 12.sp)
                                                }
                                            } else {
                                                Spacer(modifier = Modifier.width(1.dp))
                                            }

                                            // Delete icon trigger
                                            IconButton(
                                                onClick = { vaultToDeleteId = v.id }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Delete Space",
                                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onAddNewVaultSpace,
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Connect New Vault")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

