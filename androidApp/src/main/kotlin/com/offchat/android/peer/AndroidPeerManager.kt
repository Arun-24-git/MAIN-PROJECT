package com.offchat.android.peer

import android.content.Context
import android.util.Log
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy
import com.offchat.android.data.model.Peer
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

private const val SERVICE_ID = "com.offchat.nearby"
private const val TAG = "AndroidPeerManager"

class AndroidPeerManager(private val context: Context) : PeerManager {

    private val connectionsClient = Nearby.getConnectionsClient(context)

    private val _peers = MutableStateFlow<List<Peer>>(emptyList())
    override val peers: Flow<List<Peer>> = _peers.asStateFlow()

    private val _incomingPayloads = MutableSharedFlow<Pair<String, ByteArray>>(
        replay = 10,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    override val incomingPayloads: Flow<Pair<String, ByteArray>> = _incomingPayloads.asSharedFlow()

    /** Error/status events for UI feedback */
    private val _connectionEvents = MutableSharedFlow<ConnectionEvent>(
        replay = 0,
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    override val connectionEvents: Flow<ConnectionEvent> = _connectionEvents.asSharedFlow()

    private var localDeviceName: String = android.os.Build.MODEL

    /** Track endpoints with pending connections to avoid duplicates. */
    private val pendingConnections = mutableSetOf<String>()

    // ------------------------------------------------------------------
    // Connection lifecycle — auto-accept any incoming handshake
    // ------------------------------------------------------------------

    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            Log.d(TAG, "Connection initiated with $endpointId (${info.endpointName})")
            connectionsClient.acceptConnection(endpointId, payloadCallback)
            upsertPeer(
                Peer(
                    endpointId = endpointId,
                    name = info.endpointName,
                    lastSeen = System.currentTimeMillis(),
                    isConnected = false
                )
            )
            _connectionEvents.tryEmit(ConnectionEvent.Connecting(endpointId, info.endpointName))
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            pendingConnections.remove(endpointId)
            when (result.status.statusCode) {
                ConnectionsStatusCodes.STATUS_OK -> {
                    Log.d(TAG, "Connected to $endpointId")
                    updatePeerConnected(endpointId, connected = true)
                    val peerName = _peers.value.firstOrNull { it.endpointId == endpointId }?.displayName ?: endpointId
                    _connectionEvents.tryEmit(ConnectionEvent.Connected(endpointId, peerName))
                }
                ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED -> {
                    Log.w(TAG, "Connection rejected by $endpointId")
                    removePeer(endpointId)
                    _connectionEvents.tryEmit(ConnectionEvent.Rejected(endpointId))
                }
                ConnectionsStatusCodes.STATUS_ALREADY_CONNECTED_TO_ENDPOINT -> {
                    Log.d(TAG, "Already connected to $endpointId")
                    updatePeerConnected(endpointId, connected = true)
                }
                else -> {
                    Log.w(TAG, "Connection failed: ${result.status}")
                    removePeer(endpointId)
                    _connectionEvents.tryEmit(ConnectionEvent.Failed(endpointId, "Connection failed (code ${result.status.statusCode})"))
                }
            }
        }

        override fun onDisconnected(endpointId: String) {
            Log.d(TAG, "Disconnected from $endpointId")
            pendingConnections.remove(endpointId)
            // Mark as disconnected but keep in list with wasConnected = true
            _peers.update { current ->
                current.map { peer ->
                    if (peer.endpointId == endpointId)
                        peer.copy(isConnected = false, wasConnected = true, lastSeen = System.currentTimeMillis())
                    else peer
                }
            }
            val peerName = _peers.value.firstOrNull { it.endpointId == endpointId }?.displayName ?: endpointId
            _connectionEvents.tryEmit(ConnectionEvent.Disconnected(endpointId, peerName))
        }
    }

    // ------------------------------------------------------------------
    // Endpoint discovery
    // ------------------------------------------------------------------

    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            Log.d(TAG, "Discovered: $endpointId (${info.endpointName})")
            upsertPeer(
                Peer(
                    endpointId = endpointId,
                    name = info.endpointName,
                    lastSeen = System.currentTimeMillis(),
                    isConnected = false
                )
            )
        }

        override fun onEndpointLost(endpointId: String) {
            Log.d(TAG, "Lost endpoint: $endpointId")
            removePeer(endpointId)
            _connectionEvents.tryEmit(ConnectionEvent.EndpointLost(endpointId))
        }
    }

    // ------------------------------------------------------------------
    // Payload callbacks
    // ------------------------------------------------------------------

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            if (payload.type == Payload.Type.BYTES) {
                val bytes = payload.asBytes() ?: return
                _incomingPayloads.tryEmit(Pair(endpointId, bytes))
            }
        }
        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {}
    }

    // ------------------------------------------------------------------
    // Public API
    // ------------------------------------------------------------------

    override fun startDiscovery(localDeviceName: String) {
        this.localDeviceName = localDeviceName
        val advertisingOptions = AdvertisingOptions.Builder()
            .setStrategy(Strategy.P2P_CLUSTER)
            .build()

        val discoveryOptions = DiscoveryOptions.Builder()
            .setStrategy(Strategy.P2P_CLUSTER)
            .build()

        connectionsClient
            .startAdvertising(localDeviceName, SERVICE_ID, connectionLifecycleCallback, advertisingOptions)
            .addOnFailureListener { e ->
                Log.e(TAG, "Advertising failed", e)
                _connectionEvents.tryEmit(ConnectionEvent.Error("Failed to start advertising: ${e.message}"))
            }

        connectionsClient
            .startDiscovery(SERVICE_ID, endpointDiscoveryCallback, discoveryOptions)
            .addOnFailureListener { e ->
                Log.e(TAG, "Discovery failed", e)
                _connectionEvents.tryEmit(ConnectionEvent.Error("Failed to start discovery: ${e.message}"))
            }
    }

    override fun stopDiscovery() {
        connectionsClient.stopAdvertising()
        connectionsClient.stopDiscovery()
    }

    override fun requestConnection(endpointId: String) {
        if (endpointId in pendingConnections) {
            Log.d(TAG, "Connection to $endpointId already pending, skipping")
            return
        }
        pendingConnections.add(endpointId)
        Log.d(TAG, "Requesting connection to $endpointId")
        connectionsClient.requestConnection(localDeviceName, endpointId, connectionLifecycleCallback)
            .addOnFailureListener { e ->
                Log.e(TAG, "requestConnection failed for $endpointId", e)
                pendingConnections.remove(endpointId)
                _connectionEvents.tryEmit(ConnectionEvent.Failed(endpointId, "Connection request failed: ${e.message}"))
            }
    }

    override fun sendPayload(endpointId: String, payload: ByteArray) {
        connectionsClient.sendPayload(endpointId, Payload.fromBytes(payload))
            .addOnFailureListener { e ->
                Log.e(TAG, "sendPayload failed for $endpointId", e)
                _connectionEvents.tryEmit(ConnectionEvent.Error("Failed to send message: ${e.message}"))
            }
    }

    override fun disconnect(endpointId: String) {
        connectionsClient.disconnectFromEndpoint(endpointId)
        pendingConnections.remove(endpointId)
        removePeer(endpointId)
    }

    override fun release() {
        connectionsClient.stopAllEndpoints()
        pendingConnections.clear()
        _peers.value = emptyList()
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private fun upsertPeer(peer: Peer) {
        _peers.update { current ->
            val existing = current.indexOfFirst { it.endpointId == peer.endpointId }
            if (existing >= 0) {
                val old = current[existing]
                // Don't overwrite a connected peer; preserve wasConnected flag
                val merged = peer.copy(
                    isConnected = old.isConnected || peer.isConnected,
                    wasConnected = old.wasConnected || old.isConnected
                )
                current.toMutableList().also { it[existing] = merged }
            } else current + peer
        }
    }

    private fun updatePeerConnected(endpointId: String, connected: Boolean) {
        _peers.update { current ->
            current.map { peer ->
                if (peer.endpointId == endpointId) {
                    peer.copy(
                        isConnected = connected,
                        wasConnected = if (!connected && peer.isConnected) true else peer.wasConnected,
                        lastSeen = System.currentTimeMillis()
                    )
                } else peer
            }
        }
    }

    private fun removePeer(endpointId: String) {
        _peers.update { current -> current.filter { it.endpointId != endpointId } }
    }
}

// ------------------------------------------------------------------
// Connection events for UI feedback
// ------------------------------------------------------------------

sealed class ConnectionEvent {
    data class Connecting(val endpointId: String, val name: String) : ConnectionEvent()
    data class Connected(val endpointId: String, val name: String) : ConnectionEvent()
    data class Disconnected(val endpointId: String, val name: String) : ConnectionEvent()
    data class Rejected(val endpointId: String) : ConnectionEvent()
    data class Failed(val endpointId: String, val reason: String) : ConnectionEvent()
    data class EndpointLost(val endpointId: String) : ConnectionEvent()
    data class Error(val message: String) : ConnectionEvent()
}
