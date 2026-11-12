package com.example.api

import com.example.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class AdvisorApi {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    companion object {
        const val PERSONA = "شما یک مشاور حرفه‌ای در زمینه‌ی روانشناسی، روانکاوی، درک روابط، شناخت زبان بدن و درک احساسات هستید."

        val RULES = """
            قوانین رفتاری (غیرقابل نقض):
            ۱. هیچ‌گاه تاییدطلبی نکنید. هرگز «حق با شماست» یا «احساساتتان کاملاً درست است» نگویید مگر اینکه مستدل باشد. حق دارید چارچوب کاربر را به چالش بکشید.
            ۲. هر ادعایی که می‌کنید باید یکی از این سه باشد:
               - بازگویی آنچه خود کاربر گفته است (واقعیت گفتار او)، یا
               - تفسیر شما، که همیشه با برچسب «فرضیه» همراه است، یا
               - یک سؤال روشنگر.
            ۳. اگر چیزی مبهم است و ابهام آن بر تحلیل اثر دارد، یک سؤال بپرسید — اما همیشه همراه با یک فرضیه‌ی برچسب‌خورده به‌عنوان تحلیل مقدماتی.
            ۴. هرگز حدس بی‌جا نزنید و سرسری جواب ندهید. اگر اطلاعات کافی ندارید، صریحاً بگویید و سؤال بپرسید.
            ۵. اگر کاربر درباره‌ی موضوعی (مثلاً اعتیاد) صحبت کرد، روی همان موضوع تمرکز کنید؛ اما اگر آن موضوع با جنبه‌ی دیگری (مثلاً رابطه) درهم‌تنیده بود، نقطه‌ی تلاقی را تحلیل کنید و آنها را به‌زور جدا نکنید.
            ۶. به فارسی پاسخ دهید و راست‌چین باشید. از اصطلاحات لاتین استفاده نکنید. متن را جدول‌بندی نکنید.
        """.trimIndent()

        fun buildSystemPrompt(config: AdvisorConfig, memory: AdvisorMemory, thread: ThreadItem?): String {
            val lines = mutableListOf<String>()
            lines.add(PERSONA)
            lines.add("")
            lines.add(RULES)

            val specialties = config.specialties.filter { it.name.isNotBlank() && it.instruction.isNotBlank() }
            specialties.forEach { s ->
                lines.add("")
                lines.add("== تخصص: ${s.name} ==")
                lines.add(s.instruction)
            }

            lines.add("")
            lines.add("== شناخت کاربر ==")
            if (thread != null && thread.contextSummary.isNotBlank()) {
                lines.add("خلاصه‌ی گفتگوی قبلی:")
                lines.add(thread.contextSummary)
            }
            if (memory.golden.isNotEmpty()) {
                lines.add("واقعیت‌های پایدار درباره‌ی کاربر:")
                memory.golden.forEach { lines.add("- ${it.text}") }
            }
            if (memory.recent.isNotEmpty()) {
                lines.add("مشاهدات اخیر:")
                memory.recent.forEach { lines.add("- ${it.text}") }
            }
            if (memory.summary.isNotEmpty()) {
                lines.add("خلاصه‌ی گفتگوهای قدیمی‌تر:")
                memory.summary.forEach { lines.add("- $it") }
            }

            return lines.joinToString("\n")
        }
    }

    private suspend fun callLLM(
        config: AdvisorConfig,
        messages: List<Pair<String, String>>
    ): Pair<String, TokenUsage?> = withContext(Dispatchers.IO) {
        val base = config.baseUrl.trim().trimEnd('/')
        if (base.isEmpty()) throw IllegalArgumentException("آدرس پایه API تنظیم نشده است")
        if (config.model.isBlank()) throw IllegalArgumentException("نام مدل در تنظیمات مشخص نشده است")
        if (config.apiKey.isBlank()) throw IllegalArgumentException("کلید API در تنظیمات وارد نشده است")

        val url = "$base/v1/chat/completions"

        val messagesJsonArray = buildJsonArray {
            messages.forEach { (role, content) ->
                addJsonObject {
                    put("role", role)
                    put("content", content)
                }
            }
        }

        val requestBodyJson = buildJsonObject {
            put("model", config.model)
            put("messages", messagesJsonArray)
            put("temperature", config.temperature)
            put("max_tokens", config.maxTokens)
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val request = Request.Builder()
            .url(url)
            .addHeader("Content-Type", "application/json")
            .addHeader("Authorization", "Bearer ${config.apiKey.trim()}")
            .post(requestBodyJson.toString().toRequestBody(mediaType))
            .build()

        client.newCall(request).execute().use { response ->
            val responseBody = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                val errorSnippet = responseBody.take(400)
                throw RuntimeException("خطای API (${response.code}): $errorSnippet")
            }

            val parsedJson = json.parseToJsonElement(responseBody).jsonObject
            val choices = parsedJson["choices"]?.jsonArray
            val firstChoice = choices?.firstOrNull()?.jsonObject
            val content = firstChoice?.get("message")?.jsonObject?.get("content")?.jsonPrimitive?.content
                ?: throw RuntimeException("پاسخ مدل ناقص بود")

            val usageElement = parsedJson["usage"]?.jsonObject
            val usage = if (usageElement != null) {
                val promptTokens = usageElement["prompt_tokens"]?.jsonPrimitive?.longOrNull ?: 0L
                val completionTokens = usageElement["completion_tokens"]?.jsonPrimitive?.longOrNull ?: 0L
                TokenUsage(promptTokens, completionTokens)
            } else null

            Pair(content, usage)
        }
    }

    suspend fun chat(
        config: AdvisorConfig,
        messages: List<Pair<String, String>>
    ): ChatResult {
        val (content, usage) = callLLM(config, messages)
        return ChatResult(content = content, usage = usage)
    }

    suspend fun summarize(
        config: AdvisorConfig,
        conversation: List<Pair<String, String>>
    ): SummarizeResult {
        val sysPrompt = "خلاصهی فشرده و ساختاریافته از گفتگوی زیر بنویس. فقط خلاصه را برگردان، بدون توضیح اضافه. طول خلاصه حداکثر ۱۵۰ کلمه باشد."
        val allMessages = listOf(Pair("system", sysPrompt)) + conversation
        val (content, usage) = callLLM(config, allMessages)
        return SummarizeResult(summary = content, usage = usage)
    }

    suspend fun extract(
        config: AdvisorConfig,
        conversation: List<Pair<String, String>>
    ): ExtractResult {
        val sysPrompt = "از گفتگوی زیر، واقعیتهای مربوط به کاربر را استخراج کن. هر واقعیت باید دقیقاً همان چیزی باشد که کاربر گفته (نه تفسیر شما). خروجی را بهصورت JSON آرایهی اشیا با کلیدهای text و kind بده؛ kind یا \"explicit\" است (آنچه کاربر صریحاً گفته) یا \"hypothesis\" (برداشت شما). فقط JSON برگردان، هیچ چیز دیگری."
        val allMessages = listOf(Pair("system", sysPrompt)) + conversation
        val (content, usage) = callLLM(config, allMessages)

        val observations = mutableListOf<Observation>()
        try {
            val cleaned = content.replace("```json", "").replace("```", "").trim()
            val parsedArray = json.parseToJsonElement(cleaned).jsonArray
            parsedArray.forEach { elem ->
                val obj = elem.jsonObject
                val text = obj["text"]?.jsonPrimitive?.contentOrNull ?: ""
                val kind = obj["kind"]?.jsonPrimitive?.contentOrNull ?: "explicit"
                if (text.isNotBlank()) {
                    observations.add(Observation(text = text, kind = kind))
                }
            }
        } catch (e: Exception) {
            // Ignored, fallback to empty list
        }
        return ExtractResult(observations = observations, usage = usage)
    }
}
