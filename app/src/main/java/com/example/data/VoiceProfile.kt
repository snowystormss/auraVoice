package com.example.data

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.PrimaryKey

@Immutable
@Entity(tableName = "voice_profiles")
data class VoiceProfile(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val onnxFilePath: String,
    val jsonConfigPath: String,
    val sampleRate: Int = 22050,
    val speakerId: Int = 0,
    val lengthScale: Float = 1.0f, // controls speed (lower = faster)
    val noiseScale: Float = 0.667f, // controls expressiveness
    val noiseW: Float = 0.8f,
    val isActiveSystemVoice: Boolean = false,
    val addedTimestamp: Long = System.currentTimeMillis()
)