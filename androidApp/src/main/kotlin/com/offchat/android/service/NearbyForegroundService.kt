package com.offchat.android.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.offchat.android.MainActivity
import com.offchat.android.R
import com.offchat.android.crypto.Encryption
import com.offchat.android.data.model.Message
import com.offchat.android.data.model.MessagePayload
import com.offchat.android.data.model.MessageStatus
import com.offchat.android.data.repository.MessageRepository
import com.offchat.android.notification.NotificationHelper
import com.offchat.android.peer.ConnectionEvent
import com.offchat.android.peer.PeerManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import org.koin.android.ext.android.inject
import java.io.File
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

private const val TAG = "NearbyFGService"

/**
 * Foreground Service:
 * 1. Keeps Nearby Connections alive in the background
 * 2. Processes incoming messages (parse → decrypt → save to DB)
 * 3. Shows push notifications for new messages
 * 4. Auto-reconnects to previously connected peers
 */
@OptIn(ExperimentalEncodingApi::class)
class NearbyForegroundService : Service() {

    private val peerManager: PeerManager by inject()
    private val messageRepository: MessageRepository by inject()
    private val encryption: Encryption by inject()

    private lateinit var notificationHelper: NotificationHelper

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private val sessionKey: ByteArray = "offchat-fixed-32-byte-secret-key".encodeToByteArray()

    // ------------------------------------------------------------------
    // Service lifecycle
    // ------------------------------------------------------------------

    override fun onCreate() {
        super.onCreate()
        createConnectionChannel()
        notificationHelper = NotificationHelper(this)
        startObservingMessages()
        startAutoReconnect()
        startPendingMessageSync()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val deviceName = intent.getStringExtra(EXTRA_DEVICE_NAME) ?: defaultDeviceName()
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    startForeground(
                        NOTIFICATION_ID,
                        buildConnectionNotification(),
                        android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
                    )
                } else {
                    startForeground(NOTIFICATION_ID, buildConnectionNotification())
                }
                peerManager.startDiscovery(deviceName)
            }
            ACTION_STOP -> {
                peerManager.stopDiscovery()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        serviceScope.cancel()
        peerManager.release()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ------------------------------------------------------------------
    // Background message processing
    // ------------------------------------------------------------------

    private fun startObservingMessages() {
        peerManager.incomingPayloads
            .onEach { (endpointId, bytes) ->
                handleIncomingMessage(endpointId, bytes)
            }
            .catch { e ->
                Log.e(TAG, "Error observing payloads", e)
            }
            .launchIn(serviceScope)
    }

    private suspend fun handleIncomingMessage(endpointId: String, bytes: ByteArray) {
        try {
            val rawJson = bytes.decodeToString().trim { it <= ' ' || it == '\u0000' }
            if (rawJson.isEmpty()) return

            val payload = json.decodeFromString<MessagePayload>(rawJson)
            val ciphertext = Base64.decode(payload.encryptedText)
            val iv = Base64.decode(payload.iv)
            val isVoice = payload.signature == "voice"

            val content: String
            val notificationText: String

            if (isVoice) {
                val audioBytes = try {
                    encryption.decrypt(ciphertext, sessionKey, iv)
                } catch (e: Exception) {
                    null
                }
                content = if (audioBytes != null) {
                    val voiceDir = File(filesDir, "voice_messages")
                    voiceDir.mkdirs()
                    val audioFile = File(voiceDir, "${payload.messageId}.amr")
                    audioFile.writeBytes(audioBytes)
                    "voice:${audioFile.absolutePath}"
                } else {
                    "[Voice decryption error]"
                }
                notificationText = "🎤 Voice message"
            } else {
                content = try {
                    encryption.decrypt(ciphertext, sessionKey, iv).decodeToString()
                } catch (e: Exception) {
                    "[Encrypted message]"
                }
                notificationText = content
            }

            val message = Message(
                id = payload.messageId,
                senderId = payload.senderId,
                senderName = payload.senderName,
                content = content,
                encryptedContent = payload.encryptedText,
                iv = payload.iv,
                signature = payload.signature,
                timestamp = payload.timestamp,
                status = MessageStatus.DELIVERED,
                isOutgoing = false,
                peerId = payload.senderName // FIX: Use stable senderName instead of transient endpointId
            )
            messageRepository.saveMessage(message)

            // Parse display name from "Name|Phone" for notification
            val parts = payload.senderName.split("|", limit = 2)
            val displayPhone = if (parts.size >= 2) parts[1] else ""
            val displayName = parts[0].ifBlank { payload.senderName }
            val notifyTitle = if (displayPhone.isNotBlank()) "$displayPhone • $displayName" else displayName

            notificationHelper.showMessageNotification(
                senderName = notifyTitle,
                messageText = notificationText,
                peerId = payload.senderName // Use stable senderName here too
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to process background message", e)
        }
    }

    // ------------------------------------------------------------------
    // Auto-reconnect for lost connections
    // ------------------------------------------------------------------

    private fun startAutoReconnect() {
        peerManager.connectionEvents
            .filterIsInstance<ConnectionEvent.Disconnected>()
            .onEach { event ->
                Log.d(TAG, "Peer ${event.endpointId} disconnected, will retry in 5s")
                delay(5000)
                Log.d(TAG, "Auto-reconnecting to ${event.endpointId}")
                peerManager.requestConnection(event.endpointId)
            }
            .catch { Log.e(TAG, "Auto-reconnect error", it) }
            .launchIn(serviceScope)
    }

    // ------------------------------------------------------------------
    // Store-and-Forward: flush pending messages on reconnect
    // ------------------------------------------------------------------

    private fun startPendingMessageSync() {
        peerManager.connectionEvents
            .filterIsInstance<ConnectionEvent.Connected>()
            .onEach { event ->
                // Whenever a peer connects, process its queue
                processPendingMessages(event.endpointId)
            }
            .catch { Log.e(TAG, "Sync error", it) }
            .launchIn(serviceScope)
    }

    private suspend fun processPendingMessages(endpointId: String) {
        try {
            // FIX: Look up the stable name of the peer we just connected to
            val peerName = peerManager.peers.value.firstOrNull { it.endpointId == endpointId }?.name ?: return
            
            // Fetch messages using the stable name
            val pendingMessages = messageRepository.getPendingMessages(peerName)
            if (pendingMessages.isEmpty()) return

            Log.d(TAG, "Forwarding ${pendingMessages.size} stored messages to $peerName")

            for (msg in pendingMessages) {
                val payload = MessagePayload(
                    encryptedText = msg.encryptedContent,
                    iv = msg.iv,
                    signature = msg.signature,
                    senderId = msg.senderId,
                    senderName = msg.senderName,
                    messageId = msg.id,
                    timestamp = msg.timestamp
                )
                val jsonBytes = json.encodeToString(payload).encodeToByteArray()
                peerManager.sendPayload(endpointId, jsonBytes) // Send to the active socket

                messageRepository.updateStatus(msg.id, MessageStatus.SENT)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to process pending messages", e)
        }
    }

    // ------------------------------------------------------------------
    // Connection notification (persistent foreground)
    // ------------------------------------------------------------------

    private fun createConnectionChannel() {
        val channel = NotificationChannel(
            CONNECTION_CHANNEL_ID,
            "HOPE NET Connection",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Keeps peer-to-peer connections active in the background"
        }
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(channel)
    }

    private fun buildConnectionNotification() = NotificationCompat.Builder(this, CONNECTION_CHANNEL_ID)
        .setContentTitle("HOPE NET Active")
        .setContentText("Mesh network connections running")
        .setSmallIcon(R.drawable.ic_notification)
        .setOngoing(true)
        .setContentIntent(launchAppPendingIntent())
        .build()

    private fun launchAppPendingIntent(): PendingIntent {
        val launchIntent = Intent(this, MainActivity::class.java)
        return PendingIntent.getActivity(
            this, 0, launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun defaultDeviceName(): String =
        android.os.Build.MODEL ?: "HOPE NET Device"

    // ------------------------------------------------------------------
    // Companion
    // ------------------------------------------------------------------

    companion object {
        private const val CONNECTION_CHANNEL_ID = "offchat_nearby_channel"
        private const val NOTIFICATION_ID = 1001
        private const val ACTION_START = "com.offchat.android.action.START_NEARBY"
        private const val ACTION_STOP = "com.offchat.android.action.STOP_NEARBY"
        private const val EXTRA_DEVICE_NAME = "extra_device_name"

        fun startAction(context: Context, deviceName: String): Intent =
            Intent(context, NearbyForegroundService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_DEVICE_NAME, deviceName)
            }

        fun stopAction(context: Context): Intent =
            Intent(context, NearbyForegroundService::class.java).apply {
                action = ACTION_STOP
            }
    }
}
