package com.example.core.design

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ContactPage
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.example.R
import com.example.core.model.ContentCategory
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IndigoPrimary

@Composable
fun ContentCategory.localizedLabel(): String {
    return when (this) {
        ContentCategory.URL -> stringResource(R.string.category_url)
        ContentCategory.TEXT -> stringResource(R.string.category_text)
        ContentCategory.WIFI -> stringResource(R.string.category_wifi)
        ContentCategory.EMAIL -> stringResource(R.string.category_email)
        ContentCategory.PHONE -> stringResource(R.string.category_phone)
        ContentCategory.SMS -> stringResource(R.string.category_sms)
        ContentCategory.CONTACT -> stringResource(R.string.category_contact)
        ContentCategory.LOCATION -> stringResource(R.string.category_location)
        ContentCategory.CALENDAR -> stringResource(R.string.category_calendar)
        ContentCategory.PRODUCT -> stringResource(R.string.category_product)
        ContentCategory.UNKNOWN -> stringResource(R.string.category_unknown)
    }
}

fun ContentCategory.iconVector(): ImageVector {
    return when (this) {
        ContentCategory.URL -> Icons.Default.Language
        ContentCategory.TEXT -> Icons.Default.Description
        ContentCategory.WIFI -> Icons.Default.Wifi
        ContentCategory.EMAIL -> Icons.Default.Email
        ContentCategory.PHONE -> Icons.Default.Phone
        ContentCategory.SMS -> Icons.Default.Sms
        ContentCategory.CONTACT -> Icons.Default.ContactPage
        ContentCategory.LOCATION -> Icons.Default.LocationOn
        ContentCategory.CALENDAR -> Icons.Default.CalendarMonth
        ContentCategory.PRODUCT -> Icons.Default.Inventory2
        ContentCategory.UNKNOWN -> Icons.AutoMirrored.Filled.HelpOutline
    }
}

fun ContentCategory.accentColor(): Color {
    return when (this) {
        ContentCategory.URL -> IndigoPrimary
        ContentCategory.WIFI -> EmeraldSuccess
        ContentCategory.PRODUCT -> AmberWarning
        ContentCategory.LOCATION -> Color(0xFF06B6D4)
        ContentCategory.CONTACT -> Color(0xFF8B5CF6)
        ContentCategory.CALENDAR -> Color(0xFFEC4899)
        ContentCategory.EMAIL, ContentCategory.PHONE, ContentCategory.SMS -> Color(0xFF3B82F6)
        ContentCategory.TEXT, ContentCategory.UNKNOWN -> IndigoPrimary
    }
}
