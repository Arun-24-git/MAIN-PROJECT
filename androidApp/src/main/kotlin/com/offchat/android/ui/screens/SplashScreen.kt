package com.offchat.android.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SatelliteAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.offchat.android.ui.theme.HopeNetBackground
import com.offchat.android.ui.theme.HopeNetCyan
import com.offchat.android.ui.theme.HopeNetTextGray
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onNavigateToRegistration: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(3000)
        onNavigateToRegistration()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(HopeNetBackground)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(60.dp))

        Icon(
            imageVector = Icons.Default.SatelliteAlt,
            contentDescription = "Satellite",
            tint = HopeNetCyan,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "HOPE NET",
            color = Color.White,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 4.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "OFFLINE EMERGENCY MESSAGING",
            color = HopeNetTextGray,
            fontSize = 12.sp,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.weight(1f))
        RadarView()
        Spacer(modifier = Modifier.weight(1f))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.SatelliteAlt,
                contentDescription = "Secure",
                tint = HopeNetTextGray,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "MESH NETWORK: SECURE",
                color = HopeNetTextGray,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "v1.0.4",
            color = HopeNetTextGray,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp
        )
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun RadarView() {
    val infiniteTransition = rememberInfiniteTransition(label = "radar")
    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radarAlpha"
    )

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(250.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val maxRadius = size.width / 2

            drawLine(color = HopeNetCyan.copy(alpha = 0.2f), start = Offset(center.x, 0f), end = Offset(center.x, size.height))
            drawLine(color = HopeNetCyan.copy(alpha = 0.2f), start = Offset(0f, center.y), end = Offset(size.width, center.y))
            drawCircle(color = HopeNetCyan.copy(alpha = 0.2f), radius = maxRadius * 0.33f, center = center, style = Stroke(1f))
            drawCircle(color = HopeNetCyan.copy(alpha = 0.2f), radius = maxRadius * 0.66f, center = center, style = Stroke(1f))
            drawCircle(color = HopeNetCyan.copy(alpha = 0.2f), radius = maxRadius, center = center, style = Stroke(1f))
            drawCircle(color = HopeNetCyan, radius = 8f, center = center)
            drawCircle(color = HopeNetCyan.copy(alpha = alphaAnim), radius = 16f, center = center, style = Stroke(2f))
            drawCircle(color = HopeNetCyan, radius = 4f, center = Offset(center.x + 60f, center.y - 40f))
            drawCircle(color = HopeNetCyan, radius = 3f, center = Offset(center.x - 50f, center.y + 70f))
        }
    }
}
