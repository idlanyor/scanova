package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.core.model.BarcodeFormatType
import com.example.core.model.ContentCategory
import com.example.core.model.ScanRecord
import com.example.core.model.ScanSource
import com.example.core.scanner.SmartContentParser

@Entity(
    tableName = "scan_history",
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["contentType"]),
        Index(value = ["isFavorite"])
    ]
)
data class ScanEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val rawValue: String,
    val displayTitle: String,
    val contentType: String,
    val barcodeFormat: String,
    val is2D: Boolean,
    val timestamp: Long,
    val scanSource: String,
    val isFavorite: Boolean = false
) {
    fun toDomain(): ScanRecord {
        val format = BarcodeFormatType.fromName(barcodeFormat)
        val parsed = SmartContentParser.parse(rawValue, format)
        val resolvedCategory = try {
            ContentCategory.valueOf(contentType)
        } catch (_: Exception) {
            parsed.category
        }
        val resolvedSource = try {
            ScanSource.valueOf(scanSource)
        } catch (_: Exception) {
            ScanSource.CAMERA
        }

        return ScanRecord(
            id = id,
            rawValue = rawValue,
            displayTitle = displayTitle.ifBlank { parsed.summaryTitle },
            category = resolvedCategory,
            format = format,
            timestamp = timestamp,
            source = resolvedSource,
            isFavorite = isFavorite,
            parsedContent = parsed
        )
    }

    companion object {
        fun fromDomain(record: ScanRecord): ScanEntity {
            return ScanEntity(
                id = record.id,
                rawValue = record.rawValue,
                displayTitle = record.displayTitle,
                contentType = record.category.name,
                barcodeFormat = record.format.name,
                is2D = record.format.is2D,
                timestamp = record.timestamp,
                scanSource = record.source.name,
                isFavorite = record.isFavorite
            )
        }
    }
}
