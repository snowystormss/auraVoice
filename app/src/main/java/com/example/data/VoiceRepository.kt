package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class VoiceRepository(
    private val voiceDao: VoiceProfileDao,
    private val chatDao: ChatMessageDao
) {
    val allProfiles: Flow<List<VoiceProfile>> = voiceDao.getAllProfiles()

    fun getSessions(searchQuery: String): Flow<List<ChatSession>> {
        return if (searchQuery.isBlank()) {
            chatDao.getAllSessions()
        } else {
            chatDao.searchSessions(searchQuery.trim())
        }
    }

    fun getMessagesForSession(sessionId: Long): Flow<List<ChatMessage>> {
        return chatDao.getMessagesForSession(sessionId)
    }

    suspend fun createSession(title: String = "new chat"): Long = withContext(Dispatchers.IO) {
        chatDao.insertSession(ChatSession(title = title))
    }

    suspend fun updateSessionTitle(sessionId: Long, title: String) = withContext(Dispatchers.IO) {
        chatDao.updateSessionTitleAndTime(sessionId, title)
    }

    suspend fun touchSession(sessionId: Long) = withContext(Dispatchers.IO) {
        chatDao.touchSession(sessionId)
    }

    suspend fun deleteSession(session: ChatSession) = withContext(Dispatchers.IO) {
        chatDao.deleteSession(session)
    }

    suspend fun insertMessage(message: ChatMessage) = withContext(Dispatchers.IO) {
        chatDao.insertMessage(message)
        chatDao.touchSession(message.sessionId)
    }

    suspend fun insertProfile(profile: VoiceProfile): Long = withContext(Dispatchers.IO) {
        voiceDao.insert(profile)
    }

    suspend fun updateProfile(profile: VoiceProfile) = withContext(Dispatchers.IO) {
        voiceDao.update(profile)
    }

    suspend fun deleteProfile(profile: VoiceProfile) = withContext(Dispatchers.IO) {
        voiceDao.delete(profile)
    }

    suspend fun setActiveVoice(profileId: Long) = withContext(Dispatchers.IO) {
        voiceDao.setActiveVoiceTransaction(profileId)
    }
}