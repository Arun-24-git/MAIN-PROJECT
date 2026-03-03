package com.offchat.android.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.offchat.android.data.model.Peer
import com.offchat.db.OffChatDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class PeerRepositoryImpl(
    private val database: OffChatDatabase
) : PeerRepository {

    private val queries = database.peersQueries

    override fun observePeers(): Flow<List<Peer>> =
        queries.selectAll()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows -> rows.map { it.toDomain() } }

    override fun observeConnectedPeers(): Flow<List<Peer>> =
        queries.selectConnected()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows -> rows.map { it.toDomain() } }

    override suspend fun savePeer(peer: Peer) = withContext(Dispatchers.Default) {
        queries.insertPeer(
            endpointId = peer.endpointId,
            name = peer.name,
            publicKey = peer.publicKey,
            lastSeen = peer.lastSeen,
            isConnected = if (peer.isConnected) 1L else 0L
        )
    }

    override suspend fun updateConnectionStatus(
        endpointId: String,
        isConnected: Boolean,
        lastSeen: Long
    ) = withContext(Dispatchers.Default) {
        queries.updateConnectionStatus(
            isConnected = if (isConnected) 1L else 0L,
            lastSeen = lastSeen,
            endpointId = endpointId
        )
    }

    override suspend fun updatePublicKey(endpointId: String, publicKey: String) =
        withContext(Dispatchers.Default) {
            queries.updatePublicKey(publicKey = publicKey, endpointId = endpointId)
        }

    override suspend fun deletePeer(endpointId: String) = withContext(Dispatchers.Default) {
        queries.deleteById(endpointId)
    }

    override suspend fun setAllDisconnected() = withContext(Dispatchers.Default) {
        queries.setAllDisconnected()
    }
}

private fun com.offchat.db.PeerEntity.toDomain(): Peer = Peer(
    endpointId = endpointId,
    name = name,
    publicKey = publicKey,
    lastSeen = lastSeen,
    isConnected = isConnected != 0L
)
