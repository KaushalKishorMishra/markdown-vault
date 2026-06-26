package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import com.example.R

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.SyncState
import com.example.data.db.*
import com.example.ui.components.FormatAction
import com.example.ui.components.FormatToolbar
import com.example.ui.components.FormatType
import com.example.ui.components.MarkdownPreview
import com.example.ui.components.MathView
import com.example.ui.components.OptionChip
import com.example.ui.theme.*
import com.example.ui.viewmodel.VaultViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import coil.compose.AsyncImage
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.GridItemSpan

enum class DashboardScreenType {
    SETTINGS,
    EXPLORER,
    EMPTY,
    WORKSPACE
}

// Accessibility helpers
val MinimumTouchTarget = Modifier
    .sizeIn(minWidth = 48.dp, minHeight = 48.dp)

fun Modifier.minimumTouchTarget(): Modifier = this.then(MinimumTouchTarget)

fun Modifier.menuItemRole(selected: Boolean = false, action: (() -> Unit)? = null): Modifier =
    this
        .semantics(mergeDescendants = true) {
            role = Role.Button
            if (selected) {
                this.selected = true
            }
            onClick(action = { action?.invoke(); true })
        }
        .minimumTouchTarget()

fun Modifier.buttonRole(contentDescription: String, action: (() -> Unit)? = null): Modifier =
    this
        .semantics {
            role = Role.Button
            this.contentDescription = contentDescription
            if (action != null) {
                onClick(action = { action(); true })
            }
        }
        .minimumTouchTarget()

fun Modifier.switchRole(contentDescription: String, checked: Boolean, action: ((Boolean) -> Unit)? = null): Modifier =
    this
        .semantics {
            role = Role.Switch
            this.contentDescription = contentDescription
            this.stateDescription = if (checked) "On" else "Off"
            onClick(action = { action?.invoke(checked); true })
        }
        .minimumTouchTarget()

fun Modifier.textFieldRole(label: String): Modifier =
    this
        .semantics {
            role = Role.Button
            this.contentDescription = label
        }

fun Modifier.headingRole(level: Int = 1): Modifier =
    this
        .semantics {
            role = Role.Button
            contentDescription = ""
        }

// Skip to main content link for keyboard/screen reader users
@Composable
fun SkipToMainContent() {
    var isVisible by remember { mutableStateOf(false) }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .focusTarget()
            .focusProperties {
                canFocus = true
            }
            .onFocusChanged { focusState ->
                isVisible = focusState.isFocused
            }
            .background(if (isVisible) MaterialTheme.colorScheme.primary else Color.Transparent)
            .padding(if (isVisible) 16.dp else 0.dp)
            .heightIn(min = 1.dp)
    ) {
        if (isVisible) {
            Text(
                text = "Skip to main content",
                modifier = Modifier.semantics {
                    role = Role.Button
                    contentDescription = "Skip to main content. Double tap to navigate to main content area."
                }
            )
        }
    }
}

@Composable
fun LiveRegion(text: String, modifier: Modifier = Modifier) {
    Text(
        text = "",
        modifier = modifier
            .semantics {
                liveRegion = LiveRegionMode.Polite
                contentDescription = text
            }
            .alpha(0f)
    )
}
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
    var currentBrowsingPath by remember(activeVault) { mutableStateOf("") }
    val notes by viewModel.notesForActiveVault.collectAsStateWithLifecycle()
    val selectedNote by viewModel.selectedNote.collectAsStateWithLifecycle()
    val isEditMode by viewModel.isEditMode.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val syncState by viewModel.syncStatus.collectAsStateWithLifecycle()

    val gitUser by viewModel.gitUsername.collectAsStateWithLifecycle()
    val gitToken by viewModel.gitToken.collectAsStateWithLifecycle()
    val isGitConfigured by viewModel.isGitConfigured.collectAsStateWithLifecycle()
    val isBackgroundSyncEnabled by viewModel.isBackgroundSyncEnabled.collectAsStateWithLifecycle()
    val selectedTheme by viewModel.selectedTheme.collectAsStateWithLifecycle()

    // Dialog state
    var showAddVaultDialog by remember { mutableStateOf(false) }
    var showAddNoteDialog by remember { mutableStateOf(false) }
    var showSettingsScreen by remember { mutableStateOf(false) }
    var showAddFolderDialog by remember { mutableStateOf(false) }
    var showSyncConfirmationDialog by remember { mutableStateOf(false) }
    var showSearchPanel by remember { mutableStateOf(false) }
    var showDeleteNoteDialog by remember { mutableStateOf(false) }
    var showAiAssistant by remember { mutableStateOf(false) }
    val searchFocusRequester = remember { FocusRequester() }

    // Screen reader announcements for sync state changes
    val syncAnnouncement = remember(syncState) {
        when (syncState) {
            is SyncState.Syncing -> "Sync started, synchronizing vault with GitHub"
            is SyncState.Success -> "Sync completed successfully"
            is SyncState.Error -> "Sync failed: ${(syncState as SyncState.Error).error}"
            else -> ""
        }
    }

    // Drawer state (for mobile)
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    // Main scaffold
    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = selectedNote == null,
        drawerContent = {
            if (!isTablet) {
                ModalDrawerSheet(
                    drawerContainerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.width(300.dp)
                ) {
                    SidebarContent(
                        vaults = vaults,
                        activeVault = activeVault,
                        onVaultSelect = {
                            viewModel.selectVault(it)
                            showSettingsScreen = false
                            coroutineScope.launch { drawerState.close() }
                        },
                        onAddVaultClick = { showAddVaultDialog = true },
                        onSettingsClick = {
                            showSettingsScreen = true
                            viewModel.selectNote(null)
                            coroutineScope.launch { drawerState.close() }
                        }
                    )
                }
            }
        }
    ) {
        // Skip to main content link (only visible when focused)
        SkipToMainContent()
        
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
                        onVaultSelect = {
                            viewModel.selectVault(it)
                            showSettingsScreen = false
                        },
                        onAddVaultClick = { showAddVaultDialog = true },
                        onSettingsClick = {
                            showSettingsScreen = true
                            viewModel.selectNote(null)
                        }
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
                                if (showSettingsScreen) {
                                    Text(
                                        text = "Settings",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                } else if (selectedNote != null) {
                                    Text(
                                        text = selectedNote?.title ?: "",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val statusText = when (selectedNote?.syncStatus) {
                                        "SYNCED" -> "Synced"
                                        "MODIFIED" -> "Modified locally"
                                        "LOCAL_ONLY" -> "Local only"
                                        else -> selectedNote?.syncStatus ?: ""
                                    }
                                    Text(
                                        text = "$statusText • ${selectedNote?.filePath ?: ""}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                } else {
                                    Text(
                                        text = activeVault?.name ?: "No Vault Selected",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (activeVault != null) {
                                        Text(
                                            text = activeVault?.gitRepo ?: "",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        },
                        navigationIcon = {
                            if (showSettingsScreen) {
                                IconButton(
                                    onClick = { showSettingsScreen = false },
                                    modifier = Modifier.minimumTouchTarget()
                                ) {
                                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back to Settings")
                                }
                            } else if (selectedNote != null) {
                                IconButton(
                                    onClick = { viewModel.selectNote(null) },
                                    modifier = Modifier.minimumTouchTarget()
                                ) {
                                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back to Folder Explorer")
                                }
                            } else if (!isTablet) {
                                IconButton(
                                    onClick = { coroutineScope.launch { drawerState.open() } },
                                    modifier = Modifier.minimumTouchTarget()
                                ) {
                                    Icon(imageVector = Icons.Default.Menu, contentDescription = "Open Sidebar")
                                }
                            }
                        },
                        actions = {
                            if (showSettingsScreen) {
                                // No actions on settings screen
                            } else if (selectedNote != null) {
                                // AI Assistant
                                IconButton(
                                    onClick = { showAiAssistant = true },
                                    modifier = Modifier.minimumTouchTarget()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = "AI Note Assistant",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                // Delete Note
                                IconButton(
                                    onClick = { showDeleteNoteDialog = true },
                                    modifier = Modifier.minimumTouchTarget()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete Note",
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                                    )
                                }
                            } else if (activeVault != null) {
                                // Search icon
                                IconButton(
                                    onClick = { showSearchPanel = !showSearchPanel },
                                    modifier = Modifier.minimumTouchTarget()
                                ) {
                                    Icon(
                                        imageVector = if (showSearchPanel) Icons.Default.Close else Icons.Default.Search,
                                        contentDescription = if (showSearchPanel) "Close search" else "Search files",
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                if (!showSearchPanel) {
                                    // Sync indicator and trigger button (with confirmation dialog)
                                    when (syncState) {
                                        is SyncState.Syncing -> {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(24.dp),
                                                strokeWidth = 2.dp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                        }
                                        else -> {
                                            IconButton(
                                                onClick = { showSyncConfirmationDialog = true },
                                                modifier = Modifier.minimumTouchTarget()
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Refresh,
                                                    contentDescription = if (isGitConfigured) "Sync Vault with GitHub" else "Configure GitHub to enable sync",
                                                    tint = if (isGitConfigured) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
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
                floatingActionButton = {
                    if (selectedNote == null && activeVault != null && !showSettingsScreen) {
                        var showFabMenu by remember { mutableStateOf(false) }
                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (showFabMenu) {
                                // Create Folder FAB
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        tonalElevation = 4.dp
                                    ) {
                                        Text(
                                            text = "Create Folder",
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    FloatingActionButton(
                                        onClick = {
                                            showFabMenu = false
                                            showAddFolderDialog = true
                                        },
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(Icons.Default.CreateNewFolder, contentDescription = "Create Folder", modifier = Modifier.size(24.dp))
                                    }
                                }
                                
                                // Create Note FAB
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        tonalElevation = 4.dp
                                    ) {
                                        Text(
                                            text = "Create Note",
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    FloatingActionButton(
                                        onClick = {
                                            showFabMenu = false
                                            showAddNoteDialog = true
                                        },
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(Icons.Default.NoteAdd, contentDescription = "Create Note", modifier = Modifier.size(24.dp))
                                    }
                                }
                            }
                            
                            FloatingActionButton(
                                onClick = { showFabMenu = !showFabMenu },
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(56.dp)
                            ) {
                                Icon(
                                    imageVector = if (showFabMenu) Icons.Default.Close else Icons.Default.Add,
                                    contentDescription = if (showFabMenu) "Close options" else "Open options",
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                ) { innerPadding ->
                    LiveRegion(text = syncAnnouncement)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    // Search Panel (appears below top bar when activated)
                    if (showSearchPanel && selectedNote == null) {
                        LaunchedEffect(showSearchPanel) {
                            searchFocusRequester.requestFocus()
                        }
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            label = { Text("Search files and folders") },
                            placeholder = { Text("Search files and folders...") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = "Close search",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.clickable { showSearchPanel = false; viewModel.setSearchQuery("") }
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(
                                        onClick = { viewModel.setSearchQuery("") },
                                        modifier = Modifier.minimumTouchTarget()
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear search")
                                    }
                                }
                            },
                            shape = RoundedCornerShape(24.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp, top = 8.dp)
                                .focusRequester(searchFocusRequester)
                                .semantics { role = Role.Button }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    }
                    
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                    val currentScreenType = when {
                        showSettingsScreen -> DashboardScreenType.SETTINGS
                        selectedNote == null -> {
                            if (activeVault != null) DashboardScreenType.EXPLORER else DashboardScreenType.EMPTY
                        }
                        else -> DashboardScreenType.WORKSPACE
                    }

                    AnimatedContent(
                        targetState = currentScreenType,
                        transitionSpec = {
                            val springSpec = spring<IntOffset>(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            )
                            if (initialState == DashboardScreenType.EMPTY && targetState == DashboardScreenType.EXPLORER) {
                                fadeIn(animationSpec = tween(250)) togetherWith fadeOut(animationSpec = tween(150))
                            } else if (targetState == DashboardScreenType.WORKSPACE || targetState == DashboardScreenType.SETTINGS) {
                                (slideInHorizontally(animationSpec = springSpec) { width -> width } + fadeIn(animationSpec = tween(220))) togetherWith
                                        (slideOutHorizontally(animationSpec = tween(180)) { width -> -width } + fadeOut(animationSpec = tween(180)))
                            } else {
                                (slideInHorizontally(animationSpec = springSpec) { width -> -width } + fadeIn(animationSpec = tween(220))) togetherWith
                                        (slideOutHorizontally(animationSpec = tween(180)) { width -> width } + fadeOut(animationSpec = tween(180)))
                            }
                        },
                        label = "screen_transition",
                        modifier = Modifier.fillMaxSize()
                    ) { screen ->
                        when (screen) {
                            DashboardScreenType.SETTINGS -> {
                                val gitName by viewModel.gitName.collectAsStateWithLifecycle()
                                val gitEmail by viewModel.gitEmail.collectAsStateWithLifecycle()
                                val gitAvatarUrl by viewModel.gitAvatarUrl.collectAsStateWithLifecycle()
                                val selectedTheme by viewModel.selectedTheme.collectAsStateWithLifecycle()

                                SettingsScreen(
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
                                    onConfirm = { user, token ->
                                        viewModel.updateGitCredentials(user, token)
                                        showSettingsScreen = false
                                    },
                                    onClear = {
                                        viewModel.clearGitCredentials()
                                        showSettingsScreen = false
                                    },
                                    onCheckStatus = { user, token ->
                                        viewModel.syncEngine.testConnection(user, token)
                                    },
                                    onBackClick = { showSettingsScreen = false },
                                    vaults = vaults,
                                    activeVault = activeVault,
                                    onVaultSelect = { vault ->
                                        viewModel.selectVault(vault)
                                        showSettingsScreen = false
                                    },
                                    onVaultDelete = { vault ->
                                        viewModel.deleteVault(vault)
                                    },
                                    onAddVaultClick = { showAddVaultDialog = true },
                                    viewModel = viewModel
                                )
                            }
                            DashboardScreenType.EXPLORER -> {
                                val vault = activeVault
                                if (vault == null) {
                                    EmptyWorkspaceArea(
                                        activeVault = null,
                                        notesCount = 0,
                                        onAddNoteClick = { showAddNoteDialog = true },
                                        onGitConfigClick = { showSettingsScreen = true },
                                        isGitConfigured = isGitConfigured
                                    )
                                } else {
                                    FolderExplorer(
                                        activeVault = vault,
                                        notes = notes,
                                        currentPath = currentBrowsingPath,
                                        onPathChange = { currentBrowsingPath = it },
                                        onNoteSelect = { viewModel.selectNote(it) },
                                        onAddNoteClick = { showAddNoteDialog = true },
                                        onDeleteFolder = { viewModel.deleteFolder(it) },
                                        onRenameFolder = { old, new -> viewModel.renameFolder(old, new) },
                                        onRenameNote = { note, title, name -> viewModel.renameNote(note, title, name) },
                                        onDeleteNote = { viewModel.deleteNote(it) },
                                        searchQuery = searchQuery,
                                        onSearchChange = { viewModel.setSearchQuery(it) }
                                    )
                                }
                            }
                            DashboardScreenType.EMPTY -> {
                                EmptyWorkspaceArea(
                                    activeVault = null,
                                    notesCount = 0,
                                    onAddNoteClick = { showAddNoteDialog = true },
                                    onGitConfigClick = { showSettingsScreen = true },
                                    isGitConfigured = isGitConfigured
                                )
                            }
                            DashboardScreenType.WORKSPACE -> {
                                val note = selectedNote
                                if (note != null) {
                                    NoteWorkspace(
                                        note = note,
                                    isEditMode = isEditMode,
                                    isTabletLayout = isTablet,
                                    syncState = syncState,
                                    selectedTheme = selectedTheme,
                                    onContentChange = { viewModel.updateSelectedNoteContent(it) },
                                    onEditModeChange = { viewModel.setEditMode(it) },
                                    onResolveConflict = { resolution, mergedText ->
                                        viewModel.resolveMergeConflict(resolution, mergedText)
                                    }
                                )
                            }
                            }
                        }
                    }   // AnimatedContent
                    }   // Box
                }   // Column
            }   // Scaffold
        }   // Row
    }   // ModalNavigationDrawer

    // --- Dialogs & Overlays ---

    if (showAddVaultDialog) {
        AddVaultDialog(
            onDismiss = { showAddVaultDialog = false },
            onConfirm = { name, repo, branch ->
                viewModel.createVault(name, repo, branch)
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
        val defaultPrefix = if (currentBrowsingPath.isNotEmpty()) {
            if (currentBrowsingPath.endsWith("/")) currentBrowsingPath else "$currentBrowsingPath/"
        } else {
            when (activeVault?.vaultType) {
                "LOGSEQ" -> "pages/"
                else -> ""
            }
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

    if (showAddFolderDialog) {
        AddFolderDialog(
            onDismiss = { showAddFolderDialog = false },
            onConfirm = { folderName ->
                val cleanFolder = folderName.trim().removePrefix("/").removeSuffix("/")
                val finalFolder = if (currentBrowsingPath.isNotEmpty()) "$currentBrowsingPath/$cleanFolder" else cleanFolder
                val targetPath = "$finalFolder/welcome.md"
                viewModel.createNote("About $cleanFolder", targetPath)
                showAddFolderDialog = false
            }
        )
    }

    if (showSyncConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showSyncConfirmationDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Sync Vault Workspace", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "This will synchronize your local notes and folders with the remote GitHub repository.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (activeVault != null) {
                        Text(
                            text = "Repository: ${activeVault?.gitRepo ?: ""}\nBranch: ${activeVault?.branch ?: ""}",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.triggerSync()
                        showSyncConfirmationDialog = false
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Sync Now")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSyncConfirmationDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDeleteNoteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteNoteDialog = false },
            title = { Text("Delete Note?") },
            text = { Text("This will move the note to trash. You can restore it from the Recently Deleted section.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteSelectedNote()
                        showDeleteNoteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteNoteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showAiAssistant && selectedNote != null) {
        com.example.ui.components.AiAssistantBottomSheet(
            note = selectedNote!!,
            onDismiss = { showAiAssistant = false },
            onUpdateContent = { viewModel.updateSelectedNoteContent(it) }
        )
    }
}

// --- Inner Sidebar Section ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SidebarContent(
    vaults: List<Vault>,
    activeVault: Vault?,
    onVaultSelect: (Vault) -> Unit,
    onAddVaultClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
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
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.app_logo),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Markdown Vault",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                letterSpacing = (-0.5).sp
            )
        }

        // Vertical List of Vaults (Google Files Style Navigation Drawer)
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            if (vaults.isEmpty()) {
                item {
                    Text(
                        text = "No vaults found. Connect a workspace below!",
                        fontSize = 12.sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }
            } else {
                items(vaults) { v ->
                    val isActive = v.id == activeVault?.id
                    Surface(
                        onClick = { onVaultSelect(v) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = if (isActive) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumTouchTarget()
                            .semantics(mergeDescendants = true) {
                                role = Role.Button
                                contentDescription = "${v.name}, ${v.gitRepo}${if (isActive) ", active workspace" else ""}"
                                selected = isActive
                                onClick { onVaultSelect(v); true }
                            }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isActive) MaterialTheme.colorScheme.primary
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
                                    tint = if (isActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = v.name,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = v.gitRepo,
                                    fontSize = 9.sp,
                                    color = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (isActive) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Active Workspace",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Connection action item inside the list
            item {
                Surface(
                    onClick = onAddVaultClick,
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Transparent,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumTouchTarget()
                        .semantics(mergeDescendants = true) {
                            role = Role.Button
                            contentDescription = "Connect New Vault"
                            onClick { onAddVaultClick(); true }
                        }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Connect New Vault",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
        Spacer(modifier = Modifier.height(16.dp))

        // Settings Entry Button at the bottom
        Surface(
            onClick = onSettingsClick,
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
            modifier = Modifier
                .fillMaxWidth()
                .minimumTouchTarget()
                .semantics(mergeDescendants = true) {
                    role = Role.Button
                    contentDescription = "Settings"
                    onClick { onSettingsClick(); true }
                }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Settings",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // App Version text dynamically parsed from context package details
        val context = androidx.compose.ui.platform.LocalContext.current
        val appVersion = remember {
            try {
                val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                pInfo.versionName ?: "0.0.0"
            } catch (e: Exception) {
                "0.0.0"
            }
        }
        Text(
            text = "Version $appVersion",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
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
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.app_logo),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(36.dp)
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
    selectedTheme: String = "ARTISTIC",
    onContentChange: (String) -> Unit,
    onEditModeChange: (Boolean) -> Unit,
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
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Mode Selector (only on phone/mobile)
            if (!isTabletLayout) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Row(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(20.dp))
                            .padding(2.dp)
                            .semantics { role = Role.Tab }
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(18.dp))
                                .background(
                                    animateColorAsState(
                                        if (!isEditMode) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        label = "previewBg"
                                    ).value
                                )
                                .clickable { onEditModeChange(false) }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .semantics {
                                    role = Role.Tab
                                    selected = !isEditMode
                                    contentDescription = "Preview mode"
                                    onClick { onEditModeChange(false); true }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Visibility,
                                    contentDescription = null,
                                    tint = if (!isEditMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Preview",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (!isEditMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(18.dp))
                                .background(
                                    animateColorAsState(
                                        if (isEditMode) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        label = "editBg"
                                    ).value
                                )
                                .clickable { onEditModeChange(true) }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .semantics {
                                    role = Role.Tab
                                    selected = isEditMode
                                    contentDescription = "Edit mode"
                                    onClick { onEditModeChange(true); true }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = if (isEditMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Edit",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isEditMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
            
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
                        HorizontalDivider(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(1.dp),
                            color = MaterialTheme.colorScheme.outline
                        )
                        // Right: Live html Preview
                        Box(modifier = Modifier.weight(1f)) {
                            MarkdownPreview(
                                markdownString = note.content,
                                isHighContrast = selectedTheme.startsWith("HIGH_CONTRAST")
                            )
                        }
                    }
                } else {
                    Crossfade(
                        targetState = isEditMode,
                        animationSpec = tween(200),
                        label = "editPreviewCrossfade"
                    ) { editing ->
                        if (editing) {
                            MarkdownEditorArea(
                                content = note.content,
                                onContentChange = onContentChange,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            MarkdownPreview(
                                markdownString = note.content,
                                isHighContrast = selectedTheme.startsWith("HIGH_CONTRAST")
                            )
                        }
                    }
                }
            }
            
            AnimatedVisibility(
                visible = isEditMode,
                enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
                exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut()
            ) {
                val wordCount = remember(note.content) {
                    val trimmed = note.content.trim()
                    if (trimmed.isEmpty()) 0 else trimmed.split(Regex("\\s+")).size
                }
                val charCount = note.content.length
                val readingTime = (wordCount / 200).coerceAtLeast(1)
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "$wordCount words",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "$charCount chars",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "~${readingTime}min read",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
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
        FormatToolbar(
            onInsert = { action ->
                val txt = textFieldValueState.text
                val selection = textFieldValueState.selection
                val start = selection.start
                val end = selection.end
                val selected = if (start != end) txt.substring(start, end) else ""

                val inserted = when (action.type) {
                    FormatType.Wrap -> {
                        if (selected.isNotEmpty()) {
                            txt.substring(0, start) + action.prefix + selected + action.suffix + txt.substring(end)
                        } else {
                            txt.substring(0, start) + action.prefix + action.suffix + txt.substring(end)
                        }
                    }
                    FormatType.PrependLine -> {
                        txt.substring(0, start) + "\n" + action.prefix + txt.substring(end)
                    }
                    FormatType.InsertTemplate -> {
                        txt.substring(0, start) + (action.template ?: "") + txt.substring(end)
                    }
                }

                onContentChange(inserted)
            }
        )

        TextField(
            value = textFieldValueState,
            onValueChange = {
                textFieldValueState = it
                onContentChange(it.text)
            },
            label = { Text("Markdown Editor") },
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
                .semantics {
                    role = Role.Button
                    contentDescription = "Markdown Editor"
                }
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
                // Header with icon and tab switcher
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (activeEquation != null || activeMermaid != null)
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                    else MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (selectedTab == 0) "∑" else "📊",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = if (activeEquation != null || activeMermaid != null)
                                    MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (selectedTab == 0) "LaTeX Math" else "Mermaid Diagrams",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Tab Switcher
                    Row(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                            .padding(2.dp)
                            .semantics { role = Role.Tab }
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (selectedTab == 0) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .clickable { selectedTab = 0 }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .semantics {
                                    role = Role.Tab
                                    selected = selectedTab == 0
                                    contentDescription = "LaTeX Math tab"
                                    onClick { selectedTab = 0; true }
                                },
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
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .semantics {
                                    role = Role.Tab
                                    selected = selectedTab == 1
                                    contentDescription = "Mermaid Diagram tab"
                                    onClick { selectedTab = 1; true }
                                },
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
                }

                Spacer(modifier = Modifier.height(10.dp))

                    if (selectedTab == 0) {
                        // --- TAB 1: LATEX MATH ---
                        Column(modifier = Modifier.fillMaxWidth()) {
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

                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            Text(
                                text = "Quick-add math templates:",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            LazyRow(
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
                                        border = BorderStroke(
                                            width = 1.dp,
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                        ),
                                        modifier = Modifier.semantics {
                                            role = Role.Button
                                            contentDescription = "Insert $title math template"
                                            onClick {
                                                val txt = textFieldValueState.text
                                                val selection = textFieldValueState.selection
                                                val start = selection.start
                                                val end = selection.end
                                                val tag = if (title == "Pythagoras") "$" else "$$"
                                                val inserted = txt.substring(0, start) + "\n$tag\n$latexBody\n$tag\n" + txt.substring(end)
                                                onContentChange(inserted)
                                                true
                                            }
                                        }
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
                    } else {
                        // --- TAB 2: MERMAID DIAGRAMS ---
                        Column(modifier = Modifier.fillMaxWidth()) {
                            if (displayedMermaid != null) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(130.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                ) {
                                    MarkdownPreview(
                                        markdownString = "```mermaid\n" + displayedMermaid.code + "\n```",
                                        modifier = Modifier.fillMaxSize(),
                                        isHighContrast = false
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = "Active Mermaid: " + displayedMermaid.code.substringBefore("\n") + " ...",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.secondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                )

                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            Text(
                                text = "Quick-add diagram templates:",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            LazyRow(
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
                                        border = BorderStroke(
                                            width = 1.dp,
                                            color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)
                                        ),
                                        modifier = Modifier.semantics {
                                            role = Role.Button
                                            contentDescription = "Insert $title diagram template"
                                            onClick {
                                                val txt = textFieldValueState.text
                                                val selection = textFieldValueState.selection
                                                val start = selection.start
                                                val end = selection.end
                                                val inserted = txt.substring(0, start) + "\n```mermaid\n$codeBody\n```\n" + txt.substring(end)
                                                onContentChange(inserted)
                                                true
                                            }
                                        }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentUsername: String,
    currentToken: String,
    currentName: String,
    currentEmail: String,
    currentAvatarUrl: String,
    isBackgroundSyncEnabled: Boolean,
    onToggleBackgroundSync: (Boolean) -> Unit,
    selectedTheme: String,
    onSelectTheme: (String) -> Unit,
    onConfirm: (String, String) -> Unit,
    onClear: () -> Unit,
    onCheckStatus: suspend (String, String) -> Boolean,
    onBackClick: () -> Unit,
    vaults: List<Vault>,
    activeVault: Vault?,
    onVaultSelect: (Vault) -> Unit,
    onVaultDelete: (Vault) -> Unit,
    onAddVaultClick: () -> Unit,
    viewModel: VaultViewModel
) {
    var user by remember { mutableStateOf(currentUsername) }
    var token by remember { mutableStateOf(currentToken) }

    val coroutineScope = rememberCoroutineScope()
    var connectionStatus by remember { mutableStateOf(if (currentToken.isNotEmpty()) "Configured" else "Unconfigured") }
    var isChecking by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // User Profile Section
        if (currentToken.isNotEmpty()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(16.dp)
            ) {
                if (currentAvatarUrl.isNotEmpty()) {
                    AsyncImage(
                        model = currentAvatarUrl,
                        contentDescription = "GitHub Avatar",
                        modifier = Modifier
                            .size(64.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .border(2.dp, MaterialTheme.colorScheme.primary, androidx.compose.foundation.shape.CircleShape)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        val initials = (currentName.ifEmpty { currentUsername }).take(2).uppercase()
                        Text(
                            text = initials,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentName.ifEmpty { currentUsername },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (currentEmail.isNotEmpty()) {
                        Text(
                            text = currentEmail,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "@$currentUsername",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Vault Workspaces Section (Google Files style, inline delete configuration)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Vault Workspaces".uppercase(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        letterSpacing = 1.sp
                    )
                    TextButton(
                        onClick = onAddVaultClick,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Connect New", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (vaults.isEmpty()) {
                    Text(
                        text = "No local vaults connected.",
                        fontSize = 12.sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        var vaultToDeleteId by remember { mutableStateOf<String?>(null) }
                        
                        vaults.forEach { v ->
                            val isActive = v.id == activeVault?.id
                            val isConfirmingDelete = vaultToDeleteId == v.id

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                border = if (isActive) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                                         else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
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
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = v.name,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = v.gitRepo,
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        if (isActive) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(Emerald500.copy(alpha = 0.15f))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "Active",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Emerald500
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    if (isConfirmingDelete) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Confirm delete? Cached files will be erased.",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Row {
                                                TextButton(
                                                    onClick = { vaultToDeleteId = null },
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                                ) {
                                                    Text("Cancel", fontSize = 11.sp)
                                                }
                                                Button(
                                                    onClick = {
                                                        onVaultDelete(v)
                                                        vaultToDeleteId = null
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                                    shape = RoundedCornerShape(6.dp),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                                ) {
                                                    Text("Confirm", fontSize = 11.sp)
                                                }
                                            }
                                        }
                                    } else {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (!isActive) {
                                                Button(
                                                    onClick = { onVaultSelect(v) },
                                                    shape = RoundedCornerShape(6.dp),
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                                                ) {
                                                    Text("Switch Workspace", fontSize = 11.sp)
                                                }
                                            } else {
                                                Spacer(modifier = Modifier.width(1.dp))
                                            }

                                            IconButton(
                                                onClick = { vaultToDeleteId = v.id },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Delete Workspace",
                                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                                    modifier = Modifier.size(18.dp)
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

        // Trash / Recently Deleted Section (Trash Bin)
        val trashedNotes by viewModel.trashedNotesForActiveVault.collectAsStateWithLifecycle()

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Recently Deleted (Trash Bin)".uppercase(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Items in the trash will be permanently deleted and synced as deleted after 30 days.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (trashedNotes.isEmpty()) {
                    Text(
                        text = "Trash is empty",
                        fontSize = 12.sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        trashedNotes.forEach { note ->
                            val timeDiff = System.currentTimeMillis() - note.updatedAt
                            val daysLeft = 30 - (timeDiff / (24L * 60 * 60 * 1000)).toInt()
                            val daysText = if (daysLeft <= 0) "Expired (Deleting soon)" else "$daysLeft days left"

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = note.title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${note.filePath} • $daysText",
                                            fontSize = 10.sp,
                                            color = if (daysLeft < 5) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        IconButton(
                                            onClick = { viewModel.restoreNote(note) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Restore,
                                                contentDescription = "Restore",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = { viewModel.deleteNotePermanently(note) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DeleteForever,
                                                contentDescription = "Delete Permanently",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(18.dp)
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

        // Theme Selection Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Color Themes".uppercase(),
                    fontSize = 11.sp,
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
                        "LIGHT" to "Lavender Light",
                        "HIGH_CONTRAST_DARK" to "High Contrast Dark",
                        "HIGH_CONTRAST_LIGHT" to "High Contrast Light"
                    )
                    items(themes) { (themeKey, themeLabel) ->
                        OptionChip(
                            label = themeLabel,
                            isSelected = selectedTheme == themeKey,
                            onClick = { onSelectTheme(themeKey) }
                        )
                    }
                }
            }
        }

        // GitHub Credentials Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "GitHub Authentication".uppercase(),
                    fontSize = 11.sp,
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
                            Text("Check Connection", fontSize = 12.sp)
                        }
                    }

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
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = when (connectionStatus) {
                                "Active & Valid" -> Emerald500
                                "Connection Failed" -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }
        }

        // Automatic Background Sync Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Automatic Background Sync",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Auto-sync vaults periodically on connection",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = isBackgroundSyncEnabled,
                    onCheckedChange = onToggleBackgroundSync
                )
            }
        }

        // Save & Reset Action Section
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (currentToken.isNotEmpty()) {
                TextButton(
                    onClick = onClear,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Remove GitHub Auth", fontSize = 13.sp)
                }
                Spacer(modifier = Modifier.width(16.dp))
            }
            Button(
                onClick = { onConfirm(user, token) },
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Save Changes", fontSize = 13.sp)
            }
        }
    }
}

// --- Add Vault Dialog ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddVaultDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var repo by remember { mutableStateOf("") }
    var branch by remember { mutableStateOf("main") }

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
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary),
                    singleLine = true
                )
                OutlinedTextField(
                    value = repo,
                    onValueChange = { repo = it },
                    label = { Text("Repository Name") },
                    placeholder = { Text("e.g. notes-vault") },
                    supportingText = { Text("Uses your GitHub username from credentials", fontSize = 10.sp) },
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary),
                    singleLine = true
                )
                OutlinedTextField(
                    value = branch,
                    onValueChange = { branch = it },
                    label = { Text("Default Sync Branch") },
                    placeholder = { Text("main") },
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotEmpty() && repo.isNotEmpty()) onConfirm(name, repo.trim(), branch.ifEmpty { "main" }) },
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

@Composable
private fun rememberHighlightedText(text: String, query: String): AnnotatedString {
    if (query.isBlank()) return AnnotatedString(text)
    val lowerText = text.lowercase()
    val lowerQuery = query.lowercase()
    val primary = MaterialTheme.colorScheme.primary
    val bg = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
    return remember(text, query) {
        var startIndex = 0
        buildAnnotatedString {
            while (true) {
                val matchIndex = lowerText.indexOf(lowerQuery, startIndex)
                if (matchIndex == -1) {
                    append(text.substring(startIndex))
                    break
                }
                append(text.substring(startIndex, matchIndex))
                withStyle(SpanStyle(
                    color = primary,
                    fontWeight = FontWeight.Bold,
                    background = bg
                )) {
                    append(text.substring(matchIndex, matchIndex + query.length))
                }
                startIndex = matchIndex + query.length
            }
        }
    }
}

@Composable
fun FolderExplorer(
    activeVault: Vault,
    notes: List<Note>,
    currentPath: String,
    onPathChange: (String) -> Unit,
    onNoteSelect: (Note) -> Unit,
    onAddNoteClick: () -> Unit,
    onDeleteFolder: (String) -> Unit,
    onRenameFolder: (String, String) -> Unit,
    onRenameNote: (Note, String, String) -> Unit,
    onDeleteNote: (Note) -> Unit,
    searchQuery: String,
    onSearchChange: (String) -> Unit
) {
    val currentPrefix = if (currentPath.isEmpty()) "" else "$currentPath/"

    // Parse folders and files in currentPath, flattening during search to show matched files directly
    val itemsInCurrentDir = remember(notes, currentPath, searchQuery) {
        val folders = mutableSetOf<String>()
        val files = mutableListOf<Note>()
        
        notes.forEach { note ->
            if (searchQuery.isNotEmpty()) {
                files.add(note)
            } else {
                if (currentPath.isEmpty()) {
                    if (note.filePath.contains("/")) {
                        folders.add(note.filePath.substringBefore("/"))
                    } else {
                        files.add(note)
                    }
                } else {
                    if (note.filePath.startsWith(currentPrefix)) {
                        val relative = note.filePath.removePrefix(currentPrefix)
                        if (relative.contains("/")) {
                            folders.add(relative.substringBefore("/"))
                        } else {
                            files.add(note)
                        }
                    }
                }
            }
        }
        Pair(folders.toList().sorted(), files.sortedBy { it.title })
    }
    val folders = itemsInCurrentDir.first
    val files = itemsInCurrentDir.second

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Navigation Header / Breadcrumbs (Only show if not searching)
        if (searchQuery.isEmpty()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                if (currentPath.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            val parent = if (currentPath.contains("/")) currentPath.substringBeforeLast("/") else ""
                            onPathChange(parent)
                        },
                        modifier = Modifier.minimumTouchTarget()
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Navigate Up",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                // Breadcrumb Text Nodes
                val scrollState = rememberScrollState()
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(scrollState)
                        .semantics { role = Role.Button }
                ) {
                    Text(
                        text = "Cabinet Root",
                        fontSize = 14.sp,
                        fontWeight = if (currentPath.isEmpty()) FontWeight.Bold else FontWeight.Medium,
                        color = if (currentPath.isEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .clickable { onPathChange("") }
                            .padding(vertical = 4.dp, horizontal = 6.dp)
                            .semantics {
                                role = Role.Button
                                contentDescription = "Navigate to root folder"
                                onClick { onPathChange(""); true }
                            }
                    )

                    if (currentPath.isNotEmpty()) {
                        val segments = currentPath.split("/")
                        segments.forEachIndexed { index, segment ->
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(16.dp)
                            )
                            val isLast = index == segments.lastIndex
                            val targetPath = segments.take(index + 1).joinToString("/")
                            Text(
                                text = segment,
                                fontSize = 14.sp,
                                fontWeight = if (isLast) FontWeight.Bold else FontWeight.Medium,
                                color = if (isLast) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .clickable { onPathChange(targetPath) }
                                    .padding(vertical = 4.dp, horizontal = 6.dp)
                                    .semantics {
                                        role = Role.Button
                                        contentDescription = "Navigate to $segment folder"
                                        onClick { onPathChange(targetPath); true }
                                    }
                            )
                        }
                    }
                }
            }
        } else {
            // Header during active search
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Text(
                    text = "Matching results (${files.size})",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = { onSearchChange("") }) {
                    Text("Clear Search", fontSize = 12.sp)
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), modifier = Modifier.padding(bottom = 16.dp))

        if (folders.isEmpty() && files.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (searchQuery.isNotEmpty()) {
                        Icon(
                            imageVector = Icons.Default.SearchOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No results found",
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Try different keywords or clear the search",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedButton(onClick = { onSearchChange("") }) {
                            Text("Clear Search")
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "This folder is empty",
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (currentPath.isEmpty()) "Create a new note to get started" else "Move or create notes in this folder",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 140.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Render folders grid (Only if searchQuery is empty)
                if (searchQuery.isEmpty() && folders.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            text = "Folders".uppercase(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            modifier = Modifier.padding(bottom = 6.dp, top = 4.dp),
                            letterSpacing = 1.sp
                        )
                    }

                    items(folders) { folder ->
                        var showFolderMenu by remember { mutableStateOf(false) }
                        var showRenameFolderDialog by remember { mutableStateOf(false) }
                        var showDeleteFolderDialog by remember { mutableStateOf(false) }
                        val fullFolderPath = if (currentPath.isEmpty()) folder else "$currentPath/$folder"

                        Surface(
                            onClick = { onPathChange(fullFolderPath) },
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumTouchTarget()
                                .semantics {
                                    role = Role.Button
                                    contentDescription = "Open folder $folder"
                                    onClick { onPathChange(fullFolderPath); true }
                                }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(26.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = folder,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Box {
                                    IconButton(
                                        onClick = { showFolderMenu = true },
                                        modifier = Modifier.minimumTouchTarget()
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MoreVert,
                                            contentDescription = "Folder Options",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = showFolderMenu,
                                        onDismissRequest = { showFolderMenu = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Rename Folder") },
                                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                            onClick = {
                                                showFolderMenu = false
                                                showRenameFolderDialog = true
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Delete Folder", color = MaterialTheme.colorScheme.error) },
                                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp)) },
                                            onClick = {
                                                showFolderMenu = false
                                                showDeleteFolderDialog = true
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        if (showRenameFolderDialog) {
                            RenameFolderDialog(
                                currentName = folder,
                                onDismiss = { showRenameFolderDialog = false },
                                onConfirm = { newName ->
                                    val parent = if (currentPath.isEmpty()) "" else "$currentPath/"
                                    onRenameFolder(fullFolderPath, "$parent$newName")
                                    showRenameFolderDialog = false
                                }
                            )
                        }

                        if (showDeleteFolderDialog) {
                            DeleteFolderDialog(
                                folderName = folder,
                                onDismiss = { showDeleteFolderDialog = false },
                                onConfirm = {
                                    onDeleteFolder(fullFolderPath)
                                    showDeleteFolderDialog = false
                                }
                            )
                        }
                    }
                }

                // Render files grid
                if (files.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            text = if (searchQuery.isNotEmpty()) "Search Results".uppercase() else "Files".uppercase(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            modifier = Modifier.padding(bottom = 6.dp, top = 16.dp),
                            letterSpacing = 1.sp
                        )
                    }

                    items(files) { file ->
                        var showFileMenu by remember { mutableStateOf(false) }
                        var showRenameFileDialog by remember { mutableStateOf(false) }
                        var showDeleteFileDialog by remember { mutableStateOf(false) }
                        val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
                        val context = androidx.compose.ui.platform.LocalContext.current

                        val syncColor = when (file.syncStatus) {
                            "SYNCED" -> Emerald500
                            "MODIFIED" -> Sky400
                            "CONFLICT" -> Amber500
                            "LOCAL_ONLY" -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }

                        Surface(
                            onClick = { onNoteSelect(file) },
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Description,
                                        contentDescription = "File",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(androidx.compose.foundation.shape.CircleShape)
                                            .background(syncColor)
                                    )
                                    Spacer(modifier = Modifier.weight(1f))
                                    
                                    Box {
                                        IconButton(
                                            onClick = { showFileMenu = true },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.MoreVert,
                                                contentDescription = "File Options",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        DropdownMenu(
                                            expanded = showFileMenu,
                                            onDismissRequest = { showFileMenu = false }
                                        ) {
                                            DropdownMenuItem(
                                                text = { Text("Open File") },
                                                leadingIcon = { Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                                onClick = {
                                                    showFileMenu = false
                                                    onNoteSelect(file)
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Rename File") },
                                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                                onClick = {
                                                    showFileMenu = false
                                                    showRenameFileDialog = true
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Copy Path") },
                                                leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                                onClick = {
                                                    showFileMenu = false
                                                    val pathString = file.filePath
                                                    val annotated = androidx.compose.ui.text.buildAnnotatedString { append(pathString) }
                                                    clipboardManager.setText(annotated)
                                                    android.widget.Toast.makeText(context, "Copied: $pathString", android.widget.Toast.LENGTH_SHORT).show()
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Delete File", color = MaterialTheme.colorScheme.error) },
                                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp)) },
                                                onClick = {
                                                    showFileMenu = false
                                                    showDeleteFileDialog = true
                                                }
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                val highlightedTitle = if (searchQuery.isNotEmpty()) {
                                    rememberHighlightedText(file.title, searchQuery)
                                } else {
                                    AnnotatedString(file.title)
                                }
                                Text(
                                    text = highlightedTitle,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = file.filePath.substringAfterLast("/"),
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        if (showRenameFileDialog) {
                            RenameNoteDialog(
                                note = file,
                                onDismiss = { showRenameFileDialog = false },
                                onConfirm = { newTitle, newFileName ->
                                    onRenameNote(file, newTitle, newFileName)
                                    showRenameFileDialog = false
                                }
                            )
                        }

                        if (showDeleteFileDialog) {
                            AlertDialog(
                                onDismissRequest = { showDeleteFileDialog = false },
                                title = { Text("Delete Note?") },
                                text = { Text("Are you sure you want to delete note \"${file.title}\"?") },
                                confirmButton = {
                                    Button(
                                        onClick = {
                                            onDeleteNote(file)
                                            showDeleteFileDialog = false
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Delete")
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showDeleteFileDialog = false }) {
                                        Text("Cancel")
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- Nested Folders Helpers Dialogs ---

@Composable
fun AddFolderDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var folderName by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New Folder") },
        text = {
            OutlinedTextField(
                value = folderName,
                onValueChange = { folderName = it },
                label = { Text("Folder Name") },
                placeholder = { Text("e.g. journals or projects/web") },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (folderName.trim().isNotEmpty()) {
                        onConfirm(folderName.trim())
                    }
                },
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun RenameFolderDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var newName by remember { mutableStateOf(currentName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename Folder") },
        text = {
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                label = { Text("Folder Name") },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (newName.trim().isNotEmpty() && newName.trim() != currentName) {
                        onConfirm(newName.trim())
                    }
                },
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Rename")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun DeleteFolderDialog(
    folderName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Folder?") },
        text = {
            Text("Are you sure you want to delete folder \"$folderName\" and all its contents? This will delete all notes stored within this nested path.")
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Delete Everything")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun RenameNoteDialog(
    note: Note,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var title by remember { mutableStateOf(note.title) }
    val initialFileName = note.filePath.substringAfterLast("/").removeSuffix(".md").removeSuffix(".txt")
    var fileName by remember { mutableStateOf(initialFileName) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename File") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Note Title") },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = fileName,
                    onValueChange = { fileName = it },
                    label = { Text("File Name (without extension)") },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.trim().isNotEmpty() && fileName.trim().isNotEmpty()) {
                        onConfirm(title.trim(), fileName.trim())
                    }
                },
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Rename")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}


