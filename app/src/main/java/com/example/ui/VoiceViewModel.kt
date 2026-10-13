package com.example.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.ChatMessage
import com.example.data.VoiceProfile
import com.example.data.VoiceRepository
import com.example.data.api.RetrofitClient
import com.example.engine.PiperOnnxEngine
import com.example.engine.TextVoiceParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File

@Immutable
data class StudioUiState(
    val profiles: List<VoiceProfile> = emptyList(),
    val activeProfile: VoiceProfile? = null,
    val messages: List<ChatMessage> = emptyList(),
    val streamingReply: String? = null,
    val isGenerating: Boolean = false,
    val isSpeaking: Boolean = false,
    val errorMessage: String? = null
)

class VoiceViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = VoiceRepository(
        voiceDao = database.voiceProfileDao(),
        chatDao = database.chatMessageDao(),
        apiService = RetrofitClient.service
    )
    val onnxEngine = PiperOnnxEngine(application)

    private val _isGenerating = MutableStateFlow(false)
    private val _streamingReply = MutableStateFlow<String?>(null)
    private val _errorMessage = MutableStateFlow<String?>(null)

    val audioEnergy: StateFlow<Float> = onnxEngine.audioEnergy

    val uiState: StateFlow<StudioUiState> = combine(
        repository.allProfiles,
        repository.allMessages,
        _streamingReply,
        _isGenerating,
        onnxEngine.isSpeaking
    ) { profiles, messages, streamingReply, isGenerating, isSpeaking ->
        val active = profiles.firstOrNull { it.isActiveSystemVoice } ?: profiles.firstOrNull()
        StudioUiState(
            profiles = profiles,
            activeProfile = active,
            messages = messages,
            streamingReply = streamingReply,
            isGenerating = isGenerating,
            isSpeaking = isSpeaking,
            errorMessage = _errorMessage.value
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = StudioUiState()
    )

    fun importOnnxVoice(
        context: Context,
        name: String,
        onnxUri: Uri,
        jsonUri: Uri
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val voicesDir = File(context.filesDir, "onnx_voices").apply { mkdirs() }
                val safeName = name.lowercase().replace(Regex("[^a-z0-9_-]"), "_").ifBlank {
                    "voice_${System.currentTimeMillis()}"
                }
                val onnxDest = File(voicesDir, "$safeName.onnx")
                val jsonDest = File(voicesDir, "$safeName.onnx.json")

                context.contentResolver.openInputStream(onnxUri)?.use { input ->
                    onnxDest.outputStream().buffered(64 * 1024).use { output ->
                        input.copyTo(output, bufferSize = 64 * 1024)
                    }
                } ?: error("unable to read .onnx file")

                context.contentResolver.openInputStream(jsonUri)?.use { input ->
                    jsonDest.outputStream().buffered(16 * 1024).use { output ->
                        input.copyTo(output, bufferSize = 16 * 1024)
                    }
                } ?: error("unable to read .json config file")

                var detectedSampleRate = 22050
                var detectedLengthScale = 1.0f
                var detectedNoiseScale = 0.667f
                var detectedNoiseW = 0.8f

                runCatching {
                    val jsonRoot = JSONObject(jsonDest.readText())
                    jsonRoot.optJSONObject("audio")?.let { audioObj ->
                        detectedSampleRate = audioObj.optInt("sample_rate", 22050)
                    }
                    jsonRoot.optJSONObject("inference")?.let { infObj ->
                        detectedNoiseScale = infObj.optDouble("noise_scale", 0.667).toFloat()
                        detectedLengthScale = infObj.optDouble("length_scale", 1.0).toFloat()
                        detectedNoiseW = infObj.optDouble("noise_w", 0.8).toFloat()
                    }
                }

                val profile = VoiceProfile(
                    name = name.ifBlank { safeName },
                    onnxFilePath = onnxDest.absolutePath,
                    jsonConfigPath = jsonDest.absolutePath,
                    sampleRate = detectedSampleRate,
                    lengthScale = detectedLengthScale,
                    noiseScale = detectedNoiseScale,
                    noiseW = detectedNoiseW,
                    isActiveSystemVoice = uiState.value.profiles.isEmpty()
                )
                repository.insertProfile(profile)
            }.onFailure { e ->
                _errorMessage.value = "failed to import onnx model: ${e.localizedMessage}"
            }
        }
    }

    fun selectActiveVoice(profile: VoiceProfile) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.setActiveVoice(profile.id)
            onnxEngine.loadModelIfNeeded(profile)
        }
    }

    fun updateVoiceParameters(
        profile: VoiceProfile,
        lengthScale: Float,
        noiseScale: Float,
        speakerId: Int
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateProfile(
                profile.copy(
                    lengthScale = lengthScale,
                    noiseScale = noiseScale,
                    speakerId = speakerId
                )
            )
        }
    }

    fun deleteVoice(profile: VoiceProfile) {
        viewModelScope.launch(Dispatchers.IO) {
            File(profile.onnxFilePath).delete()
            File(profile.jsonConfigPath).delete()
            repository.deleteProfile(profile)
        }
    }

    fun sendPrompt(
        promptText: String,
        apiKey: String = "",
        modelName: String = "gemini-2.5-flash",
        systemInstruction: String = "Keep responses concise, natural, and conversational for voice synthesis."
    ) {
        if (promptText.isBlank() || _isGenerating.value) return
        val currentState = uiState.value
        val activeVoice = currentState.activeProfile

        viewModelScope.launch {
            _isGenerating.value = true
            _errorMessage.value = null

            val userMsg = ChatMessage(
                role = "user",
                text = promptText,
                voiceId = activeVoice?.id?.toInt(),
                voiceName = activeVoice?.name
            )
            repository.insertMessage(userMsg)

            // When no API key is provided, act as a direct local TTS reader
            if (apiKey.isBlank()) {
                if (activeVoice != null) {
                    onnxEngine.enqueueSpeech(promptText, activeVoice)
                } else {
                    _errorMessage.value = "load an .onnx voice model first to synthesize speech"
                }
                _isGenerating.value = false
                return@launch
            }

            val result = repository.getGeminiResponse(
                model = modelName,
                prompt = promptText,
                systemInstruction = systemInstruction,
                history = currentState.messages,
                apiKey = apiKey
            )

            val replyText = result.text.trim()
            val modelMsg = ChatMessage(
                role = "model",
                text = replyText,
                voiceId = activeVoice?.id?.toInt(),
                voiceName = activeVoice?.name
            )
            repository.insertMessage(modelMsg)

            if (activeVoice != null) {
                val sentences = TextVoiceParser.cleanForSpeech(replyText)
                    .split(Regex("(?<=[.!?\\n])\\s+"))
                    .filter { it.isNotBlank() }

                for (sentence in sentences) {
                    onnxEngine.enqueueSpeech(sentence, activeVoice)
                }
            }

            _isGenerating.value = false
        }
    }

    fun speakTextDirect(text: String) {
        val activeVoice = uiState.value.activeProfile ?: return
        onnxEngine.enqueueSpeech(text, activeVoice)
    }

    fun stopSpeaking() {
        onnxEngine.stopPlayback()
    }

    fun dismissError() {
        _errorMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        onnxEngine.release()
    }
}