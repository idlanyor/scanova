package com.example.feature.settings

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.BurstMode
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.BuildConfig
import com.example.R
import com.example.data.preferences.AppLanguage
import com.example.data.preferences.AppThemeMode
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel
) {
    val preferences by viewModel.preferences.collectAsStateWithLifecycle()
    val historyCount by viewModel.historyCount.collectAsStateWithLifecycle()
    val showClearDialog by viewModel.showClearDialog.collectAsStateWithLifecycle()
    val showPrivacyDialog by viewModel.showPrivacyDialog.collectAsStateWithLifecycle()
    val showAboutDialog by viewModel.showAboutDialog.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val deletedMsg = stringResource(R.string.result_deleted_toast)
    val csvSuccessMsg = stringResource(R.string.history_csv_export_success)
    val csvErrorMsg = stringResource(R.string.history_csv_export_error)

    val csvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            viewModel.exportCsv(context, uri) { ok ->
                scope.launch {
                    snackbarHostState.showSnackbar(if (ok) csvSuccessMsg else csvErrorMsg)
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.setShowClearDialog(false) },
            title = { Text(stringResource(R.string.history_clear_dialog_title)) },
            text = { Text(stringResource(R.string.history_clear_dialog_message)) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllHistory {
                            scope.launch { snackbarHostState.showSnackbar(deletedMsg) }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("settings_confirm_clear_button")
                ) {
                    Text(stringResource(R.string.common_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.setShowClearDialog(false) }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }

    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.setShowPrivacyDialog(false) },
            title = { Text(stringResource(R.string.settings_privacy_info_title)) },
            text = { Text(stringResource(R.string.settings_privacy_dialog_body)) },
            confirmButton = {
                TextButton(onClick = { viewModel.setShowPrivacyDialog(false) }) {
                    Text(stringResource(R.string.common_close))
                }
            }
        )
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.setShowAboutDialog(false) },
            title = { Text(stringResource(R.string.settings_about_title)) },
            text = { Text(stringResource(R.string.settings_about_dialog_body)) },
            confirmButton = {
                TextButton(onClick = { viewModel.setShowAboutDialog(false) }) {
                    Text(stringResource(R.string.common_close))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_title),
                        style = MaterialTheme.typography.titleLarge
                    )
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
                .padding(innerPadding)
                .testTag("settings_screen_root"),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 640.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .testTag("settings_scroll_column"),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // 1. Scan Preferences Section
                SettingsSectionCard(title = stringResource(R.string.settings_section_scan)) {
                    SettingsSwitchRow(
                        icon = Icons.AutoMirrored.Filled.VolumeUp,
                        title = stringResource(R.string.settings_sound_title),
                        subtitle = stringResource(R.string.settings_sound_subtitle),
                        checked = preferences.soundOnScan,
                        onCheckedChange = viewModel::setSoundOnScan,
                        testTag = "settings_sound_switch"
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    SettingsSwitchRow(
                        icon = Icons.Default.Vibration,
                        title = stringResource(R.string.settings_vibration_title),
                        subtitle = stringResource(R.string.settings_vibration_subtitle),
                        checked = preferences.vibrationOnScan,
                        onCheckedChange = viewModel::setVibrationOnScan,
                        testTag = "settings_vibration_switch"
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    SettingsSwitchRow(
                        icon = Icons.Default.History,
                        title = stringResource(R.string.settings_autosave_title),
                        subtitle = stringResource(R.string.settings_autosave_subtitle),
                        checked = preferences.autoSaveHistory,
                        onCheckedChange = viewModel::setAutoSaveHistory,
                        testTag = "settings_autosave_switch"
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    SettingsSwitchRow(
                        icon = Icons.Default.BurstMode,
                        title = stringResource(R.string.settings_batch_title),
                        subtitle = stringResource(R.string.settings_batch_subtitle),
                        checked = preferences.batchScanMode,
                        onCheckedChange = viewModel::setBatchScanMode,
                        testTag = "settings_batch_switch"
                    )
                }

                // 2. Appearance Section
                SettingsSectionCard(title = stringResource(R.string.settings_section_appearance)) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            SettingsIconBadge(icon = Icons.Default.DarkMode)
                            Text(
                                text = stringResource(R.string.settings_theme_title),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = preferences.themeMode == AppThemeMode.SYSTEM,
                                onClick = { viewModel.setThemeMode(AppThemeMode.SYSTEM) },
                                label = { Text(stringResource(R.string.settings_theme_system)) },
                                modifier = Modifier.testTag("settings_theme_system_chip")
                            )
                            FilterChip(
                                selected = preferences.themeMode == AppThemeMode.LIGHT,
                                onClick = { viewModel.setThemeMode(AppThemeMode.LIGHT) },
                                label = { Text(stringResource(R.string.settings_theme_light)) },
                                modifier = Modifier.testTag("settings_theme_light_chip")
                            )
                            FilterChip(
                                selected = preferences.themeMode == AppThemeMode.DARK,
                                onClick = { viewModel.setThemeMode(AppThemeMode.DARK) },
                                label = { Text(stringResource(R.string.settings_theme_dark)) },
                                modifier = Modifier.testTag("settings_theme_dark_chip")
                            )
                        }
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        SettingsSwitchRow(
                            icon = Icons.Default.ColorLens,
                            title = stringResource(R.string.settings_dynamic_color_title),
                            subtitle = stringResource(R.string.settings_dynamic_color_subtitle),
                            checked = preferences.dynamicColor,
                            onCheckedChange = viewModel::setDynamicColor,
                            testTag = "settings_dynamic_color_switch"
                        )
                    }
                }

                // 3. Data & Privacy Section
                SettingsSectionCard(title = stringResource(R.string.settings_section_privacy)) {
                    SettingsActionRow(
                        icon = Icons.Default.Download,
                        title = stringResource(R.string.settings_export_csv_title),
                        subtitle = stringResource(R.string.settings_export_csv_subtitle),
                        onClick = {
                            val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
                            csvLauncher.launch("scanova_history_$stamp.csv")
                        },
                        testTag = "settings_export_csv_row"
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    SettingsActionRow(
                        icon = Icons.Default.DeleteForever,
                        title = stringResource(R.string.settings_clear_history_title),
                        subtitle = "${stringResource(R.string.settings_clear_history_subtitle)} ($historyCount)",
                        onClick = { viewModel.setShowClearDialog(true) },
                        testTag = "settings_clear_history_row"
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    SettingsActionRow(
                        icon = Icons.Default.Shield,
                        title = stringResource(R.string.settings_privacy_info_title),
                        subtitle = stringResource(R.string.settings_privacy_info_subtitle),
                        onClick = { viewModel.setShowPrivacyDialog(true) },
                        testTag = "settings_privacy_info_row"
                    )
                }

                // 4. General & About Section
                SettingsSectionCard(title = stringResource(R.string.settings_section_general)) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            SettingsIconBadge(icon = Icons.Default.Language)
                            Text(
                                text = stringResource(R.string.settings_language_title),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = preferences.language == AppLanguage.INDONESIAN,
                                onClick = { viewModel.setLanguage(AppLanguage.INDONESIAN) },
                                label = { Text(stringResource(R.string.settings_language_id)) },
                                modifier = Modifier.testTag("settings_lang_id_chip")
                            )
                            FilterChip(
                                selected = preferences.language == AppLanguage.ENGLISH,
                                onClick = { viewModel.setLanguage(AppLanguage.ENGLISH) },
                                label = { Text(stringResource(R.string.settings_language_en)) },
                                modifier = Modifier.testTag("settings_lang_en_chip")
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                    SettingsActionRow(
                        icon = Icons.Default.Info,
                        title = stringResource(R.string.settings_about_title),
                        subtitle = "${stringResource(R.string.settings_version_title)}: ${BuildConfig.VERSION_NAME} • ${stringResource(R.string.settings_about_subtitle)}",
                        onClick = { viewModel.setShowAboutDialog(true) },
                        testTag = "settings_about_row"
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 6.dp)
        )
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                content()
            }
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        SettingsIconBadge(icon = icon)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag)
        )
    }
}

@Composable
private fun SettingsActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        SettingsIconBadge(icon = icon)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SettingsIconBadge(icon: ImageVector) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.size(40.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
