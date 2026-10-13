package com.example.engine

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.*
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Handles live audio recording via AudioRecord at 22050Hz mono 16-bit PCM,
 * generates standard RIFF/WAVE header files, measures real-time amplitude,
 * and handles importing external .wav clips.
 */
class AudioRecorder(private val context: Context) {

    private val sampleRate = 22050
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT

    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _amplitude = MutableStateFlow(0f)
    val amplitude: StateFlow<Float> = _amplitude.asStateFlow()

    private val _recordedDurationSec = MutableStateFlow(0)
    val recordedDurationSec: StateFlow<Int> = _recordedDurationSec.asStateFlow()

    private var currentOutputFile: File? = null

    @SuppressLint("MissingPermission")
    fun startRecording(onAmplitudeUpdate: ((Float) -> Unit)? = null): File? {
        val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
            Log.e("AudioRecorder", "Unable to get valid buffer size")
            return null
        }

        val outputDir = File(context.filesDir, "voice_samples").apply { mkdirs() }
        val pcmFile = File(outputDir, "temp_recording.raw")
        val wavFile = File(outputDir, "reference_clip_${System.currentTimeMillis()}.wav")
        currentOutputFile = wavFile

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize * 2
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e("AudioRecorder", "AudioRecord failed to initialize")
                return null
            }

            audioRecord?.startRecording()
            _isRecording.value = true
            _recordedDurationSec.value = 0

            recordingJob = CoroutineScope(Dispatchers.IO).launch {
                val pcmBuffer = ShortArray(bufferSize / 2)
                val outputStream = FileOutputStream(pcmFile)
                val startTime = System.currentTimeMillis()

                try {
                    while (isActive && _isRecording.value) {
                        val readCount = audioRecord?.read(pcmBuffer, 0, pcmBuffer.size) ?: 0
                        if (readCount > 0) {
                            // Convert short array to little-endian bytes
                            val byteBuffer = ByteArray(readCount * 2)
                            var sumSquares = 0.0
                            for (i in 0 until readCount) {
                                val s = pcmBuffer[i]
                                byteBuffer[i * 2] = (s.toInt() and 0xFF).toByte()
                                byteBuffer[i * 2 + 1] = ((s.toInt() shr 8) and 0xFF).toByte()
                                sumSquares += (s.toDouble() * s.toDouble())
                            }
                            outputStream.write(byteBuffer)

                            // RMS Amplitude calculation normalized to 0.0 .. 1.0
                            val rms = sqrt(sumSquares / readCount)
                            val normalized = (rms / 12000.0).coerceIn(0.0, 1.0).toFloat()
                            _amplitude.value = normalized
                            onAmplitudeUpdate?.invoke(normalized)

                            val elapsedSec = ((System.currentTimeMillis() - startTime) / 1000).toInt()
                            _recordedDurationSec.value = elapsedSec
                        }
                    }
                } finally {
                    outputStream.close()
                }

                // Convert raw PCM to proper 44-byte WAV header
                rawPcmToWav(pcmFile, wavFile, sampleRate, 1, 16)
                pcmFile.delete()
                _amplitude.value = 0f
            }

            return wavFile
        } catch (e: Exception) {
            Log.e("AudioRecorder", "Exception starting audio recording: ${e.message}")
            _isRecording.value = false
            return null
        }
    }

    fun stopRecording(): File? {
        if (!_isRecording.value) return currentOutputFile

        _isRecording.value = false
        try {
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
        } catch (e: Exception) {
            Log.e("AudioRecorder", "Error stopping AudioRecord: ${e.message}")
        }

        recordingJob?.cancel()
        _amplitude.value = 0f
        return currentOutputFile
    }

    /**
     * Imports an external .wav file provided by the user via Intent/Uri.
     */
    fun importWavFromUri(uri: Uri): File? {
        val outputDir = File(context.filesDir, "voice_samples").apply { mkdirs() }
        val importedFile = File(outputDir, "imported_${System.currentTimeMillis()}.wav")

        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(importedFile).use { output ->
                    input.copyTo(output)
                }
            }
            importedFile
        } catch (e: Exception) {
            Log.e("AudioRecorder", "Failed to import WAV from URI: ${e.message}")
            null
        }
    }

    private fun rawPcmToWav(
        pcmFile: File,
        wavFile: File,
        sampleRate: Int,
        channels: Int,
        bitsPerSample: Int
    ) {
        val pcmDataLength = pcmFile.length()
        val totalDataLength = pcmDataLength + 36
        val byteRate = sampleRate * channels * bitsPerSample / 8

        val header = ByteArray(44)
        val pcmIn = FileInputStream(pcmFile)
        val wavOut = FileOutputStream(wavFile)

        try {
            // RIFF chunk descriptor
            header[0] = 'R'.code.toByte()
            header[1] = 'I'.code.toByte()
            header[2] = 'F'.code.toByte()
            header[3] = 'F'.code.toByte()
            header[4] = (totalDataLength and 0xff).toByte()
            header[5] = ((totalDataLength shr 8) and 0xff).toByte()
            header[6] = ((totalDataLength shr 16) and 0xff).toByte()
            header[7] = ((totalDataLength shr 24) and 0xff).toByte()
            header[8] = 'W'.code.toByte()
            header[9] = 'A'.code.toByte()
            header[10] = 'V'.code.toByte()
            header[11] = 'E'.code.toByte()

            // fmt sub-chunk
            header[12] = 'f'.code.toByte()
            header[13] = 'm'.code.toByte()
            header[14] = 't'.code.toByte()
            header[15] = ' '.code.toByte()
            header[16] = 16 // Subchunk1Size for PCM
            header[17] = 0
            header[18] = 0
            header[19] = 0
            header[20] = 1 // AudioFormat: 1 = PCM
            header[21] = 0
            header[22] = channels.toByte()
            header[23] = 0
            header[24] = (sampleRate and 0xff).toByte()
            header[25] = ((sampleRate shr 8) and 0xff).toByte()
            header[26] = ((sampleRate shr 16) and 0xff).toByte()
            header[27] = ((sampleRate shr 24) and 0xff).toByte()
            header[28] = (byteRate and 0xff).toByte()
            header[29] = ((byteRate shr 8) and 0xff).toByte()
            header[30] = ((byteRate shr 16) and 0xff).toByte()
            header[31] = ((byteRate shr 24) and 0xff).toByte()
            header[32] = (channels * bitsPerSample / 8).toByte() // BlockAlign
            header[33] = 0
            header[34] = bitsPerSample.toByte()
            header[35] = 0

            // data sub-chunk
            header[36] = 'd'.code.toByte()
            header[37] = 'a'.code.toByte()
            header[38] = 't'.code.toByte()
            header[39] = 'a'.code.toByte()
            header[40] = (pcmDataLength and 0xff).toByte()
            header[41] = ((pcmDataLength shr 8) and 0xff).toByte()
            header[42] = ((pcmDataLength shr 16) and 0xff).toByte()
            header[43] = ((pcmDataLength shr 24) and 0xff).toByte()

            wavOut.write(header, 0, 44)
            pcmIn.copyTo(wavOut)
        } finally {
            pcmIn.close()
            wavOut.close()
        }
    }
}
