package com.offchat.android

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.offchat.android.service.NearbyForegroundService
import com.offchat.android.ui.OffChatApp

class MainActivity : ComponentActivity() {

    private val requiredPermissions: Array<String> by lazy {
        buildList {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.RECORD_AUDIO)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(Manifest.permission.BLUETOOTH_SCAN)
                add(Manifest.permission.BLUETOOTH_ADVERTISE)
                add(Manifest.permission.BLUETOOTH_CONNECT)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.NEARBY_WIFI_DEVICES)
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }.toTypedArray()
    }

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
            // Start the service even if some optional permissions (audio, notifications)
            // are denied – Nearby Connections only needs location + BT.
            val coreGranted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            if (coreGranted) startNearbyService()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OffChatApp()
        }
        requestPermissionsIfNeeded()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            startService(NearbyForegroundService.stopAction(this))
        }
    }

    private fun requestPermissionsIfNeeded() {
        val missing = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isEmpty()) {
            startNearbyService()
        } else {
            permissionLauncher.launch(missing.toTypedArray())
        }
    }

    private fun startNearbyService() {
        val prefs = getSharedPreferences("offchat_prefs", MODE_PRIVATE)
        val displayName = prefs.getString("user_display_name", null)
        val phoneNumber = prefs.getString("user_phone_number", null)
        val deviceName = if (displayName != null && phoneNumber != null) {
            "$displayName|$phoneNumber"
        } else {
            prefs.getString("device_name", Build.MODEL) ?: Build.MODEL
        }
        val currentTTL = prefs.getLong("message_ttl_duration", -1L)
        android.util.Log.d("MainActivity", "Starting nearby service. Current saved TTL: $currentTTL")
        
        val intent = NearbyForegroundService.startAction(this, deviceName)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }
}
