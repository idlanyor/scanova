package com.example.data.repository

import com.example.core.model.BarcodeFormatType
import com.example.core.model.ScanRecord
import com.example.core.model.ScanSource
import com.example.core.scanner.SmartContentParser
import com.example.data.local.dao.ScanDao
import com.example.data.local.entity.ScanEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ScanHistoryRepository(
    private val scanDao: ScanDao
) {
    private val transientLastScan = MutableStateFlow<ScanRecord?>(null)

    // Session duplicate guard: prevents rapid repeated frame insertions of the exact same code within a short window
    private var lastInsertedSignature: String? = null
    private var lastInsertedEpochMillis: Long = 0L

    val allScans: Flow<List<ScanRecord>> = scanDao.observeAllScans()
        .map { entities -> entities.map { it.toDomain() } }

    fun observeScanById(id: Long): Flow<ScanRecord?> {
        if (id < 0L) {
            return transientLastScan.map { record ->
                if (record?.id == id) record else null
            }
        }
        return scanDao.observeScanById(id).map { it?.toDomain() }
    }

    suspend fun getScanById(id: Long): ScanRecord? {
        if (id < 0L) {
            return transientLastScan.value?.takeIf { it.id == id }
        }
        return scanDao.getScanById(id)?.toDomain()
    }

    suspend fun recordScan(
        rawValue: String,
        format: BarcodeFormatType,
        source: ScanSource,
        autoSave: Boolean,
        nowMillis: Long = System.currentTimeMillis(),
        dedupeWindowMs: Long = 2200L
    ): ScanRecord {
        val parsed = SmartContentParser.parse(rawValue, format)
        val signature = "${format.name}:${rawValue.trim()}"

        if (
            autoSave &&
            signature == lastInsertedSignature &&
            (nowMillis - lastInsertedEpochMillis) in 0..dedupeWindowMs
        ) {
            val recent = scanDao.getAllScansSnapshot().firstOrNull()
            if (recent != null && recent.rawValue.trim() == rawValue.trim()) {
                return recent.toDomain()
            }
        }

        val draftRecord = ScanRecord(
            id = if (autoSave) 0L else -1L,
            rawValue = rawValue,
            displayTitle = parsed.summaryTitle,
            category = parsed.category,
            format = format,
            timestamp = nowMillis,
            source = source,
            isFavorite = false,
            parsedContent = parsed
        )

        return if (autoSave) {
            val insertedId = scanDao.insertScan(ScanEntity.fromDomain(draftRecord))
            lastInsertedSignature = signature
            lastInsertedEpochMillis = nowMillis
            draftRecord.copy(id = insertedId)
        } else {
            transientLastScan.value = draftRecord
            draftRecord
        }
    }

    suspend fun saveTransientRecordToHistory(record: ScanRecord): Long {
        val entity = ScanEntity.fromDomain(record.copy(id = 0L))
        val newId = scanDao.insertScan(entity)
        if (transientLastScan.value?.id == record.id) {
            transientLastScan.value = record.copy(id = newId)
        }
        return newId
    }

    suspend fun setFavorite(id: Long, isFavorite: Boolean) {
        if (id < 0L) {
            val current = transientLastScan.value
            if (current != null && current.id == id) {
                transientLastScan.value = current.copy(isFavorite = isFavorite)
            }
            return
        }
        scanDao.updateFavorite(id, isFavorite)
    }

    suspend fun deleteById(id: Long) {
        if (id < 0L) {
            transientLastScan.value = null
            return
        }
        scanDao.deleteById(id)
    }

    suspend fun deleteByIds(ids: Collection<Long>) {
        if (ids.isEmpty()) return
        scanDao.deleteByIds(ids.toList())
    }

    suspend fun clearAllHistory() {
        lastInsertedSignature = null
        lastInsertedEpochMillis = 0L
        scanDao.clearAll()
    }

    fun resetSessionDuplicateGuard() {
        lastInsertedSignature = null
        lastInsertedEpochMillis = 0L
    }

    suspend fun buildCsvExport(): String {
        val items = scanDao.getAllScansSnapshot().map { it.toDomain() }
        return formatRecordsAsCsv(items)
    }

    companion object {
        fun formatRecordsAsCsv(records: List<ScanRecord>): String {
            val sb = StringBuilder()
            sb.append("ID,Timestamp,ISO_Date,Category,Format,Is_2D,Source,Favorite,Title,Raw_Value\n")
            val isoFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

            for (item in records) {
                val isoDate = isoFormat.format(Date(item.timestamp))
                val row = listOf(
                    item.id.toString(),
                    item.timestamp.toString(),
                    escapeCsv(isoDate),
                    escapeCsv(item.category.name),
                    escapeCsv(item.format.displayName),
                    item.format.is2D.toString(),
                    escapeCsv(item.source.name),
                    item.isFavorite.toString(),
                    escapeCsv(item.displayTitle),
                    escapeCsv(item.rawValue)
                )
                sb.append(row.joinToString(",")).append("\n")
            }
            return sb.toString()
        }

        private fun escapeCsv(value: String): String {
            val clean = value.replace("\"", "\"\"")
            return if (clean.contains(",") || clean.contains("\n") || clean.contains("\r") || clean.contains("\"")) {
                "\"$clean\""
            } else {
                clean
            }
        }
    }
}
