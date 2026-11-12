package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.api.AdvisorApi
import com.example.data.AdvisorStore
import com.example.model.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class DialogType {
    NONE, SETTINGS, PROFILE, DELETE_THREAD, SUMMARY
}

class AdvisorViewModel(application: Application) : AndroidViewModel(application) {

    private val store = AdvisorStore(application)
    private val api = AdvisorApi()

    private val _threads = MutableStateFlow<List<ThreadItem>>(emptyList())
    val threads: StateFlow<List<ThreadItem>> = _threads.asStateFlow()

    private val _activeThread = MutableStateFlow<ThreadItem?>(null)
    val activeThread: StateFlow<ThreadItem?> = _activeThread.asStateFlow()

    private val _activeBranch = MutableStateFlow<List<MessageNode>>(emptyList())
    val activeBranch: StateFlow<List<MessageNode>> = _activeBranch.asStateFlow()

    private val _leaves = MutableStateFlow<List<MessageNode>>(emptyList())
    val leaves: StateFlow<List<MessageNode>> = _leaves.asStateFlow()

    private val _config = MutableStateFlow(store.config)
    val config: StateFlow<AdvisorConfig> = _config.asStateFlow()

    private val _memory = MutableStateFlow(store.memory)
    val memory: StateFlow<AdvisorMemory> = _memory.asStateFlow()

    private val _dayTokens = MutableStateFlow(store.dayTokens())
    val dayTokens: StateFlow<Long> = _dayTokens.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _isTyping = MutableStateFlow(false)
    val isTyping: StateFlow<Boolean> = _isTyping.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _activeDialog = MutableStateFlow(DialogType.NONE)
    val activeDialog: StateFlow<DialogType> = _activeDialog.asStateFlow()

    private val _threadToDelete = MutableStateFlow<ThreadItem?>(null)
    val threadToDelete: StateFlow<ThreadItem?> = _threadToDelete.asStateFlow()

    private val _currentSummary = MutableStateFlow<String?>(null)
    val currentSummary: StateFlow<String?> = _currentSummary.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private val _sidebarOpen = MutableStateFlow(false)
    val sidebarOpen: StateFlow<Boolean> = _sidebarOpen.asStateFlow()

    init {
        refreshState()
        startAutoExportMonitor()
    }

    fun toggleSidebar() {
        _sidebarOpen.value = !_sidebarOpen.value
    }

    fun setSidebarOpen(open: Boolean) {
        _sidebarOpen.value = open
    }

    fun showToast(msg: String) {
        _toastMessage.value = msg
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    fun openDialog(type: DialogType) {
        _activeDialog.value = type
    }

    fun closeDialog() {
        _activeDialog.value = DialogType.NONE
        _threadToDelete.value = null
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    private fun refreshState() {
        _threads.value = store.threads.toList()
        val cur = store.getActiveThread()
        _activeThread.value = cur
        if (cur != null) {
            _activeBranch.value = store.activeBranchNodes(cur)
            _leaves.value = store.allLeaves(cur)
        } else {
            _activeBranch.value = emptyList()
            _leaves.value = emptyList()
        }
        _config.value = store.config
        _memory.value = store.memory
        _dayTokens.value = store.dayTokens()
    }

    fun selectThread(threadId: String) {
        store.setActiveThread(threadId)
        refreshState()
    }

    fun createNewChat() {
        store.createThread("گفتگوی جدید", "", withWelcome = true)
        refreshState()
        showToast("گفتگوی جدید آغاز شد")
    }

    fun requestDeleteThread(thread: ThreadItem) {
        _threadToDelete.value = thread
        _activeDialog.value = DialogType.DELETE_THREAD
    }

    fun confirmDeleteThread() {
        val t = _threadToDelete.value ?: return
        store.deleteThread(t.id)
        closeDialog()
        refreshState()
        showToast("گفتگو حذف شد")
    }

    fun branchFromNode(nodeId: String) {
        val cur = store.getActiveThread() ?: return
        store.branchFrom(cur, nodeId)
        refreshState()
        showToast("شاخه‌ی جدید ایجاد شد")
    }

    fun selectLeaf(leafId: String) {
        val cur = store.getActiveThread() ?: return
        store.setActiveLeaf(cur, leafId)
        refreshState()
    }

    fun sendMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return

        val cfg = store.config
        if (cfg.model.isBlank() || cfg.apiKey.isBlank()) {
            showToast("ابتدا در «تنظیمات» نام مدل و کلید API را وارد کنید.")
            openDialog(DialogType.SETTINGS)
            return
        }

        val budget = cfg.dailyBudget
        if (budget > 0 && store.dayTokens() >= budget) {
            showToast("به سقف بودجه‌ی روزانه‌ی توکن رسیده‌اید ($budget توکن). سقف را در تنظیمات افزایش دهید.")
            return
        }

        var t = store.getActiveThread()
        if (t == null) {
            t = store.createThread(trimmed.take(40), "", withWelcome = false)
        } else if (t.title == "گفتگوی جدید") {
            t.title = trimmed.take(40)
            store.persist()
        }

        store.addMessage(t, "user", trimmed)
        refreshState()

        val nodes = store.activeBranchNodes(t)

        viewModelScope.launch {
            _isBusy.value = true
            _isTyping.value = true
            try {
                val apiMessages = mutableListOf<Pair<String, String>>()
                // If the user previously created a manual summary for this thread, include it
                if (t.autoSummary.isNotBlank()) {
                    apiMessages.add(Pair("system", "خلاصه‌ی بخش‌های قبلی گفتگو:\n${t.autoSummary}"))
                }
                nodes.forEach { n ->
                    if (n.role != "system") {
                        apiMessages.add(Pair(if (n.role == "user") "user" else "assistant", n.content))
                    }
                }

                val systemPrompt = AdvisorApi.buildSystemPrompt(cfg, store.memory, t)
                apiMessages.add(0, Pair("system", systemPrompt))

                val reply = api.chat(cfg, apiMessages)
                _isTyping.value = false
                store.addMessage(t, "assistant", reply.content)
                reply.usage?.let { store.recordUsage(it.promptTokens, it.completionTokens) }

                if (cfg.autoProfile) {
                    store.updateProfileSnapshot()
                }
                refreshState()
            } catch (e: Exception) {
                _isTyping.value = false
                showToast("خطا: ${e.localizedMessage ?: e.message}")
            } finally {
                _isBusy.value = false
                _isTyping.value = false
                refreshState()
            }
        }
    }

    /**
     * Manual summarization triggered at the user's discretion.
     */
    fun summarizeCurrentConversation() {
        val t = store.getActiveThread()
        if (t == null || t.nodes.isEmpty()) {
            showToast("ابتدا یک گفتگو داشته باشید.")
            return
        }

        val cfg = store.config
        if (cfg.model.isBlank() || cfg.apiKey.isBlank()) {
            showToast("ابتدا در «تنظیمات» نام مدل و کلید API را وارد کنید.")
            openDialog(DialogType.SETTINGS)
            return
        }

        val conversation = store.activeBranchNodes(t)
            .filter { it.role != "system" }
            .map { Pair(if (it.role == "user") "user" else "assistant", it.content) }

        if (conversation.isEmpty()) {
            showToast("پیامی برای خلاصه‌سازی وجود ندارد.")
            return
        }

        viewModelScope.launch {
            _isBusy.value = true
            showToast("در حال خلاصه‌سازی گفتگو…")
            try {
                val sumRes = api.summarize(cfg, conversation)
                sumRes.usage?.let { store.recordUsage(it.promptTokens, it.completionTokens) }

                t.autoSummary = sumRes.summary
                if (!store.memory.summary.contains(sumRes.summary)) {
                    store.memory.summary.add(sumRes.summary)
                }
                store.persist()

                _currentSummary.value = sumRes.summary
                openDialog(DialogType.SUMMARY)
                refreshState()
                showToast("خلاصه‌سازی گفتگو با موفقیت انجام شد.")
            } catch (e: Exception) {
                showToast("خطا در خلاصه‌سازی: ${e.localizedMessage ?: e.message}")
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun continueInNewChat() {
        val t = store.getActiveThread()
        if (t == null || t.nodes.isEmpty()) {
            showToast("ابتدا یک گفتگو داشته باشید.")
            return
        }

        val cfg = store.config
        if (cfg.model.isBlank() || cfg.apiKey.isBlank()) {
            showToast("ابتدا در «تنظیمات» نام مدل و کلید API را وارد کنید.")
            openDialog(DialogType.SETTINGS)
            return
        }

        val conversation = store.activeBranchNodes(t)
            .filter { it.role != "system" }
            .map { Pair(if (it.role == "user") "user" else "assistant", it.content) }

        viewModelScope.launch {
            _isBusy.value = true
            try {
                val sumRes = api.summarize(cfg, conversation)
                val extRes = api.extract(cfg, conversation)
                sumRes.usage?.let { store.recordUsage(it.promptTokens, it.completionTokens) }
                extRes.usage?.let { store.recordUsage(it.promptTokens, it.completionTokens) }

                store.mergeObservations(extRes.observations)
                store.createThread(null, sumRes.summary, withWelcome = true)
                refreshState()
                showToast("گفتگوی جدید با حفظ حافظه ساخته شد.")
            } catch (e: Exception) {
                showToast("خطا: ${e.localizedMessage ?: e.message}")
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun updateConfig(newConfig: AdvisorConfig) {
        store.config.baseUrl = newConfig.baseUrl.trim().ifEmpty { "https://api.openai.com" }
        store.config.model = newConfig.model.trim()
        store.config.apiKey = newConfig.apiKey.trim()
        store.config.temperature = newConfig.temperature
        store.config.maxTokens = newConfig.maxTokens
        store.config.autoProfile = newConfig.autoProfile
        store.config.autoExportMin = newConfig.autoExportMin
        store.config.dailyBudget = newConfig.dailyBudget
        store.config.specialties = newConfig.specialties.filter { it.name.isNotBlank() }.toMutableList()
        store.persist()
        refreshState()
        closeDialog()
        showToast("تنظیمات با موفقیت ذخیره شد.")
    }

    fun updateProfile(goldenLines: List<String>, recentLines: List<String>) {
        val mem = store.memory
        mem.golden = goldenLines.map { it.trim() }.filter { it.isNotEmpty() }
            .map { Observation(text = it, count = 2, kind = "explicit") }.toMutableList()
        mem.recent = recentLines.map { it.trim() }.filter { it.isNotEmpty() }
            .map { Observation(text = it, count = 1, kind = "explicit") }.toMutableList()
        store.updateProfileSnapshot()
        refreshState()
        showToast("تغییرات پروفایل ذخیره شد.")
    }

    fun manualExportProfile() {
        store.exportProfileSnapshot()
        refreshState()
        showToast("نسخه‌ی زمان‌دار پروفایل ذخیره شد.")
    }

    private fun startAutoExportMonitor() {
        viewModelScope.launch {
            while (true) {
                delay(30000L) // check every 30 seconds as in original web app
                if (store.checkAutoExport()) {
                    refreshState()
                    showToast("پروفایل به‌صورت خودکار ذخیره شد.")
                }
            }
        }
    }
}
