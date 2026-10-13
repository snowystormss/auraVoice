package com.example.data

import com.example.data.api.Content
import com.example.data.api.GeminiApiService
import com.example.data.api.GenerateContentRequest
import com.example.data.api.Part
import com.example.data.api.UsageMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

data class GeminiResult(
    val text: String,
    val usageMetadata: UsageMetadata? = null
)

class VoiceRepository(
    private val voiceDao: VoiceProfileDao,
    private val chatDao: ChatMessageDao,
    private val apiService: GeminiApiService
) {
    val allVoices: Flow<List<VoiceProfile>> = voiceDao.getAllVoices()
    val allMessages: Flow<List<ChatMessage>> = chatDao.getAllMessages()

    suspend fun insertVoice(profile: VoiceProfile) = withContext(Dispatchers.IO) {
        voiceDao.insertVoice(profile)
    }

    suspend fun updateVoice(profile: VoiceProfile) = withContext(Dispatchers.IO) {
        voiceDao.insertVoice(profile)
    }

    suspend fun deleteVoice(profile: VoiceProfile) = withContext(Dispatchers.IO) {
        voiceDao.deleteVoice(profile)
    }

    suspend fun insertMessage(message: ChatMessage) = withContext(Dispatchers.IO) {
        chatDao.insertMessage(message)
    }

    suspend fun clearChatHistory() = withContext(Dispatchers.IO) {
        chatDao.clearHistory()
    }

    suspend fun checkAndSeedPresets() = withContext(Dispatchers.IO) {
        // Pure Voice Cloning Studio: Preset voices are deprecated and removed.
        // Purge any legacy preset voices so the user only sees their own cloned voices.
        val current = voiceDao.getAllVoices().first()
        for (v in current) {
            if (v.isPreset) {
                voiceDao.deleteVoice(v)
            }
        }
    }

    suspend fun getGeminiResponse(
        model: String,
        prompt: String,
        systemInstruction: String,
        history: List<ChatMessage>,
        apiKey: String
    ): GeminiResult = withContext(Dispatchers.IO) {
        // Construct chat history parts for Gemini context
        val contextContents = mutableListOf<Content>()

        // Limit context size to keep it simple and within token windows
        val chatTurns = history.takeLast(10)
        for (msg in chatTurns) {
            contextContents.add(
                Content(
                    role = if (msg.role == "user") "user" else "model",
                    parts = listOf(Part(text = msg.text))
                )
            )
        }

        // Add the current prompt
        contextContents.add(
            Content(
                role = "user",
                parts = listOf(Part(text = prompt))
            )
        )

        val sysInstructionRequest = if (systemInstruction.isNotEmpty()) {
            Content(parts = listOf(Part(text = systemInstruction)))
        } else {
            null
        }

        val request = GenerateContentRequest(
            contents = contextContents,
            systemInstruction = sysInstructionRequest
        )

        try {
            val response = apiService.generateContent(model, apiKey, request)
            val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: "I apologize, but I could not synthesize a proper textual description at this moment."
            GeminiResult(text, response.usageMetadata)
        } catch (e: Exception) {
            GeminiResult("Network Error: Unable to query Gemini. Reason: ${e.message ?: "Unknown API issue."}", null)
        }
    }
}
