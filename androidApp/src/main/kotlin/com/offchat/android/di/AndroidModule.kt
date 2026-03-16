package com.offchat.android.di

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.core.content.edit
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.offchat.android.crypto.Encryption
import com.offchat.android.data.repository.MessageRepository
import com.offchat.android.data.repository.MessageRepositoryImpl
import com.offchat.android.data.repository.PeerRepository
import com.offchat.android.data.repository.PeerRepositoryImpl
import com.offchat.android.peer.AndroidPeerManager
import com.offchat.android.peer.PeerManager
import com.offchat.android.ui.viewmodel.ChatViewModel
import com.offchat.android.ui.viewmodel.ChatsListViewModel
import com.offchat.android.ui.viewmodel.PeerDiscoveryViewModel
import com.offchat.android.ui.viewmodel.SettingsViewModel
import com.offchat.db.OffChatDatabase
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module
import java.util.UUID

val androidModule = module {

    // ------------------------------------------------------------------
    // Preferences
    // ------------------------------------------------------------------
    single<SharedPreferences> {
        androidContext().getSharedPreferences("offchat_prefs", Context.MODE_PRIVATE)
    }

    // ------------------------------------------------------------------
    // Device identity
    // ------------------------------------------------------------------
    single(named("deviceId")) {
        val prefs = get<SharedPreferences>()
        prefs.getString("device_id", null) ?: UUID.randomUUID().toString().also { id ->
            prefs.edit { putString("device_id", id) }
        }
    }

    // Factory so it re-reads prefs after registration completes
    factory(named("deviceName")) {
        val prefs = get<SharedPreferences>()
        val displayName = prefs.getString("user_display_name", null)
        val phoneNumber = prefs.getString("user_phone_number", null)
        if (displayName != null && phoneNumber != null) {
            "$displayName|$phoneNumber"
        } else {
            prefs.getString("device_name", null) ?: Build.MODEL
        }
    }

    // ------------------------------------------------------------------
    // SQLDelight database
    // ------------------------------------------------------------------
    single<SqlDriver> {
        AndroidSqliteDriver(
            schema = OffChatDatabase.Schema,
            context = androidContext(),
            name = "offchat.db"
        )
    }

    single { OffChatDatabase(get()) }

    // ------------------------------------------------------------------
    // Repositories
    // ------------------------------------------------------------------
    single<MessageRepository> { MessageRepositoryImpl(get()) }
    single<PeerRepository> { PeerRepositoryImpl(get()) }

    // ------------------------------------------------------------------
    // Encryption
    // ------------------------------------------------------------------
    single { Encryption() }

    // ------------------------------------------------------------------
    // PeerManager (Nearby Connections)
    // ------------------------------------------------------------------
    single<PeerManager> { AndroidPeerManager(androidContext()) }

    // ------------------------------------------------------------------
    // ViewModels
    // ------------------------------------------------------------------
    factory {
        ChatViewModel(
            messageRepository = get(),
            peerManager = get(),
            encryption = get(),
            appContext = androidContext(),
            localDeviceId = get<String>(qualifier = named("deviceId")),
            localDeviceName = get<String>(qualifier = named("deviceName"))
        )
    }

    factory {
        PeerDiscoveryViewModel(
            peerManager = get(),
            peerRepository = get(),
            localDeviceName = get<String>(qualifier = named("deviceName"))
        )
    }

    factory {
        ChatsListViewModel(
            messageRepository = get(),
            peerRepository = get(),
            peerManager = get()
        )
    }

    viewModel {
        val prefs = get<SharedPreferences>()
        SettingsViewModel(
            getDeviceName = {
                prefs.getString("user_display_name", null) ?: prefs.getString("device_name", Build.MODEL) ?: Build.MODEL
            },
            saveDeviceName = { name ->
                prefs.edit {
                    putString("user_display_name", name)
                    // Update the formatted device_name for advertising
                    val phone = prefs.getString("user_phone_number", "") ?: ""
                    putString("device_name", if (phone.isNotBlank()) "$name|$phone" else name)
                }
            },
            getTTLDuration = {
                val duration = prefs.getLong("message_ttl_duration", 86400000L)
                android.util.Log.d("AndroidModule", "Retrieving TTL duration: $duration")
                duration
            },
            saveTTLDuration = { duration ->
                android.util.Log.d("AndroidModule", "Saving TTL duration: $duration")
                prefs.edit(commit = true) {
                    putLong("message_ttl_duration", duration)
                }
            }
        )
    }
}
