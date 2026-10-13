package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatMessageDao {
    // keeps memory flat on low-ram phones by only holding the latest 100 messages in UI state
    @Query("SELECT * FROM (SELECT * FROM chat_messages ORDER BY id DESC LIMIT 100) ORDER BY id ASC")
    fun getRecentMessages(): Flow<List<ChatMessage>>

    @Insert
    suspend fun insert(message: ChatMessage)

    @Query("DELETE FROM chat_messages")
    suspend fun clearAll()
}