package com.example.feature.result

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.model.ScanRecord
import com.example.data.repository.ScanHistoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class ResultUiState(
    val isLoading: Boolean = true,
    val record: ScanRecord? = null,
    val isPasswordRevealed: Boolean = false,
    val isDeleted: Boolean = false
)

class ResultViewModel(
    private val repository: ScanHistoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ResultUiState())
    val uiState: StateFlow<ResultUiState> = _uiState.asStateFlow()

    fun loadScanRecord(scanId: Long) {
        viewModelScope.launch {
            _uiState.value = ResultUiState(isLoading = true)
            repository.observeScanById(scanId).collectLatest { record ->
                _uiState.value = ResultUiState(
                    isLoading = false,
                    record = record,
                    isPasswordRevealed = _uiState.value.isPasswordRevealed
                )
            }
        }
    }

    fun togglePasswordVisibility() {
        _uiState.value = _uiState.value.copy(
            isPasswordRevealed = !_uiState.value.isPasswordRevealed
        )
    }

    fun toggleFavorite() {
        val current = _uiState.value.record ?: return
        viewModelScope.launch {
            repository.setFavorite(current.id, !current.isFavorite)
        }
    }

    fun saveTransientToHistory(onSaved: (Long) -> Unit) {
        val current = _uiState.value.record ?: return
        if (current.id >= 0L) return
        viewModelScope.launch {
            val newId = repository.saveTransientRecordToHistory(current)
            loadScanRecord(newId)
            onSaved(newId)
        }
    }

    fun deleteCurrentRecord(onDeleted: () -> Unit) {
        val current = _uiState.value.record ?: return
        viewModelScope.launch {
            repository.deleteById(current.id)
            _uiState.value = _uiState.value.copy(isDeleted = true)
            onDeleted()
        }
    }
}
