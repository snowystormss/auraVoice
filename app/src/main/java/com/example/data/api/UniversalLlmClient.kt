package com.example.data.api

import androidx.compose.runtime.Immutable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import okhttp3.ConnectionPool
import okhttp3.Dispatcher
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

@Immutable
data class LlmSettings(
    val apiUrl: String = "https://generativelanguage.googleapis.com",
    val apiKey: String = "",
    val modelName: String = "gemini-2.5-flash",
    val systemPrompt: String = "you are a helpful assistant. keep answers concise and conversational for voice synthesis."
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

    private val sseFactory = EventSources.createFactory(client)

    fun streamChat(
        settings: LlmSettings,
        history: List<Pair<String, String>>,
        prompt: String
    ): Flow<String> = callbackFlow {
        val cleanUrl = settings.apiUrl.trim().trimEnd('/')
        val isGeminiNative = cleanUrl.contains("generativelanguage.googleapis.com") &&
            !cleanUrl.contains("/openai") ||
            (cleanUrl.isEmpty() && settings.apiKey.startsWith("AIza"))

        if (isGeminiNative) {
            streamGemini(cleanUrl, settings, history, prompt, this)
        } else {
            streamOpenAiCompatible(cleanUrl, settings, history, prompt, this)
        }
        awaitClose {}
    }.flowOn(Dispatchers.IO)

    private fun streamGemini(
        cleanUrl: String,
        settings: LlmSettings,
        history: List<Pair<String, String>>,
        prompt: String,
        scope: ProducerScope<String>
    ) {
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

        val request = Request.Builder()
            .url(url)
            .post(root.toString().toRequestBody("application/json".toMediaType()))
            .build()

        sseFactory.newEventSource(request, object : EventSourceListener() {
            override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                runCatching {
                    val chunk = JSONObject(data)
                        .optJSONArray("candidates")
                        ?.optJSONObject(0)
                        ?.optJSONObject("content")
                        ?.optJSONArray("parts")
                        ?.optJSONObject(0)
                        ?.optString("text")
                    if (!chunk.isNullOrEmpty()) {
                        scope.trySend(chunk)
                    }
                }
            }

            override fun onClosed(eventSource: EventSource) {
                scope.close()
            }

            override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                scope.close(t ?: Exception("gemini error: http ${response?.code ?: "connection failed"}"))
            }
        })
    }

    private fun streamOpenAiCompatible(
        cleanUrl: String,
        settings: LlmSettings,
        history: List<Pair<String, String>>,
        prompt: String,
        scope: ProducerScope<String>
    ) {
        // automatically normalize urls for openrouter, openai, and llama.cpp
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

        val requestBuilder = Request.Builder()
            .url(endpointUrl)
            .post(root.toString().toRequestBody("application/json".toMediaType()))

        if (settings.apiKey.isNotBlank()) {
            requestBuilder.addHeader("Authorization", "Bearer ${settings.apiKey}")
        }

        sseFactory.newEventSource(requestBuilder.build(), object : EventSourceListener() {
            override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                if (data.trim() == "[DONE]") {
                    scope.close()
                    return
                }
                runCatching {
                    val delta = JSONObject(data)
                        .optJSONArray("choices")
                        ?.optJSONObject(0)
                        ?.optJSONObject("delta")
                        ?.optString("content")
                    if (!delta.isNullOrEmpty()) {
                        scope.trySend(delta)
                    }
                }
            }

            override fun onClosed(eventSource: EventSource) {
                scope.close()
            }

            override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                scope.close(t ?: Exception("api error: http ${response?.code ?: "connection failed"}"))
            }
        })
    }
}