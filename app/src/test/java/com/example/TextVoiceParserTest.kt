package com.example

import com.example.engine.TextVoiceParser
import org.junit.Assert.*
import org.junit.Test

class TextVoiceParserTest {

    @Test
    fun testDeepBaritoneDescriptionParsing() {
        val description = "Warm deep baritone narrator with deliberate slow cadence and formal delivery"
        val traits = TextVoiceParser.parseDescription(description)

        assertTrue("Expected pitch to be lower than 1.0 for deep baritone", traits.pitch < 1.0f)
        assertTrue("Expected speech rate to be slower than 1.0 for slow cadence", traits.speechRate < 1.0f)
        assertEquals("formal", traits.emotion)
        assertTrue(traits.detectedTags.contains("Ultra Deep") || traits.detectedTags.contains("Deep"))
        assertTrue(traits.detectedTags.contains("Relaxed Pace") || traits.detectedTags.contains("Very Slow"))
    }

    @Test
    fun testHighEnergeticAnimeDescriptionParsing() {
        val description = "High pitch energetic anime companion, rapid speech, bubbly and cheerful"
        val traits = TextVoiceParser.parseDescription(description)

        assertTrue("Expected pitch to be higher than 1.0", traits.pitch > 1.0f)
        assertTrue("Expected speech rate to be faster than 1.0", traits.speechRate > 1.0f)
        assertTrue(traits.emotion == "enthusiastic" || traits.emotion == "happy")
        assertTrue(traits.detectedTags.contains("High & Bright"))
    }

    @Test
    fun testTextBasedRefinementPitchAndSpeed() {
        val initialPitch = 1.0f
        val initialSpeed = 1.0f
        val initialEmotion = "calm"

        // Refine to be deeper and faster
        val result = TextVoiceParser.refineVoice(
            currentPitch = initialPitch,
            currentSpeed = initialSpeed,
            currentEmotion = initialEmotion,
            refinementCommand = "Make it deeper and speak faster"
        )

        assertTrue("Pitch should have decreased", result.newPitch < initialPitch)
        assertTrue("Speed should have increased", result.newSpeechRate > initialSpeed)
        assertTrue("Result summary should describe changes", result.summary.contains("Tuned:"))
    }

    @Test
    fun testEmotionRefinement() {
        val result = TextVoiceParser.refineVoice(
            currentPitch = 0.9f,
            currentSpeed = 1.0f,
            currentEmotion = "calm",
            refinementCommand = "Make it more enthusiastic and cheerful"
        )

        assertTrue("Emotion should be updated", result.newEmotion == "enthusiastic" || result.newEmotion == "happy")
    }
}
