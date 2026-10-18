package com.example.feature.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.preferences.AppLanguage
import com.example.data.preferences.AppThemeMode
import com.example.data.preferences.UserPreferences
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.ScanHistoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsViewModel(
    private val preferencesRepository: UserPreferencesRepository,
    private val historyRepository: ScanHistoryRepository
) : ViewModel() {

    val preferences: StateFlow<UserPreferences> = preferencesRepository.preferencesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserPreferences()
        )

    val historyCount: StateFlow<Int> = historyRepository.allScans
        .map { it.size }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    private val _showClearDialog = MutableStateFlow(false)
    val showClearDialog: StateFlow<Boolean> = _showClearDialog.asStateFlow()

    private val _showPrivacyDialog = MutableStateFlow(false)
    val showPrivacyDialog: StateFlow<Boolean> = _showPrivacyDialog.asStateFlow()

    private val _showAboutDialog = MutableStateFlow(false)
    val showAboutDialog: StateFlow<Boolean> = _showAboutDialog.asStateFlow()

    fun setSoundOnScan(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setSoundOnScan(enabled) }
    }

    fun setVibrationOnScan(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setVibrationOnScan(enabled) }
    }

    fun setAutoSaveHistory(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setAutoSaveHistory(enabled) }
    }

    fun setBatchScanMode(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setBatchScanMode(enabled) }
    }

    fun setThemeMode(mode: AppThemeMode) {
        viewModelScope.launch { preferencesRepository.setThemeMode(mode) }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setDynamicColor(enabled) }
    }

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch { preferencesRepository.setLanguage(language) }
    }

    fun setShowClearDialog(show: Boolean) {
        _showClearDialog.value = show
    }

    fun setShowPrivacyDialog(show: Boolean) {
        _showPrivacyDialog.value = show
    }

    fun setShowAboutDialog(show: Boolean) {
        _showAboutDialog.value = show
    }

    fun clearAllHistory(onCleared: () -> Unit) {
        viewModelScope.launch {
            historyRepository.clearAllHistory()
            _showClearDialog.value = false
            onCleared()
        }
    }

    fun exportCsv(context: Context, uri: Uri, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) {
                try {
                    val csv = historyRepository.buildCsvExport()
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(csv.toByteArray(Charsets.UTF_8))
                    } ?: return@withContext false
                    true
                } catch (_: Exception) {
                    false
                }
            }
            onComplete(ok)
        }
    }
}
