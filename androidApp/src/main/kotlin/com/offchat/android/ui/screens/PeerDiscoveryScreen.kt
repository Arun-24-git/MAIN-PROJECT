package com.offchat.android.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.WifiFind
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.offchat.android.data.model.Peer
import com.offchat.android.ui.theme.*
import com.offchat.android.ui.viewmodel.PeerDiscoveryViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun PeerDiscoveryScreen(
    onPeerChat: (peerId: String, peerName: String) -> Unit,
    onSettingsClick: () -> Unit,
    onBackClick: () -> Unit
) {
    val viewModel: PeerDiscoveryViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(HopeNetBackground)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top bar
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Text("DISCOVER", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp, letterSpacing = 2.sp)
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = HopeNetTextGray)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Radar
            item {
                DiscoveryRadar(isScanning = uiState.isScanning)
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Status message
            item {
                uiState.statusMessage?.let { msg ->
                    Text(msg, color = HopeNetCyan, fontSize = 12.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(bottom = 8.dp))
                }
            }

            // Scan button
            item {
                if (uiState.isScanning) {
                    OutlinedButton(
                        onClick = viewModel::stopScanning,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = HopeNetCyan),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text("STOP SCANNING", letterSpacing = 1.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = viewModel::startScanning,
                        colors = ButtonDefaults.buttonColors(containerColor = HopeNetCyan),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Icon(Icons.Default.Radar, contentDescription = null, tint = Color.Black, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("SCAN FOR PEERS", color = Color.Black, letterSpacing = 1.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // ── CONNECTED PEERS ──
            if (uiState.connectedPeers.isNotEmpty()) {
                item {
                    SectionHeader(
                        label = "CONNECTED",
                        count = uiState.connectedPeers.size,
                        color = HopeNetGreen
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                items(uiState.connectedPeers, key = { "c_${it.endpointId}" }) { peer ->
                    ConnectedPeerCard(
                        peer = peer,
                        onChat = { onPeerChat(peer.name, peer.displayName) }, // FIX: Pass peer.name
                        onDisconnect = { viewModel.disconnectFromPeer(peer) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                item { Spacer(modifier = Modifier.height(12.dp)) }
            }

            // ── CONNECTION LOST ──
            if (uiState.disconnectedPeers.isNotEmpty()) {
                item {
                    SectionHeader(
                        label = "CONNECTION LOST",
                        count = uiState.disconnectedPeers.size,
                        color = HopeNetOrange
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                items(uiState.disconnectedPeers, key = { "d_${it.endpointId}" }) { peer ->
                    DisconnectedPeerCard(
                        peer = peer,
                        onReconnect = { viewModel.reconnectToPeer(peer) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                item { Spacer(modifier = Modifier.height(12.dp)) }
            }

            // ── DISCOVERED (not connected) ──
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("NEARBY NODES", color = HopeNetTextGray, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Text("${uiState.discoveredPeers.size} found", color = HopeNetCyan, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (uiState.discoveredPeers.isEmpty()) {
                item {
                    Text(
                        if (uiState.isScanning) "Scanning for nearby devices…" else "Tap SCAN to discover peers in range",
                        color = HopeNetTextGray,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
            } else {
                items(uiState.discoveredPeers, key = { "n_${it.endpointId}" }) { peer ->
                    DiscoveredPeerCard(
                        peer = peer,
                        onConnect = { viewModel.connectToPeer(peer) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)
        )
    }
}

// ── Section header ──
@Composable
private fun SectionHeader(label: String, count: Int, color: Color) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).background(color, CircleShape))
        Spacer(modifier = Modifier.width(8.dp))
        Text(label, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Spacer(modifier = Modifier.width(8.dp))
        Text("$count", color = color, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
    }
}

// ── Connected Peer Card ──
@Composable
private fun ConnectedPeerCard(peer: Peer, onChat: () -> Unit, onDisconnect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(HopeNetCardColor, RoundedCornerShape(16.dp))
            .border(1.dp, HopeNetGreen.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        Box(
            modifier = Modifier.size(40.dp).background(HopeNetGreen.copy(alpha = 0.1f), CircleShape)
                .border(1.dp, HopeNetGreen.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Box(modifier = Modifier.size(10.dp).background(HopeNetGreen, CircleShape))
        }
        Spacer(modifier = Modifier.width(12.dp))

        // Phone primary, Name secondary
        Column(modifier = Modifier.weight(1f)) {
            if (peer.displayPhone.isNotBlank()) {
                Text(peer.displayPhone, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(peer.displayName, color = HopeNetTextGray, fontSize = 12.sp)
            } else {
                Text(peer.displayName, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
            Text("CONNECTED", color = HopeNetGreen, fontSize = 10.sp, fontFamily = FontFamily.Monospace, letterSpacing = 1.sp)
        }

        Button(
            onClick = onChat,
            colors = ButtonDefaults.buttonColors(containerColor = HopeNetCyan),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.height(36.dp)
        ) {
            Icon(Icons.AutoMirrored.Filled.Message, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("CHAT", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.width(8.dp))
        IconButton(onClick = onDisconnect, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.LinkOff, contentDescription = "Disconnect", tint = HopeNetRed, modifier = Modifier.size(18.dp))
        }
    }
}

// ── Disconnected / Connection Lost Card ──
@Composable
private fun DisconnectedPeerCard(peer: Peer, onReconnect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(HopeNetCardColor, RoundedCornerShape(16.dp))
            .border(1.dp, HopeNetOrange.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(40.dp).background(HopeNetOrange.copy(alpha = 0.1f), CircleShape)
                .border(1.dp, HopeNetOrange.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.LinkOff, contentDescription = null, tint = HopeNetOrange, modifier = Modifier.size(16.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            if (peer.displayPhone.isNotBlank()) {
                Text(peer.displayPhone, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(peer.displayName, color = HopeNetTextGray, fontSize = 12.sp)
            } else {
                Text(peer.displayName, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
            Text("CONNECTION LOST", color = HopeNetOrange, fontSize = 10.sp, fontFamily = FontFamily.Monospace, letterSpacing = 1.sp)
        }

        Button(
            onClick = onReconnect,
            colors = ButtonDefaults.buttonColors(containerColor = HopeNetOrange),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.height(36.dp)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("RECONNECT", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// ── Discovered (never connected) Card ──
@Composable
private fun DiscoveredPeerCard(peer: Peer, onConnect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(HopeNetCardColor, RoundedCornerShape(16.dp))
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(40.dp).background(Color.Black.copy(alpha = 0.3f), CircleShape)
                .border(1.dp, HopeNetCyan.copy(alpha = 0.3f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Box(modifier = Modifier.size(10.dp).background(HopeNetCyan.copy(alpha = 0.5f), CircleShape))
        }
        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            if (peer.displayPhone.isNotBlank()) {
                Text(peer.displayPhone, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(peer.displayName, color = HopeNetTextGray, fontSize = 12.sp)
            } else {
                Text(peer.displayName, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
            Text("AVAILABLE", color = HopeNetTextGray, fontSize = 10.sp, fontFamily = FontFamily.Monospace, letterSpacing = 1.sp)
        }

        Button(
            onClick = onConnect,
            colors = ButtonDefaults.buttonColors(containerColor = HopeNetOrange),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.height(36.dp)
        ) {
            Icon(Icons.Default.Sync, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("CONNECT", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// ── Radar Animation ──
@Composable
private fun DiscoveryRadar(isScanning: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "scanAnim")
    val sweepAlpha by infiniteTransition.animateFloat(
        initialValue = 0.7f, targetValue = 0f,
        animationSpec = infiniteRepeatable(animation = tween(1500, easing = LinearEasing), repeatMode = RepeatMode.Restart),
        label = "sweepAlpha"
    )
    val sweepRadius by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(1500, easing = LinearEasing), repeatMode = RepeatMode.Restart),
        label = "sweepRadius"
    )

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(150.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val maxRadius = size.minDimension / 2f
            drawCircle(color = HopeNetCyan.copy(alpha = 0.15f), radius = maxRadius * 0.33f, center = center, style = Stroke(1f))
            drawCircle(color = HopeNetCyan.copy(alpha = 0.15f), radius = maxRadius * 0.66f, center = center, style = Stroke(1f))
            drawCircle(color = HopeNetCyan.copy(alpha = 0.2f), radius = maxRadius, center = center, style = Stroke(1.5f))
            drawLine(color = HopeNetCyan.copy(alpha = 0.1f), start = Offset(center.x, 0f), end = Offset(center.x, size.height))
            drawLine(color = HopeNetCyan.copy(alpha = 0.1f), start = Offset(0f, center.y), end = Offset(size.width, center.y))
            drawCircle(color = HopeNetCyan, radius = 6f, center = center)
            if (isScanning) {
                drawCircle(color = HopeNetCyan.copy(alpha = sweepAlpha), radius = maxRadius * sweepRadius, center = center, style = Stroke(2f))
            }
        }
        Icon(
            imageVector = if (isScanning) Icons.Default.WifiFind else Icons.Default.BluetoothSearching,
            contentDescription = null, tint = HopeNetCyan, modifier = Modifier.size(28.dp)
        )
    }
}
