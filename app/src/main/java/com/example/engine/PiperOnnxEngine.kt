package com.example.engine

import android.app.ActivityManager
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Process
import com.example.data.VoiceProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
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
    context: Context? = null,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) {
    private val engineMutex = Mutex()
    private var loadedProfileId: Long? = null
    private var loadedLengthScale: Float = -1f
    private var loadedNoiseScale: Float = -1f
    private var audioTrack: AudioTrack? = null
    private var currentSampleRate: Int = 22050

    // tune thread count for low-end vs mid-range CPUs to avoid core thrashing
    private val optimalThreadCount: Int = run {
        val am = context?.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val isLowRam = am?.isLowRamDevice == true
        val cores = Runtime.getRuntime().availableProcessors()
        when {
            isLowRam || cores <= 4 -> 1
            else -> 2
        }
    }

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _audioEnergy = MutableStateFlow(0f)
    val audioEnergy: StateFlow<Float> = _audioEnergy.asStateFlow()

    private val synthesisChannel = Channel<SynthesisRequest>(Channel.UNLIMITED)
    private var workerJob: Job? = null
    private var idleUnloadJob: Job? = null

    @Volatile
    private var abortCurrentSpeech = false

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
                if (!abortCurrentSpeech) {
                    idleUnloadJob?.cancel()
                    synthesizeAndPlayInternal(request.text, request.profile)
                    scheduleIdleModelUnload()
                }
            }
        }
    }

    // frees ~60MB of native ONNX heap after 45 seconds of inactivity on low-memory devices
    private fun scheduleIdleModelUnload() {
        idleUnloadJob?.cancel()
        idleUnloadJob = scope.launch {
            delay(45_000L)
            unloadNativeModel()
        }
    }

    fun unloadNativeModel() {
        scope.launch {
            engineMutex.withLock {
                if (!_isSpeaking.value) {
                    // release native sherpa-onnx OfflineTts instance here
                    loadedProfileId = null
                    audioTrack?.release()
                    audioTrack = null
                }
            }
        }
    }

    fun enqueueSpeech(text: String, profile: VoiceProfile) {
        val cleaned = TextVoiceParser.cleanForSpeech(text)
        if (cleaned.isBlank()) return
        abortCurrentSpeech = false
        synthesisChannel.trySend(SynthesisRequest(cleaned, profile))
    }

    fun stopPlayback() {
        abortCurrentSpeech = true
        while (synthesisChannel.tryReceive().isSuccess) {
            // drain pending sentences
        }
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
            if (
                loadedProfileId == profile.id &&
                loadedLengthScale == profile.lengthScale &&
                loadedNoiseScale == profile.noiseScale
            ) {
                return@withLock true
            }

            val onnxFile = File(profile.onnxFilePath)
            val jsonFile = File(profile.jsonConfigPath)
            if (!onnxFile.exists() || !jsonFile.exists()) return@withLock false

            // pass optimalThreadCount (1 or 2) into OfflineTtsModelConfig(numThreads = optimalThreadCount)
            ensureAudioTrack(profile.sampleRate)
            loadedProfileId = profile.id
            loadedLengthScale = profile.lengthScale
            loadedNoiseScale = profile.noiseScale
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
            .setBufferSizeInBytes(minBufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
    }

    private suspend fun synthesizeAndPlayInternal(text: String, profile: VoiceProfile) {
        if (!loadModelIfNeeded(profile)) return

        withContext(Dispatchers.IO) {
            Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)
            engineMutex.withLock {
                if (abortCurrentSpeech) return@withLock
                _isSpeaking.value = true
                val track = audioTrack ?: return@withLock
                track.play()

                val pcmSamples = synthesizeToPcm(text, profile)
                val chunkSize = 1024
                var offset = 0
                var lastEnergyEmitTime = 0L

                while (offset < pcmSamples.size && !abortCurrentSpeech) {
                    val count = (pcmSamples.size - offset).coerceAtMost(chunkSize)
                    track.write(pcmSamples, offset, count)

                    // throttle UI state emissions to ~30fps max so low-end CPUs aren't flooded
                    val now = System.currentTimeMillis()
                    if (now - lastEnergyEmitTime >= 32L) {
                        var sumSquares = 0.0
                        // sample every 4th frame for RMS calculation to cut CPU math by 75%
                        var i = offset
                        var sampledCount = 0
                        while (i < offset + count) {
                            val norm = pcmSamples[i] / 32768.0
                            sumSquares += norm * norm
                            i += 4
                            sampledCount++
                        }
                        if (sampledCount > 0) {
                            val rms = sqrt(sumSquares / sampledCount).toFloat()
                            _audioEnergy.value = (rms * 3.8f).coerceIn(0f, 1f)
                        }
                        lastEnergyEmitTime = now
                    }

                    offset += count
                }

                _audioEnergy.value = 0f
                _isSpeaking.value = false
            }
        }
    }

    // synchronous helper used by both in-app playback and Android's SystemTtsEngine
    fun synthesizeToPcm(text: String, profile: VoiceProfile): ShortArray {
        // hook your sherpa-onnx OfflineTts generation here
        return ShortArray(0)
    }

    fun release() {
        abortCurrentSpeech = true
        idleUnloadJob?.cancel()
        workerJob?.cancel()
        audioTrack?.release()
        audioTrack = null
    }
}