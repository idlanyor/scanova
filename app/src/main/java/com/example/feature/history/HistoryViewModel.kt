package com.example.feature.history

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.model.BarcodeFormatType
import com.example.core.model.ContentCategory
import com.example.core.model.ScanRecord
import com.example.core.model.ScanSource
import com.example.data.repository.ScanHistoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class HistoryTypeFilter {
    ALL,
    FAVORITES,
    QR_2D,
    BARCODE_1D
}

enum class HistorySortOrder {
    NEWEST_FIRST,
    OLDEST_FIRST
}

data class HistoryFilterState(
    val searchQuery: String = "",
    val typeFilter: HistoryTypeFilter = HistoryTypeFilter.ALL,
    val selectedCategory: ContentCategory? = null,
    val sortOrder: HistorySortOrder = HistorySortOrder.NEWEST_FIRST,
    val selectedIds: Set<Long> = emptySet(),
    val showClearConfirmation: Boolean = false,
    val showQrGeneratorSheet: Boolean = false
)

data class HistoryUiState(
    val allCount: Int = 0,
    val filteredRecords: List<ScanRecord> = emptyList(),
    val filterState: HistoryFilterState = HistoryFilterState()
) {
    val isSelectionMode: Boolean get() = filterState.selectedIds.isNotEmpty()
}

class HistoryViewModel(
    private val repository: ScanHistoryRepository
) : ViewModel() {

    private val filterState = MutableStateFlow(HistoryFilterState())

    val uiState: StateFlow<HistoryUiState> = combine(
        repository.allScans,
        filterState
    ) { allScans, filter ->
        val query = filter.searchQuery.trim().lowercase()
        val filtered = allScans.filter { record ->
            val matchesType = when (filter.typeFilter) {
                HistoryTypeFilter.ALL -> true
                HistoryTypeFilter.FAVORITES -> record.isFavorite
                HistoryTypeFilter.QR_2D -> record.format.is2D
                HistoryTypeFilter.BARCODE_1D -> !record.format.is2D
            }
            val matchesCategory = filter.selectedCategory == null || record.category == filter.selectedCategory
            val matchesQuery = query.isEmpty() ||
                record.rawValue.lowercase().contains(query) ||
                record.displayTitle.lowercase().contains(query) ||
                record.format.displayName.lowercase().contains(query) ||
                record.category.name.lowercase().contains(query)

            matchesType && matchesCategory && matchesQuery
        }.let { list ->
            when (filter.sortOrder) {
                HistorySortOrder.NEWEST_FIRST -> list.sortedByDescending { it.timestamp }
                HistorySortOrder.OLDEST_FIRST -> list.sortedBy { it.timestamp }
            }
        }

        // Clean up any selected IDs that no longer exist
        val existingIds = allScans.map { it.id }.toSet()
        val validSelection = filter.selectedIds.intersect(existingIds)

        HistoryUiState(
            allCount = allScans.size,
            filteredRecords = filtered,
            filterState = filter.copy(selectedIds = validSelection)
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HistoryUiState()
    )

    fun setSearchQuery(query: String) {
        filterState.update { it.copy(searchQuery = query) }
    }

    fun setTypeFilter(typeFilter: HistoryTypeFilter) {
        filterState.update { it.copy(typeFilter = typeFilter) }
    }

    fun toggleCategoryFilter(category: ContentCategory) {
        filterState.update { current ->
            val next = if (current.selectedCategory == category) null else category
            current.copy(selectedCategory = next)
        }
    }

    fun toggleSortOrder() {
        filterState.update { current ->
            val next = if (current.sortOrder == HistorySortOrder.NEWEST_FIRST) {
                HistorySortOrder.OLDEST_FIRST
            } else {
                HistorySortOrder.NEWEST_FIRST
            }
            current.copy(sortOrder = next)
        }
    }

    fun toggleItemSelection(id: Long) {
        filterState.update { current ->
            val next = if (id in current.selectedIds) {
                current.selectedIds - id
            } else {
                current.selectedIds + id
            }
            current.copy(selectedIds = next)
        }
    }

    fun clearSelection() {
        filterState.update { it.copy(selectedIds = emptySet()) }
    }

    fun toggleFavorite(record: ScanRecord) {
        viewModelScope.launch {
            repository.setFavorite(record.id, !record.isFavorite)
        }
    }

    fun deleteRecord(id: Long) {
        viewModelScope.launch {
            repository.deleteById(id)
        }
    }

    fun deleteSelectedRecords() {
        val ids = filterState.value.selectedIds
        if (ids.isEmpty()) return
        viewModelScope.launch {
            repository.deleteByIds(ids)
            filterState.update { it.copy(selectedIds = emptySet()) }
        }
    }

    fun setShowClearConfirmation(show: Boolean) {
        filterState.update { it.copy(showClearConfirmation = show) }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearAllHistory()
            filterState.update { it.copy(selectedIds = emptySet(), showClearConfirmation = false) }
        }
    }

    fun setShowQrGeneratorSheet(show: Boolean) {
        filterState.update { it.copy(showQrGeneratorSheet = show) }
    }

    fun saveGeneratedQrToHistory(payload: String) {
        if (payload.isBlank()) return
        viewModelScope.launch {
            repository.recordScan(
                rawValue = payload,
                format = BarcodeFormatType.QR_CODE,
                source = ScanSource.GALLERY,
                autoSave = true
            )
        }
    }

    fun exportHistoryToCsvUri(
        context: Context,
        uri: Uri,
        onComplete: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            val success = withContext(Dispatchers.IO) {
                try {
                    val csvContent = repository.buildCsvExport()
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(csvContent.toByteArray(Charsets.UTF_8))
                    } ?: return@withContext false
                    true
                } catch (_: Exception) {
                    false
                }
            }
            onComplete(success)
        }
    }
}
