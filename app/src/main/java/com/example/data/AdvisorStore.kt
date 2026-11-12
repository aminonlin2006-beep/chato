package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.Calendar
import java.util.UUID

class AdvisorStore(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("advisor_prefs", Context.MODE_PRIVATE)

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    companion object {
        const val WELCOME_MESSAGE =
            "سلام. من مشاور شما هستم؛ در زمینهی روانشناسی، روانکاوی، روابط، زبان بدن و احساسات همراهتان هستم. هرچه بگویید با دقت میشنوم و تحلیل میکنم — بدون تایید بیجا و بدون حدس بیاساس. آمادهام؛ شروع کنید."

        private const val KEY_CONFIG = "advisor.config"
        private const val KEY_MEMORY = "advisor.memory"
        private const val KEY_THREADS = "advisor.threads"
        private const val KEY_ACTIVE = "advisor.activeThread"
    }

    var config: AdvisorConfig = loadConfig()
        private set

    var memory: AdvisorMemory = loadMemory()
        private set

    var threads: MutableList<ThreadItem> = loadThreads()
        private set

    var activeThreadId: String? = prefs.getString(KEY_ACTIVE, null)
        private set

    init {
        // If no active thread exists or threads is empty, ensure at least one initial thread with welcome
        if (threads.isEmpty()) {
            createThread("گفتگوی جدید", "", withWelcome = true)
        } else if (activeThreadId == null || threads.none { it.id == activeThreadId }) {
            activeThreadId = threads.last().id
            prefs.edit().putString(KEY_ACTIVE, activeThreadId).apply()
        }
    }

    fun uid(): String = UUID.randomUUID().toString().replace("-", "").take(10)

    private fun loadConfig(): AdvisorConfig {
        val s = prefs.getString(KEY_CONFIG, null) ?: return AdvisorConfig()
        return try {
            json.decodeFromString<AdvisorConfig>(s)
        } catch (e: Exception) {
            AdvisorConfig()
        }
    }

    private fun loadMemory(): AdvisorMemory {
        val s = prefs.getString(KEY_MEMORY, null) ?: return AdvisorMemory()
        return try {
            json.decodeFromString<AdvisorMemory>(s)
        } catch (e: Exception) {
            AdvisorMemory()
        }
    }

    private fun loadThreads(): MutableList<ThreadItem> {
        val s = prefs.getString(KEY_THREADS, null) ?: return mutableListOf()
        return try {
            json.decodeFromString<List<ThreadItem>>(s).toMutableList()
        } catch (e: Exception) {
            mutableListOf()
        }
    }

    fun persist() {
        prefs.edit()
            .putString(KEY_CONFIG, json.encodeToString(config))
            .putString(KEY_MEMORY, json.encodeToString(memory))
            .putString(KEY_THREADS, json.encodeToString(threads))
            .putString(KEY_ACTIVE, activeThreadId)
            .apply()
    }

    fun getActiveThread(): ThreadItem? {
        return threads.find { it.id == activeThreadId }
    }

    fun setActiveThread(threadId: String) {
        if (threads.any { it.id == threadId }) {
            activeThreadId = threadId
            persist()
        }
    }

    fun createThread(title: String? = null, contextSummary: String = "", withWelcome: Boolean = true): ThreadItem {
        val newThread = ThreadItem(
            id = uid(),
            title = if (!title.isNullOrBlank()) title else "گفتگوی جدید",
            createdAt = System.currentTimeMillis(),
            nodes = mutableListOf(),
            leafId = null,
            contextSummary = contextSummary
        )
        if (withWelcome) {
            val welcomeNode = MessageNode(
                id = uid(),
                parentId = null,
                role = "assistant",
                content = WELCOME_MESSAGE,
                ts = System.currentTimeMillis()
            )
            newThread.nodes.add(welcomeNode)
            newThread.leafId = welcomeNode.id
        }
        threads.add(newThread)
        activeThreadId = newThread.id
        persist()
        return newThread
    }

    fun activeBranchNodes(thread: ThreadItem): List<MessageNode> {
        if (thread.nodes.isEmpty()) return emptyList()
        val byId = thread.nodes.associateBy { it.id }
        val path = mutableListOf<MessageNode>()
        var cur = thread.leafId?.let { byId[it] } ?: thread.nodes.firstOrNull()
        while (cur != null) {
            path.add(0, cur)
            cur = cur.parentId?.let { byId[it] }
        }
        return path
    }

    fun addMessage(thread: ThreadItem, role: String, content: String): MessageNode {
        val node = MessageNode(
            id = uid(),
            parentId = thread.leafId,
            role = role,
            content = content,
            ts = System.currentTimeMillis()
        )
        thread.nodes.add(node)
        thread.leafId = node.id
        persist()
        return node
    }

    fun branchFrom(thread: ThreadItem, nodeId: String): MessageNode? {
        val parent = thread.nodes.find { it.id == nodeId } ?: return null
        val child = MessageNode(
            id = uid(),
            parentId = nodeId,
            role = "system",
            content = "— شاخه‌ی جدید از این پیام —",
            ts = System.currentTimeMillis()
        )
        thread.nodes.add(child)
        thread.leafId = child.id
        persist()
        return child
    }

    fun setActiveLeaf(thread: ThreadItem, leafId: String) {
        if (thread.nodes.any { it.id == leafId }) {
            thread.leafId = leafId
            persist()
        }
    }

    fun deleteThread(id: String) {
        threads.removeAll { it.id == id }
        if (activeThreadId == id) {
            activeThreadId = threads.lastOrNull()?.id
        }
        if (threads.isEmpty()) {
            createThread("گفتگوی جدید", "", withWelcome = true)
        } else {
            persist()
        }
    }

    fun allLeaves(thread: ThreadItem): List<MessageNode> {
        val parentIds = thread.nodes.mapNotNull { it.parentId }.toSet()
        return thread.nodes.filter { !parentIds.contains(it.id) }
    }

    private fun todayKey(): String {
        val cal = Calendar.getInstance()
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1
        val day = cal.get(Calendar.DAY_OF_MONTH)
        return "$year-$month-$day"
    }

    fun recordUsage(promptTokens: Long, completionTokens: Long) {
        val total = promptTokens + completionTokens
        if (total <= 0) return
        val u = memory.usage
        u.total += total
        val key = todayKey()
        if (u.dayKey != key) {
            u.dayKey = key
            u.day = 0
        }
        u.day += total
        persist()
    }

    fun dayTokens(): Long {
        val u = memory.usage
        val key = todayKey()
        if (u.dayKey != key) {
            u.dayKey = key
            u.day = 0
            persist()
        }
        return u.day
    }

    fun mergeObservations(obs: List<Observation>) {
        obs.forEach { o ->
            val text = o.text.trim()
            if (text.isEmpty()) return@forEach
            val kind = if (o.kind == "hypothesis") "hypothesis" else "explicit"
            if (kind == "explicit") {
                val existing = memory.recent.find { it.text == text }
                if (existing != null) {
                    existing.count += 1
                    if (existing.count >= 2 && memory.golden.none { it.text == text }) {
                        memory.golden.add(Observation(text = text, kind = "explicit"))
                    }
                } else {
                    memory.recent.add(Observation(text = text, count = 1, kind = "explicit"))
                }
            } else {
                memory.recent.add(Observation(text = text, count = 1, kind = "hypothesis"))
            }
        }
        if (memory.recent.size > 15) {
            memory.recent = memory.recent.takeLast(15).toMutableList()
        }
        persist()
    }

    fun buildProfileText(): String {
        val lines = mutableListOf<String>()
        lines.add("شناخت کاربر (خروجی قابل حمل)")
        lines.add("====================")
        if (memory.golden.isNotEmpty()) {
            lines.add("واقعیتهای پایدار:")
            memory.golden.forEach { lines.add("- ${it.text}") }
        }
        if (memory.recent.isNotEmpty()) {
            lines.add("مشاهدات اخیر:")
            memory.recent.forEach { lines.add("- ${it.text}") }
        }
        if (memory.summary.isNotEmpty()) {
            lines.add("خلاصهی گفتگوهای قبلی:")
            memory.summary.forEach { lines.add("- $it") }
        }
        return lines.joinToString("\n")
    }

    fun updateProfileSnapshot() {
        memory.profileSnapshot = buildProfileText()
        persist()
    }

    fun exportProfileSnapshot(): ExportVersion {
        val text = buildProfileText()
        memory.profileSnapshot = text
        val entry = ExportVersion(ts = System.currentTimeMillis(), text = text)
        memory.exports.add(entry)
        if (memory.exports.size > 50) {
            memory.exports = memory.exports.takeLast(50).toMutableList()
        }
        persist()
        return entry
    }

    fun checkAutoExport(): Boolean {
        val min = config.autoExportMin
        if (min <= 0) return false
        val now = System.currentTimeMillis()
        val last = config.lastExport
        if (now - last >= min * 60000L) {
            config.lastExport = now
            exportProfileSnapshot()
            return true
        }
        return false
    }
}
