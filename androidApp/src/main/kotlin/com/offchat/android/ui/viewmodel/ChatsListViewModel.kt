package com.offchat.android.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.offchat.android.data.model.Message
import com.offchat.android.data.model.Peer
import com.offchat.android.data.repository.MessageRepository
import com.offchat.android.data.repository.PeerRepository
import com.offchat.android.peer.PeerManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

data class ChatSummary(
    val peerId: String,
    val peerName: String,
    val peerPhone: String = "",
    val lastMessage: String,
    val lastTimestamp: Long,
    val messageCount: Int,
    val isConnected: Boolean
)

data class ChatsListUiState(
    val chats: List<ChatSummary> = emptyList(),
    val isLoading: Boolean = true
)

class ChatsListViewModel(
    private val messageRepository: MessageRepository,
    private val peerRepository: PeerRepository,
    private val peerManager: PeerManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatsListUiState())
    val uiState: StateFlow<ChatsListUiState> = _uiState.asStateFlow()

    init {
        observeChats()
    }

    private fun observeChats() {
        combine(
            messageRepository.observeAllMessages(),
            peerRepository.observePeers(),
            peerManager.peers
        ) { messages, dbPeers, livePeers ->
            buildChatSummaries(messages, dbPeers, livePeers)
        }
        .onEach { summaries ->
            _uiState.value = ChatsListUiState(chats = summaries, isLoading = false)
        }
        .catch { e ->
            _uiState.value = ChatsListUiState(chats = _uiState.value.chats, isLoading = false)
        }
        .launchIn(viewModelScope)
    }

    private fun buildChatSummaries(
        messages: List<Message>,
        dbPeers: List<Peer>,
        livePeers: List<Peer>
    ): List<ChatSummary> {
        if (messages.isEmpty()) return emptyList()

        val grouped = messages.groupBy { it.peerId }
        val liveMap = livePeers.associateBy { it.endpointId }
        val dbMap = dbPeers.associateBy { it.endpointId }

        return grouped.map { (peerId, peerMessages) ->
            val sortedMessages = peerMessages.sortedByDescending { it.timestamp }
            val lastMsg = sortedMessages.first()

            val livePeer = liveMap[peerId]
            val dbPeer = dbMap[peerId]
            val incomingName = peerMessages.firstOrNull { !it.isOutgoing }?.senderName

            // Parse display name from "Name|Phone" format
            val rawName = livePeer?.name ?: dbPeer?.name ?: incomingName ?: peerId.take(8)
            val parts = rawName.split("|", limit = 2)
            val displayName = parts[0].ifBlank { rawName }
            val displayPhone = if (parts.size >= 2) parts[1] else ""

            val isConnected = livePeer?.isConnected ?: false

            ChatSummary(
                peerId = peerId,
                peerName = displayName,
                peerPhone = displayPhone,
                lastMessage = lastMsg.content,
                lastTimestamp = lastMsg.timestamp,
                messageCount = peerMessages.size,
                isConnected = isConnected
            )
        }.sortedByDescending { it.lastTimestamp }
    }
}
