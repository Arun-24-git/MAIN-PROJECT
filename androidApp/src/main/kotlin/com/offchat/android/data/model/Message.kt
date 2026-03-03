package com.offchat.android.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Message(
    val id: String,
    val senderId: String,
    val senderName: String,
    val content: String,
    val encryptedContent: String,
    val iv: String,
    val signature: String,
    val timestamp: Long,
    val status: MessageStatus,
    val isOutgoing: Boolean,
    val peerId: String
)

@Serializable
enum class MessageStatus {
    SENDING,
    SENT,
    DELIVERED,
    READ,
    FAILED
}

/** Wire-format payload transmitted over Nearby Connections. */
@Serializable
data class MessagePayload(
    val encryptedText: String,
    val iv: String,
    val signature: String,
    val senderId: String,
    val senderName: String,
    val messageId: String,
    val timestamp: Long
)
