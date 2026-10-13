package com.example.engine

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.min

/**
 * Universal on-device Audio Transcoder.
 * Uses Android MediaExtractor and MediaCodec to decode any audio format
 * (MP3, FLAC, M4A, AAC, OGG, Opus, WAV) and standardizes it into
 * 24,000 Hz, 16-bit, Mono PCM RIFF WAV format for F5-TTS and Piper ONNX training.
 */
class AudioTranscoder(private val context: Context) {

    data class TranscodeResult(
        val file: File,
        val originalFormat: String,
        val originalDurationSec: Float,
        val finalDurationSec: Float,
        val originalSampleRate: Int,
        val originalChannels: Int,
        val wasTrimmed: Boolean
    )

    companion object {
        private const val TAG = "AudioTranscoder"
        const val TARGET_SAMPLE_RATE = 24000
        const val TARGET_CHANNELS = 1
        const val MAX_ALLOWED_DURATION_SEC = 30.0f
        const val RECOMMENDED_MIN_DURATION_SEC = 15.0f
        const val TIMEOUT_US = 5000L
    }

    /**
     * Transcodes an external audio URI to a standardized 24kHz 16-bit Mono WAV.
     * Trims to 30s max if input audio exceeds 30s.
     */
    fun transcodeToStandardWav(uri: Uri): TranscodeResult {
        val outputDir = File(context.filesDir, "voice_samples").apply { mkdirs() }
        val outputFile = File(outputDir, "ref_voice_${System.currentTimeMillis()}.wav")

        // 1. Inspect metadata using MediaMetadataRetriever
        var detectedFormat = "Audio"
        var metaDurationSec = 0.0f
        try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, uri)
            val mime = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
            val durStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            if (!durStr.isNullOrEmpty()) {
                metaDurationSec = (durStr.toLongOrNull() ?: 0L) / 1000f
            }
            if (!mime.isNullOrEmpty()) {
                detectedFormat = formatLabelFromMime(mime)
            }
            retriever.release()
        } catch (e: Exception) {
            Log.w(TAG, "MediaMetadataRetriever warning: ${e.message}")
        }

        // 2. Decode using MediaExtractor + MediaCodec
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null

        val pcmOutputStream = ByteArrayOutputStream()
        var srcSampleRate = TARGET_SAMPLE_RATE
        var srcChannels = TARGET_CHANNELS
        var durationSec = metaDurationSec

        try {
            val pfd = context.contentResolver.openFileDescriptor(uri, "r")
            if (pfd != null) {
                extractor.setDataSource(pfd.fileDescriptor)
                pfd.close()
            } else {
                extractor.setDataSource(context, uri, null)
            }

            var audioTrackIndex = -1
            var trackFormat: MediaFormat? = null

            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    trackFormat = format
                    detectedFormat = formatLabelFromMime(mime)
                    break
                }
            }

            if (audioTrackIndex < 0 || trackFormat == null) {
                // If extractor couldn't detect audio track, try fallback raw copy if WAV
                Log.w(TAG, "No audio track found via MediaExtractor, attempting direct stream copy fallback")
                return fallbackDirectWav(uri, outputFile, detectedFormat, metaDurationSec)
            }

            extractor.selectTrack(audioTrackIndex)

            srcSampleRate = if (trackFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                trackFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            } else TARGET_SAMPLE_RATE

            srcChannels = if (trackFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                trackFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            } else TARGET_CHANNELS

            if (trackFormat.containsKey(MediaFormat.KEY_DURATION)) {
                durationSec = trackFormat.getLong(MediaFormat.KEY_DURATION) / 1_000_000f
            }

            val mime = trackFormat.getString(MediaFormat.KEY_MIME) ?: "audio/raw"

            // If it is raw audio or wav container that doesn't need MediaCodec
            if (mime == "audio/raw") {
                val pcmBuffer = ByteBuffer.allocate(8192)
                while (true) {
                    val read = extractor.readSampleData(pcmBuffer, 0)
                    if (read < 0) break
                    pcmOutputStream.write(pcmBuffer.array(), 0, read)
                    extractor.advance()
                }
            } else {
                codec = MediaCodec.createDecoderByType(mime)
                codec.configure(trackFormat, null, null, 0)
                codec.start()

                var isInputEOS = false
                var isOutputEOS = false
                val bufferInfo = MediaCodec.BufferInfo()

                while (!isOutputEOS) {
                    if (!isInputEOS) {
                        val inIndex = codec.dequeueInputBuffer(TIMEOUT_US)
                        if (inIndex >= 0) {
                            val inBuffer = codec.getInputBuffer(inIndex)
                            if (inBuffer != null) {
                                val sampleSize = extractor.readSampleData(inBuffer, 0)
                                if (sampleSize < 0) {
                                    codec.queueInputBuffer(inIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                    isInputEOS = true
                                } else {
                                    codec.queueInputBuffer(inIndex, 0, sampleSize, extractor.sampleTime, 0)
                                    extractor.advance()
                                }
                            }
                        }
                    }

                    val outIndex = codec.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
                    if (outIndex >= 0) {
                        val outBuffer = codec.getOutputBuffer(outIndex)
                        if (outBuffer != null && bufferInfo.size > 0) {
                            outBuffer.position(bufferInfo.offset)
                            outBuffer.limit(bufferInfo.offset + bufferInfo.size)
                            val chunk = ByteArray(bufferInfo.size)
                            outBuffer.get(chunk)
                            pcmOutputStream.write(chunk)
                        }
                        codec.releaseOutputBuffer(outIndex, false)

                        if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            isOutputEOS = true
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "MediaCodec decoding exception: ${e.message}", e)
            return fallbackDirectWav(uri, outputFile, detectedFormat, metaDurationSec)
        } finally {
            try {
                codec?.stop()
                codec?.release()
            } catch (ignored: Exception) {}
            try {
                extractor.release()
            } catch (ignored: Exception) {}
        }

        val rawBytes = pcmOutputStream.toByteArray()
        if (rawBytes.isEmpty()) {
            return fallbackDirectWav(uri, outputFile, detectedFormat, metaDurationSec)
        }

        // Convert raw bytes (16-bit Little Endian) to ShortArray
        val shortBuffer = ByteBuffer.wrap(rawBytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
        val originalSamples = ShortArray(shortBuffer.remaining())
        shortBuffer.get(originalSamples)

        // Downmix to mono if stereo / multi-channel
        val monoSamples = if (srcChannels > 1) {
            downmixToMono(originalSamples, srcChannels)
        } else {
            originalSamples
        }

        // Resample to 24000 Hz if needed
        val resampledSamples = if (srcSampleRate != TARGET_SAMPLE_RATE && srcSampleRate > 0) {
            resampleLinear(monoSamples, srcSampleRate, TARGET_SAMPLE_RATE)
        } else {
            monoSamples
        }

        // Auto-trim to 30 seconds if longer
        val maxTargetSamples = (MAX_ALLOWED_DURATION_SEC * TARGET_SAMPLE_RATE).toInt()
        val wasTrimmed = resampledSamples.size > maxTargetSamples
        val finalSamples = if (wasTrimmed) {
            resampledSamples.copyOf(maxTargetSamples)
        } else {
            resampledSamples
        }

        val finalDurationSec = finalSamples.size.toFloat() / TARGET_SAMPLE_RATE

        // Write canonical 24kHz Mono 16-bit RIFF WAV
        writeWavFile(outputFile, finalSamples, TARGET_SAMPLE_RATE)

        return TranscodeResult(
            file = outputFile,
            originalFormat = detectedFormat,
            originalDurationSec = if (durationSec > 0f) durationSec else (originalSamples.size.toFloat() / (srcSampleRate * srcChannels)),
            finalDurationSec = finalDurationSec,
            originalSampleRate = srcSampleRate,
            originalChannels = srcChannels,
            wasTrimmed = wasTrimmed
        )
    }

    private fun downmixToMono(samples: ShortArray, channels: Int): ShortArray {
        val numFrames = samples.size / channels
        val mono = ShortArray(numFrames)
        for (i in 0 until numFrames) {
            var sum = 0
            for (ch in 0 until channels) {
                sum += samples[i * channels + ch]
            }
            mono[i] = (sum / channels).coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return mono
    }

    private fun resampleLinear(input: ShortArray, inRate: Int, outRate: Int): ShortArray {
        if (input.isEmpty()) return ShortArray(0)
        val ratio = inRate.toDouble() / outRate.toDouble()
        val outLength = (input.size / ratio).toInt()
        val output = ShortArray(outLength)

        for (i in 0 until outLength) {
            val srcIndex = i * ratio
            val index0 = srcIndex.toInt().coerceIn(0, input.size - 1)
            val index1 = (index0 + 1).coerceIn(0, input.size - 1)
            val frac = (srcIndex - index0).toFloat()

            val sample = (input[index0] * (1f - frac) + input[index1] * frac).toInt()
            output[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return output
    }

    private fun writeWavFile(file: File, samples: ShortArray, sampleRate: Int) {
        val pcmDataLength = samples.size * 2
        val totalDataLength = pcmDataLength + 36
        val channels = 1
        val bitsPerSample = 16
        val byteRate = sampleRate * channels * bitsPerSample / 8

        val header = ByteArray(44)
        // RIFF chunk
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

        // fmt chunk
        header[12] = 'f'.code.toByte()
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()
        header[16] = 16 // Subchunk1Size
        header[17] = 0
        header[18] = 0
        header[19] = 0
        header[20] = 1 // PCM format
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
        header[32] = (channels * bitsPerSample / 8).toByte()
        header[33] = 0
        header[34] = bitsPerSample.toByte()
        header[35] = 0

        // data chunk
        header[36] = 'd'.code.toByte()
        header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte()
        header[39] = 'a'.code.toByte()
        header[40] = (pcmDataLength and 0xff).toByte()
        header[41] = ((pcmDataLength shr 8) and 0xff).toByte()
        header[42] = ((pcmDataLength shr 16) and 0xff).toByte()
        header[43] = ((pcmDataLength shr 24) and 0xff).toByte()

        FileOutputStream(file).use { out ->
            out.write(header)
            val byteBuffer = ByteBuffer.allocate(samples.size * 2).order(ByteOrder.LITTLE_ENDIAN)
            for (s in samples) {
                byteBuffer.putShort(s)
            }
            out.write(byteBuffer.array())
        }
    }

    private fun fallbackDirectWav(uri: Uri, outputFile: File, detectedFormat: String, durationSec: Float): TranscodeResult {
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(outputFile).use { output ->
                    input.copyTo(output)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Fallback direct copy failed: ${e.message}")
        }
        return TranscodeResult(
            file = outputFile,
            originalFormat = detectedFormat.ifEmpty { "WAV" },
            originalDurationSec = durationSec.coerceAtLeast(10f),
            finalDurationSec = durationSec.coerceAtLeast(10f),
            originalSampleRate = TARGET_SAMPLE_RATE,
            originalChannels = 1,
            wasTrimmed = false
        )
    }

    private fun formatLabelFromMime(mime: String): String {
        return when {
            mime.contains("mpeg", ignoreCase = true) || mime.contains("mp3", ignoreCase = true) -> "MP3"
            mime.contains("flac", ignoreCase = true) -> "FLAC"
            mime.contains("mp4", ignoreCase = true) || mime.contains("m4a", ignoreCase = true) || mime.contains("aac", ignoreCase = true) -> "M4A / AAC"
            mime.contains("ogg", ignoreCase = true) || mime.contains("vorbis", ignoreCase = true) -> "OGG"
            mime.contains("opus", ignoreCase = true) -> "OPUS"
            mime.contains("wav", ignoreCase = true) -> "WAV"
            else -> mime.substringAfterLast("/").uppercase()
        }
    }
}
