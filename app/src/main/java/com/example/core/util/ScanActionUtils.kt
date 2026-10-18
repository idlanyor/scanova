package com.example.core.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.CalendarContract
import android.provider.ContactsContract
import android.provider.Settings
import androidx.core.content.FileProvider
import com.example.core.model.ParsedScanContent
import java.io.File
import java.io.FileOutputStream
import java.net.URLEncoder

object ScanFeedbackHelper {

    fun triggerScanSuccessFeedback(
        context: Context,
        soundEnabled: Boolean,
        vibrationEnabled: Boolean
    ) {
        if (vibrationEnabled) {
            vibrateDevice(context)
        }
        if (soundEnabled) {
            playBeepTone()
        }
    }

    private fun vibrateDevice(context: Context) {
        try {
            val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                manager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            if (vibrator?.hasVibrator() == true) {
                vibrator.vibrate(
                    VibrationEffect.createOneShot(70L, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            }
        } catch (_: Exception) {
            // Ignore vibration failures on unsupported devices
        }
    }

    private fun playBeepTone() {
        try {
            val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 75)
            toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 110)
        } catch (_: Exception) {
            // Ignore audio hardware failures
        }
    }
}

object ExternalIntentHelper {

    fun copyToClipboard(context: Context, label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText(label, text))
    }

    fun shareText(context: Context, text: String): Boolean {
        return try {
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            }
            val chooser = Intent.createChooser(sendIntent, null).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun shareQrBitmap(context: Context, bitmap: Bitmap, payloadSummary: String): Boolean {
        return try {
            val dir = File(context.cacheDir, "shared_qr").apply { mkdirs() }
            val file = File(dir, "scanova_qr_${System.currentTimeMillis()}.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, payloadSummary)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, null))
            true
        } catch (_: Exception) {
            false
        }
    }

    fun openSafeUrl(context: Context, rawUrl: String): Boolean {
        return try {
            val uri = Uri.parse(rawUrl.trim())
            val scheme = uri.scheme?.lowercase() ?: return false
            if (scheme != "http" && scheme != "https") return false
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                addCategory(Intent.CATEGORY_BROWSABLE)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun openWifiSettings(context: Context): Boolean {
        return try {
            val intent = Intent(Settings.ACTION_WIFI_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun dialPhoneNumber(context: Context, phoneNumber: String): Boolean {
        return try {
            val uri = Uri.fromParts("tel", phoneNumber.trim(), null)
            val intent = Intent(Intent.ACTION_DIAL, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun composeEmail(context: Context, email: ParsedScanContent.Email): Boolean {
        return try {
            val uri = Uri.fromParts("mailto", email.address.trim(), null)
            val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
                if (email.subject.isNotBlank()) putExtra(Intent.EXTRA_SUBJECT, email.subject)
                if (email.body.isNotBlank()) putExtra(Intent.EXTRA_TEXT, email.body)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun composeSms(context: Context, sms: ParsedScanContent.Sms): Boolean {
        return try {
            val uri = Uri.parse("smsto:${Uri.encode(sms.phoneNumber.trim())}")
            val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
                if (sms.message.isNotBlank()) {
                    putExtra("sms_body", sms.message)
                }
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun insertContact(context: Context, contact: ParsedScanContent.Contact): Boolean {
        return try {
            val intent = Intent(ContactsContract.Intents.Insert.ACTION).apply {
                type = ContactsContract.RawContacts.CONTENT_TYPE
                if (contact.fullName.isNotBlank()) {
                    putExtra(ContactsContract.Intents.Insert.NAME, contact.fullName)
                }
                if (contact.organization.isNotBlank()) {
                    putExtra(ContactsContract.Intents.Insert.COMPANY, contact.organization)
                }
                if (contact.title.isNotBlank()) {
                    putExtra(ContactsContract.Intents.Insert.JOB_TITLE, contact.title)
                }
                contact.phones.getOrNull(0)?.let {
                    putExtra(ContactsContract.Intents.Insert.PHONE, it)
                }
                contact.phones.getOrNull(1)?.let {
                    putExtra(ContactsContract.Intents.Insert.SECONDARY_PHONE, it)
                }
                contact.emails.getOrNull(0)?.let {
                    putExtra(ContactsContract.Intents.Insert.EMAIL, it)
                }
                if (contact.address.isNotBlank()) {
                    putExtra(ContactsContract.Intents.Insert.POSTAL, contact.address)
                }
                if (contact.note.isNotBlank()) {
                    putExtra(ContactsContract.Intents.Insert.NOTES, contact.note)
                }
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun openGeographicLocation(context: Context, location: ParsedScanContent.Location): Boolean {
        val uriString = if (location.query.isNotBlank()) {
            "geo:${location.latitude},${location.longitude}?q=${Uri.encode(location.query)}"
        } else {
            "geo:${location.latitude},${location.longitude}?q=${location.latitude},${location.longitude}"
        }
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uriString)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            // Fallback to web maps if no geo app is installed
            openSafeUrl(
                context,
                "https://www.google.com/maps/search/?api=1&query=${location.latitude},${location.longitude}"
            )
        }
    }

    fun addCalendarEvent(context: Context, event: ParsedScanContent.CalendarEvent): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_INSERT).apply {
                data = CalendarContract.Events.CONTENT_URI
                putExtra(CalendarContract.Events.TITLE, event.title)
                if (event.location.isNotBlank()) {
                    putExtra(CalendarContract.Events.EVENT_LOCATION, event.location)
                }
                if (event.description.isNotBlank()) {
                    putExtra(CalendarContract.Events.DESCRIPTION, event.description)
                }
                event.startEpochMillis?.let {
                    putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, it)
                }
                event.endEpochMillis?.let {
                    putExtra(CalendarContract.EXTRA_EVENT_END_TIME, it)
                }
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun searchProductOnGoogle(context: Context, code: String): Boolean {
        val encoded = try {
            URLEncoder.encode(code.trim(), "UTF-8")
        } catch (_: Exception) {
            code.trim()
        }
        return openSafeUrl(context, "https://www.google.com/search?q=$encoded")
    }

    fun searchProductOnOpenFoodFacts(context: Context, code: String): Boolean {
        val clean = code.filter { it.isDigit() }
        return openSafeUrl(context, "https://world.openfoodfacts.org/product/$clean")
    }

    fun openAppDetailsSettings(context: Context): Boolean {
        return try {
            val intent = Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", context.packageName, null)
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }
}
