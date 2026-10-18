package com.example.feature.scanner

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.scanner.QrCodeGenerator
import com.example.core.util.ExternalIntentHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class QrGeneratorType {
    TEXT,
    URL,
    WIFI
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrGeneratorSheet(
    onDismiss: () -> Unit,
    onSaveGeneratedPayload: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current

    var selectedType by remember { mutableStateOf(QrGeneratorType.TEXT) }
    var textInput by remember { mutableStateOf("Scanova — Scan Smarter, Instantly.") }
    var urlInput by remember { mutableStateOf("https://") }
    var wifiSsid by remember { mutableStateOf("") }
    var wifiPassword by remember { mutableStateOf("") }
    var wifiSecurity by remember { mutableStateOf("WPA") }

    val activePayload = remember(selectedType, textInput, urlInput, wifiSsid, wifiPassword, wifiSecurity) {
        when (selectedType) {
            QrGeneratorType.TEXT -> textInput.trim()
            QrGeneratorType.URL -> urlInput.trim()
            QrGeneratorType.WIFI -> if (wifiSsid.isBlank()) "" else {
                QrCodeGenerator.buildWifiPayload(
                    ssid = wifiSsid,
                    password = wifiPassword,
                    security = wifiSecurity
                )
            }
        }
    }

    var generatedBitmap by remember { mutableStateOf(QrCodeGenerator.generateQrBitmap(activePayload, 640)) }

    LaunchedEffect(activePayload) {
        generatedBitmap = withContext(Dispatchers.Default) {
            QrCodeGenerator.generateQrBitmap(activePayload, 640)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.qr_gen_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("qr_gen_close_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.common_close)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedType == QrGeneratorType.TEXT,
                    onClick = { selectedType = QrGeneratorType.TEXT },
                    label = { Text(stringResource(R.string.qr_gen_type_text)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    modifier = Modifier.testTag("qr_gen_chip_text")
                )
                FilterChip(
                    selected = selectedType == QrGeneratorType.URL,
                    onClick = { selectedType = QrGeneratorType.URL },
                    label = { Text(stringResource(R.string.qr_gen_type_url)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    modifier = Modifier.testTag("qr_gen_chip_url")
                )
                FilterChip(
                    selected = selectedType == QrGeneratorType.WIFI,
                    onClick = { selectedType = QrGeneratorType.WIFI },
                    label = { Text(stringResource(R.string.qr_gen_type_wifi)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Wifi,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    modifier = Modifier.testTag("qr_gen_chip_wifi")
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (selectedType) {
                QrGeneratorType.TEXT -> {
                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        label = { Text(stringResource(R.string.qr_gen_input_text_label)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("qr_gen_text_input"),
                        minLines = 2,
                        maxLines = 4
                    )
                }

                QrGeneratorType.URL -> {
                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = { urlInput = it },
                        label = { Text(stringResource(R.string.qr_gen_input_url_label)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("qr_gen_url_input"),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Done
                        )
                    )
                }

                QrGeneratorType.WIFI -> {
                    OutlinedTextField(
                        value = wifiSsid,
                        onValueChange = { wifiSsid = it },
                        label = { Text(stringResource(R.string.qr_gen_input_ssid_label)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("qr_gen_ssid_input"),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = wifiPassword,
                        onValueChange = { wifiPassword = it },
                        label = { Text(stringResource(R.string.qr_gen_input_password_label)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("qr_gen_password_input"),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("WPA", "WEP", "nopass").forEach { sec ->
                            FilterChip(
                                selected = wifiSecurity == sec,
                                onClick = { wifiSecurity = sec },
                                label = { Text(if (sec == "nopass") "Open" else sec) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            val bitmap = generatedBitmap
            if (bitmap != null && activePayload.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color.White,
                    tonalElevation = 4.dp,
                    shadowElevation = 6.dp,
                    modifier = Modifier.padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(220.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color.White)
                            .padding(14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = stringResource(R.string.qr_gen_title),
                            modifier = Modifier.size(192.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            onSaveGeneratedPayload(activePayload)
                            onDismiss()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("qr_gen_save_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.BookmarkAdd,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.qr_gen_save_to_history))
                    }

                    Button(
                        onClick = {
                            ExternalIntentHelper.shareQrBitmap(context, bitmap, activePayload)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("qr_gen_share_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.qr_gen_share_button))
                    }
                }
            }
        }
    }
}
