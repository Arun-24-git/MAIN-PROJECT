package com.offchat.android.ui.screens

import android.app.Activity
import android.content.Intent
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SignalWifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.offchat.android.data.model.Message
import com.offchat.android.data.model.MessageStatus
import com.offchat.android.notification.ActiveChatTracker
import com.offchat.android.ui.theme.*
import com.offchat.android.ui.viewmodel.ChatViewModel
import org.koin.androidx.compose.koinViewModel
import java.io.File
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Parse "Name|Phone" → display name */
private fun parseDisplayName(raw: String): String {
    val parts = raw.split("|", limit = 2)
    return parts[0].ifBlank { raw }
}

/** Parse "Name|Phone" → phone number */
private fun parsePhone(raw: String): String {
    val parts = raw.split("|", limit = 2)
    return if (parts.size >= 2) parts[1] else ""
}

@Composable
fun ChatScreen(
    peerId: String,
    peerName: String,
    onBackClick: () -> Unit
) {
    val viewModel: ChatViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    var messageText by rememberSaveable { mutableStateOf("") }
    val context = LocalContext.current
    var isRecording by remember { mutableStateOf(false) }

    // Parse peer info — phone is primary
    val displayName = parseDisplayName(peerName)
    val displayPhone = parsePhone(peerName)

    // Track active chat for notification suppression
    DisposableEffect(peerId) {
        ActiveChatTracker.activePeerId = peerId
        onDispose {
            ActiveChatTracker.activePeerId = null
        }
    }

    LaunchedEffect(peerId) {
        viewModel.observeMessages(peerId)
        viewModel.observeConnectionStatus(peerId)
    }

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.lastIndex)
        }
    }

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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
        ) {
            // ── Top Bar ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(HopeNetCardColor)
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(HopeNetCyan.copy(alpha = 0.15f), CircleShape)
                        .border(1.dp, HopeNetCyan.copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = displayName.firstOrNull()?.uppercase() ?: "?",
                        color = HopeNetCyan,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    // Phone number is primary
                    if (displayPhone.isNotBlank()) {
                        Text(displayPhone, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(displayName, color = HopeNetTextGray, fontSize = 12.sp)
                    } else {
                        Text(displayName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = HopeNetGreen, modifier = Modifier.size(10.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("E2E ENCRYPTED", color = HopeNetGreen, fontSize = 10.sp, fontFamily = FontFamily.Monospace, letterSpacing = 1.sp)
                    }
                }
            }

            // ── Connection Lost Banner ──
            if (!uiState.isConnected) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(HopeNetOrange.copy(alpha = 0.15f))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.SignalWifiOff, contentDescription = null, tint = HopeNetOrange, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Connection lost", color = HopeNetOrange, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                    Button(
                        onClick = { viewModel.reconnect(peerId) },
                        colors = ButtonDefaults.buttonColors(containerColor = HopeNetOrange),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("RECONNECT", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = HopeNetCyan)
                }
            } else {
                // ── Messages ──
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(uiState.messages, key = { it.id }) { message ->
                        if (message.content.startsWith("voice:")) {
                            VoiceMessageBubble(message = message)
                        } else {
                            MessageBubble(message = message)
                        }
                    }
                }

                // ── Recording indicator ──
                if (isRecording) {
                    RecordingIndicator()
                }

                // ── Input Bar ──
                MessageInputBar(
                    text = messageText,
                    onTextChange = { messageText = it },
                    onSend = {
                        if (messageText.isNotBlank()) {
                            viewModel.sendMessage(peerId, messageText.trim())
                            messageText = ""
                        }
                    },
                    isRecording = isRecording,
                    onVoiceHoldStart = {
                        val file = File(context.cacheDir, "voice_${System.currentTimeMillis()}.amr")
                        try {
                            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                MediaRecorder(context)
                            } else {
                                @Suppress("DEPRECATION")
                                MediaRecorder()
                            }
                            recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
                            recorder.setOutputFormat(MediaRecorder.OutputFormat.AMR_NB)
                            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                            recorder.setOutputFile(file.absolutePath)
                            recorder.prepare()
                            recorder.start()
                            isRecording = true
                            Pair(recorder, file)
                        } catch (e: Exception) {
                            null
                        }
                    },
                    onVoiceHoldEnd = { recorderAndFile ->
                        isRecording = false
                        if (recorderAndFile != null) {
                            val (recorder, file) = recorderAndFile
                            try {
                                recorder.stop()
                                recorder.release()
                                if (file.exists() && file.length() > 0) {
                                    viewModel.sendVoiceMessage(peerId, file.absolutePath)
                                }
                            } catch (e: Exception) {
                                try { recorder.release() } catch (_: Exception) {}
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(HopeNetCardColor)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)
        )
    }
}

// ── Recording Indicator ──
@Composable
private fun RecordingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "record")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.8f, targetValue = 1.2f,
        animationSpec = infiniteRepeatable(tween(600, easing = LinearEasing), RepeatMode.Reverse),
        label = "pulse"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(HopeNetRed.copy(alpha = 0.1f))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .scale(pulse)
                .background(HopeNetRed, CircleShape)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text("Recording… release to send", color = HopeNetRed, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
    }
}

// ── Text Bubble ──
@Composable
private fun MessageBubble(message: Message) {
    val outgoingColor = HopeNetCyan.copy(alpha = 0.15f)
    val incomingColor = HopeNetCardColor
    val bubbleColor by animateColorAsState(
        if (message.isOutgoing) outgoingColor else incomingColor,
        label = "bubbleColor"
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isOutgoing) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(bubbleShape(message.isOutgoing))
                .background(bubbleColor)
                .border(1.dp,
                    if (message.isOutgoing) HopeNetCyan.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f),
                    bubbleShape(message.isOutgoing)
                )
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            if (!message.isOutgoing) {
                // Show parsed display name
                Text(
                    text = parseDisplayName(message.senderName),
                    color = HopeNetCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(2.dp))
            }
            Text(text = message.content, color = Color.White, fontSize = 14.sp)
            MessageMeta(message)
        }
    }
}

// ── Voice Bubble ──
@Composable
private fun VoiceMessageBubble(message: Message) {
    val filePath = message.content.removePrefix("voice:")
    var isPlaying by remember { mutableStateOf(false) }
    val mediaPlayerRef = remember { mutableStateOf<MediaPlayer?>(null) }
    val outgoingColor = HopeNetCyan.copy(alpha = 0.15f)
    val incomingColor = HopeNetCardColor
    val bubbleColor = if (message.isOutgoing) outgoingColor else incomingColor

    DisposableEffect(Unit) {
        onDispose {
            mediaPlayerRef.value?.release()
            mediaPlayerRef.value = null
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isOutgoing) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 260.dp)
                .clip(bubbleShape(message.isOutgoing))
                .background(bubbleColor)
                .border(1.dp,
                    if (message.isOutgoing) HopeNetCyan.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f),
                    bubbleShape(message.isOutgoing)
                )
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            if (!message.isOutgoing) {
                Text(
                    text = parseDisplayName(message.senderName),
                    color = HopeNetCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Play/Stop button
                IconButton(
                    onClick = {
                        if (isPlaying) {
                            mediaPlayerRef.value?.stop()
                            mediaPlayerRef.value?.release()
                            mediaPlayerRef.value = null
                            isPlaying = false
                        } else {
                            try {
                                val mp = MediaPlayer()
                                mp.setDataSource(filePath)
                                mp.prepare()
                                mp.start()
                                isPlaying = true
                                mediaPlayerRef.value = mp
                                mp.setOnCompletionListener {
                                    isPlaying = false
                                    mp.release()
                                    mediaPlayerRef.value = null
                                }
                            } catch (e: Exception) {
                                isPlaying = false
                            }
                        }
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            if (isPlaying) HopeNetOrange.copy(alpha = 0.2f) else HopeNetCyan.copy(alpha = 0.2f),
                            CircleShape
                        )
                ) {
                    Icon(
                        if (isPlaying) Icons.Default.PauseCircle else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Stop" else "Play",
                        tint = if (isPlaying) HopeNetOrange else HopeNetCyan,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        "🎤 Voice message",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        if (isPlaying) "Playing…" else "Tap to play",
                        color = HopeNetTextGray,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            MessageMeta(message)
        }
    }
}

// ── Shared meta row (time + status) ──
@Composable
private fun MessageMeta(message: Message) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = formatTimestamp(message.timestamp),
            color = HopeNetTextGray,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace
        )
        if (message.isOutgoing) {
            Spacer(Modifier.width(4.dp))
            StatusIcon(status = message.status, tint = HopeNetTextGray)
        }
    }
}

@Composable
private fun StatusIcon(status: MessageStatus, tint: Color) {
    val icon = when (status) {
        MessageStatus.SENDING -> Icons.Default.Schedule
        MessageStatus.SENT -> Icons.Default.Check
        MessageStatus.DELIVERED -> Icons.Default.DoneAll
        MessageStatus.READ -> Icons.Default.DoneAll
        MessageStatus.FAILED -> Icons.Default.Close
    }
    Icon(
        icon,
        contentDescription = status.name,
        tint = if (status == MessageStatus.READ) HopeNetCyan else tint,
        modifier = Modifier.size(14.dp)
    )
}

// ── Input Bar with voice hold-to-record ──
@Composable
private fun MessageInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    isRecording: Boolean,
    onVoiceHoldStart: () -> Pair<MediaRecorder, File>?,
    onVoiceHoldEnd: (Pair<MediaRecorder, File>?) -> Unit,
    modifier: Modifier = Modifier
) {
    var activeRecording by remember { mutableStateOf<Pair<MediaRecorder, File>?>(null) }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Voice hold-to-record button
        Box(
            modifier = Modifier
                .size(42.dp)
                .background(
                    if (isRecording) HopeNetRed.copy(alpha = 0.3f) else HopeNetOrange.copy(alpha = 0.15f),
                    CircleShape
                )
                .border(
                    1.dp,
                    if (isRecording) HopeNetRed.copy(alpha = 0.6f) else HopeNetOrange.copy(alpha = 0.3f),
                    CircleShape
                )
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            val result = onVoiceHoldStart()
                            activeRecording = result
                            val released = tryAwaitRelease()
                            onVoiceHoldEnd(if (released) activeRecording else null)
                            activeRecording = null
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Mic,
                contentDescription = "Hold to record",
                tint = if (isRecording) HopeNetRed else HopeNetOrange,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(Modifier.width(8.dp))

        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            placeholder = { Text("Type a message…", color = HopeNetTextGray) },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(24.dp),
            maxLines = 4,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { onSend() }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = HopeNetCyan,
                unfocusedBorderColor = HopeNetCyan.copy(alpha = 0.3f),
                cursorColor = HopeNetCyan,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )

        Spacer(Modifier.width(8.dp))

        // Send button
        IconButton(
            onClick = onSend,
            modifier = Modifier
                .size(48.dp)
                .background(HopeNetCyan, CircleShape)
        ) {
            Icon(
                Icons.AutoMirrored.Filled.Send,
                contentDescription = "Send",
                tint = Color.Black
            )
        }
    }
}

private fun bubbleShape(isOutgoing: Boolean) = RoundedCornerShape(
    topStart = 16.dp,
    topEnd = 16.dp,
    bottomStart = if (isOutgoing) 16.dp else 4.dp,
    bottomEnd = if (isOutgoing) 4.dp else 16.dp
)

private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())

private fun formatTimestamp(millis: Long): String {
    val instant = Instant.ofEpochMilli(millis)
    val localDateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
    return timeFormatter.format(localDateTime)
}
