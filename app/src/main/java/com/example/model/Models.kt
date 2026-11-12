package com.example.model

import kotlinx.serialization.Serializable

@Serializable
data class MessageNode(
    val id: String,
    val parentId: String? = null,
    val role: String, // "user", "assistant", "system"
    val content: String,
    val ts: Long = System.currentTimeMillis()
)

@Serializable
data class ThreadItem(
    val id: String,
    var title: String = "گفتگوی جدید",
    val createdAt: Long = System.currentTimeMillis(),
    val nodes: MutableList<MessageNode> = mutableListOf(),
    var leafId: String? = null,
    var contextSummary: String = "",
    var autoSummary: String = ""
)

@Serializable
data class Specialty(
    var name: String = "",
    var instruction: String = ""
)

@Serializable
data class Observation(
    val text: String,
    var count: Int = 1,
    val kind: String = "explicit" // "explicit" or "hypothesis"
)

@Serializable
data class ExportVersion(
    val ts: Long = System.currentTimeMillis(),
    val text: String
)

@Serializable
data class UsageData(
    var total: Long = 0,
    var day: Long = 0,
    var dayKey: String = ""
)

@Serializable
data class AdvisorConfig(
    var baseUrl: String = "https://api.openai.com",
    var model: String = "",
    var apiKey: String = "",
    var temperature: Float = 0.7f,
    var maxTokens: Int = 1200,
    var specialties: MutableList<Specialty> = mutableListOf(),
    var autoProfile: Boolean = true,
    var autoExportMin: Int = 0,
    var lastExport: Long = 0L,
    var dailyBudget: Long = 50000L
)

@Serializable
data class AdvisorMemory(
    var golden: MutableList<Observation> = mutableListOf(),
    var recent: MutableList<Observation> = mutableListOf(),
    var summary: MutableList<String> = mutableListOf(),
    var usage: UsageData = UsageData(),
    var profileSnapshot: String = "",
    var exports: MutableList<ExportVersion> = mutableListOf()
)

data class TokenUsage(
    val promptTokens: Long = 0,
    val completionTokens: Long = 0
) {
    val totalTokens: Long get() = promptTokens + completionTokens
}

data class ChatResult(
    val content: String,
    val usage: TokenUsage?
)

data class SummarizeResult(
    val summary: String,
    val usage: TokenUsage?
)

data class ExtractResult(
    val observations: List<Observation>,
    val usage: TokenUsage?
)
