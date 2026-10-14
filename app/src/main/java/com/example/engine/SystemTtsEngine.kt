package com.example.engine

import android.content.ComponentCallbacks2
import android.media.AudioFormat
import android.os.Process
import android.speech.tts.SynthesisCallback
import android.speech.tts.SynthesisRequest
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeechService
import com.example.data.AppDatabase
import kotlinx.coroutines.runBlocking
import java.util.Locale

class SystemTtsEngine : TextToSpeechService() {

    private lateinit var database: AppDatabase
    private lateinit var onnxEngine: PiperOnnxEngine
    private val reusableByteBuffer = ByteArray(4096)

    @Volatile
    private var isStopped = false

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getDatabase(applicationContext)
        onnxEngine = PiperOnnxEngine(applicationContext)
    }

    override fun onIsLanguageAvailable(lang: String?, country: String?, variant: String?): Int {
        return TextToSpeech.LANG_AVAILABLE
    }

    override fun onGetLanguage(): Array<String> {
        return arrayOf(Locale.US.isO3Language, Locale.US.isO3Country, "")
    }

    override fun onLoadLanguage(lang: String?, country: String?, variant: String?): Int {
        return TextToSpeech.LANG_AVAILABLE
    }

    override fun onStop() {
        isStopped = true
        onnxEngine.stopPlayback()
    }

    override fun onSynthesizeText(request: SynthesisRequest?, callback: SynthesisCallback?) {
        if (request == null || callback == null) return
        val rawText = request.charSequenceText?.toString() ?: return
        if (rawText.isBlank()) {
            callback.start(22050, AudioFormat.ENCODING_PCM_16BIT, 1)
            callback.done()
            return
        }

        isStopped = false
        Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)

        val dao = database.voiceProfileDao()
        val profile = dao.getActiveProfileSync() ?: dao.getFallbackProfileSync()
        if (profile == null) {
            callback.error()
            return
        }

        val rateMultiplier = (request.speechRate / 100f).coerceIn(0.5f, 2.5f)
        val effectiveProfile = profile.copy(
            lengthScale = (profile.lengthScale / rateMultiplier).coerceIn(0.4f, 2.0f)
        )

        val modelLoaded = runBlocking {
            onnxEngine.loadModelIfNeeded(effectiveProfile)
        }
        if (!modelLoaded) {
            callback.error()
            return
        }

        callback.start(effectiveProfile.sampleRate, AudioFormat.ENCODING_PCM_16BIT, 1)

        val sentences = TextVoiceParser.cleanForSpeech(rawText)
            .split(Regex("(?<=[.!?])\\s+"))
            .filter { it.isNotBlank() }

        val maxChunkBytes = callback.maxBufferSize.coerceAtMost(reusableByteBuffer.size)
        val safeChunkBytes = maxChunkBytes - (maxChunkBytes % 2)
        val samplesPerChunk = safeChunkBytes / 2

        for (sentence in sentences) {
            if (isStopped) break

            val pcmSamples = onnxEngine.synthesizeToPcm(sentence, effectiveProfile)
            var sampleOffset = 0

            while (sampleOffset < pcmSamples.size && !isStopped) {
                val count = (pcmSamples.size - sampleOffset).coerceAtMost(samplesPerChunk)
                var byteIndex = 0

                for (i in 0 until count) {
                    val sample = pcmSamples[sampleOffset + i].toInt()
                    reusableByteBuffer[byteIndex++] = (sample and 0xFF).toByte()
                    reusableByteBuffer[byteIndex++] = ((sample shr 8) and 0xFF).toByte()
                }

                val status = callback.audioAvailable(reusableByteBuffer, 0, byteIndex)
                if (status == TextToSpeech.ERROR) {
                    return
                }
                sampleOffset += count
            }
        }

        if (!isStopped) {
            callback.done()
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW ||
            level >= ComponentCallbacks2.TRIM_MEMORY_BACKGROUND
        ) {
            onnxEngine.unloadNativeModel()
        }
    }

    override fun onDestroy() {
        onnxEngine.release()
        super.onDestroy()
    }
}