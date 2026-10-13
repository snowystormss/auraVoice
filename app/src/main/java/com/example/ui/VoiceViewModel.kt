package com.example.ui

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ChatMessage
import com.example.data.VoiceProfile
import com.example.data.VoiceRepository
import com.example.data.api.GeminiService
import com.example.engine.PiperOnnxEngine
import com.example.engine.TextVoiceParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Immutable
data class StudioUiState(
    val profiles: List<VoiceProfile> = emptyList(),
    val activeProfile: VoiceProfile? = null,
    val messages: List<ChatMessage> = emptyList(),
    val isGenerating: Boolean = false,
    val isSpeaking: Boolean = false,
    val errorMessage: String? = null
)

class VoiceViewModel(
    private val repository: VoiceRepository,
    private val geminiService: GeminiService,
    val onnxEngine: PiperOnnxEngine
) : ViewModel() {

    private val _isGenerating = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)

    val audioEnergy: StateFlow<Float> = onnxEngine.audioEnergy

    val uiState: StateFlow<StudioUiState> = combine(
        repository.allProfiles,
        repository.allMessages,
        _isGenerating,
        onnxEngine.isSpeaking,
        _errorMessage
    ) { profiles, messages, isGenerating, isSpeaking, error ->
        val active = profiles.firstOrNull { it.isActiveSystemVoice } ?: profiles.firstOrNull()
        StudioUiState(
            profiles = profiles,
            activeProfile = active,
            messages = messages,
            isGenerating = isGenerating,
            isSpeaking = isSpeaking,
            errorMessage = error
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
                val safeName = name.lowercase().replace(Regex("[^a-z0-9_-]"), "_")
                val onnxDest = File(voicesDir, "$safeName.onnx")
                val jsonDest = File(voicesDir, "$safeName.onnx.json")

                context.contentResolver.openInputStream(onnxUri)?.use { input ->
                    onnxDest.outputStream().use { output -> input.copyTo(output) }
                } ?: error("unable to read .onnx file")

                context.contentResolver.openInputStream(jsonUri)?.use { input ->
                    jsonDest.outputStream().use { output -> input.copyTo(output) }
                } ?: error("unable to read .json config file")

                val profile = VoiceProfile(
                    name = name.ifBlank { safeName },
                    onnxFilePath = onnxDest.absolutePath,
                    jsonConfigPath = jsonDest.absolutePath,
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

    fun sendPrompt(promptText: String) {
        if (promptText.isBlank() || _isGenerating.value) return
        val activeVoice = uiState.value.activeProfile

        viewModelScope.launch {
            _isGenerating.value = true
            _errorMessage.value = null

            repository.insertMessage(ChatMessage(text = promptText, isUser = true))

            val sentenceBuffer = StringBuilder()
            val fullResponse = StringBuilder()

            runCatching {
                geminiService.streamResponse(promptText).collect { tokenChunk ->
                    fullResponse.append(tokenChunk)
                    sentenceBuffer.append(tokenChunk)

                    val extractedSentences = TextVoiceParser.extractCompleteSentences(sentenceBuffer)
                    if (activeVoice != null) {
                        extractedSentences.forEach { sentence ->
                            onnxEngine.enqueueSpeech(sentence, activeVoice)
                        }
                    }
                }

                // Flush remaining text in buffer
                val remaining = sentenceBuffer.toString().trim()
                if (remaining.isNotEmpty() && activeVoice != null) {
                    onnxEngine.enqueueSpeech(remaining, activeVoice)
                }

                repository.insertMessage(
                    ChatMessage(text = fullResponse.toString().trim(), isUser = false)
                )
            }.onFailure { e ->
                _errorMessage.value = e.localizedMessage ?: "generation failed"
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