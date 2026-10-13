package com.example.engine

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * Wraps the Android System TextToSpeech engine.
 */
class SystemTtsEngine(private val context: Context) {

    enum class EngineState {
        UNINITIALIZED,
        INITIALIZING,
        READY,
        ERROR
    }

    private val _engineState = MutableStateFlow(EngineState.UNINITIALIZED)
    val engineState: StateFlow<EngineState> = _engineState.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private var tts: TextToSpeech? = null

    init {
        initializeTts()
    }

    private fun initializeTts() {
        _engineState.value = EngineState.INITIALIZING
        
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _isSpeaking.value = true
                    }
                    override fun onDone(utteranceId: String?) {
                        _isSpeaking.value = false
                    }
                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        _isSpeaking.value = false
                        Log.e("SystemTtsEngine", "Inference error on Utterance: $utteranceId")
                    }
                })
                
                try {
                    tts?.language = Locale.US
                } catch (e: Exception) {
                    Log.e("SystemTtsEngine", "Failed to set default language locale: ${e.message}")
                }
                
                _engineState.value = EngineState.READY
                Log.d("SystemTtsEngine", "System TTS Engine loaded and READY.")
            } else {
                _engineState.value = EngineState.ERROR
                Log.e("SystemTtsEngine", "Failed to initialize hardware TTS engine.")
            }
        }
    }

    fun synthesizeSpeech(
        text: String,
        pitch: Float = 1.0f,
        speechRate: Float = 1.0f,
        languageCode: String = "en-US"
    ) {
        if (_engineState.value != EngineState.READY) {
            Log.w("SystemTtsEngine", "Engine not ready. Current state: ${_engineState.value}")
            return
        }

        try {
            tts?.setPitch(pitch)
            tts?.setSpeechRate(speechRate)

            val localeTokens = languageCode.split("-", "_")
            val locale = if (localeTokens.size == 2) {
                Locale(localeTokens[0], localeTokens[1])
            } else {
                Locale(languageCode)
            }
            tts?.language = locale

            val params = HashMap<String, String>()
            params[TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID] = "system_tts_utterance"
            
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params)
        } catch (e: Exception) {
            Log.e("SystemTtsEngine", "Inference failed: ${e.message}")
            _isSpeaking.value = false
        }
    }

    fun stopSynthesis() {
        tts?.stop()
        _isSpeaking.value = false
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        _engineState.value = EngineState.UNINITIALIZED
    }
}
