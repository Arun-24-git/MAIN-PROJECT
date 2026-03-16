package com.offchat.android.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.offchat.android.data.model.Message
import com.offchat.android.data.model.MessageStatus
import com.offchat.db.OffChatDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class MessageRepositoryImpl(
    private val database: OffChatDatabase
) : MessageRepository {

    private val queries = database.messagesQueries

    override fun observeMessages(peerId: String): Flow<List<Message>> =
        queries.selectAllByPeer(peerId)
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows -> rows.map { it.toDomain() } }

    override fun observeAllMessages(): Flow<List<Message>> =
        queries.selectAll()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows -> rows.map { it.toDomain() } }

    override suspend fun saveMessage(message: Message) = withContext(Dispatchers.Default) {
        queries.insertMessage(
            id = message.id,
            senderId = message.senderId,
            senderName = message.senderName,
            content = message.content,
            encryptedContent = message.encryptedContent,
            iv = message.iv,
            signature = message.signature,
            timestamp = message.timestamp,
            status = message.status.name,
            isOutgoing = if (message.isOutgoing) 1L else 0L,
            peerId = message.peerId
        )
    }

    override suspend fun updateStatus(messageId: String, status: MessageStatus) =
        withContext(Dispatchers.Default) {
            queries.updateStatus(status = status.name, id = messageId)
        }

    override suspend fun deleteMessagesForPeer(peerId: String) =
        withContext(Dispatchers.Default) {
            queries.deleteByPeer(peerId)
        }

    override suspend fun clearAll() = withContext(Dispatchers.Default) {
        queries.deleteAll()
    }

    override suspend fun getPendingMessages(peerId: String): List<Message> =
        withContext(Dispatchers.Default) {
            queries.selectPendingByPeer(peerId)
                .executeAsList()
                .map { it.toDomain() }
        }
}

private fun com.offchat.db.MessageEntity.toDomain(): Message = Message(
    id = id,
    senderId = senderId,
    senderName = senderName,
    content = content,
    encryptedContent = encryptedContent,
    iv = iv,
    signature = signature,
    timestamp = timestamp,
    status = MessageStatus.valueOf(status),
    isOutgoing = isOutgoing != 0L,
    peerId = peerId
)
