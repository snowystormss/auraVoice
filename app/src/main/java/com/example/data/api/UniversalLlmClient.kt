package com.example.data.api

import androidx.compose.runtime.Immutable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.ConnectionPool
import okhttp3.Dispatcher
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

@Immutable
data class LlmSettings(
    val apiUrl: String = "https://generativelanguage.googleapis.com",
    val apiKey: String = "",
    val modelName: String = "gemini-2.5-flash",
    val systemPrompt: String = "keep answers concise, natural, and conversational."
)

class UniversalLlmClient {

    private val lowFootprintDispatcher = Dispatcher().apply {
        maxRequests = 4
        maxRequestsPerHost = 2
    }

    private val client = OkHttpClient.Builder()
        .dispatcher(lowFootprintDispatcher)
        .connectionPool(ConnectionPool(1, 30, TimeUnit.SECONDS))
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    fun streamChat(
        settings: LlmSettings,
        history: List<Pair<String, String>>,
        prompt: String
    ): Flow<String> = flow {
        val cleanUrl = settings.apiUrl.trim().trimEnd('/')
        val isGeminiNative = (cleanUrl.contains("generativelanguage.googleapis.com") &&
            !cleanUrl.contains("/openai")) ||
            (cleanUrl.isEmpty() && settings.apiKey.startsWith("AIza"))

        val request = if (isGeminiNative) {
            buildGeminiRequest(cleanUrl, settings, history, prompt)
        } else {
            buildOpenAiRequest(cleanUrl, settings, history, prompt)
        }

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                error("http ${response.code}: ${response.message}")
            }
            val source = response.body?.source() ?: return@use

            while (!source.exhausted()) {
                val line = source.readUtf8Line() ?: break
                if (!line.startsWith("data:")) continue
                val data = line.removePrefix("data:").trim()
                if (data.isEmpty() || data == "[DONE]") continue

                val token = runCatching {
                    val json = JSONObject(data)
                    if (isGeminiNative) {
                        json.optJSONArray("candidates")
                            ?.optJSONObject(0)
                            ?.optJSONObject("content")
                            ?.optJSONArray("parts")
                            ?.optJSONObject(0)
                            ?.optString("text")
                    } else {
                        json.optJSONArray("choices")
                            ?.optJSONObject(0)
                            ?.optJSONObject("delta")
                            ?.optString("content")
                    }
                }.getOrNull()

                if (!token.isNullOrEmpty()) {
                    emit(token)
                }
            }
        }
    }.flowOn(Dispatchers.IO)

    private fun buildGeminiRequest(
        cleanUrl: String,
        settings: LlmSettings,
        history: List<Pair<String, String>>,
        prompt: String
    ): Request {
        val base = cleanUrl.ifEmpty { "https://generativelanguage.googleapis.com" }
        val model = settings.modelName.ifBlank { "gemini-2.5-flash" }.removePrefix("models/")
        val url = "$base/v1beta/models/$model:streamGenerateContent?alt=sse&key=${settings.apiKey}"

        val contentsArray = JSONArray()
        for ((role, text) in history.takeLast(6)) {
            contentsArray.put(
                JSONObject()
                    .put("role", if (role == "user") "user" else "model")
                    .put("parts", JSONArray().put(JSONObject().put("text", text)))
            )
        }
        contentsArray.put(
            JSONObject()
                .put("role", "user")
                .put("parts", JSONArray().put(JSONObject().put("text", prompt)))
        )

        val root = JSONObject().put("contents", contentsArray)
        if (settings.systemPrompt.isNotBlank()) {
            root.put(
                "systemInstruction",
                JSONObject().put("parts", JSONArray().put(JSONObject().put("text", settings.systemPrompt)))
            )
        }

        return Request.Builder()
            .url(url)
            .post(root.toString().toRequestBody("application/json".toMediaType()))
            .build()
    }

    private fun buildOpenAiRequest(
        cleanUrl: String,
        settings: LlmSettings,
        history: List<Pair<String, String>>,
        prompt: String
    ): Request {
        val endpointUrl = when {
            cleanUrl.endsWith("/chat/completions") -> cleanUrl
            cleanUrl.endsWith("/v1") -> "$cleanUrl/chat/completions"
            else -> "$cleanUrl/v1/chat/completions"
        }

        val messagesArray = JSONArray()
        if (settings.systemPrompt.isNotBlank()) {
            messagesArray.put(
                JSONObject().put("role", "system").put("content", settings.systemPrompt)
            )
        }
        for ((role, text) in history.takeLast(6)) {
            messagesArray.put(
                JSONObject()
                    .put("role", if (role == "user") "user" else "assistant")
                    .put("content", text)
            )
        }
        messagesArray.put(
            JSONObject().put("role", "user").put("content", prompt)
        )

        val root = JSONObject()
            .put("model", settings.modelName.ifBlank { "local-model" })
            .put("messages", messagesArray)
            .put("stream", true)

        val builder = Request.Builder()
            .url(endpointUrl)
            .post(root.toString().toRequestBody("application/json".toMediaType()))

        if (settings.apiKey.isNotBlank()) {
            builder.addHeader("Authorization", "Bearer ${settings.apiKey}")
        }
        return builder.build()
    }
}