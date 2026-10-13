package com.example.engine

import kotlin.math.max
import kotlin.math.min

/**
 * Pure on-device rule-based acoustic parser for text descriptions and iterative refinements.
 * Converts natural language descriptions (e.g. "Warm deep baritone, slow cadence, formal")
 * into concrete TTS acoustic parameters (pitch, speed, emotion, personality prompt).
 */
object TextVoiceParser {

    data class ParsedVoiceTraits(
        val pitch: Float = 1.0f,
        val speechRate: Float = 1.0f,
        val emotion: String = "calm",
        val detectedTags: List<String> = emptyList(),
        val personalityPrompt: String = ""
    )

    data class RefinementResult(
        val newPitch: Float,
        val newSpeechRate: Float,
        val newEmotion: String,
        val appliedChanges: List<String>,
        val summary: String
    )

    /**
     * Parses a free-form natural language description into initial acoustic parameters.
     */
    fun parseDescription(description: String): ParsedVoiceTraits {
        if (description.isBlank()) {
            return ParsedVoiceTraits()
        }

        val text = description.lowercase()
        val tags = mutableListOf<String>()

        // 1. Pitch estimation
        var pitch = 1.0f
        when {
            containsAny(text, "very deep", "ultra deep", "sub bass", "deep baritone", "heavy rumble") -> {
                pitch = 0.68f
                tags.add("Ultra Deep")
            }
            containsAny(text, "deep", "baritone", "bass", "gravelly", "low pitch", "low tone", "gruff", "raspy", "dark voice") -> {
                pitch = 0.82f
                tags.add("Deep")
            }
            containsAny(text, "somewhat deep", "warm lower", "mellow low", "husky") -> {
                pitch = 0.90f
                tags.add("Warm Low")
            }
            containsAny(text, "very high", "squeaky", "child", "toddler", "cartoon", "elf", "helium") -> {
                pitch = 1.45f
                tags.add("High Squeak")
            }
            containsAny(text, "high", "soprano", "youthful", "young girl", "airy", "bright pitch", "light voice", "anime") -> {
                pitch = 1.25f
                tags.add("High & Bright")
            }
            containsAny(text, "medium high", "perky", "cheerful pitch") -> {
                pitch = 1.12f
                tags.add("Elevated Pitch")
            }
            else -> {
                pitch = 1.0f
                tags.add("Balanced Pitch")
            }
        }

        // 2. Speech Rate / Cadence estimation
        var rate = 1.0f
        when {
            containsAny(text, "very slow", "extremely slow", "crawl", "sluggish", "sleepy") -> {
                rate = 0.68f
                tags.add("Very Slow")
            }
            containsAny(text, "slow", "deliberate", "leisurely", "unhurried", "measured", "gentle pacing", "meditative", "languid") -> {
                rate = 0.82f
                tags.add("Relaxed Pace")
            }
            containsAny(text, "laid back", "calm pace", "steady") -> {
                rate = 0.92f
                tags.add("Steady Pace")
            }
            containsAny(text, "very fast", "rapid fire", "auctioneer", "extremely fast", "super fast", "hyper") -> {
                rate = 1.45f
                tags.add("Ultra Rapid")
            }
            containsAny(text, "fast", "rapid", "brisk", "speedy", "quick", "fast-paced", "hurried", "energetic speech") -> {
                rate = 1.25f
                tags.add("Brisk Pace")
            }
            containsAny(text, "dynamic", "punchy") -> {
                rate = 1.12f
                tags.add("Dynamic")
            }
            else -> {
                rate = 1.0f
                tags.add("Normal Speed")
            }
        }

        // 3. Emotion classification
        val emotion: String = when {
            containsAny(text, "enthusiastic", "excited", "energetic", "hyped", "passionate", "vibrant", "eager") -> {
                tags.add("Enthusiastic")
                "enthusiastic"
            }
            containsAny(text, "happy", "joyful", "cheerful", "upbeat", "friendly", "smiling", "sunny", "playful") -> {
                tags.add("Happy")
                "happy"
            }
            containsAny(text, "sad", "somber", "melancholic", "sorrowful", "gloomy", "depressed", "mournful") -> {
                tags.add("Sad")
                "sad"
            }
            containsAny(text, "formal", "professional", "serious", "authoritative", "news anchor", "corporate", "documentary", "academic") -> {
                tags.add("Formal")
                "formal"
            }
            else -> {
                tags.add("Calm")
                "calm"
            }
        }

        val personalityPrompt = buildString {
            append("Voice style: ")
            append(tags.joinToString(", "))
            append(". Target acoustic profile: pitch ")
            append(String.format("%.2f", pitch))
            append("x, rate ")
            append(String.format("%.2f", rate))
            append("x, emotion: ")
            append(emotion)
        }

        return ParsedVoiceTraits(
            pitch = pitch,
            speechRate = rate,
            emotion = emotion,
            detectedTags = tags,
            personalityPrompt = personalityPrompt
        )
    }

    /**
     * Applies incremental text-based fine-tuning commands to modify existing acoustic parameters.
     */
    fun refineVoice(
        currentPitch: Float,
        currentSpeed: Float,
        currentEmotion: String,
        refinementCommand: String
    ): RefinementResult {
        val text = refinementCommand.lowercase().trim()
        if (text.isEmpty()) {
            return RefinementResult(
                newPitch = currentPitch,
                newSpeechRate = currentSpeed,
                newEmotion = currentEmotion,
                appliedChanges = emptyList(),
                summary = "No refinement instructions provided."
            )
        }

        var pitch = currentPitch
        var speed = currentSpeed
        var emotion = currentEmotion
        val changes = mutableListOf<String>()

        // Pitch refinements
        when {
            containsAny(text, "much deeper", "a lot deeper", "way deeper", "much lower", "very deep") -> {
                pitch = max(0.55f, pitch - 0.22f)
                changes.add("Pitch lowered significantly (-0.22)")
            }
            containsAny(text, "deeper", "lower pitch", "lower", "more bass", "more baritone", "deepen", "gravelly", "gruffer") -> {
                pitch = max(0.55f, pitch - 0.12f)
                changes.add("Pitch deepened (-0.12)")
            }
            containsAny(text, "slightly deeper", "a bit deeper", "a little lower", "a tad deeper") -> {
                pitch = max(0.55f, pitch - 0.06f)
                changes.add("Pitch subtly lowered (-0.06)")
            }
            containsAny(text, "much higher", "way higher", "a lot higher", "very high", "much lighter") -> {
                pitch = min(1.80f, pitch + 0.22f)
                changes.add("Pitch raised significantly (+0.22)")
            }
            containsAny(text, "higher", "higher pitch", "raise pitch", "lighter tone", "brighter", "more soprano", "younger") -> {
                pitch = min(1.80f, pitch + 0.12f)
                changes.add("Pitch raised (+0.12)")
            }
            containsAny(text, "slightly higher", "a bit higher", "a little higher", "a tad higher") -> {
                pitch = min(1.80f, pitch + 0.06f)
                changes.add("Pitch subtly raised (+0.06)")
            }
            containsAny(text, "reset pitch", "default pitch", "normal pitch") -> {
                pitch = 1.0f
                changes.add("Pitch reset to 1.0")
            }
        }

        // Speed / Cadence refinements
        when {
            containsAny(text, "much slower", "way slower", "a lot slower", "very slow", "slow way down") -> {
                speed = max(0.55f, speed - 0.25f)
                changes.add("Speed slowed down significantly (-0.25)")
            }
            containsAny(text, "slower", "slow down", "more deliberate", "more leisurely", "unhurried", "reduce speed", "relax pace") -> {
                speed = max(0.55f, speed - 0.14f)
                changes.add("Speed slowed down (-0.14)")
            }
            containsAny(text, "slightly slower", "a bit slower", "a little slower", "a tad slower") -> {
                speed = max(0.55f, speed - 0.06f)
                changes.add("Speed subtly slowed (-0.06)")
            }
            containsAny(text, "much faster", "way faster", "a lot faster", "super fast", "speed up a lot") -> {
                speed = min(1.90f, speed + 0.25f)
                changes.add("Speed increased significantly (+0.25)")
            }
            containsAny(text, "faster", "speed up", "quicker", "more rapid", "brisk", "increase speed", "pick up pace") -> {
                speed = min(1.90f, speed + 0.14f)
                changes.add("Speed accelerated (+0.14)")
            }
            containsAny(text, "slightly faster", "a bit faster", "a little faster", "a tad faster") -> {
                speed = min(1.90f, speed + 0.06f)
                changes.add("Speed subtly accelerated (+0.06)")
            }
            containsAny(text, "reset speed", "default speed", "normal speed", "regular pace") -> {
                speed = 1.0f
                changes.add("Speed reset to 1.0")
            }
        }

        // Emotion refinements
        when {
            containsAny(text, "more enthusiastic", "add excitement", "more energetic", "pump up", "hyped", "enthusiastic") -> {
                emotion = "enthusiastic"
                changes.add("Emotion set to Enthusiastic")
            }
            containsAny(text, "more cheerful", "sound happier", "happier", "friendly tone", "upbeat", "happy") -> {
                emotion = "happy"
                changes.add("Emotion set to Happy")
            }
            containsAny(text, "calmer", "more calm", "peaceful", "relaxing", "chill", "soften tone", "gentle") -> {
                emotion = "calm"
                changes.add("Emotion set to Calm")
            }
            containsAny(text, "more serious", "more formal", "professional tone", "authoritative", "formal", "corporate") -> {
                emotion = "formal"
                changes.add("Emotion set to Formal")
            }
            containsAny(text, "sad tone", "somber", "melancholic", "mournful", "sad") -> {
                emotion = "sad"
                changes.add("Emotion set to Sad")
            }
        }

        if (changes.isEmpty()) {
            changes.add("Style conditioning prompt updated with instructions")
        }

        val summary = "Tuned: " + changes.joinToString("; ")

        return RefinementResult(
            newPitch = pitch,
            newSpeechRate = speed,
            newEmotion = emotion,
            appliedChanges = changes,
            summary = summary
        )
    }

    private fun containsAny(text: String, vararg keywords: String): Boolean {
        for (keyword in keywords) {
            if (text.contains(keyword)) return true
        }
        return false
    }
}
