package com.example.data

import com.example.data.api.Content
import com.example.data.api.GeminiApiService
import com.example.data.api.GenerateContentRequest
import com.example.data.api.Part
import com.example.data.api.UsageMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
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
    val allProfiles: Flow<List<VoiceProfile>> = voiceDao.getAllProfiles()
    val allMessages: Flow<List<ChatMessage>> = chatDao.getRecentMessages()

    suspend fun insertProfile(profile: VoiceProfile): Long = withContext(Dispatchers.IO) {
        voiceDao.insert(profile)
    }

    suspend fun updateProfile(profile: VoiceProfile) = withContext(Dispatchers.IO) {
        voiceDao.update(profile)
    }

    suspend fun deleteProfile(profile: VoiceProfile) = withContext(Dispatchers.IO) {
        voiceDao.delete(profile)
    }

    suspend fun setActiveVoice(profileId: Long) = withContext(Dispatchers.IO) {
        voiceDao.setActiveVoiceTransaction(profileId)
    }

    suspend fun insertMessage(message: ChatMessage) = withContext(Dispatchers.IO) {
        chatDao.insert(message)
    }

    suspend fun clearChatHistory() = withContext(Dispatchers.IO) {
        chatDao.clearAll()
    }

    suspend fun getGeminiResponse(
        model: String,
        prompt: String,
        systemInstruction: String,
        history: List<ChatMessage>,
        apiKey: String
    ): GeminiResult = withContext(Dispatchers.IO) {
        val chatTurns = history.takeLast(6)
        val contextContents = ArrayList<Content>(chatTurns.size + 1)

        for (msg in chatTurns) {
            contextContents.add(
                Content(
                    role = if (msg.role == "user") "user" else "model",
                    parts = listOf(Part(text = msg.text))
                )
            )
        }

        contextContents.add(
            Content(
                role = "user",
                parts = listOf(Part(text = prompt))
            )
        )

        val sysInstructionRequest = if (systemInstruction.isNotBlank()) {
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
                ?: "i could not generate a response right now."
            GeminiResult(text, response.usageMetadata)
        } catch (e: Exception) {
            GeminiResult("network error: ${e.message ?: "unknown api issue"}", null)
        }
    }
}