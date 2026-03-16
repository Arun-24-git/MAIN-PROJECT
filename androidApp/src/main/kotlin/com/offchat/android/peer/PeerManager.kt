package com.offchat.android.peer

import com.offchat.android.data.model.Peer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Contract for peer discovery and messaging via Google Nearby Connections.
 */
interface PeerManager {
    val peers: StateFlow<List<Peer>>
    val incomingPayloads: Flow<Pair<String, ByteArray>>
    val connectionEvents: Flow<ConnectionEvent>
    fun startDiscovery(localDeviceName: String)
    fun stopDiscovery()
    fun requestConnection(endpointId: String)
    fun sendPayload(endpointId: String, payload: ByteArray)
    fun disconnect(endpointId: String)
    fun release()
}
