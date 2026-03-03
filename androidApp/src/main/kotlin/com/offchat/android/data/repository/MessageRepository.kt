package com.offchat.android.data.repository

import com.offchat.android.data.model.Message
import com.offchat.android.data.model.MessageStatus
import kotlinx.coroutines.flow.Flow

interface MessageRepository {
    fun observeMessages(peerId: String): Flow<List<Message>>
    fun observeAllMessages(): Flow<List<Message>>
    suspend fun saveMessage(message: Message)
    suspend fun updateStatus(messageId: String, status: MessageStatus)
    suspend fun deleteMessagesForPeer(peerId: String)
    suspend fun clearAll()
}
