package com.offchat.android.data.repository

import com.offchat.android.data.model.Peer
import kotlinx.coroutines.flow.Flow

interface PeerRepository {
    fun observePeers(): Flow<List<Peer>>
    fun observeConnectedPeers(): Flow<List<Peer>>
    suspend fun savePeer(peer: Peer)
    suspend fun updateConnectionStatus(endpointId: String, isConnected: Boolean, lastSeen: Long)
    suspend fun updatePublicKey(endpointId: String, publicKey: String)
    suspend fun deletePeer(endpointId: String)
    suspend fun setAllDisconnected()
}
