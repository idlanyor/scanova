package com.example.feature.scanner

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.model.BarcodeFormatType
import com.example.core.model.DetectedBarcodeCandidate
import com.example.core.model.ScanRecord
import com.example.core.model.ScanSource
import com.example.core.scanner.MlKitBarcodeAnalyzer
import com.example.data.preferences.UserPreferences
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.ScanHistoryRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface ScannerEvent {
    data class NavigateToResult(val scanId: Long) : ScannerEvent
    data class TriggerScanFeedback(val sound: Boolean, val vibration: Boolean) : ScannerEvent
    data object ShowGalleryNoCodeFound : ScannerEvent
    data object ShowGalleryDecodeError : ScannerEvent
    data object ShowSavedToast : ScannerEvent
}

data class ScannerUiState(
    val isScanningPaused: Boolean = false,
    val torchEnabled: Boolean = false,
    val hasFlashUnit: Boolean = false,
    val currentZoomRatio: Float = 1f,
    val minZoomRatio: Float = 1f,
    val maxZoomRatio: Float = 4f,
    val isGalleryLoading: Boolean = false,
    val cameraInitError: Boolean = false,
    val detectedCandidates: List<DetectedBarcodeCandidate> = emptyList(),
    val selectedCandidate: DetectedBarcodeCandidate? = null,
    val multiCodeSource: ScanSource = ScanSource.CAMERA,
    val batchSessionScans: List<ScanRecord> = emptyList(),
    val showQrGeneratorSheet: Boolean = false
)

class ScannerViewModel(
    private val historyRepository: ScanHistoryRepository,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScannerUiState())
    val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()

    val preferences: StateFlow<UserPreferences> = preferencesRepository.preferencesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserPreferences()
        )

    private val _events = Channel<ScannerEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    // Prevents rapid consecutive frame detections of the same code in Batch mode
    private var lastBatchSignature: String? = null
    private var lastBatchTimeMillis: Long = 0L

    fun onCameraFramesDetected(candidates: List<DetectedBarcodeCandidate>) {
        if (_uiState.value.isScanningPaused || candidates.isEmpty()) return

        viewModelScope.launch {
            val prefs = preferences.first()

            if (prefs.batchScanMode) {
                val primary = candidates.first()
                val now = System.currentTimeMillis()
                val sig = "${primary.format.name}:${primary.rawValue.trim()}"

                if (sig == lastBatchSignature && (now - lastBatchTimeMillis) < 2000L) {
                    return@launch
                }
                lastBatchSignature = sig
                lastBatchTimeMillis = now

                val savedRecord = historyRepository.recordScan(
                    rawValue = primary.rawValue,
                    format = primary.format,
                    source = ScanSource.CAMERA,
                    autoSave = true,
                    nowMillis = now
                )
                _events.send(
                    ScannerEvent.TriggerScanFeedback(
                        sound = prefs.soundOnScan,
                        vibration = prefs.vibrationOnScan
                    )
                )
                _uiState.update { state ->
                    val updatedList = (listOf(savedRecord) + state.batchSessionScans)
                        .distinctBy { "${it.format.name}:${it.rawValue}" }
                    state.copy(batchSessionScans = updatedList)
                }
            } else if (candidates.size == 1) {
                // Single code detected: pause immediately, record, and navigate to Result Detail
                _uiState.update { it.copy(isScanningPaused = true) }
                val primary = candidates.first()
                _events.send(
                    ScannerEvent.TriggerScanFeedback(
                        sound = prefs.soundOnScan,
                        vibration = prefs.vibrationOnScan
                    )
                )
                val record = historyRepository.recordScan(
                    rawValue = primary.rawValue,
                    format = primary.format,
                    source = ScanSource.CAMERA,
                    autoSave = prefs.autoSaveHistory
                )
                _events.send(ScannerEvent.NavigateToResult(record.id))
            } else {
                // Multiple codes in frame: pause & highlight primary central code with tap-to-switch bounding boxes
                _uiState.update {
                    it.copy(
                        isScanningPaused = true,
                        detectedCandidates = candidates,
                        selectedCandidate = candidates.first(),
                        multiCodeSource = ScanSource.CAMERA
                    )
                }
                _events.send(
                    ScannerEvent.TriggerScanFeedback(
                        sound = prefs.soundOnScan,
                        vibration = prefs.vibrationOnScan
                    )
                )
            }
        }
    }

    fun selectCandidate(candidate: DetectedBarcodeCandidate) {
        _uiState.update { it.copy(selectedCandidate = candidate) }
    }

    fun openSelectedCandidateDetails() {
        val candidate = _uiState.value.selectedCandidate ?: return
        val source = _uiState.value.multiCodeSource
        viewModelScope.launch {
            val prefs = preferences.first()
            val record = historyRepository.recordScan(
                rawValue = candidate.rawValue,
                format = candidate.format,
                source = source,
                autoSave = prefs.autoSaveHistory
            )
            _uiState.update {
                it.copy(
                    detectedCandidates = emptyList(),
                    selectedCandidate = null,
                    isScanningPaused = true
                )
            }
            _events.send(ScannerEvent.NavigateToResult(record.id))
        }
    }

    fun resumeScanning() {
        historyRepository.resetSessionDuplicateGuard()
        _uiState.update {
            it.copy(
                isScanningPaused = false,
                detectedCandidates = emptyList(),
                selectedCandidate = null,
                cameraInitError = false
            )
        }
    }

    fun pauseScanning() {
        _uiState.update { it.copy(isScanningPaused = true) }
    }

    fun toggleBatchMode() {
        viewModelScope.launch {
            val current = preferences.value.batchScanMode
            preferencesRepository.setBatchScanMode(!current)
            if (current) {
                // Turning off batch mode
                _uiState.update { it.copy(batchSessionScans = emptyList(), isScanningPaused = false) }
            } else {
                _uiState.update {
                    it.copy(
                        isScanningPaused = false,
                        detectedCandidates = emptyList(),
                        selectedCandidate = null
                    )
                }
            }
        }
    }

    fun clearBatchSession() {
        lastBatchSignature = null
        lastBatchTimeMillis = 0L
        _uiState.update { it.copy(batchSessionScans = emptyList()) }
    }

    fun onGalleryImagePicked(context: Context, uri: Uri?) {
        if (uri == null) return
        viewModelScope.launch {
            _uiState.update { it.copy(isGalleryLoading = true, isScanningPaused = true) }
            val scanResult = MlKitBarcodeAnalyzer.scanFromUri(context.applicationContext, uri)
            val prefs = preferences.first()

            scanResult.fold(
                onSuccess = { candidates ->
                    if (candidates.isEmpty()) {
                        _uiState.update { it.copy(isGalleryLoading = false, isScanningPaused = false) }
                        _events.send(ScannerEvent.ShowGalleryNoCodeFound)
                    } else if (candidates.size == 1) {
                        val single = candidates.first()
                        _events.send(
                            ScannerEvent.TriggerScanFeedback(
                                sound = prefs.soundOnScan,
                                vibration = prefs.vibrationOnScan
                            )
                        )
                        val record = historyRepository.recordScan(
                            rawValue = single.rawValue,
                            format = single.format,
                            source = ScanSource.GALLERY,
                            autoSave = prefs.autoSaveHistory
                        )
                        _uiState.update { it.copy(isGalleryLoading = false) }
                        _events.send(ScannerEvent.NavigateToResult(record.id))
                    } else {
                        _events.send(
                            ScannerEvent.TriggerScanFeedback(
                                sound = prefs.soundOnScan,
                                vibration = prefs.vibrationOnScan
                            )
                        )
                        _uiState.update {
                            it.copy(
                                isGalleryLoading = false,
                                isScanningPaused = true,
                                detectedCandidates = candidates,
                                selectedCandidate = candidates.first(),
                                multiCodeSource = ScanSource.GALLERY
                            )
                        }
                    }
                },
                onFailure = {
                    _uiState.update { it.copy(isGalleryLoading = false, isScanningPaused = false) }
                    _events.send(ScannerEvent.ShowGalleryDecodeError)
                }
            )
        }
    }

    fun updateCameraCapabilities(
        hasFlash: Boolean,
        minZoom: Float,
        maxZoom: Float,
        currentZoom: Float
    ) {
        _uiState.update {
            it.copy(
                hasFlashUnit = hasFlash,
                minZoomRatio = minZoom.coerceAtLeast(1f),
                maxZoomRatio = maxZoom.coerceAtLeast(1f),
                currentZoomRatio = currentZoom.coerceAtLeast(1f),
                cameraInitError = false
            )
        }
    }

    fun setTorchEnabled(enabled: Boolean) {
        _uiState.update { it.copy(torchEnabled = enabled && it.hasFlashUnit) }
    }

    fun setZoomRatio(zoom: Float) {
        _uiState.update { state ->
            val clamped = zoom.coerceIn(state.minZoomRatio, state.maxZoomRatio)
            state.copy(currentZoomRatio = clamped)
        }
    }

    fun onCameraInitError() {
        _uiState.update { it.copy(cameraInitError = true) }
    }

    fun setShowQrGeneratorSheet(show: Boolean) {
        _uiState.update { it.copy(showQrGeneratorSheet = show) }
    }

    fun saveGeneratedQrToHistory(payload: String) {
        if (payload.isBlank()) return
        viewModelScope.launch {
            historyRepository.recordScan(
                rawValue = payload,
                format = BarcodeFormatType.QR_CODE,
                source = ScanSource.GALLERY,
                autoSave = true
            )
            _events.send(ScannerEvent.ShowSavedToast)
        }
    }
}
