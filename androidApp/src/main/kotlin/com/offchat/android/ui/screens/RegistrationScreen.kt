package com.offchat.android.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import com.offchat.android.ui.theme.*

@Composable
fun RegistrationScreen(onComplete: () -> Unit) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("offchat_prefs", Context.MODE_PRIVATE)
    var displayName by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(HopeNetBackground)
            .padding(24.dp)
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text = "User Registration",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Create your secure identity for the\nHOPE NET offline mesh.",
            color = HopeNetTextGray,
            fontSize = 14.sp,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(36.dp))

        // Display Name
        Text("Display Name", color = HopeNetTextGray, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = displayName,
            onValueChange = { displayName = it; showError = false },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("e.g. John", color = HopeNetTextGray.copy(alpha = 0.5f)) },
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = HopeNetCyan) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = HopeNetCyan,
                unfocusedBorderColor = HopeNetCyan.copy(alpha = 0.5f),
                cursorColor = HopeNetCyan,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            shape = RoundedCornerShape(12.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text("Visible to other nodes as your name.", color = HopeNetTextGray, fontSize = 11.sp)

        Spacer(modifier = Modifier.height(20.dp))

        // Phone Number
        Text("Phone Number", color = HopeNetTextGray, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = phoneNumber,
            onValueChange = { phoneNumber = it; showError = false },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("e.g. +91 98765 43210", color = HopeNetTextGray.copy(alpha = 0.5f)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = HopeNetCyan) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = HopeNetCyan,
                unfocusedBorderColor = HopeNetCyan.copy(alpha = 0.5f),
                cursorColor = HopeNetCyan,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            shape = RoundedCornerShape(12.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text("Shown alongside your name in discovery.", color = HopeNetTextGray, fontSize = 11.sp)

        if (showError) {
            Spacer(modifier = Modifier.height(12.dp))
            Text("Please fill in both fields.", color = HopeNetRed, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Security credentials
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Lock, contentDescription = null, tint = HopeNetTextGray, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("SECURITY CREDENTIALS", color = HopeNetTextGray, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        }

        Spacer(modifier = Modifier.height(12.dp))

        CredentialCard(title = "DEVICE ID", icon = Icons.Default.Fingerprint)
        Spacer(modifier = Modifier.height(10.dp))
        CredentialCard(title = "ENCRYPTION KEY", icon = Icons.Default.Key)

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                val name = displayName.trim()
                val phone = phoneNumber.trim()
                if (name.isNotBlank() && phone.isNotBlank()) {
                    // Persist registration
                    prefs.edit {
                        putString("user_display_name", name)
                        putString("user_phone_number", phone)
                        putString("device_name", "$name|$phone")
                        putBoolean("is_registered", true)
                    }
                    onComplete()
                } else {
                    showError = true
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = HopeNetCyan),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Register & Continue", color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.Black)
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "SECURE HANDSHAKE PROTOCOL V2.0",
            color = HopeNetTextGray,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun CredentialCard(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(HopeNetCardColor, RoundedCornerShape(12.dp))
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Column {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(title, color = HopeNetTextGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Icon(icon, contentDescription = null, tint = HopeNetTextGray, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(HopeNetBackground, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = HopeNetCyan, modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("[ AUTO-GENERATED ]", color = HopeNetCyan, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                }
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = HopeNetGreen, modifier = Modifier.size(20.dp))
            }
        }
    }
}
