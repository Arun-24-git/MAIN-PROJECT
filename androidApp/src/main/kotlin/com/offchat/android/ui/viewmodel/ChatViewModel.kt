package com.offchat.android.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.offchat.android.crypto.Encryption
import com.offchat.android.data.model.Message
import com.offchat.android.data.model.MessagePayload
import com.offchat.android.data.model.MessageStatus
import com.offchat.android.data.repository.MessageRepository
import com.offchat.android.peer.PeerManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

data class ChatUiState(
    val messages: List<Message> = emptyList(),
    val isLoading: Boolean = false,
    val isConnected: Boolean = true,
    val error: String? = null
)

@OptIn(ExperimentalEncodingApi::class)
class ChatViewModel(
    private val messageRepository: MessageRepository,
    private val peerManager: PeerManager,
    private val encryption: Encryption,
    private val appContext: Context,
    private val localDeviceId: String,
    private val localDeviceName: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState(isLoading = true))
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var sessionKey: ByteArray = "offchat-fixed-32-byte-secret-key".encodeToByteArray()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    init {
        observeIncomingPayloads()
    }

    fun observeMessages(peerId: String) {
        messageRepository.observeMessages(peerId)
            .onEach { messages ->
                _uiState.value = _uiState.value.copy(messages = messages, isLoading = false)
            }
            .catch { e ->
                _uiState.value = _uiState.value.copy(error = e.message, isLoading = false)
            }
            .launchIn(viewModelScope)
    }

    /** Observe connection status for a specific peer */
    fun observeConnectionStatus(peerId: String) {
        peerManager.peers
            .map { peers -> peers.any { it.endpointId == peerId && it.isConnected } }
            .distinctUntilChanged()
            .onEach { connected ->
                _uiState.value = _uiState.value.copy(isConnected = connected)
            }
            .launchIn(viewModelScope)
    }

    /** Attempt to reconnect to a peer */
    fun reconnect(peerId: String) {
        _uiState.value = _uiState.value.copy(error = null)
        peerManager.requestConnection(peerId)
    }

    // ------------------------------------------------------------------
    // Text messages
    // ------------------------------------------------------------------

    fun sendMessage(peerId: String, text: String) {
        viewModelScope.launch {
            try {
                val result = encryption.encrypt(text.encodeToByteArray(), sessionKey)
                val payload = MessagePayload(
                    encryptedText = Base64.encode(result.ciphertext),
                    iv = Base64.encode(result.iv),
                    signature = "text",
                    senderId = localDeviceId,
                    senderName = localDeviceName,
                    messageId = UUID.randomUUID().toString(),
                    timestamp = System.currentTimeMillis()
                )
                val jsonBytes = json.encodeToString(payload).encodeToByteArray()
                peerManager.sendPayload(peerId, jsonBytes)

                val message = Message(
                    id = payload.messageId,
                    senderId = localDeviceId,
                    senderName = localDeviceName,
                    content = text,
                    encryptedContent = payload.encryptedText,
                    iv = payload.iv,
                    signature = "text",
                    timestamp = payload.timestamp,
                    status = MessageStatus.SENT,
                    isOutgoing = true,
                    peerId = peerId
                )
                messageRepository.saveMessage(message)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Failed to send: ${e.message}")
            }
        }
    }

    // ------------------------------------------------------------------
    // Voice messages
    // ------------------------------------------------------------------

    fun sendVoiceMessage(peerId: String, audioFilePath: String) {
        viewModelScope.launch {
            try {
                val audioBytes = File(audioFilePath).readBytes()
                if (audioBytes.size > 28_000) {
                    _uiState.value = _uiState.value.copy(error = "Voice message too long. Keep under 15 seconds.")
                    return@launch
                }

                val result = encryption.encrypt(audioBytes, sessionKey)
                val payload = MessagePayload(
                    encryptedText = Base64.encode(result.ciphertext),
                    iv = Base64.encode(result.iv),
                    signature = "voice",
                    senderId = localDeviceId,
                    senderName = localDeviceName,
                    messageId = UUID.randomUUID().toString(),
                    timestamp = System.currentTimeMillis()
                )
                val jsonBytes = json.encodeToString(payload).encodeToByteArray()
                peerManager.sendPayload(peerId, jsonBytes)

                // Store voice file permanently
                val voiceDir = File(appContext.filesDir, "voice_messages")
                voiceDir.mkdirs()
                val permanentFile = File(voiceDir, "${payload.messageId}.amr")
                File(audioFilePath).copyTo(permanentFile, overwrite = true)

                val message = Message(
                    id = payload.messageId,
                    senderId = localDeviceId,
                    senderName = localDeviceName,
                    content = "voice:${permanentFile.absolutePath}",
                    encryptedContent = payload.encryptedText,
                    iv = payload.iv,
                    signature = "voice",
                    timestamp = payload.timestamp,
                    status = MessageStatus.SENT,
                    isOutgoing = true,
                    peerId = peerId
                )
                messageRepository.saveMessage(message)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Failed to send voice: ${e.message}")
            }
        }
    }

    // ------------------------------------------------------------------
    // Incoming payload handling
    // ------------------------------------------------------------------

    private fun observeIncomingPayloads() {
        peerManager.incomingPayloads
            .onEach { (endpointId, bytes) ->
                handleIncomingPayload(endpointId, bytes)
            }
            .catch { e ->
                _uiState.value = _uiState.value.copy(error = "Connection error: ${e.message}")
            }
            .launchIn(viewModelScope)
    }

    private suspend fun handleIncomingPayload(endpointId: String, bytes: ByteArray) {
        try {
            val rawJson = bytes.decodeToString().trim { it <= ' ' || it == '\u0000' }
            if (rawJson.isEmpty()) return

            val payload = json.decodeFromString<MessagePayload>(rawJson)
            val ciphertext = Base64.decode(payload.encryptedText)
            val iv = Base64.decode(payload.iv)
            val isVoice = payload.signature == "voice"

            if (isVoice) {
                // Decrypt audio bytes and save to file
                val audioBytes = try {
                    encryption.decrypt(ciphertext, sessionKey, iv)
                } catch (e: Exception) {
                    null
                }

                val content = if (audioBytes != null) {
                    val voiceDir = File(appContext.filesDir, "voice_messages")
                    voiceDir.mkdirs()
                    val audioFile = File(voiceDir, "${payload.messageId}.amr")
                    audioFile.writeBytes(audioBytes)
                    "voice:${audioFile.absolutePath}"
                } else {
                    "[Voice decryption error]"
                }

                val message = Message(
                    id = payload.messageId,
                    senderId = payload.senderId,
                    senderName = payload.senderName,
                    content = content,
                    encryptedContent = payload.encryptedText,
                    iv = payload.iv,
                    signature = "voice",
                    timestamp = payload.timestamp,
                    status = MessageStatus.DELIVERED,
                    isOutgoing = false,
                    peerId = endpointId
                )
                messageRepository.saveMessage(message)
            } else {
                // Text message
                val plaintext = try {
                    encryption.decrypt(ciphertext, sessionKey, iv).decodeToString()
                } catch (e: Exception) {
                    "[Decryption Error]"
                }

                val message = Message(
                    id = payload.messageId,
                    senderId = payload.senderId,
                    senderName = payload.senderName,
                    content = plaintext,
                    encryptedContent = payload.encryptedText,
                    iv = payload.iv,
                    signature = payload.signature,
                    timestamp = payload.timestamp,
                    status = MessageStatus.DELIVERED,
                    isOutgoing = false,
                    peerId = endpointId
                )
                messageRepository.saveMessage(message)
            }
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(error = "Parse error: ${e.message}")
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
