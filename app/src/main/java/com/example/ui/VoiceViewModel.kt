package com.example.ui

import android.app.Application
import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.*
import com.example.data.api.RetrofitClient
import com.example.engine.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File

class VoiceViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = VoiceRepository(
        voiceDao = database.voiceProfileDao(),
        chatDao = database.chatMessageDao(),
        apiService = RetrofitClient.service
    )

    val allVoices: StateFlow<List<VoiceProfile>> = repository.allVoices
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allMessages: StateFlow<List<ChatMessage>> = repository.allMessages
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _selectedVoice = MutableStateFlow<VoiceProfile?>(null)
    val selectedVoice = _selectedVoice.asStateFlow()

    // Engines
    private val systemTtsEngine = SystemTtsEngine(application)
    private val piperOnnxEngine = PiperOnnxEngine(application)
    private val f5TtsEngine = F5TtsEngine(application)
    private val piperTrainer = PiperFineTuningPipeline(application)
    private val audioRecorder = AudioRecorder(application)
    private val audioTranscoder = AudioTranscoder(application)
    private val hardwareMonitor = HardwareTelemetryMonitor(application)
    val hardwareTelemetry: StateFlow<HardwareTelemetry> = hardwareMonitor.telemetry

    // Unified speech output indicator
    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating = _isGenerating.asStateFlow()

    private val _autoSpeak = MutableStateFlow(true)
    val autoSpeak = _autoSpeak.asStateFlow()

    private val sharedPrefs = application.getSharedPreferences("aura_voice_prefs", Context.MODE_PRIVATE)

    private val _localApiKey = MutableStateFlow(sharedPrefs.getString("local_api_key", "") ?: "")
    val localApiKey = _localApiKey.asStateFlow()

    private val _selectedModel = MutableStateFlow(sharedPrefs.getString("selected_model", "gemini-1.5-flash") ?: "gemini-1.5-flash")
    val selectedModel = _selectedModel.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    private val _themePalette = MutableStateFlow(sharedPrefs.getString("theme_palette", "AURA_INDIGO") ?: "AURA_INDIGO")
    val themePalette = _themePalette.asStateFlow()

    // ==========================================
    // NAVIGATION & STUDIO TABS
    // ==========================================
    enum class StudioTab {
        CLONER,
        TEXT_STUDIO,
        LIBRARY
    }

    private val _selectedTab = MutableStateFlow(StudioTab.CLONER)
    val selectedTab: StateFlow<StudioTab> = _selectedTab.asStateFlow()

    fun selectTab(tab: StudioTab) {
        _selectedTab.value = tab
    }

    // ==========================================
    // TEXT-TO-VOICE STUDIO & FINE-TUNING STATES
    // ==========================================
    private val _textVoiceDescription = MutableStateFlow("Warm deep baritone narrator, calm cadence, formal tone")
    val textVoiceDescription: StateFlow<String> = _textVoiceDescription.asStateFlow()

    private val _textVoiceName = MutableStateFlow("Atlas Baritone")
    val textVoiceName: StateFlow<String> = _textVoiceName.asStateFlow()

    private val _parsedVoiceTraits = MutableStateFlow(TextVoiceParser.parseDescription(_textVoiceDescription.value))
    val parsedVoiceTraits: StateFlow<TextVoiceParser.ParsedVoiceTraits> = _parsedVoiceTraits.asStateFlow()

    private val _textRefinementHistory = MutableStateFlow<List<String>>(emptyList())
    val textRefinementHistory: StateFlow<List<String>> = _textRefinementHistory.asStateFlow()

    private val _textAuditionSample = MutableStateFlow("The birch canoe slid on the smooth dark water.")
    val textAuditionSample: StateFlow<String> = _textAuditionSample.asStateFlow()

    private val _lastRefinementSummary = MutableStateFlow<String?>(null)
    val lastRefinementSummary: StateFlow<String?> = _lastRefinementSummary.asStateFlow()

    data class TokenUsageRecord(val model: String, val tokens: Int)
    data class TokenSummary(
        val sessionTokens: Int = 0,
        val sessionRequests: Int = 0,
        val history: List<TokenUsageRecord> = emptyList()
    )

    private val _tokenSummary = MutableStateFlow(TokenSummary())
    val tokenSummary = _tokenSummary.asStateFlow()

    // ==========================================
    // VOICE CLONER & TRAINING PIPELINE STATES
    // ==========================================
    enum class TrainingState {
        IDLE,
        RECORDING_AUDIO,
        AUDIO_READY,
        PREVIEW_GENERATING,
        PREVIEW_READY,
        GENERATING_300_SAMPLES,
        RECLAIMING_MEMORY,
        PIPER_FINE_TUNING,
        NAMING_VOICE,
        COMPLETED
    }

    private val _trainingState = MutableStateFlow(TrainingState.IDLE)
    val trainingState: StateFlow<TrainingState> = _trainingState.asStateFlow()

    // Audio Ingestion
    val isRecordingMic: StateFlow<Boolean> = audioRecorder.isRecording
    val micAmplitude: StateFlow<Float> = audioRecorder.amplitude
    val recordedDurationSec: StateFlow<Int> = audioRecorder.recordedDurationSec

    private val _referenceAudioFile = MutableStateFlow<File?>(null)
    val referenceAudioFile: StateFlow<File?> = _referenceAudioFile.asStateFlow()

    // Transcoder status & metrics
    private val _isTranscoding = MutableStateFlow(false)
    val isTranscoding: StateFlow<Boolean> = _isTranscoding.asStateFlow()

    private val _transcodeResult = MutableStateFlow<AudioTranscoder.TranscodeResult?>(null)
    val transcodeResult: StateFlow<AudioTranscoder.TranscodeResult?> = _transcodeResult.asStateFlow()

    // Reference audio playback
    private val _isReferencePlaying = MutableStateFlow(false)
    val isReferencePlaying: StateFlow<Boolean> = _isReferencePlaying.asStateFlow()
    private var referenceMediaPlayer: MediaPlayer? = null

    // 1-Sentence Quick Preview
    private val _previewAudioFile = MutableStateFlow<File?>(null)
    val previewAudioFile: StateFlow<File?> = _previewAudioFile.asStateFlow()

    val sampleHarvardSentences: List<String> = listOf(
        "The oak tree was tall and green.",
        "The birch canoe slid on the smooth planks.",
        "Glue the sheet to the dark blue background.",
        "Four hours of steady work brought success.",
        "These days a chicken leg is a rare dish.",
        "Rice is often served in round bowls.",
        "The juice of lemons makes fine punch.",
        "The box was thrown beside the parked truck."
    )

    private val _previewSentenceText = MutableStateFlow(sampleHarvardSentences.first())
    val previewSentenceText: StateFlow<String> = _previewSentenceText.asStateFlow()

    private val _previewOdeStep = MutableStateFlow(0)
    val previewOdeStep: StateFlow<Int> = _previewOdeStep.asStateFlow()

    private val _isPreviewPlaying = MutableStateFlow(false)
    val isPreviewPlaying: StateFlow<Boolean> = _isPreviewPlaying.asStateFlow()
    private var previewMediaPlayer: MediaPlayer? = null

    // 300-Sample Dataset Generation Telemetry
    private val _datasetProgress = MutableStateFlow<F5TtsEngine.DatasetProgress?>(null)
    val datasetProgress: StateFlow<F5TtsEngine.DatasetProgress?> = _datasetProgress.asStateFlow()

    // Memory Reclamation
    private val _reclaimedMemoryMb = MutableStateFlow(0f)
    val reclaimedMemoryMb: StateFlow<Float> = _reclaimedMemoryMb.asStateFlow()

    // Piper Training Telemetry
    private val _piperProgress = MutableStateFlow<PiperFineTuningPipeline.TrainingProgress?>(null)
    val piperProgress: StateFlow<PiperFineTuningPipeline.TrainingProgress?> = _piperProgress.asStateFlow()

    private val _lossHistory = MutableStateFlow<List<Float>>(emptyList())
    val lossHistory: StateFlow<List<Float>> = _lossHistory.asStateFlow()

    private val _finalTrainedResult = MutableStateFlow<PiperFineTuningPipeline.TrainingResult?>(null)
    val finalTrainedResult: StateFlow<PiperFineTuningPipeline.TrainingResult?> = _finalTrainedResult.asStateFlow()

    // Voice Model ZIP Export states
    private val _isExportingVoice = MutableStateFlow(false)
    val isExportingVoice: StateFlow<Boolean> = _isExportingVoice.asStateFlow()

    private val _exportedResult = MutableStateFlow<VoiceModelExporter.ExportResult?>(null)
    val exportedResult: StateFlow<VoiceModelExporter.ExportResult?> = _exportedResult.asStateFlow()

    private val _exportMessage = MutableStateFlow<String?>(null)
    val exportMessage: StateFlow<String?> = _exportMessage.asStateFlow()

    // Multilingual Model Training State
    private val _cloningLanguage = MutableStateFlow<LanguageInfo>(LanguageCorpusRegistry.ENGLISH)
    val cloningLanguage: StateFlow<LanguageInfo> = _cloningLanguage.asStateFlow()

    fun setCloningLanguage(language: LanguageInfo) {
        _cloningLanguage.value = language
    }

    private var activeTrainingJob: Job? = null

    init {
        // Seed default voice presets if Room is empty on initial run
        viewModelScope.launch {
            repository.checkAndSeedPresets()
            allVoices.collect { voices ->
                if (voices.isNotEmpty()) {
                    if (_selectedVoice.value == null || !voices.any { it.id == _selectedVoice.value?.id }) {
                        _selectedVoice.value = voices.firstOrNull { it.isTrained } ?: voices.first()
                    }
                } else {
                    _selectedVoice.value = null
                }
            }
        }

        // Combine engine speech states
        viewModelScope.launch {
            combine(systemTtsEngine.isSpeaking, piperOnnxEngine.isSpeaking) { sys, pip ->
                sys || pip
            }.collect { speaking ->
                _isSpeaking.value = speaking
            }
        }
    }

    fun selectVoice(voice: VoiceProfile) {
        _selectedVoice.value = voice
        stopSpeaking()
    }

    fun toggleAutoSpeak() {
        _autoSpeak.value = !_autoSpeak.value
    }

    fun setLocalApiKey(key: String) {
        val newKey = key.trim()
        _localApiKey.value = newKey
        sharedPrefs.edit().putString("local_api_key", newKey).apply()
    }

    fun setModel(model: String) {
        _selectedModel.value = model
        sharedPrefs.edit().putString("selected_model", model).apply()
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    fun setThemePalette(palette: String) {
        _themePalette.value = palette
        sharedPrefs.edit().putString("theme_palette", palette).apply()
    }

    // ==========================================
    // VOICE CLONING WORKFLOW
    // ==========================================

    fun startRecordingMic() {
        stopReferenceAudio()
        stopPreviewAudio()
        stopSpeaking()
        val file = audioRecorder.startRecording()
        if (file != null) {
            _referenceAudioFile.value = file
            _transcodeResult.value = null
            _trainingState.value = TrainingState.RECORDING_AUDIO
        }
    }

    fun stopRecordingMic() {
        val file = audioRecorder.stopRecording()
        if (file != null && file.exists()) {
            _referenceAudioFile.value = file
            _transcodeResult.value = AudioTranscoder.TranscodeResult(
                file = file,
                originalFormat = "MIC",
                originalDurationSec = recordedDurationSec.value.toFloat(),
                finalDurationSec = recordedDurationSec.value.toFloat(),
                originalSampleRate = 24000,
                originalChannels = 1,
                wasTrimmed = false
            )
            _trainingState.value = TrainingState.AUDIO_READY
        } else {
            _trainingState.value = TrainingState.IDLE
        }
    }

    fun importAudioUri(uri: Uri) {
        stopReferenceAudio()
        stopPreviewAudio()
        stopSpeaking()
        _isTranscoding.value = true
        _trainingState.value = TrainingState.RECORDING_AUDIO

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = audioTranscoder.transcodeToStandardWav(uri)
                _transcodeResult.value = result
                if (result.file.exists() && result.file.length() > 44) {
                    _referenceAudioFile.value = result.file
                    _trainingState.value = TrainingState.AUDIO_READY
                } else {
                    _errorMessage.value = "Failed to process audio. Please check file format."
                    _trainingState.value = TrainingState.IDLE
                }
            } catch (e: Exception) {
                Log.e("VoiceViewModel", "Audio import failed: ${e.message}", e)
                _errorMessage.value = "Audio import failed: ${e.message}"
                _trainingState.value = TrainingState.IDLE
            } finally {
                _isTranscoding.value = false
            }
        }
    }

    fun importWavUri(uri: Uri) = importAudioUri(uri)

    fun playReferenceAudio() {
        val file = _referenceAudioFile.value ?: return
        stopReferenceAudio()
        stopPreviewAudio()
        stopSpeaking()
        try {
            referenceMediaPlayer = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                prepare()
                setOnCompletionListener {
                    _isReferencePlaying.value = false
                }
                start()
                _isReferencePlaying.value = true
            }
        } catch (e: Exception) {
            Log.e("VoiceViewModel", "Error playing reference: ${e.message}")
            _isReferencePlaying.value = false
        }
    }

    fun stopReferenceAudio() {
        try {
            referenceMediaPlayer?.apply {
                if (isPlaying) stop()
                release()
            }
        } catch (e: Exception) {
            // Ignore
        }
        referenceMediaPlayer = null
        _isReferencePlaying.value = false
    }

    fun clearReferenceAudio() {
        stopReferenceAudio()
        stopPreviewAudio()
        _referenceAudioFile.value = null
        _previewAudioFile.value = null
        _transcodeResult.value = null
        _trainingState.value = TrainingState.IDLE
    }

    fun setPreviewSentence(sentence: String) {
        _previewSentenceText.value = sentence
    }

    /**
     * Stage 1: Generate 1-sentence quick preview sample with 32 ODE Flow-Matching steps.
     * Invokes F5-TTS on a single Harvard sentence to let the user listen before committing to 300 samples.
     */
    fun generateQuickPreview(customSentence: String? = null) {
        val refAudio = _referenceAudioFile.value ?: return
        val sentence = customSentence ?: _previewSentenceText.value
        _previewSentenceText.value = sentence
        _trainingState.value = TrainingState.PREVIEW_GENERATING
        _previewOdeStep.value = 0

        viewModelScope.launch {
            try {
                val previewFile = f5TtsEngine.generatePreview(
                    referenceWav = refAudio,
                    sentenceText = sentence,
                    onStepProgress = { step, _ ->
                        _previewOdeStep.value = step
                    }
                )
                _previewAudioFile.value = previewFile
                _trainingState.value = TrainingState.PREVIEW_READY
                playPreviewAudio()
            } catch (e: Exception) {
                Log.e("VoiceViewModel", "Preview generation failed: ${e.message}")
                _errorMessage.value = "Failed to generate preview: ${e.message}"
                _trainingState.value = TrainingState.AUDIO_READY
            }
        }
    }

    fun playPreviewAudio() {
        val file = _previewAudioFile.value ?: return
        stopPreviewAudio()
        try {
            previewMediaPlayer = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                prepare()
                setOnCompletionListener {
                    _isPreviewPlaying.value = false
                }
                start()
                _isPreviewPlaying.value = true
            }
        } catch (e: Exception) {
            Log.e("VoiceViewModel", "Error playing preview: ${e.message}")
            _isPreviewPlaying.value = false
        }
    }

    fun stopPreviewAudio() {
        try {
            previewMediaPlayer?.apply {
                if (isPlaying) stop()
                release()
            }
        } catch (e: Exception) {
            // Ignore
        }
        previewMediaPlayer = null
        _isPreviewPlaying.value = false
    }

    /**
     * Stage 2 to 4: Full training pipeline.
     * Generates 300 Harvard samples at 32 ODE steps -> unloads F5-TTS to reclaim RAM -> trains Piper ONNX model.
     */
    fun startFullTrainingPipeline() {
        val refAudio = _referenceAudioFile.value ?: return
        stopPreviewAudio()
        stopSpeaking()

        // Launch real-time hardware telemetry monitor
        hardwareMonitor.startMonitoring {
            val st = _trainingState.value
            when (st) {
                TrainingState.GENERATING_300_SAMPLES -> {
                    val count = _datasetProgress.value?.completedCount?.toFloat() ?: 0f
                    val fraction = (count / 300f).coerceIn(0f, 1f) * 0.65f
                    val remaining = _datasetProgress.value?.estimatedRemainingSec ?: 35
                    Pair(fraction, remaining)
                }
                TrainingState.RECLAIMING_MEMORY -> {
                    Pair(0.70f, 2)
                }
                TrainingState.PIPER_FINE_TUNING -> {
                    val piperFrac = _piperProgress.value?.progressFraction ?: 0f
                    val fraction = 0.75f + (piperFrac * 0.25f)
                    val remaining = _piperProgress.value?.estimatedRemainingSec ?: 10
                    Pair(fraction.coerceIn(0f, 1f), remaining)
                }
                TrainingState.NAMING_VOICE, TrainingState.COMPLETED -> {
                    Pair(1.0f, 0)
                }
                else -> Pair(0f, 0)
            }
        }

        activeTrainingJob?.cancel()
        activeTrainingJob = viewModelScope.launch {
            try {
                val trainingLang = _cloningLanguage.value
                // 1. Generate 300 language-balanced sentences
                _trainingState.value = TrainingState.GENERATING_300_SAMPLES
                _datasetProgress.value = null

                val generatedSamples = f5TtsEngine.generate300Samples(
                    referenceWav = refAudio,
                    sentences = trainingLang.trainingSentences,
                    onProgress = { progress ->
                        _datasetProgress.value = progress
                    },
                    onBatchComplete = { _, _ -> }
                )

                // 2. Terminate F5-TTS & Reclaim RAM
                _trainingState.value = TrainingState.RECLAIMING_MEMORY
                val memReport = f5TtsEngine.terminateAndReclaimMemory()
                _reclaimedMemoryMb.value = memReport.memoryReclaimedMb
                delay(600) // Visual pause showing RAM reclaimed

                // 3. Fine-Tune Piper ONNX model
                _trainingState.value = TrainingState.PIPER_FINE_TUNING
                _lossHistory.value = emptyList()

                val trainingResult = piperTrainer.fineTunePiperModel(
                    trainingSamples = generatedSamples,
                    languageCode = trainingLang.code,
                    onProgress = { progress ->
                        _piperProgress.value = progress
                        _lossHistory.value = progress.lossHistory
                    }
                )

                _finalTrainedResult.value = trainingResult
                _trainingState.value = TrainingState.NAMING_VOICE
            } catch (e: CancellationException) {
                Log.d("VoiceViewModel", "Training pipeline was cancelled")
                _trainingState.value = TrainingState.AUDIO_READY
            } catch (e: Exception) {
                Log.e("VoiceViewModel", "Training pipeline failed: ${e.message}")
                _errorMessage.value = "Voice training encountered an error: ${e.message}"
                _trainingState.value = TrainingState.AUDIO_READY
            } finally {
                if (_trainingState.value != TrainingState.NAMING_VOICE && _trainingState.value != TrainingState.COMPLETED) {
                    hardwareMonitor.stopMonitoring()
                }
            }
        }
    }

    fun cancelTrainingPipeline() {
        hardwareMonitor.stopMonitoring()
        activeTrainingJob?.cancel()
        activeTrainingJob = null
        stopPreviewAudio()
        viewModelScope.launch {
            f5TtsEngine.terminateAndReclaimMemory()
            _trainingState.value = TrainingState.AUDIO_READY
            _datasetProgress.value = null
            _piperProgress.value = null
            _finalTrainedResult.value = null
            _exportMessage.value = "Voice training cancelled. Hardware resources reclaimed."
        }
    }

    fun saveClonedVoice(name: String, description: String = "") {
        val result = _finalTrainedResult.value
        val safeName = if (name.isNotBlank()) name.trim() else "Custom Piper Voice"
        val lang = _cloningLanguage.value
        val desc = if (description.isNotBlank()) description.trim() else "Fine-tuned via F5-TTS 32-step flow matching on ${lang.corpusName}."

        viewModelScope.launch {
            val newVoice = VoiceProfile(
                name = safeName,
                description = desc,
                pitch = 1.0f,
                speechRate = 1.0f,
                languageCode = lang.code,
                personalityPrompt = "You are $safeName, an ultra-low latency, custom-cloned voice running on Piper ONNX in ${lang.displayName}.",
                isPreset = false,
                emotion = "calm",
                trainedClipPath = _referenceAudioFile.value?.absolutePath,
                isTrained = true,
                modelType = "PIPER_ONNX",
                generatorModel = "F5-TTS (32-step Flow Matching)",
                lossScore = result?.finalLoss ?: 0.045f,
                trainingDurationSec = result?.trainingDurationSec ?: 45,
                onnxModelPath = result?.modelPath
            )

            repository.insertVoice(newVoice)
            _selectedVoice.value = newVoice
            _trainingState.value = TrainingState.COMPLETED
            selectTab(StudioTab.LIBRARY)

            // Quick speech check with the new voice
            delay(400)
            speakText("Voice cloning complete! Your new Piper model is active and running locally on device.", newVoice)
        }
    }

    fun dismissCompletedState() {
        hardwareMonitor.stopMonitoring()
        _trainingState.value = TrainingState.IDLE
        _referenceAudioFile.value = null
        _previewAudioFile.value = null
        _datasetProgress.value = null
        _piperProgress.value = null
        _finalTrainedResult.value = null
        selectTab(StudioTab.LIBRARY)
    }

    // ==========================================
    // PLAYBACK & CHAT INTEGRATION
    // ==========================================

    fun speakText(text: String, voice: VoiceProfile? = _selectedVoice.value) {
        val speaker = voice ?: _selectedVoice.value ?: return
        stopSpeaking()

        val pitchMultiplier = when (speaker.emotion.lowercase()) {
            "happy" -> 1.15f
            "sad" -> 0.80f
            "enthusiastic" -> 1.25f
            "calm" -> 0.95f
            "formal" -> 1.02f
            else -> 1.0f
        }
        val rateMultiplier = when (speaker.emotion.lowercase()) {
            "happy" -> 1.05f
            "sad" -> 0.80f
            "enthusiastic" -> 1.18f
            "calm" -> 0.88f
            "formal" -> 1.02f
            else -> 1.0f
        }

        val finalPitch = (speaker.pitch * pitchMultiplier).coerceIn(0.5f, 2.0f)
        val finalRate = (speaker.speechRate * rateMultiplier).coerceIn(0.5f, 2.0f)

        if (speaker.modelType == "PIPER_ONNX" || speaker.isTrained) {
            // Low-latency on-device Piper inference with language prosody
            val modelFile = speaker.onnxModelPath?.let { File(it) }
            piperOnnxEngine.speak(
                text = text,
                modelFile = modelFile,
                pitch = finalPitch,
                speechRate = finalRate,
                languageCode = speaker.languageCode
            )
        } else {
            // Standard hardware TTS
            systemTtsEngine.synthesizeSpeech(
                text = text,
                pitch = finalPitch,
                speechRate = finalRate,
                languageCode = speaker.languageCode
            )
        }
    }

    fun stopSpeaking() {
        piperOnnxEngine.stop()
        systemTtsEngine.stopSynthesis()
        stopPreviewAudio()
    }

    fun deleteVoice(profile: VoiceProfile) {
        viewModelScope.launch {
            repository.deleteVoice(profile)
            if (_selectedVoice.value?.id == profile.id) {
                _selectedVoice.value = allVoices.value.firstOrNull { it.id != profile.id }
            }
        }
    }

    fun updateVoiceLanguage(profile: VoiceProfile, newLanguage: LanguageInfo) {
        viewModelScope.launch {
            val updated = profile.copy(languageCode = newLanguage.code)
            repository.updateVoice(updated)
            if (_selectedVoice.value?.id == profile.id) {
                _selectedVoice.value = updated
            }
            _exportMessage.value = "Updated \"${profile.name}\" language to ${newLanguage.displayName} (${newLanguage.shortCode})"
        }
    }

    fun editVoiceInStudio(profile: VoiceProfile) {
        _selectedVoice.value = profile
        selectTab(StudioTab.TEXT_STUDIO)
    }

    fun startLanguageRetraining(profile: VoiceProfile, targetLang: LanguageInfo) {
        _cloningLanguage.value = targetLang
        _selectedVoice.value = profile
        // Pre-fill preview sentence for that language
        _previewSentenceText.value = targetLang.previewSentence
        selectTab(StudioTab.CLONER)
        _exportMessage.value = "Selected ${targetLang.displayName} (${targetLang.corpusName}) for retraining \"${profile.name}\"."
    }

    // ==========================================
    // VOICE MODEL ZIP EXPORT
    // ==========================================
    fun exportVoiceModel(profile: VoiceProfile, onExportComplete: ((VoiceModelExporter.ExportResult) -> Unit)? = null) {
        viewModelScope.launch {
            _isExportingVoice.value = true
            _exportMessage.value = null
            try {
                val result = VoiceModelExporter.exportVoiceToZip(getApplication(), profile)
                _exportedResult.value = result
                _exportMessage.value = "Exported \"${profile.name}\" to ZIP (${VoiceModelExporter.formatFileSize(result.zipSizeBytes)})"
                onExportComplete?.invoke(result)
            } catch (e: Exception) {
                Log.e("VoiceViewModel", "Failed to export voice model: ${e.message}", e)
                _exportMessage.value = "Export failed: ${e.message}"
            } finally {
                _isExportingVoice.value = false
            }
        }
    }

    fun saveExportedZipToUri(targetUri: Uri, onSaved: () -> Unit) {
        val result = _exportedResult.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                getApplication<Application>().contentResolver.openOutputStream(targetUri)?.use { os ->
                    VoiceModelExporter.copyZipToDestination(result.zipFile, os)
                }
                withContext(Dispatchers.Main) {
                    _exportMessage.value = "ZIP saved successfully to selected location!"
                    onSaved()
                }
            } catch (e: Exception) {
                Log.e("VoiceViewModel", "Failed saving ZIP to destination: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    _exportMessage.value = "Failed saving ZIP: ${e.message}"
                }
            }
        }
    }

    fun dismissExportNotification() {
        _exportMessage.value = null
    }

    fun clearExportResult() {
        _exportedResult.value = null
        _exportMessage.value = null
    }

    fun updateVoiceParameters(profile: VoiceProfile, pitch: Float, speechRate: Float) {
        viewModelScope.launch {
            val updated = profile.copy(pitch = pitch, speechRate = speechRate)
            repository.insertVoice(updated)
            if (_selectedVoice.value?.id == profile.id) {
                _selectedVoice.value = updated
            }
        }
    }

    fun updateVoiceEmotion(profile: VoiceProfile, targetEmotion: String) {
        viewModelScope.launch {
            val updated = profile.copy(emotion = targetEmotion)
            repository.insertVoice(updated)
            if (_selectedVoice.value?.id == profile.id) {
                _selectedVoice.value = updated
            }
        }
    }

    fun createVoice(
        name: String,
        description: String,
        pitch: Float,
        speechRate: Float,
        languageCode: String,
        personalityPrompt: String,
        emotion: String = "calm"
    ) {
        viewModelScope.launch {
            val newVoice = VoiceProfile(
                name = name,
                description = description,
                pitch = pitch,
                speechRate = speechRate,
                languageCode = languageCode,
                personalityPrompt = personalityPrompt,
                isPreset = false,
                emotion = emotion
            )
            repository.insertVoice(newVoice)
            _selectedVoice.value = newVoice
        }
    }

    // ==========================================
    // TEXT-TO-VOICE STUDIO & FINE-TUNING ACTIONS
    // ==========================================
    fun updateTextVoiceDescription(desc: String) {
        _textVoiceDescription.value = desc
        _parsedVoiceTraits.value = TextVoiceParser.parseDescription(desc)
    }

    fun updateTextVoiceName(name: String) {
        _textVoiceName.value = name
    }

    fun updateTextAuditionSample(text: String) {
        _textAuditionSample.value = text
    }

    fun applyTextRefinement(refinementInstruction: String) {
        if (refinementInstruction.isBlank()) return
        val current = _parsedVoiceTraits.value
        val result = TextVoiceParser.refineVoice(
            currentPitch = current.pitch,
            currentSpeed = current.speechRate,
            currentEmotion = current.emotion,
            refinementCommand = refinementInstruction
        )
        _parsedVoiceTraits.value = current.copy(
            pitch = result.newPitch,
            speechRate = result.newSpeechRate,
            emotion = result.newEmotion,
            detectedTags = (current.detectedTags + result.appliedChanges.take(2)).distinct()
        )
        _lastRefinementSummary.value = result.summary
        _textRefinementHistory.value = (_textRefinementHistory.value + "${refinementInstruction.trim()} → ${result.summary}").takeLast(6)
    }

    fun auditionTextVoice(customText: String? = null) {
        val text = customText ?: _textAuditionSample.value
        val traits = _parsedVoiceTraits.value
        stopSpeaking()
        systemTtsEngine.synthesizeSpeech(
            text = text,
            pitch = traits.pitch,
            speechRate = traits.speechRate
        )
    }

    fun saveTextCraftedVoice(name: String, description: String = _textVoiceDescription.value) {
        val traits = _parsedVoiceTraits.value
        viewModelScope.launch {
            val newVoice = VoiceProfile(
                name = name.ifBlank { "Text Voice #${System.currentTimeMillis() % 1000}" },
                description = description.ifBlank { "Pure text-crafted acoustic profile" },
                pitch = traits.pitch,
                speechRate = traits.speechRate,
                emotion = traits.emotion,
                personalityPrompt = traits.personalityPrompt,
                modelType = "SYSTEM_TTS",
                isPreset = false,
                generatorModel = "Text-Guided Acoustic Profiler"
            )
            repository.insertVoice(newVoice)
            _selectedVoice.value = newVoice
            _selectedTab.value = StudioTab.LIBRARY
        }
    }

    fun refineSavedVoiceWithText(profile: VoiceProfile, refinementText: String) {
        if (refinementText.isBlank()) return
        val result = TextVoiceParser.refineVoice(
            currentPitch = profile.pitch,
            currentSpeed = profile.speechRate,
            currentEmotion = profile.emotion,
            refinementCommand = refinementText
        )
        viewModelScope.launch {
            val updated = profile.copy(
                pitch = result.newPitch,
                speechRate = result.newSpeechRate,
                emotion = result.newEmotion,
                description = if (profile.description.contains(" (Tuned:")) {
                    profile.description.substringBefore(" (Tuned:") + " (" + result.summary + ")"
                } else {
                    "${profile.description} (${result.summary})"
                }
            )
            repository.insertVoice(updated)
            if (_selectedVoice.value?.id == profile.id) {
                _selectedVoice.value = updated
            }
        }
    }

    fun sendMessage(userText: String) {
        if (userText.isBlank()) return

        val activeVoice = _selectedVoice.value ?: return
        val activeKey = getEffectiveApiKey()

        if (activeKey.isEmpty() || activeKey == "MY_GEMINI_API_KEY") {
            _errorMessage.value = "Gemini API Key is missing. Please configure it in Settings or under AI Studio Secrets."
            return
        }

        viewModelScope.launch {
            _isGenerating.value = true
            _errorMessage.value = null
            stopSpeaking()

            val userMsg = ChatMessage(role = "user", text = userText)
            repository.insertMessage(userMsg)

            val emotionDirective = when (activeVoice.emotion.lowercase()) {
                "happy" -> "\n[Vocal Tone Modulation: Express cheerfulness, warmth, joyful satisfaction.]"
                "sad" -> "\n[Vocal Tone Modulation: Express empathetic softness, gentle comfort.]"
                "enthusiastic" -> "\n[Vocal Tone Modulation: Express extreme energy and optimism!]"
                "formal" -> "\n[Vocal Tone Modulation: Express elegant sophistication and professional diplomacy.]"
                "calm" -> "\n[Vocal Tone Modulation: Express serene, gentle, reassuring, and relaxed energy.]"
                else -> ""
            }
            val systemInstructions = activeVoice.personalityPrompt + emotionDirective
            val history = allMessages.value

            val geminiResult = repository.getGeminiResponse(
                model = _selectedModel.value,
                prompt = userText,
                systemInstruction = systemInstructions,
                history = history,
                apiKey = activeKey
            )
            val replyText = geminiResult.text

            geminiResult.usageMetadata?.totalTokenCount?.let { tokens ->
                val current = _tokenSummary.value
                val newRecord = TokenUsageRecord(_selectedModel.value, tokens)
                _tokenSummary.value = current.copy(
                    sessionTokens = current.sessionTokens + tokens,
                    sessionRequests = current.sessionRequests + 1,
                    history = current.history + newRecord
                )
            }

            val modelMsg = ChatMessage(
                role = "model",
                text = replyText,
                voiceId = activeVoice.id,
                voiceName = activeVoice.name
            )
            repository.insertMessage(modelMsg)
            _isGenerating.value = false

            if (_autoSpeak.value) {
                speakText(replyText, activeVoice)
            }
        }
    }

    fun clearChat() {
        viewModelScope.launch {
            stopSpeaking()
            repository.clearChatHistory()
        }
    }

    fun getEffectiveApiKey(): String {
        return if (_localApiKey.value.isNotBlank()) {
            _localApiKey.value
        } else {
            BuildConfig.GEMINI_API_KEY
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopSpeaking()
        stopPreviewAudio()
        stopReferenceAudio()
        systemTtsEngine.shutdown()
    }
}
