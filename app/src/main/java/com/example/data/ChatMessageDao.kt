package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatMessageDao {

    @Query("SELECT * FROM chat_sessions ORDER BY updatedAt DESC")
    fun getAllSessions(): Flow<List<ChatSession>>

    @Query("""
        SELECT DISTINCT s.* FROM chat_sessions s
        LEFT JOIN chat_messages m ON s.id = m.sessionId
        WHERE s.title LIKE '%' || :query || '%' 
           OR m.text LIKE '%' || :query || '%'
        ORDER BY s.updatedAt DESC
    """)
    fun searchSessions(query: String): Flow<List<ChatSession>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ChatSession): Long

    @Query("UPDATE chat_sessions SET title = :title, updatedAt = :timestamp WHERE id = :sessionId")
    suspend fun updateSessionTitleAndTime(sessionId: Long, title: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE chat_sessions SET updatedAt = :timestamp WHERE id = :sessionId")
    suspend fun touchSession(sessionId: Long, timestamp: Long = System.currentTimeMillis())

    @Delete
    suspend fun deleteSession(session: ChatSession)

    @Query("""
        SELECT * FROM (
            SELECT * FROM chat_messages 
            WHERE sessionId = :sessionId 
            ORDER BY id DESC LIMIT 100
        ) ORDER BY id ASC
    """)
    fun getMessagesForSession(sessionId: Long): Flow<List<ChatMessage>>

    @Insert
    suspend fun insertMessage(message: ChatMessage)

    @Query("DELETE FROM chat_messages")
    suspend fun clearAll()
}