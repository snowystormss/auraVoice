package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "voice_profiles")
data class VoiceProfile(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val description: String,
    val pitch: Float = 1.0f,
    val speechRate: Float = 1.0f,
    val languageCode: String = "en-US",
    val personalityPrompt: String = "",
    val isPreset: Boolean = false,
    val emotion: String = "calm", // happy, sad, enthusiastic, calm, formal
    val trainedClipPath: String? = null,
    val isTrained: Boolean = false,
    val modelType: String = "SYSTEM_TTS", // "SYSTEM_TTS" or "PIPER_ONNX"
    val generatorModel: String = "F5-TTS (32-step Flow Matching)",
    val lossScore: Float = 0f,
    val trainingDurationSec: Int = 0,
    val sampleRate: Int = 22050,
    val onnxModelPath: String? = null
)
