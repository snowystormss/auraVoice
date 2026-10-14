package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface VoiceProfileDao {
    @Query("SELECT * FROM voice_profiles ORDER BY isActiveSystemVoice DESC, addedTimestamp DESC")
    fun getAllProfiles(): Flow<List<VoiceProfile>>

    @Query("SELECT * FROM voice_profiles WHERE isActiveSystemVoice = 1 LIMIT 1")
    fun getActiveProfileSync(): VoiceProfile?

    @Query("SELECT * FROM voice_profiles ORDER BY addedTimestamp DESC LIMIT 1")
    fun getFallbackProfileSync(): VoiceProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profile: VoiceProfile): Long

    @Update
    suspend fun update(profile: VoiceProfile)

    @Delete
    suspend fun delete(profile: VoiceProfile)

    @Query("UPDATE voice_profiles SET isActiveSystemVoice = 0")
    suspend fun clearActiveFlags()

    @Query("UPDATE voice_profiles SET isActiveSystemVoice = 1 WHERE id = :profileId")
    suspend fun markActive(profileId: Long)

    @Transaction
    suspend fun setActiveVoiceTransaction(profileId: Long) {
        clearActiveFlags()
        markActive(profileId)
    }
}