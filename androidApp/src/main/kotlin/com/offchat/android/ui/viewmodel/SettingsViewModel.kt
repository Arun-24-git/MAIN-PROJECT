package com.offchat.android.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val deviceName: String = "",
    val ttlDuration: Long = 86400000L, // Default 24 hours
    val isSaved: Boolean = false
)

class SettingsViewModel(
    private val getDeviceName: () -> String,
    private val saveDeviceName: suspend (String) -> Unit,
    private val getTTLDuration: () -> Long,
    private val saveTTLDuration: suspend (Long) -> Unit
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SettingsUiState(
            deviceName = getDeviceName(),
            ttlDuration = getTTLDuration()
        ).also { 
            Log.d("SettingsViewModel", "Initialized with deviceName=${it.deviceName}, ttlDuration=${it.ttlDuration}")
        }
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun updateDeviceName(name: String) {
        _uiState.value = _uiState.value.copy(deviceName = name, isSaved = false)
    }

    fun updateTTLDuration(duration: Long) {
        Log.d("SettingsViewModel", "Updating TTL duration to: $duration and saving immediately")
        _uiState.value = _uiState.value.copy(ttlDuration = duration)
        viewModelScope.launch {
            saveTTLDuration(duration)
        }
    }

    fun saveSettings() {
        Log.d("SettingsViewModel", "Saving settings: deviceName=${_uiState.value.deviceName}, ttlDuration=${_uiState.value.ttlDuration}")
        viewModelScope.launch {
            saveDeviceName(_uiState.value.deviceName)
            saveTTLDuration(_uiState.value.ttlDuration)
            _uiState.value = _uiState.value.copy(isSaved = true)
        }
    }
}
