package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.ui.AdvisorViewModel
import com.example.ui.DialogType
import com.example.ui.components.*
import com.example.ui.theme.*

class MainActivity : ComponentActivity() {

    private val viewModel: AdvisorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MoshaverTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    MoshaverApp(viewModel)
                }
            }
        }
    }
}

@Composable
fun MoshaverApp(viewModel: AdvisorViewModel) {
    val threads by viewModel.threads.collectAsState()
    val activeThread by viewModel.activeThread.collectAsState()
    val activeBranch by viewModel.activeBranch.collectAsState()
    val leaves by viewModel.leaves.collectAsState()
    val config by viewModel.config.collectAsState()
    val memory by viewModel.memory.collectAsState()
    val dayTokens by viewModel.dayTokens.collectAsState()
    val isBusy by viewModel.isBusy.collectAsState()
    val isTyping by viewModel.isTyping.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val activeDialog by viewModel.activeDialog.collectAsState()
    val threadToDelete by viewModel.threadToDelete.collectAsState()
    val currentSummary by viewModel.currentSummary.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()
    val sidebarOpen by viewModel.sidebarOpen.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short)
            viewModel.clearToast()
        }
    }

    // Handle back button: close sidebar if open
    BackHandler(enabled = sidebarOpen) {
        viewModel.setSidebarOpen(false)
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("main_scaffold"),
        containerColor = ColorBg,
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .padding(bottom = 60.dp)
                    .testTag("app_snackbar")
            ) { data ->
                Snackbar(
                    containerColor = ColorPanel2,
                    contentColor = ColorText,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp)
                ) {
                    Text(text = data.visuals.message, style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        topBar = {
            AdvisorTopBar(
                dayTokens = dayTokens,
                dailyBudget = config.dailyBudget,
                onNewChat = { viewModel.createNewChat() },
                onSummarize = { viewModel.summarizeCurrentConversation() },
                onContinueInNewChat = { viewModel.continueInNewChat() },
                onToggleSidebar = { viewModel.toggleSidebar() },
                onOpenProfile = { viewModel.openDialog(DialogType.PROFILE) },
                onOpenSettings = { viewModel.openDialog(DialogType.SETTINGS) },
                isBusy = isBusy
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Main Chat Area
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("chat_layout")
            ) {
                ChatMessageList(
                    nodes = activeBranch,
                    isTyping = isTyping,
                    onBranchFrom = { nodeId -> viewModel.branchFromNode(nodeId) },
                    modifier = Modifier.weight(1f)
                )

                ChatComposer(
                    isBusy = isBusy,
                    onSendMessage = { text -> viewModel.sendMessage(text) }
                )
            }

            // Animated Sidebar overlay for threads & branches
            AnimatedVisibility(
                visible = sidebarOpen,
                enter = slideInHorizontally(initialOffsetX = { -it }) + fadeIn(),
                exit = slideOutHorizontally(targetOffsetX = { -it }) + fadeOut(),
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                SidebarContent(
                    threads = threads,
                    activeThreadId = activeThread?.id,
                    leaves = leaves,
                    activeLeafId = activeThread?.leafId,
                    searchQuery = searchQuery,
                    onSearchChange = { viewModel.setSearchQuery(it) },
                    onSelectThread = {
                        viewModel.selectThread(it)
                        viewModel.setSidebarOpen(false)
                    },
                    onDeleteThread = { viewModel.requestDeleteThread(it) },
                    onSelectLeaf = {
                        viewModel.selectLeaf(it)
                        viewModel.setSidebarOpen(false)
                    },
                    modifier = Modifier
                        .width(280.dp)
                        .fillMaxHeight()
                )
            }
        }
    }

    // Settings Dialog
    if (activeDialog == DialogType.SETTINGS) {
        SettingsDialog(
            initialConfig = config,
            onSave = { updated -> viewModel.updateConfig(updated) },
            onDismiss = { viewModel.closeDialog() }
        )
    }

    // Profile Dialog
    if (activeDialog == DialogType.PROFILE) {
        ProfileDialog(
            memory = memory,
            onSaveProfile = { golden, recent -> viewModel.updateProfile(golden, recent) },
            onManualExport = { viewModel.manualExportProfile() },
            onDismiss = { viewModel.closeDialog() },
            showToast = { viewModel.showToast(it) }
        )
    }

    // Manual Summary Dialog
    if (activeDialog == DialogType.SUMMARY && currentSummary != null) {
        SummaryDialog(
            summary = currentSummary ?: "",
            onDismiss = { viewModel.closeDialog() },
            showToast = { viewModel.showToast(it) }
        )
    }

    // Delete Thread Confirmation Dialog
    if (activeDialog == DialogType.DELETE_THREAD && threadToDelete != null) {
        AlertDialog(
            onDismissRequest = { viewModel.closeDialog() },
            containerColor = ColorPanel,
            title = {
                Text(
                    text = "حذف گفتگو",
                    style = MaterialTheme.typography.titleMedium,
                    color = ColorText
                )
            },
            text = {
                Text(
                    text = "آیا از حذف گفتگوی «${threadToDelete?.title}» اطمینان دارید؟",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ColorMuted
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmDeleteThread() },
                    colors = ButtonDefaults.buttonColors(containerColor = ColorDanger),
                    modifier = Modifier.testTag("confirm_delete_thread_btn")
                ) {
                    Text("حذف", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { viewModel.closeDialog() },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ColorText),
                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(ColorBorder))
                ) {
                    Text("انصراف")
                }
            }
        )
    }
}
