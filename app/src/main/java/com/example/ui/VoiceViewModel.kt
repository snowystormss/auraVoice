package com.example.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.ChatMessage
import com.example.data.ChatSession
import com.example.data.VoiceProfile
import com.example.data.VoiceRepository
import com.example.data.api.LlmProvider
import com.example.data.api.LlmSettings
import com.example.data.api.UniversalLlmClient
import com.example.engine.PiperOnnxEngine
import com.example.engine.TextVoiceParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File

@Immutable
data class StudioUiState(
    val profiles: List<VoiceProfile> = emptyList(),
    val activeProfile: VoiceProfile? = null,
    val sessions: List<ChatSession> = emptyList(),
    val currentSessionId: Long? = null,
    val searchQuery: String = "",
    val messages: List<ChatMessage> = emptyList(),
    val streamingReply: String? = null,
    val isGenerating: Boolean = false,
    val isSpeaking: Boolean = false,
    val llmSettings: LlmSettings = LlmSettings(),
    val errorMessage: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class VoiceViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("auravoice_settings", Context.MODE_PRIVATE)
    private val database = AppDatabase.getDatabase(application)
    private val repository = VoiceRepository(
        voiceDao = database.voiceProfileDao(),
        chatDao = database.chatMessageDao()
    )
    val onnxEngine = PiperOnnxEngine(application)
    private val llmClient = UniversalLlmClient()

    private val _currentSessionId = MutableStateFlow<Long?>(null)
    private val _searchQuery = MutableStateFlow("")
    private val _isGenerating = MutableStateFlow(false)
    private val _streamingReply = MutableStateFlow<String?>(null)
    private val _errorMessage = MutableStateFlow<String?>(null)
    private val _llmSettings = MutableStateFlow(loadSettings())

    val audioEnergy: StateFlow<Float> = onnxEngine.audioEnergy

    private val filteredSessionsFlow = _searchQuery
        .debounce(120L)
        .flatMapLatest { query -> repository.getSessions(query) }

    private val currentMessagesFlow = _currentSessionId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList()) else repository.getMessagesForSession(id)
        }

    val uiState: StateFlow<StudioUiState> = combine(
        repository.allProfiles,
        filteredSessionsFlow,
        currentMessagesFlow,
        combine(_currentSessionId, _searchQuery, _streamingReply) { id, q, stream -> Triple(id, q, stream) },
        combine(_isGenerating, onnxEngine.isSpeaking, _llmSettings, _errorMessage) { gen, speak, llm, err ->
             arrayOf(gen, speak, llm, err)
        }
    ) { profiles, sessions, messages, sessionTriple, statusArray ->
        val active = profiles.firstOrNull { it.isActiveSystemVoice } ?: profiles.firstOrNull()
        val activeSessionId = sessionTriple.first ?: sessions.firstOrNull()?.id
        if (_currentSessionId.value == null && activeSessionId != null) {
            _currentSessionId.value = activeSessionId
        }

        StudioUiState(
            profiles = profiles,
            activeProfile = active,
            sessions = sessions,
            currentSessionId = activeSessionId,
            searchQuery = sessionTriple.second,
            messages = messages,
            streamingReply = sessionTriple.third,
            isGenerating = statusArray[0] as Boolean,
            isSpeaking = statusArray[1] as Boolean,
            llmSettings = statusArray[2] as LlmSettings,
            errorMessage = statusArray[3] as String?
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = StudioUiState()
    )

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun startNewChat() {
        viewModelScope.launch {
            onnxEngine.stopPlayback()
            val newId = repository.createSession("new conversation")
            _currentSessionId.value = newId
        }
    }

    fun selectSession(sessionId: Long) {
        onnxEngine.stopPlayback()
        _currentSessionId.value = sessionId
    }

    fun deleteSession(session: ChatSession) {
        viewModelScope.launch {
            repository.deleteSession(session)
            if (_currentSessionId.value == session.id) {
                _currentSessionId.value = null
            }
        }
    }

    fun sendPrompt(promptText: String) {
        if (promptText.isBlank() || _isGenerating.value) return
        val currentSettings = _llmSettings.value
        val activeVoice = uiState.value.activeProfile

        viewModelScope.launch {
            _isGenerating.value = true
            _errorMessage.value = null
            _streamingReply.value = ""

            // Ensure a session exists, or create one automatically
            var sessionId = _currentSessionId.value
            val isFirstMessageInSession = sessionId == null || uiState.value.messages.isEmpty()
            if (sessionId == null) {
                sessionId = repository.createSession(promptText.take(36))
                _currentSessionId.value = sessionId
            } else if (isFirstMessageInSession) {
                repository.updateSessionTitle(sessionId, promptText.take(36))
            }

            val userMsg = ChatMessage(
                sessionId = sessionId,
                role = "user",
                text = promptText,
                voiceId = activeVoice?.id?.toInt(),
                voiceName = activeVoice?.name
            )
            repository.insertMessage(userMsg)

            // If API key is required for the chosen cloud provider and missing, fall back to direct TTS
            val isCloudUrl = currentSettings.apiUrl.contains("googleapis.com") ||
                currentSettings.apiUrl.contains("openai.com") ||
                currentSettings.apiUrl.contains("openrouter.ai")

            if (currentSettings.apiUrl.isBlank() || (isCloudUrl && currentSettings.apiKey.isBlank())) {
                _streamingReply.value = null
                if (activeVoice != null) {
                    onnxEngine.enqueueSpeech(promptText, activeVoice)
                } else {
                    _errorMessage.value = "set your api link / key in settings or load an .onnx voice"
                }
                _isGenerating.value = false
                return@launch
            }

            val history = uiState.value.messages.takeLast(8).map { it.role to it.text }
            val sentenceBuffer = StringBuilder()
            val fullResponse = StringBuilder()

            runCatching {
                llmClient.streamChat(currentSettings, history, promptText).collect { tokenChunk ->
                    fullResponse.append(tokenChunk)
                    _streamingReply.value = fullResponse.toString()
                    sentenceBuffer.append(tokenChunk)

                    val sentences = TextVoiceParser.extractCompleteSentences(sentenceBuffer)
                    if (activeVoice != null) {
                        for (sentence in sentences) {
                            onnxEngine.enqueueSpeech(sentence, activeVoice)
                        }
                    }
                }

                val remaining = sentenceBuffer.toString().trim()
                if (remaining.isNotEmpty() && activeVoice != null) {
                    onnxEngine.enqueueSpeech(remaining, activeVoice)
                }

                val finalReply = fullResponse.toString().trim()
                _streamingReply.value = null
                if (finalReply.isNotEmpty()) {
                    repository.insertMessage(
                        ChatMessage(
                            sessionId = sessionId,
                            role = "model",
                            text = finalReply,
                            voiceId = activeVoice?.id?.toInt(),
                            voiceName = activeVoice?.name
                        )
                    )
                }
            }.onFailure { e ->
                _streamingReply.value = null
                _errorMessage.value = "server error: ${e.localizedMessage ?: "check url / api key"}"
            }

            _isGenerating.value = false
        }
    }

    private fun loadSettings(): LlmSettings {
        return LlmSettings(
            apiUrl = prefs.getString("api_url", "https://generativelanguage.googleapis.com") ?: "",
            apiKey = prefs.getString("api_key", "") ?: "",
            modelName = prefs.getString("model_name", "gemini-2.5-flash") ?: "",
            systemPrompt = prefs.getString(
                "system_prompt",
                "you are a helpful assistant. keep answers concise and conversational."
            ) ?: ""
        )
    }

    fun updateSettings(newSettings: LlmSettings) {
        _llmSettings.value = newSettings
        prefs.edit()
            .putString("api_url", newSettings.apiUrl)
            .putString("api_key", newSettings.apiKey)
            .putString("model_name", newSettings.modelName)
            .putString("system_prompt", newSettings.systemPrompt)
            .apply()
    }

    fun importOnnxVoice(context: Context, name: String, onnxUri: Uri, jsonUri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val voicesDir = File(context.filesDir, "onnx_voices").apply { mkdirs() }
                val safeName = name.lowercase().replace(Regex("[^a-z0-9_-]"), "_").ifBlank { "voice_${System.currentTimeMillis()}" }
                val onnxDest = File(voicesDir, "$safeName.onnx")
                val jsonDest = File(voicesDir, "$safeName.onnx.json")

                context.contentResolver.openInputStream(onnxUri)?.use { input ->
                    onnxDest.outputStream().buffered(64 * 1024).use { output -> input.copyTo(output, bufferSize = 64 * 1024) }
                } ?: error("cannot read .onnx file")

                context.contentResolver.openInputStream(jsonUri)?.use { input ->
                    jsonDest.outputStream().buffered(16 * 1024).use { output -> input.copyTo(output, bufferSize = 16 * 1024) }
                } ?: error("cannot read .json config")

                var detectedSampleRate = 22050
                var detectedLengthScale = 1.0f
                var detectedNoiseScale = 0.667f
                var detectedNoiseW = 0.8f

                runCatching {
                    val root = JSONObject(jsonDest.readText())
                    root.optJSONObject("audio")?.let { detectedSampleRate = it.optInt("sample_rate", 22050) }
                    root.optJSONObject("inference")?.let {
                        detectedNoiseScale = it.optDouble("noise_scale", 0.667).toFloat()
                        detectedLengthScale = it.optDouble("length_scale", 1.0).toFloat()
                        detectedNoiseW = it.optDouble("noise_w", 0.8).toFloat()
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
                _errorMessage.value = "import error: ${e.localizedMessage}"
            }
        }
    }

    fun selectActiveVoice(profile: VoiceProfile) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.setActiveVoice(profile.id)
            onnxEngine.loadModelIfNeeded(profile)
        }
    }

    fun updateVoiceParameters(profile: VoiceProfile, lengthScale: Float, noiseScale: Float, speakerId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateProfile(profile.copy(lengthScale = lengthScale, noiseScale = noiseScale, speakerId = speakerId))
        }
    }

    fun deleteVoice(profile: VoiceProfile) {
        viewModelScope.launch(Dispatchers.IO) {
            File(profile.onnxFilePath).delete()
            File(profile.jsonConfigPath).delete()
            repository.deleteProfile(profile)
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