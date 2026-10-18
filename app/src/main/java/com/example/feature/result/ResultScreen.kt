package com.example.feature.result

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.core.design.accentColor
import com.example.core.design.iconVector
import com.example.core.design.localizedLabel
import com.example.core.model.ParsedScanContent
import com.example.core.model.ScanRecord
import com.example.core.model.ScanSource
import com.example.core.util.ExternalIntentHelper
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.JetBrainsMonoFamily
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ResultScreen(
    scanId: Long,
    viewModel: ResultViewModel,
    onNavigateBack: () -> Unit,
    onScanAnother: () -> Unit
) {
    BackHandler {
        onNavigateBack()
    }

    LaunchedEffect(scanId) {
        viewModel.loadScanRecord(scanId)
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val copiedMsg = stringResource(R.string.result_copied_toast)
    val savedMsg = stringResource(R.string.result_saved_toast)
    val noAppMsg = stringResource(R.string.result_no_app_error)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.result_screen_title),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("result_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.common_back)
                        )
                    }
                },
                actions = {
                    val record = uiState.record
                    if (record != null) {
                        IconButton(
                            onClick = viewModel::toggleFavorite,
                            modifier = Modifier.testTag("result_favorite_button")
                        ) {
                            Icon(
                                imageVector = if (record.isFavorite) {
                                    Icons.Default.Favorite
                                } else {
                                    Icons.Default.FavoriteBorder
                                },
                                contentDescription = if (record.isFavorite) {
                                    stringResource(R.string.result_action_unfavorite)
                                } else {
                                    stringResource(R.string.result_action_favorite)
                                },
                                tint = if (record.isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(
                            onClick = {
                                viewModel.deleteCurrentRecord(onDeleted = onNavigateBack)
                            },
                            modifier = Modifier.testTag("result_delete_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = stringResource(R.string.result_action_delete_history)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            when {
                uiState.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                uiState.record == null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        OutlinedButton(onClick = onNavigateBack) {
                            Text(stringResource(R.string.common_back))
                        }
                    }
                }

                else -> {
                    val record = uiState.record!!
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 640.dp)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Header Summary Card
                        ResultHeaderCard(record = record)

                        // Context-Aware Primary Actions
                        ContextAwareActionSection(
                            record = record,
                            onShowMessage = { msg -> scope.launch { snackbarHostState.showSnackbar(msg) } },
                            copiedMsg = copiedMsg,
                            noAppMsg = noAppMsg
                        )

                        // Structured Parsed Information Card
                        ParsedDetailsCard(
                            content = record.parsedContent,
                            isPasswordRevealed = uiState.isPasswordRevealed,
                            onTogglePassword = viewModel::togglePasswordVisibility
                        )

                        // Raw Decoded Payload Card
                        RawPayloadCard(
                            rawValue = record.rawValue,
                            onCopyRaw = {
                                ExternalIntentHelper.copyToClipboard(context, "Scanova Raw", record.rawValue)
                                scope.launch { snackbarHostState.showSnackbar(copiedMsg) }
                            }
                        )

                        // Secondary Actions (Save if auto-save was off + Scan Another Code)
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (record.id < 0L) {
                                OutlinedButton(
                                    onClick = {
                                        viewModel.saveTransientToHistory {
                                            scope.launch { snackbarHostState.showSnackbar(savedMsg) }
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(50.dp)
                                        .testTag("result_save_history_button")
                                ) {
                                    Icon(imageVector = Icons.Default.BookmarkAdd, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(stringResource(R.string.result_action_save_history))
                                }
                            }

                            Button(
                                onClick = onScanAnother,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                                    .testTag("result_scan_again_button")
                            ) {
                                Icon(imageVector = Icons.Default.QrCodeScanner, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.result_action_scan_again))
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultHeaderCard(record: ScanRecord) {
    val accent = record.category.accentColor()
    val dateFormatted = remember(record.timestamp) {
        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.getDefault())
            .format(Date(record.timestamp))
    }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("result_header_card")
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = accent.copy(alpha = 0.16f),
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = record.category.iconVector(),
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = record.category.localizedLabel(),
                        style = MaterialTheme.typography.labelLarge,
                        color = accent
                    )
                    Text(
                        text = record.displayTitle,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BadgePill(
                        text = record.format.displayName,
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    BadgePill(
                        text = if (record.source == ScanSource.CAMERA) {
                            stringResource(R.string.source_camera)
                        } else {
                            stringResource(R.string.source_gallery)
                        },
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = dateFormatted,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun BadgePill(
    text: String,
    containerColor: androidx.compose.ui.graphics.Color,
    contentColor: androidx.compose.ui.graphics.Color
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = containerColor
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = contentColor,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ContextAwareActionSection(
    record: ScanRecord,
    onShowMessage: (String) -> Unit,
    copiedMsg: String,
    noAppMsg: String
) {
    val context = LocalContext.current
    val parsed = record.parsedContent

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Primary context-specific buttons
        when (parsed) {
            is ParsedScanContent.Url -> {
                if (!parsed.isSuspiciousScheme) {
                    Button(
                        onClick = {
                            if (!ExternalIntentHelper.openSafeUrl(context, parsed.url)) {
                                onShowMessage(noAppMsg)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("result_open_url_button")
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.result_action_open_url))
                    }
                }
            }

            is ParsedScanContent.Wifi -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (parsed.password.isNotBlank()) {
                        Button(
                            onClick = {
                                ExternalIntentHelper.copyToClipboard(context, "Wi-Fi Password", parsed.password)
                                onShowMessage(copiedMsg)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("result_copy_wifi_password_button")
                        ) {
                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.result_action_copy_password))
                        }
                    }
                    FilledTonalButton(
                        onClick = {
                            if (!ExternalIntentHelper.openWifiSettings(context)) {
                                onShowMessage(noAppMsg)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("result_open_wifi_settings_button")
                    ) {
                        Icon(imageVector = Icons.Default.Wifi, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.result_action_open_wifi_settings))
                    }
                }
            }

            is ParsedScanContent.Email -> {
                Button(
                    onClick = {
                        if (!ExternalIntentHelper.composeEmail(context, parsed)) {
                            onShowMessage(noAppMsg)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("result_send_email_button")
                ) {
                    Icon(imageVector = Icons.Default.Email, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.result_action_send_email))
                }
            }

            is ParsedScanContent.Phone -> {
                Button(
                    onClick = {
                        if (!ExternalIntentHelper.dialPhoneNumber(context, parsed.phoneNumber)) {
                            onShowMessage(noAppMsg)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("result_dial_phone_button")
                ) {
                    Icon(imageVector = Icons.Default.Call, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.result_action_dial_phone))
                }
            }

            is ParsedScanContent.Sms -> {
                Button(
                    onClick = {
                        if (!ExternalIntentHelper.composeSms(context, parsed)) {
                            onShowMessage(noAppMsg)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("result_send_sms_button")
                ) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.result_action_send_sms))
                }
            }

            is ParsedScanContent.Contact -> {
                Button(
                    onClick = {
                        if (!ExternalIntentHelper.insertContact(context, parsed)) {
                            onShowMessage(noAppMsg)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("result_add_contact_button")
                ) {
                    Icon(imageVector = Icons.Default.PersonAdd, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.result_action_add_contact))
                }
            }

            is ParsedScanContent.Location -> {
                Button(
                    onClick = {
                        if (!ExternalIntentHelper.openGeographicLocation(context, parsed)) {
                            onShowMessage(noAppMsg)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("result_open_map_button")
                ) {
                    Icon(imageVector = Icons.Default.Map, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.result_action_open_map))
                }
            }

            is ParsedScanContent.CalendarEvent -> {
                Button(
                    onClick = {
                        if (!ExternalIntentHelper.addCalendarEvent(context, parsed)) {
                            onShowMessage(noAppMsg)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("result_add_calendar_button")
                ) {
                    Icon(imageVector = Icons.Default.CalendarMonth, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.result_action_add_calendar))
                }
            }

            is ParsedScanContent.Product -> {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            if (!ExternalIntentHelper.searchProductOnGoogle(context, parsed.code)) {
                                onShowMessage(noAppMsg)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("result_search_google_button")
                    ) {
                        Icon(imageVector = Icons.Default.Search, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.result_action_search_google))
                    }
                    FilledTonalButton(
                        onClick = {
                            if (!ExternalIntentHelper.searchProductOnOpenFoodFacts(context, parsed.code)) {
                                onShowMessage(noAppMsg)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("result_search_openfoodfacts_button")
                    ) {
                        Icon(imageVector = Icons.Default.TravelExplore, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.result_action_search_openfoodfacts))
                    }
                }
            }

            is ParsedScanContent.PlainText, is ParsedScanContent.Unknown -> Unit
        }

        // Universal Copy & Share Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FilledTonalButton(
                onClick = {
                    ExternalIntentHelper.copyToClipboard(context, "Scanova Result", record.rawValue)
                    onShowMessage(copiedMsg)
                },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("result_copy_button")
            ) {
                Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.result_action_copy))
            }

            FilledTonalButton(
                onClick = {
                    if (!ExternalIntentHelper.shareText(context, record.rawValue)) {
                        onShowMessage(noAppMsg)
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("result_share_button")
            ) {
                Icon(imageVector = Icons.Default.Share, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.result_action_share))
            }
        }
    }
}

@Composable
private fun ParsedDetailsCard(
    content: ParsedScanContent,
    isPasswordRevealed: Boolean,
    onTogglePassword: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("result_parsed_details_card")
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.result_parsed_section),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            when (content) {
                is ParsedScanContent.Url -> {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (content.isHttps) {
                            EmeraldSuccess.copy(alpha = 0.12f)
                        } else {
                            AmberWarning.copy(alpha = 0.15f)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = if (content.isHttps) Icons.Default.Security else Icons.Default.WarningAmber,
                                contentDescription = null,
                                tint = if (content.isHttps) EmeraldSuccess else AmberWarning
                            )
                            Text(
                                text = stringResource(R.string.result_security_notice_url),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    DetailFieldRow(label = "Host", value = content.host)
                    DetailFieldRow(label = "URL", value = content.url, monospace = true)
                }

                is ParsedScanContent.Wifi -> {
                    DetailFieldRow(
                        label = stringResource(R.string.result_wifi_ssid),
                        value = content.ssid
                    )
                    DetailFieldRow(
                        label = stringResource(R.string.result_wifi_security),
                        value = content.securityType
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.result_wifi_password),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val passDisplay = when {
                                content.password.isEmpty() -> stringResource(R.string.result_wifi_no_password)
                                isPasswordRevealed -> content.password
                                else -> "•".repeat(content.password.length.coerceIn(8, 16))
                            }
                            Text(
                                text = passDisplay,
                                style = MaterialTheme.typography.bodyLarge,
                                fontFamily = JetBrainsMonoFamily,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.testTag("wifi_password_value")
                            )
                        }
                        if (content.password.isNotEmpty()) {
                            IconButton(
                                onClick = onTogglePassword,
                                modifier = Modifier.testTag("wifi_password_toggle_button")
                            ) {
                                Icon(
                                    imageVector = if (isPasswordRevealed) {
                                        Icons.Default.VisibilityOff
                                    } else {
                                        Icons.Default.Visibility
                                    },
                                    contentDescription = if (isPasswordRevealed) {
                                        stringResource(R.string.result_wifi_hide_password)
                                    } else {
                                        stringResource(R.string.result_wifi_show_password)
                                    }
                                )
                            }
                        }
                    }
                    DetailFieldRow(
                        label = stringResource(R.string.result_wifi_hidden),
                        value = if (content.isHidden) {
                            stringResource(R.string.common_yes)
                        } else {
                            stringResource(R.string.common_no)
                        }
                    )
                }

                is ParsedScanContent.Email -> {
                    DetailFieldRow(
                        label = stringResource(R.string.result_field_email),
                        value = content.address
                    )
                    if (content.subject.isNotBlank()) {
                        DetailFieldRow(
                            label = stringResource(R.string.result_field_subject),
                            value = content.subject
                        )
                    }
                    if (content.body.isNotBlank()) {
                        DetailFieldRow(
                            label = stringResource(R.string.result_field_body),
                            value = content.body
                        )
                    }
                }

                is ParsedScanContent.Phone -> {
                    DetailFieldRow(
                        label = stringResource(R.string.result_field_phone),
                        value = content.phoneNumber,
                        monospace = true
                    )
                }

                is ParsedScanContent.Sms -> {
                    DetailFieldRow(
                        label = stringResource(R.string.result_field_phone),
                        value = content.phoneNumber,
                        monospace = true
                    )
                    if (content.message.isNotBlank()) {
                        DetailFieldRow(
                            label = stringResource(R.string.result_field_body),
                            value = content.message
                        )
                    }
                }

                is ParsedScanContent.Contact -> {
                    if (content.fullName.isNotBlank()) {
                        DetailFieldRow(stringResource(R.string.result_field_name), content.fullName)
                    }
                    if (content.organization.isNotBlank()) {
                        DetailFieldRow(stringResource(R.string.result_field_org), content.organization)
                    }
                    if (content.title.isNotBlank()) {
                        DetailFieldRow(stringResource(R.string.result_field_title), content.title)
                    }
                    if (content.phones.isNotEmpty()) {
                        DetailFieldRow(
                            stringResource(R.string.result_field_phone),
                            content.phones.joinToString(", "),
                            monospace = true
                        )
                    }
                    if (content.emails.isNotEmpty()) {
                        DetailFieldRow(
                            stringResource(R.string.result_field_email),
                            content.emails.joinToString(", ")
                        )
                    }
                    if (content.address.isNotBlank()) {
                        DetailFieldRow(stringResource(R.string.result_field_address), content.address)
                    }
                    if (content.website.isNotBlank()) {
                        DetailFieldRow(stringResource(R.string.result_field_website), content.website)
                    }
                    if (content.note.isNotBlank()) {
                        DetailFieldRow(stringResource(R.string.result_field_note), content.note)
                    }
                }

                is ParsedScanContent.Location -> {
                    DetailFieldRow(
                        label = stringResource(R.string.result_field_coordinates),
                        value = "${content.latitude}, ${content.longitude}",
                        monospace = true
                    )
                    if (content.query.isNotBlank()) {
                        DetailFieldRow(
                            label = stringResource(R.string.result_field_query),
                            value = content.query
                        )
                    }
                }

                is ParsedScanContent.CalendarEvent -> {
                    DetailFieldRow(
                        label = stringResource(R.string.result_field_event_title),
                        value = content.title
                    )
                    if (content.startRaw.isNotBlank()) {
                        DetailFieldRow(
                            label = stringResource(R.string.result_field_event_start),
                            value = content.startRaw
                        )
                    }
                    if (content.endRaw.isNotBlank()) {
                        DetailFieldRow(
                            label = stringResource(R.string.result_field_event_end),
                            value = content.endRaw
                        )
                    }
                    if (content.location.isNotBlank()) {
                        DetailFieldRow(
                            label = stringResource(R.string.result_field_event_location),
                            value = content.location
                        )
                    }
                    if (content.description.isNotBlank()) {
                        DetailFieldRow(
                            label = stringResource(R.string.result_field_note),
                            value = content.description
                        )
                    }
                }

                is ParsedScanContent.Product -> {
                    DetailFieldRow(
                        label = stringResource(R.string.result_field_product_code),
                        value = content.code,
                        monospace = true
                    )
                    DetailFieldRow(
                        label = stringResource(R.string.result_field_symbology),
                        value = content.format.displayName
                    )
                    if (content.countryOriginHint.isNotBlank()) {
                        DetailFieldRow(
                            label = stringResource(R.string.result_field_country_origin),
                            value = content.countryOriginHint
                        )
                    }
                }

                is ParsedScanContent.PlainText -> {
                    if (content.isFlaggedScheme) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = AmberWarning.copy(alpha = 0.16f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.WarningAmber,
                                    contentDescription = null,
                                    tint = AmberWarning
                                )
                                Text(
                                    text = stringResource(R.string.result_security_warning_scheme),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                    SelectionContainer {
                        Text(
                            text = content.text,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                is ParsedScanContent.Unknown -> {
                    SelectionContainer {
                        Text(
                            text = content.rawValue,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailFieldRow(
    label: String,
    value: String,
    monospace: Boolean = false
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        SelectionContainer {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = if (monospace) JetBrainsMonoFamily else MaterialTheme.typography.bodyLarge.fontFamily,
                    fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun RawPayloadCard(
    rawValue: String,
    onCopyRaw: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("result_raw_payload_card")
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.result_raw_section),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                IconButton(
                    onClick = onCopyRaw,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = stringResource(R.string.result_action_copy),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            SelectionContainer {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(14.dp)
                        )
                        .padding(14.dp)
                ) {
                    Text(
                        text = rawValue,
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = JetBrainsMonoFamily,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
