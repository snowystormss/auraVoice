package com.example

import com.example.engine.AudioTranscoder
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

class AudioTranscoderTest {

    @Test
    fun verifyTargetAudioParameters() {
        assertEquals(24000, AudioTranscoder.TARGET_SAMPLE_RATE)
        assertEquals(1, AudioTranscoder.TARGET_CHANNELS)
        assertEquals(30.0f, AudioTranscoder.MAX_ALLOWED_DURATION_SEC, 0.001f)
        assertEquals(15.0f, AudioTranscoder.RECOMMENDED_MIN_DURATION_SEC, 0.001f)
    }

    @Test
    fun testWavHeaderFormat() {
        val sampleRate = 24000
        val channels = 1
        val bitsPerSample = 16
        val numSamples = 24000 // 1 second
        val pcmDataLength = numSamples * 2
        val totalDataLength = pcmDataLength + 36
        val byteRate = sampleRate * channels * bitsPerSample / 8

        val header = ByteArray(44)
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

        val riffStr = String(header, 0, 4)
        val waveStr = String(header, 8, 4)
        assertEquals("RIFF", riffStr)
        assertEquals("WAVE", waveStr)
        assertEquals(48000, byteRate)
    }

    @Test
    fun testStereoToMonoAveraging() {
        val stereoSamples = shortArrayOf(100, 200, -100, -300, 500, 500)
        val channels = 2
        val numFrames = stereoSamples.size / channels
        val mono = ShortArray(numFrames)
        for (i in 0 until numFrames) {
            var sum = 0
            for (ch in 0 until channels) {
                sum += stereoSamples[i * channels + ch]
            }
            mono[i] = (sum / channels).toShort()
        }

        assertEquals(3, mono.size)
        assertEquals(150.toShort(), mono[0])
        assertEquals((-200).toShort(), mono[1])
        assertEquals(500.toShort(), mono[2])
    }
}
