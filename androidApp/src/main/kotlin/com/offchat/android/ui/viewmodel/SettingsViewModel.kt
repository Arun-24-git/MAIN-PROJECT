package com.offchat.android.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val deviceName: String = "",
    val isSaved: Boolean = false
)

class SettingsViewModel(
    private val getDeviceName: () -> String,
    private val saveDeviceName: suspend (String) -> Unit
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState(deviceName = getDeviceName()))
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun updateDeviceName(name: String) {
        _uiState.value = _uiState.value.copy(deviceName = name, isSaved = false)
    }

    fun saveSettings() {
        viewModelScope.launch {
            saveDeviceName(_uiState.value.deviceName)
            _uiState.value = _uiState.value.copy(isSaved = true)
        }
    }
}
