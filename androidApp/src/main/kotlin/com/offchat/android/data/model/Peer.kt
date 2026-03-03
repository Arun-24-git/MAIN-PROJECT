package com.offchat.android.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Peer(
    val endpointId: String,
    val name: String,
    val phoneNumber: String = "",
    val publicKey: String = "",
    val lastSeen: Long,
    val isConnected: Boolean,
    val wasConnected: Boolean = false
) {
    /** The display name (first part before "|") */
    val displayName: String
        get() {
            val parts = name.split("|", limit = 2)
            return parts[0].ifBlank { name }
        }

    /** The phone number (second part after "|"), or the explicit phoneNumber field */
    val displayPhone: String
        get() {
            if (phoneNumber.isNotBlank()) return phoneNumber
            val parts = name.split("|", limit = 2)
            return if (parts.size >= 2) parts[1] else ""
        }
}
