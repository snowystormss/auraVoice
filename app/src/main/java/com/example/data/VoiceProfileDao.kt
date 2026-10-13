package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface VoiceProfileDao {
    @Query("SELECT * FROM voice_profiles ORDER BY id ASC")
    fun getAllVoices(): Flow<List<VoiceProfile>>

    @Query("SELECT * FROM voice_profiles WHERE id = :id")
    suspend fun getVoiceById(id: Int): VoiceProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVoice(profile: VoiceProfile)

    @Delete
    suspend fun deleteVoice(profile: VoiceProfile)
}
