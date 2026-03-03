package com.offchat.android.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.offchat.android.data.model.Peer
import com.offchat.android.data.repository.PeerRepository
import com.offchat.android.peer.ConnectionEvent
import com.offchat.android.peer.PeerManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

data class PeerDiscoveryUiState(
    val discoveredPeers: List<Peer> = emptyList(),
    val connectedPeers: List<Peer> = emptyList(),
    val disconnectedPeers: List<Peer> = emptyList(),
    val isScanning: Boolean = false,
    val error: String? = null,
    val statusMessage: String? = null
)

class PeerDiscoveryViewModel(
    private val peerManager: PeerManager,
    private val peerRepository: PeerRepository,
    private val localDeviceName: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(PeerDiscoveryUiState())
    val uiState: StateFlow<PeerDiscoveryUiState> = _uiState.asStateFlow()

    init {
        observePeers()
        observeConnectionEvents()
    }

    fun startScanning() {
        _uiState.value = _uiState.value.copy(isScanning = true, error = null, statusMessage = "Scanning for nearby devices…")
        peerManager.startDiscovery(localDeviceName)
    }

    fun stopScanning() {
        _uiState.value = _uiState.value.copy(isScanning = false, statusMessage = null)
        peerManager.stopDiscovery()
    }

    fun connectToPeer(peer: Peer) {
        _uiState.value = _uiState.value.copy(statusMessage = "Connecting to ${peer.displayName}…")
        peerManager.requestConnection(peer.endpointId)
    }

    fun disconnectFromPeer(peer: Peer) {
        peerManager.disconnect(peer.endpointId)
        _uiState.value = _uiState.value.copy(statusMessage = "Disconnected from ${peer.displayName}")
    }

    /** Attempt to reconnect to a previously connected peer */
    fun reconnectToPeer(peer: Peer) {
        _uiState.value = _uiState.value.copy(statusMessage = "Reconnecting to ${peer.displayName}…")
        peerManager.requestConnection(peer.endpointId)
    }

    private fun observePeers() {
        peerManager.peers
            .onEach { peers ->
                _uiState.value = _uiState.value.copy(
                    discoveredPeers = peers.filter { !it.isConnected && !it.wasConnected },
                    connectedPeers = peers.filter { it.isConnected },
                    disconnectedPeers = peers.filter { !it.isConnected && it.wasConnected }
                )
                // Persist connected peers to DB for chat history
                peers.filter { it.isConnected }.forEach { peer ->
                    viewModelScope.launch {
                        peerRepository.savePeer(peer)
                    }
                }
            }
            .catch { e ->
                _uiState.value = _uiState.value.copy(error = "Discovery error: ${e.message}", isScanning = false)
            }
            .launchIn(viewModelScope)
    }

    private fun observeConnectionEvents() {
        peerManager.connectionEvents
            .onEach { event ->
                when (event) {
                    is ConnectionEvent.Connected -> {
                        _uiState.value = _uiState.value.copy(
                            statusMessage = "✓ Connected to ${event.name}",
                            error = null
                        )
                    }
                    is ConnectionEvent.Disconnected -> {
                        _uiState.value = _uiState.value.copy(
                            error = "Connection lost with ${event.name}",
                            statusMessage = null
                        )
                    }
                    is ConnectionEvent.Rejected -> {
                        _uiState.value = _uiState.value.copy(
                            error = "Connection was rejected",
                            statusMessage = null
                        )
                    }
                    is ConnectionEvent.Failed -> {
                        _uiState.value = _uiState.value.copy(
                            error = event.reason,
                            statusMessage = null
                        )
                    }
                    is ConnectionEvent.Error -> {
                        _uiState.value = _uiState.value.copy(
                            error = event.message,
                            statusMessage = null
                        )
                    }
                    is ConnectionEvent.Connecting -> {
                        _uiState.value = _uiState.value.copy(
                            statusMessage = "Connecting to ${event.name}…"
                        )
                    }
                    is ConnectionEvent.EndpointLost -> {
                        // Endpoint went out of range
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearStatus() {
        _uiState.value = _uiState.value.copy(statusMessage = null)
    }

    override fun onCleared() {
        super.onCleared()
        peerManager.stopDiscovery()
    }
}
