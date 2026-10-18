package com.example.core.model

enum class ScanSource {
    CAMERA,
    GALLERY
}

enum class ContentCategory {
    URL,
    TEXT,
    WIFI,
    EMAIL,
    PHONE,
    SMS,
    CONTACT,
    LOCATION,
    CALENDAR,
    PRODUCT,
    UNKNOWN
}

enum class BarcodeFormatType(
    val displayName: String,
    val is2D: Boolean
) {
    QR_CODE("QR Code", true),
    DATA_MATRIX("Data Matrix", true),
    PDF417("PDF417", true),
    AZTEC("Aztec", true),
    EAN_13("EAN-13", false),
    EAN_8("EAN-8", false),
    UPC_A("UPC-A", false),
    UPC_E("UPC-E", false),
    CODE_128("Code 128", false),
    CODE_39("Code 39", false),
    CODE_93("Code 93", false),
    CODABAR("Codabar", false),
    ITF("ITF", false),
    UNKNOWN("Unknown", false);

    companion object {
        fun fromName(name: String): BarcodeFormatType {
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: UNKNOWN
        }
    }
}

sealed class ParsedScanContent {
    abstract val category: ContentCategory
    abstract val summaryTitle: String

    data class Url(
        val url: String,
        val host: String,
        val isHttps: Boolean,
        val isSuspiciousScheme: Boolean = false
    ) : ParsedScanContent() {
        override val category: ContentCategory = if (isSuspiciousScheme) ContentCategory.TEXT else ContentCategory.URL
        override val summaryTitle: String = host.ifBlank { url }
    }

    data class Wifi(
        val ssid: String,
        val password: String,
        val securityType: String,
        val isHidden: Boolean
    ) : ParsedScanContent() {
        override val category: ContentCategory = ContentCategory.WIFI
        override val summaryTitle: String = ssid.ifBlank { "Wi-Fi Network" }
    }

    data class Email(
        val address: String,
        val subject: String = "",
        val body: String = ""
    ) : ParsedScanContent() {
        override val category: ContentCategory = ContentCategory.EMAIL
        override val summaryTitle: String = address
    }

    data class Phone(
        val phoneNumber: String
    ) : ParsedScanContent() {
        override val category: ContentCategory = ContentCategory.PHONE
        override val summaryTitle: String = phoneNumber
    }

    data class Sms(
        val phoneNumber: String,
        val message: String
    ) : ParsedScanContent() {
        override val category: ContentCategory = ContentCategory.SMS
        override val summaryTitle: String = phoneNumber.ifBlank { message.take(32) }
    }

    data class Contact(
        val fullName: String,
        val organization: String = "",
        val title: String = "",
        val phones: List<String> = emptyList(),
        val emails: List<String> = emptyList(),
        val address: String = "",
        val website: String = "",
        val note: String = ""
    ) : ParsedScanContent() {
        override val category: ContentCategory = ContentCategory.CONTACT
        override val summaryTitle: String = fullName.ifBlank {
            organization.ifBlank { phones.firstOrNull() ?: "Contact" }
        }
    }

    data class Location(
        val latitude: Double,
        val longitude: Double,
        val query: String = ""
    ) : ParsedScanContent() {
        override val category: ContentCategory = ContentCategory.LOCATION
        override val summaryTitle: String = if (query.isNotBlank()) {
            "$query ($latitude, $longitude)"
        } else {
            "$latitude, $longitude"
        }
    }

    data class CalendarEvent(
        val title: String,
        val startRaw: String = "",
        val endRaw: String = "",
        val startEpochMillis: Long? = null,
        val endEpochMillis: Long? = null,
        val location: String = "",
        val description: String = ""
    ) : ParsedScanContent() {
        override val category: ContentCategory = ContentCategory.CALENDAR
        override val summaryTitle: String = title.ifBlank { "Calendar Event" }
    }

    data class Product(
        val code: String,
        val format: BarcodeFormatType,
        val countryOriginHint: String = ""
    ) : ParsedScanContent() {
        override val category: ContentCategory = ContentCategory.PRODUCT
        override val summaryTitle: String = code
    }

    data class PlainText(
        val text: String,
        val isFlaggedScheme: Boolean = false
    ) : ParsedScanContent() {
        override val category: ContentCategory = ContentCategory.TEXT
        override val summaryTitle: String = text.lineSequence().firstOrNull()?.take(60)?.trim()
            ?.ifBlank { text.take(60) } ?: text.take(60)
    }

    data class Unknown(
        val rawValue: String
    ) : ParsedScanContent() {
        override val category: ContentCategory = ContentCategory.UNKNOWN
        override val summaryTitle: String = rawValue.take(60).ifBlank { "Unknown Code" }
    }
}

data class NormalizedBoundingBox(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    val centerX: Float get() = (left + right) / 2f
    val centerY: Float get() = (top + bottom) / 2f
}

data class DetectedBarcodeCandidate(
    val id: String,
    val rawValue: String,
    val format: BarcodeFormatType,
    val parsedContent: ParsedScanContent,
    val boundingBox: NormalizedBoundingBox? = null
)

data class ScanRecord(
    val id: Long = 0L,
    val rawValue: String,
    val displayTitle: String,
    val category: ContentCategory,
    val format: BarcodeFormatType,
    val timestamp: Long,
    val source: ScanSource,
    val isFavorite: Boolean = false,
    val parsedContent: ParsedScanContent
)
