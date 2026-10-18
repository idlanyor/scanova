package com.example.core.scanner

import com.example.core.model.BarcodeFormatType
import com.example.core.model.ParsedScanContent
import java.net.URI
import java.net.URLDecoder
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

object SmartContentParser {

    private val PRODUCT_FORMATS = setOf(
        BarcodeFormatType.EAN_13,
        BarcodeFormatType.EAN_8,
        BarcodeFormatType.UPC_A,
        BarcodeFormatType.UPC_E
    )

    private val SUSPICIOUS_SCHEMES = setOf(
        "javascript", "data", "vbscript", "file", "intent", "content", "chrome", "about"
    )

    private val EMAIL_REGEX = Regex(
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    )

    private val PHONE_REGEX = Regex(
        "^\\+?[0-9][0-9\\s().-]{5,18}[0-9]$"
    )

    fun parse(rawValue: String, format: BarcodeFormatType): ParsedScanContent {
        val trimmed = rawValue.trim()
        if (trimmed.isEmpty()) {
            return ParsedScanContent.Unknown(rawValue)
        }

        // 1. Wi-Fi network configuration (WIFI:S:MySSID;T:WPA;P:secret;H:true;;)
        if (trimmed.startsWith("WIFI:", ignoreCase = true)) {
            parseWifi(trimmed)?.let { return it }
        }

        // 2. Contact cards (vCard or MECARD)
        if (trimmed.contains("BEGIN:VCARD", ignoreCase = true)) {
            parseVCard(trimmed)?.let { return it }
        }
        if (trimmed.startsWith("MECARD:", ignoreCase = true) || trimmed.startsWith("BIZCARD:", ignoreCase = true)) {
            parseMeCard(trimmed)?.let { return it }
        }

        // 3. Calendar Event (BEGIN:VEVENT or BEGIN:VCALENDAR)
        if (trimmed.contains("BEGIN:VEVENT", ignoreCase = true)) {
            parseCalendarEvent(trimmed)?.let { return it }
        }

        // 4. Email formats (mailto:, MATMSG:)
        if (trimmed.startsWith("MATMSG:", ignoreCase = true)) {
            parseMatMsgEmail(trimmed)?.let { return it }
        }
        if (trimmed.startsWith("mailto:", ignoreCase = true)) {
            parseMailto(trimmed)?.let { return it }
        }

        // 5. SMS formats (smsto:, sms:, MMSTO:)
        if (
            trimmed.startsWith("smsto:", ignoreCase = true) ||
            trimmed.startsWith("sms:", ignoreCase = true) ||
            trimmed.startsWith("mmsto:", ignoreCase = true)
        ) {
            parseSms(trimmed)?.let { return it }
        }

        // 6. Telephone URI (tel:)
        if (trimmed.startsWith("tel:", ignoreCase = true)) {
            val number = trimmed.substring(4).trim()
            if (number.isNotEmpty()) {
                return ParsedScanContent.Phone(number)
            }
        }

        // 7. Geographic location (geo:lat,lng?q=...)
        if (trimmed.startsWith("geo:", ignoreCase = true)) {
            parseGeo(trimmed)?.let { return it }
        }

        // 8. Check for suspicious/dangerous URI schemes before general URL checks
        val schemePrefix = trimmed.substringBefore(':', "").lowercase(Locale.ROOT)
        if (schemePrefix in SUSPICIOUS_SCHEMES && trimmed.contains(":")) {
            return ParsedScanContent.PlainText(
                text = trimmed,
                isFlaggedScheme = true
            )
        }

        // 9. HTTP / HTTPS URLs
        if (
            trimmed.startsWith("https://", ignoreCase = true) ||
            trimmed.startsWith("http://", ignoreCase = true)
        ) {
            parseUrl(trimmed)?.let { return it }
        }

        // 10. Product Barcodes (EAN-13, EAN-8, UPC-A, UPC-E) or 8/12/13/14-digit retail GTIN on 1D formats
        if (format in PRODUCT_FORMATS && trimmed.all { it.isDigit() } && trimmed.length in 6..14) {
            return ParsedScanContent.Product(
                code = trimmed,
                format = format,
                countryOriginHint = resolveGs1Country(trimmed, format)
            )
        }

        // 11. Standalone Email Address
        if (!trimmed.contains("\\s".toRegex()) && EMAIL_REGEX.matches(trimmed)) {
            return ParsedScanContent.Email(address = trimmed)
        }

        // 12. Standalone Phone Number (only on 2D codes or explicitly formatted with + to avoid confusing 1D serials)
        if (format.is2D && PHONE_REGEX.matches(trimmed) && trimmed.count { it.isDigit() } in 7..15) {
            return ParsedScanContent.Phone(phoneNumber = trimmed)
        }

        // 13. Bare domain URLs (e.g. www.example.com)
        if (trimmed.startsWith("www.", ignoreCase = true) && !trimmed.contains(" ") && trimmed.length > 6) {
            val normalizedUrl = "https://$trimmed"
            parseUrl(normalizedUrl)?.let { return it }
        }

        // Default to safe PlainText
        return ParsedScanContent.PlainText(text = rawValue)
    }

    private fun parseUrl(urlString: String): ParsedScanContent? {
        return try {
            val uri = URI(urlString.trim())
            val scheme = uri.scheme?.lowercase(Locale.ROOT) ?: return null
            if (scheme != "http" && scheme != "https") {
                return ParsedScanContent.PlainText(text = urlString, isFlaggedScheme = true)
            }
            val host = uri.host ?: uri.authority ?: ""
            if (host.isBlank() || !host.contains(".")) {
                return ParsedScanContent.PlainText(text = urlString)
            }
            ParsedScanContent.Url(
                url = urlString.trim(),
                host = host,
                isHttps = scheme == "https",
                isSuspiciousScheme = false
            )
        } catch (_: Exception) {
            ParsedScanContent.PlainText(text = urlString)
        }
    }

    private fun parseWifi(raw: String): ParsedScanContent.Wifi? {
        val body = if (raw.startsWith("WIFI:", ignoreCase = true)) {
            raw.substring(5)
        } else {
            return null
        }
        if (body.isBlank()) return null

        val fields = parseEscapedSemicolonFields(body)
        val ssid = fields["S"] ?: fields["s"] ?: ""
        val password = fields["P"] ?: fields["p"] ?: ""
        val rawType = (fields["T"] ?: fields["t"] ?: "nopass").uppercase(Locale.ROOT)
        val securityType = when {
            rawType.contains("WPA3") || rawType.contains("SAE") -> "WPA3"
            rawType.contains("WPA") -> "WPA/WPA2"
            rawType.contains("WEP") -> "WEP"
            rawType.isBlank() || rawType == "NOPASS" || rawType == "NONE" -> "None"
            else -> rawType
        }
        val hiddenStr = (fields["H"] ?: fields["h"] ?: "false").lowercase(Locale.ROOT)
        val isHidden = hiddenStr == "true" || hiddenStr == "1"

        if (ssid.isBlank() && password.isBlank()) return null

        return ParsedScanContent.Wifi(
            ssid = ssid,
            password = password,
            securityType = securityType,
            isHidden = isHidden
        )
    }

    private fun parseVCard(raw: String): ParsedScanContent.Contact? {
        val lines = raw.lines()
        var fullName = ""
        var structuredName = ""
        var org = ""
        var title = ""
        val phones = mutableListOf<String>()
        val emails = mutableListOf<String>()
        var address = ""
        var website = ""
        var note = ""

        for (rawLine in lines) {
            val line = rawLine.trim()
            if (line.isEmpty() || !line.contains(":")) continue
            val keyPart = line.substringBefore(":").uppercase(Locale.ROOT)
            val valuePart = line.substringAfter(":").trim()
            if (valuePart.isEmpty()) continue

            val baseKey = keyPart.substringBefore(";")
            when (baseKey) {
                "FN" -> fullName = unescapeVCardValue(valuePart)
                "N" -> {
                    val parts = valuePart.split(";").map { unescapeVCardValue(it).trim() }.filter { it.isNotEmpty() }
                    if (parts.size >= 2) {
                        structuredName = "${parts[1]} ${parts[0]}".trim()
                    } else if (parts.isNotEmpty()) {
                        structuredName = parts[0]
                    }
                }
                "ORG" -> org = unescapeVCardValue(valuePart.replace(";", " ").trim())
                "TITLE" -> title = unescapeVCardValue(valuePart)
                "TEL" -> phones.add(valuePart)
                "EMAIL" -> emails.add(valuePart)
                "ADR" -> {
                    val cleanAddr = valuePart.split(";")
                        .map { unescapeVCardValue(it).trim() }
                        .filter { it.isNotEmpty() }
                        .joinToString(", ")
                    if (cleanAddr.isNotEmpty()) address = cleanAddr
                }
                "URL" -> website = valuePart
                "NOTE" -> note = unescapeVCardValue(valuePart)
            }
        }

        val resolvedName = fullName.ifBlank { structuredName }
        if (resolvedName.isBlank() && phones.isEmpty() && emails.isEmpty() && org.isBlank()) {
            return null
        }

        return ParsedScanContent.Contact(
            fullName = resolvedName,
            organization = org,
            title = title,
            phones = phones.distinct(),
            emails = emails.distinct(),
            address = address,
            website = website,
            note = note
        )
    }

    private fun parseMeCard(raw: String): ParsedScanContent.Contact? {
        val body = raw.substringAfter(":")
        val fields = parseEscapedSemicolonFields(body)
        val rawName = fields["N"] ?: fields["n"] ?: ""
        val fullName = if (rawName.contains(",")) {
            val parts = rawName.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            if (parts.size >= 2) "${parts[1]} ${parts[0]}" else rawName
        } else {
            rawName
        }
        val org = fields["ORG"] ?: fields["org"] ?: ""
        val phone = fields["TEL"] ?: fields["tel"] ?: ""
        val email = fields["EMAIL"] ?: fields["email"] ?: ""
        val address = (fields["ADR"] ?: fields["adr"] ?: "").replace(",", ", ").trim()
        val url = fields["URL"] ?: fields["url"] ?: ""
        val note = fields["NOTE"] ?: fields["note"] ?: ""

        if (fullName.isBlank() && phone.isBlank() && email.isBlank()) return null

        return ParsedScanContent.Contact(
            fullName = fullName,
            organization = org,
            phones = if (phone.isNotBlank()) listOf(phone) else emptyList(),
            emails = if (email.isNotBlank()) listOf(email) else emptyList(),
            address = address,
            website = url,
            note = note
        )
    }

    private fun parseCalendarEvent(raw: String): ParsedScanContent.CalendarEvent? {
        var summary = ""
        var dtStart = ""
        var dtEnd = ""
        var location = ""
        var description = ""

        for (rawLine in raw.lines()) {
            val line = rawLine.trim()
            if (!line.contains(":")) continue
            val key = line.substringBefore(":").substringBefore(";").uppercase(Locale.ROOT)
            val value = line.substringAfter(":").trim()
            when (key) {
                "SUMMARY" -> summary = unescapeVCardValue(value)
                "DTSTART" -> dtStart = value
                "DTEND" -> dtEnd = value
                "LOCATION" -> location = unescapeVCardValue(value)
                "DESCRIPTION" -> description = unescapeVCardValue(value)
            }
        }

        if (summary.isBlank() && dtStart.isBlank()) return null

        return ParsedScanContent.CalendarEvent(
            title = summary.ifBlank { "Event" },
            startRaw = formatIcsDateReadable(dtStart),
            endRaw = formatIcsDateReadable(dtEnd),
            startEpochMillis = parseIcsDateMillis(dtStart),
            endEpochMillis = parseIcsDateMillis(dtEnd),
            location = location,
            description = description
        )
    }

    private fun parseMatMsgEmail(raw: String): ParsedScanContent.Email? {
        val body = if (raw.startsWith("MATMSG:", ignoreCase = true)) {
            raw.substring(7)
        } else {
            return null
        }
        val fields = parseEscapedSemicolonFields(body)
        val to = fields["TO"] ?: fields["to"] ?: ""
        val sub = fields["SUB"] ?: fields["sub"] ?: ""
        val msg = fields["BODY"] ?: fields["body"] ?: ""
        if (to.isBlank()) return null
        return ParsedScanContent.Email(
            address = to,
            subject = sub,
            body = msg
        )
    }

    private fun parseMailto(raw: String): ParsedScanContent.Email? {
        val withoutScheme = raw.substring(7).trim()
        val addressPart = withoutScheme.substringBefore("?").trim()
        val queryPart = withoutScheme.substringAfter("?", "")
        var subject = ""
        var body = ""

        if (queryPart.isNotEmpty()) {
            queryPart.split("&").forEach { param ->
                val k = param.substringBefore("=").lowercase(Locale.ROOT)
                val v = decodeUrlComponent(param.substringAfter("=", ""))
                when (k) {
                    "subject" -> subject = v
                    "body" -> body = v
                }
            }
        }

        if (addressPart.isBlank() && subject.isBlank() && body.isBlank()) return null
        return ParsedScanContent.Email(
            address = decodeUrlComponent(addressPart),
            subject = subject,
            body = body
        )
    }

    private fun parseSms(raw: String): ParsedScanContent.Sms? {
        val content = raw.substringAfter(":").trim()
        if (content.isEmpty()) return null

        return if (content.contains("?")) {
            val number = content.substringBefore("?").trim()
            val query = content.substringAfter("?", "")
            var body = ""
            query.split("&").forEach { param ->
                if (param.substringBefore("=").equals("body", ignoreCase = true)) {
                    body = decodeUrlComponent(param.substringAfter("=", ""))
                }
            }
            ParsedScanContent.Sms(phoneNumber = number, message = body)
        } else if (content.contains(":")) {
            val number = content.substringBefore(":").trim()
            val message = content.substringAfter(":").trim()
            ParsedScanContent.Sms(phoneNumber = number, message = message)
        } else {
            ParsedScanContent.Sms(phoneNumber = content, message = "")
        }
    }

    private fun parseGeo(raw: String): ParsedScanContent.Location? {
        val content = raw.substring(4).trim()
        val coordsPart = content.substringBefore("?")
        val queryPart = content.substringAfter("?", "")

        val coordTokens = coordsPart.split(",")
        if (coordTokens.size < 2) return null

        val lat = coordTokens[0].trim().toDoubleOrNull() ?: return null
        val lng = coordTokens[1].trim().toDoubleOrNull() ?: return null
        if (lat !in -90.0..90.0 || lng !in -180.0..180.0) return null

        var query = ""
        if (queryPart.isNotEmpty()) {
            queryPart.split("&").forEach { param ->
                if (param.substringBefore("=").equals("q", ignoreCase = true)) {
                    query = decodeUrlComponent(param.substringAfter("=", ""))
                }
            }
        }

        return ParsedScanContent.Location(
            latitude = lat,
            longitude = lng,
            query = query
        )
    }

    private fun parseEscapedSemicolonFields(input: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        val tokenBuilder = StringBuilder()
        var escaped = false

        fun flushToken(token: String) {
            if (token.contains(":")) {
                val key = token.substringBefore(":").trim()
                val value = token.substringAfter(":")
                if (key.isNotEmpty()) {
                    result[key] = value
                }
            }
        }

        for (ch in input) {
            when {
                escaped -> {
                    tokenBuilder.append(ch)
                    escaped = false
                }
                ch == '\\' -> {
                    escaped = true
                }
                ch == ';' -> {
                    flushToken(tokenBuilder.toString())
                    tokenBuilder.clear()
                }
                else -> tokenBuilder.append(ch)
            }
        }
        if (tokenBuilder.isNotEmpty()) {
            flushToken(tokenBuilder.toString())
        }
        return result
    }

    private fun unescapeVCardValue(value: String): String {
        return value
            .replace("\\n", "\n")
            .replace("\\N", "\n")
            .replace("\\,", ",")
            .replace("\\;", ";")
            .trim()
    }

    private fun decodeUrlComponent(encoded: String): String {
        return try {
            URLDecoder.decode(encoded, "UTF-8")
        } catch (_: Exception) {
            encoded
        }
    }

    private fun parseIcsDateMillis(raw: String): Long? {
        if (raw.isBlank()) return null
        val clean = raw.trim()
        val patterns = listOf(
            "yyyyMMdd'T'HHmmss'Z'" to true,
            "yyyyMMdd'T'HHmmss" to false,
            "yyyyMMdd" to false
        )
        for ((pattern, isUtc) in patterns) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.US)
                if (isUtc) sdf.timeZone = TimeZone.getTimeZone("UTC")
                val parsed = sdf.parse(clean)
                if (parsed != null) return parsed.time
            } catch (_: Exception) {
                // try next pattern
            }
        }
        return null
    }

    private fun formatIcsDateReadable(raw: String): String {
        val millis = parseIcsDateMillis(raw) ?: return raw
        val formatter = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
        return formatter.format(millis)
    }

    private fun resolveGs1Country(code: String, format: BarcodeFormatType): String {
        val normalized = when (format) {
            BarcodeFormatType.UPC_A -> "0$code"
            else -> code
        }
        if (normalized.length < 3) return ""
        val prefix = normalized.take(3).toIntOrNull() ?: return ""
        return when (prefix) {
            in 0..19, in 30..39, in 60..139 -> "USA & Canada"
            in 300..379 -> "France"
            in 400..440 -> "Germany"
            in 450..459, in 490..499 -> "Japan"
            in 471..471 -> "Taiwan"
            in 480..480 -> "Philippines"
            in 500..509 -> "United Kingdom"
            in 690..699 -> "China"
            in 880..880 -> "South Korea"
            in 885..885 -> "Thailand"
            in 888..888 -> "Singapore"
            in 890..890 -> "India"
            in 893..893 -> "Vietnam"
            in 899..899 -> "Indonesia"
            in 930..939 -> "Australia"
            in 955..955 -> "Malaysia"
            else -> "GS1 ($prefix)"
        }
    }
}
