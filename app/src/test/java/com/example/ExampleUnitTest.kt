package com.example

import com.example.core.model.BarcodeFormatType
import com.example.core.model.ContentCategory
import com.example.core.model.ParsedScanContent
import com.example.core.model.ScanRecord
import com.example.core.model.ScanSource
import com.example.core.scanner.QrCodeGenerator
import com.example.core.scanner.SmartContentParser
import com.example.data.local.dao.ScanDao
import com.example.data.local.entity.ScanEntity
import com.example.data.repository.ScanHistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun `parse HTTPS URL classifies as URL with host and HTTPS flag`() {
        val parsed = SmartContentParser.parse("https://developer.android.com/jetpack/compose", BarcodeFormatType.QR_CODE)
        assertTrue(parsed is ParsedScanContent.Url)
        val url = parsed as ParsedScanContent.Url
        assertEquals(ContentCategory.URL, url.category)
        assertEquals("developer.android.com", url.host)
        assertTrue(url.isHttps)
        assertFalse(url.isSuspiciousScheme)
    }

    @Test
    fun `parse suspicious javascript URI scheme flags as safe PlainText`() {
        val parsed = SmartContentParser.parse("javascript:alert('xss')", BarcodeFormatType.QR_CODE)
        assertTrue(parsed is ParsedScanContent.PlainText)
        val text = parsed as ParsedScanContent.PlainText
        assertEquals(ContentCategory.TEXT, text.category)
        assertTrue(text.isFlaggedScheme)
    }

    @Test
    fun `parse Wi-Fi payload extracts SSID, password, WPA security, and hidden flag`() {
        val raw = "WIFI:T:WPA;S:KantorScanova;P:rahasia123\\;456;H:true;;"
        val parsed = SmartContentParser.parse(raw, BarcodeFormatType.QR_CODE)
        assertTrue(parsed is ParsedScanContent.Wifi)
        val wifi = parsed as ParsedScanContent.Wifi
        assertEquals("KantorScanova", wifi.ssid)
        assertEquals("rahasia123;456", wifi.password)
        assertEquals("WPA/WPA2", wifi.securityType)
        assertTrue(wifi.isHidden)
    }

    @Test
    fun `parse mailto and MATMSG email payloads`() {
        val mailto = SmartContentParser.parse(
            "mailto:halo@scanova.id?subject=Halo%20Tim&body=Tes%20Pesan",
            BarcodeFormatType.QR_CODE
        )
        assertTrue(mailto is ParsedScanContent.Email)
        val email1 = mailto as ParsedScanContent.Email
        assertEquals("halo@scanova.id", email1.address)
        assertEquals("Halo Tim", email1.subject)
        assertEquals("Tes Pesan", email1.body)

        val matmsg = SmartContentParser.parse(
            "MATMSG:TO:support@scanova.id;SUB:Laporan;BODY:Berhasil;;",
            BarcodeFormatType.QR_CODE
        )
        assertTrue(matmsg is ParsedScanContent.Email)
        val email2 = matmsg as ParsedScanContent.Email
        assertEquals("support@scanova.id", email2.address)
        assertEquals("Laporan", email2.subject)
        assertEquals("Berhasil", email2.body)
    }

    @Test
    fun `parse tel and smsto payloads`() {
        val tel = SmartContentParser.parse("tel:+6281234567890", BarcodeFormatType.QR_CODE)
        assertTrue(tel is ParsedScanContent.Phone)
        assertEquals("+6281234567890", (tel as ParsedScanContent.Phone).phoneNumber)

        val sms = SmartContentParser.parse("SMSTO:081299998888:Halo dari Scanova", BarcodeFormatType.QR_CODE)
        assertTrue(sms is ParsedScanContent.Sms)
        val parsedSms = sms as ParsedScanContent.Sms
        assertEquals("081299998888", parsedSms.phoneNumber)
        assertEquals("Halo dari Scanova", parsedSms.message)
    }

    @Test
    fun `parse vCard and MeCard contact payloads`() {
        val vcard = """
            BEGIN:VCARD
            VERSION:3.0
            FN:Budi Santoso
            ORG:Scanova Labs
            TITLE:Lead Engineer
            TEL:+62811223344
            EMAIL:budi@scanova.id
            ADR:;;Jl. Sudirman No. 1;Jakarta;;;Indonesia
            END:VCARD
        """.trimIndent()
        val parsed = SmartContentParser.parse(vcard, BarcodeFormatType.QR_CODE)
        assertTrue(parsed is ParsedScanContent.Contact)
        val contact = parsed as ParsedScanContent.Contact
        assertEquals("Budi Santoso", contact.fullName)
        assertEquals("Scanova Labs", contact.organization)
        assertEquals("+62811223344", contact.phones.first())
        assertEquals("budi@scanova.id", contact.emails.first())
    }

    @Test
    fun `parse geographic location and calendar event payloads`() {
        val geo = SmartContentParser.parse("geo:-6.2088,106.8456?q=Monas%20Jakarta", BarcodeFormatType.QR_CODE)
        assertTrue(geo is ParsedScanContent.Location)
        val loc = geo as ParsedScanContent.Location
        assertEquals(-6.2088, loc.latitude, 0.0001)
        assertEquals(106.8456, loc.longitude, 0.0001)
        assertEquals("Monas Jakarta", loc.query)

        val vevent = """
            BEGIN:VEVENT
            SUMMARY:Rapat Produk Scanova
            DTSTART:20261020T090000Z
            DTEND:20261020T100000Z
            LOCATION:Ruang Inovasi Lt. 5
            DESCRIPTION:Review rilis v1.0
            END:VEVENT
        """.trimIndent()
        val cal = SmartContentParser.parse(vevent, BarcodeFormatType.QR_CODE)
        assertTrue(cal is ParsedScanContent.CalendarEvent)
        val event = cal as ParsedScanContent.CalendarEvent
        assertEquals("Rapat Produk Scanova", event.title)
        assertEquals("Ruang Inovasi Lt. 5", event.location)
    }

    @Test
    fun `parse EAN-13 product barcode resolves Indonesia GS1 prefix 899`() {
        val product = SmartContentParser.parse("8991234567890", BarcodeFormatType.EAN_13)
        assertTrue(product is ParsedScanContent.Product)
        val prod = product as ParsedScanContent.Product
        assertEquals("8991234567890", prod.code)
        assertEquals("Indonesia", prod.countryOriginHint)
    }

    @Test
    fun `repository prevents rapid duplicate camera frame insertions within dedupe window`() = runTest {
        val fakeDao = InMemoryScanDao()
        val repository = ScanHistoryRepository(fakeDao)

        val first = repository.recordScan(
            rawValue = "https://scanova.id",
            format = BarcodeFormatType.QR_CODE,
            source = ScanSource.CAMERA,
            autoSave = true,
            nowMillis = 10_000L
        )
        val duplicateFrame = repository.recordScan(
            rawValue = "https://scanova.id",
            format = BarcodeFormatType.QR_CODE,
            source = ScanSource.CAMERA,
            autoSave = true,
            nowMillis = 10_500L
        )

        val allItems = repository.allScans.first()
        assertEquals(1, allItems.size)
        assertEquals(first.id, duplicateFrame.id)

        // Separate scan session after resetting guard or after window expires records normally
        repository.resetSessionDuplicateGuard()
        repository.recordScan(
            rawValue = "https://scanova.id",
            format = BarcodeFormatType.QR_CODE,
            source = ScanSource.CAMERA,
            autoSave = true,
            nowMillis = 20_000L
        )
        assertEquals(2, repository.allScans.first().size)
    }

    @Test
    fun `CSV export escapes commas and quotes properly`() {
        val wifiPayload = QrCodeGenerator.buildWifiPayload("My,Home\"WiFi", "pass123")
        val parsed = SmartContentParser.parse(wifiPayload, BarcodeFormatType.QR_CODE)
        val record = ScanRecord(
            id = 42L,
            rawValue = wifiPayload,
            displayTitle = parsed.summaryTitle,
            category = parsed.category,
            format = BarcodeFormatType.QR_CODE,
            timestamp = 1700000000000L,
            source = ScanSource.CAMERA,
            isFavorite = true,
            parsedContent = parsed
        )
        val csv = ScanHistoryRepository.formatRecordsAsCsv(listOf(record))
        assertTrue(csv.contains("ID,Timestamp,ISO_Date,Category,Format"))
        assertTrue(csv.contains("\"My,Home\"\"WiFi\""))
    }
}

class InMemoryScanDao : ScanDao {
    private val items = MutableStateFlow<List<ScanEntity>>(emptyList())
    private var nextId = 1L

    override fun observeAllScans(): Flow<List<ScanEntity>> = items.map { list ->
        list.sortedByDescending { it.timestamp }
    }

    override suspend fun getAllScansSnapshot(): List<ScanEntity> =
        items.value.sortedByDescending { it.timestamp }

    override fun observeScanById(id: Long): Flow<ScanEntity?> = items.map { list ->
        list.firstOrNull { it.id == id }
    }

    override suspend fun getScanById(id: Long): ScanEntity? =
        items.value.firstOrNull { it.id == id }

    override suspend fun insertScan(scan: ScanEntity): Long {
        val assignedId = if (scan.id == 0L) nextId++ else scan.id
        val entity = scan.copy(id = assignedId)
        items.value = items.value.filterNot { it.id == assignedId } + entity
        return assignedId
    }

    override suspend fun updateFavorite(id: Long, isFavorite: Boolean) {
        items.value = items.value.map { if (it.id == id) it.copy(isFavorite = isFavorite) else it }
    }

    override suspend fun deleteById(id: Long) {
        items.value = items.value.filterNot { it.id == id }
    }

    override suspend fun deleteByIds(ids: List<Long>) {
        val set = ids.toSet()
        items.value = items.value.filterNot { it.id in set }
    }

    override suspend fun clearAll() {
        items.value = emptyList()
    }
}
