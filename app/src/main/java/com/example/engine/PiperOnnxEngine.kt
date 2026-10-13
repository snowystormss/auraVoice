package com.example.engine

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.example.data.VoiceProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.sqrt

class PiperOnnxEngine(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
    private val engineMutex = Mutex()
    private var loadedProfileId: Long? = null
    private var audioTrack: AudioTrack? = null
    private var currentSampleRate: Int = 22050

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    // Normalized 0f..1f energy level used for draw-phase UI waveform animations
    private val _audioEnergy = MutableStateFlow(0f)
    val audioEnergy: StateFlow<Float> = _audioEnergy.asStateFlow()

    private val synthesisChannel = Channel<SynthesisRequest>(Channel.BUFFERED)
    private var workerJob: Job? = null

    data class SynthesisRequest(
        val text: String,
        val profile: VoiceProfile
    )

    init {
        startSynthesisQueue()
    }

    private fun startSynthesisQueue() {
        workerJob = scope.launch {
            for (request in synthesisChannel) {
                synthesizeAndPlayInternal(request.text, request.profile)
            }
        }
    }

    fun enqueueSpeech(text: String, profile: VoiceProfile) {
        if (text.isBlank()) return
        synthesisChannel.trySend(SynthesisRequest(text.trim(), profile))
    }

    fun stopPlayback() {
        scope.launch {
            engineMutex.withLock {
                audioTrack?.run {
                    if (playState == AudioTrack.PLAYSTATE_PLAYING) {
                        pause()
                        flush()
                    }
                }
                _isSpeaking.value = false
                _audioEnergy.value = 0f
            }
        }
    }

    suspend fun loadModelIfNeeded(profile: VoiceProfile): Boolean = withContext(Dispatchers.IO) {
        engineMutex.withLock {
            if (loadedProfileId == profile.id) return@withLock true
            val onnxFile = File(profile.onnxFilePath)
            val jsonFile = File(profile.jsonConfigPath)
            if (!onnxFile.exists() || !jsonFile.exists()) return@withLock false

            // Initialize or swap your sherpa-onnx / onnxruntime session here
            ensureAudioTrack(profile.sampleRate)
            loadedProfileId = profile.id
            true
        }
    }

    private fun ensureAudioTrack(sampleRate: Int) {
        if (audioTrack != null && currentSampleRate == sampleRate) return
        audioTrack?.release()
        currentSampleRate = sampleRate

        val minBufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(4096)

        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANT)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(minBufferSize * 2)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
    }

    private suspend fun synthesizeAndPlayInternal(text: String, profile: VoiceProfile) {
        if (!loadModelIfNeeded(profile)) return

        withContext(Dispatchers.IO) {
            engineMutex.withLock {
                _isSpeaking.value = true
                val track = audioTrack ?: return@withLock
                track.play()

                // Stream synthesized PCM chunks (hook your native ONNX inference output here)
                val pcmSamples = runNativeOnnxInference(text, profile)
                val chunkSize = 1024
                var offset = 0

                while (offset < pcmSamples.size && _isSpeaking.value) {
                    val end = (offset + chunkSize).coerceAtMost(pcmSamples.size)
                    val chunk = pcmSamples.copyOfRange(offset, end)
                    track.write(chunk, 0, chunk.size)

                    // Calculate RMS amplitude for smooth UI reactive visualizer
                    var sumSquares = 0.0
                    for (sample in chunk) {
                        val normalized = sample / 32768.0
                        sumSquares += normalized * normalized
                    }
                    val rms = sqrt(sumSquares / chunk.size).toFloat()
                    _audioEnergy.value = (rms * 3.5f).coerceIn(0f, 1f)

                    offset = end
                }

                _audioEnergy.value = 0f
                _isSpeaking.value = false
            }
        }
    }

    // Replace body with your existing JNI/ONNX tensor execution from PiperOnnxEngine
    private fun runNativeOnnxInference(text: String, profile: VoiceProfile): ShortArray {
        return ShortArray(0)
    }

    fun release() {
        workerJob?.cancel()
        audioTrack?.release()
        audioTrack = null
    }
}