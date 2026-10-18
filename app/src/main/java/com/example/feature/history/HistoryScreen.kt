package com.example.feature.history

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.core.design.accentColor
import com.example.core.design.iconVector
import com.example.core.design.localizedLabel
import com.example.core.model.ContentCategory
import com.example.core.model.ScanRecord
import com.example.feature.scanner.QrGeneratorSheet
import com.example.ui.theme.JetBrainsMonoFamily
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onOpenScanDetail: (Long) -> Unit,
    onNavigateToScanner: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val filter = uiState.filterState
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val csvSuccessMsg = stringResource(R.string.history_csv_export_success)
    val csvErrorMsg = stringResource(R.string.history_csv_export_error)

    BackHandler(enabled = uiState.isSelectionMode) {
        viewModel.clearSelection()
    }

    val csvExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            viewModel.exportHistoryToCsvUri(context, uri) { success ->
                scope.launch {
                    snackbarHostState.showSnackbar(if (success) csvSuccessMsg else csvErrorMsg)
                }
            }
        }
    }

    if (filter.showClearConfirmation) {
        AlertDialog(
            onDismissRequest = { viewModel.setShowClearConfirmation(false) },
            title = { Text(stringResource(R.string.history_clear_dialog_title)) },
            text = { Text(stringResource(R.string.history_clear_dialog_message)) },
            confirmButton = {
                Button(
                    onClick = viewModel::clearAllHistory,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("history_confirm_clear_button")
                ) {
                    Text(stringResource(R.string.common_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.setShowClearConfirmation(false) }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }

    if (filter.showQrGeneratorSheet) {
        QrGeneratorSheet(
            onDismiss = { viewModel.setShowQrGeneratorSheet(false) },
            onSaveGeneratedPayload = viewModel::saveGeneratedQrToHistory
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (uiState.isSelectionMode) {
                        Text(
                            text = stringResource(R.string.history_select_mode_title, filter.selectedIds.size),
                            style = MaterialTheme.typography.titleLarge
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.history_title),
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                },
                navigationIcon = {
                    if (uiState.isSelectionMode) {
                        IconButton(onClick = viewModel::clearSelection) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.common_cancel)
                            )
                        }
                    }
                },
                actions = {
                    if (uiState.isSelectionMode) {
                        TextButton(
                            onClick = viewModel::deleteSelectedRecords,
                            modifier = Modifier.testTag("history_delete_selected_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.history_delete_selected, filter.selectedIds.size),
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    } else {
                        IconButton(
                            onClick = { viewModel.setShowQrGeneratorSheet(true) },
                            modifier = Modifier.testTag("history_open_qr_gen_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCode2,
                                contentDescription = stringResource(R.string.scanner_create_qr_button)
                            )
                        }
                        if (uiState.allCount > 0) {
                            IconButton(
                                onClick = {
                                    val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
                                    csvExportLauncher.launch("scanova_history_$stamp.csv")
                                },
                                modifier = Modifier.testTag("history_export_csv_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = stringResource(R.string.history_export_csv)
                                )
                            }
                            IconButton(
                                onClick = viewModel::toggleSortOrder,
                                modifier = Modifier.testTag("history_sort_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SwapVert,
                                    contentDescription = if (filter.sortOrder == HistorySortOrder.NEWEST_FIRST) {
                                        stringResource(R.string.history_sort_newest)
                                    } else {
                                        stringResource(R.string.history_sort_oldest)
                                    }
                                )
                            }
                            IconButton(
                                onClick = { viewModel.setShowClearConfirmation(true) },
                                modifier = Modifier.testTag("history_clear_all_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteSweep,
                                    contentDescription = stringResource(R.string.history_clear_all)
                                )
                            }
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
                .padding(innerPadding)
                .testTag("history_screen_root"),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 680.dp)
            ) {
                if (uiState.allCount > 0) {
                    // Search & Filter Bar
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = filter.searchQuery,
                            onValueChange = viewModel::setSearchQuery,
                            placeholder = {
                                Text(
                                    text = stringResource(R.string.history_search_placeholder),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.Search, contentDescription = null)
                            },
                            trailingIcon = {
                                if (filter.searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = stringResource(R.string.common_close)
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(18.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("history_search_input")
                        )

                        // Type Filter Chips + Category Filter Chips
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = filter.typeFilter == HistoryTypeFilter.ALL && filter.selectedCategory == null,
                                onClick = {
                                    viewModel.setTypeFilter(HistoryTypeFilter.ALL)
                                    filter.selectedCategory?.let { viewModel.toggleCategoryFilter(it) }
                                },
                                label = { Text(stringResource(R.string.history_filter_all)) },
                                modifier = Modifier.testTag("history_chip_all")
                            )
                            FilterChip(
                                selected = filter.typeFilter == HistoryTypeFilter.FAVORITES,
                                onClick = { viewModel.setTypeFilter(HistoryTypeFilter.FAVORITES) },
                                label = { Text(stringResource(R.string.history_filter_favorites)) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Favorite,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                modifier = Modifier.testTag("history_chip_favorites")
                            )
                            FilterChip(
                                selected = filter.typeFilter == HistoryTypeFilter.QR_2D,
                                onClick = { viewModel.setTypeFilter(HistoryTypeFilter.QR_2D) },
                                label = { Text(stringResource(R.string.history_filter_qr)) },
                                modifier = Modifier.testTag("history_chip_qr")
                            )
                            FilterChip(
                                selected = filter.typeFilter == HistoryTypeFilter.BARCODE_1D,
                                onClick = { viewModel.setTypeFilter(HistoryTypeFilter.BARCODE_1D) },
                                label = { Text(stringResource(R.string.history_filter_barcode)) },
                                modifier = Modifier.testTag("history_chip_barcode")
                            )

                            val categories = listOf(
                                ContentCategory.URL,
                                ContentCategory.WIFI,
                                ContentCategory.PRODUCT,
                                ContentCategory.TEXT,
                                ContentCategory.CONTACT,
                                ContentCategory.LOCATION
                            )
                            categories.forEach { category ->
                                FilterChip(
                                    selected = filter.selectedCategory == category,
                                    onClick = { viewModel.toggleCategoryFilter(category) },
                                    label = { Text(category.localizedLabel()) }
                                )
                            }
                        }
                    }
                }

                when {
                    uiState.allCount == 0 -> {
                        HistoryEmptyState(
                            isSearchFilterEmpty = false,
                            onStartScanning = onNavigateToScanner
                        )
                    }

                    uiState.filteredRecords.isEmpty() -> {
                        HistoryEmptyState(
                            isSearchFilterEmpty = true,
                            onStartScanning = {
                                viewModel.setSearchQuery("")
                                viewModel.setTypeFilter(HistoryTypeFilter.ALL)
                            }
                        )
                    }

                    else -> {
                        val todayLabel = stringResource(R.string.history_group_today)
                        val yesterdayLabel = stringResource(R.string.history_group_yesterday)
                        val grouped = remember(uiState.filteredRecords, todayLabel, yesterdayLabel) {
                            uiState.filteredRecords.groupBy { record ->
                                formatDateGroupHeader(record.timestamp, todayLabel, yesterdayLabel)
                            }
                        }

                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("history_lazy_list")
                        ) {
                            grouped.forEach { (dateHeader, itemsInGroup) ->
                                item(key = "header_$dateHeader") {
                                    Text(
                                        text = dateHeader,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(top = 6.dp, bottom = 2.dp, start = 4.dp)
                                    )
                                }

                                items(
                                    items = itemsInGroup,
                                    key = { it.id }
                                ) { record ->
                                    val isSelected = record.id in filter.selectedIds
                                    HistoryItemCard(
                                        record = record,
                                        isSelected = isSelected,
                                        onClick = {
                                            if (uiState.isSelectionMode) {
                                                viewModel.toggleItemSelection(record.id)
                                            } else {
                                                onOpenScanDetail(record.id)
                                            }
                                        },
                                        onLongClick = {
                                            viewModel.toggleItemSelection(record.id)
                                        },
                                        onToggleFavorite = { viewModel.toggleFavorite(record) },
                                        onDelete = { viewModel.deleteRecord(record.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HistoryItemCard(
    record: ScanRecord,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit
) {
    val accent = record.category.accentColor()
    val timeText = remember(record.timestamp) {
        DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault()).format(Date(record.timestamp))
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("history_item_card_${record.id}")
            .clip(RoundedCornerShape(20.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    accent.copy(alpha = 0.14f)
                },
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isSelected) {
                            Icons.Default.CheckCircle
                        } else {
                            record.category.iconVector()
                        },
                        contentDescription = null,
                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else accent,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = record.displayTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }

                Text(
                    text = record.rawValue,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = JetBrainsMonoFamily,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = record.format.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = "• ${record.category.localizedLabel()} • $timeText",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.testTag("history_favorite_btn_${record.id}")
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
                        tint = if (record.isFavorite) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.testTag("history_delete_btn_${record.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = stringResource(R.string.result_action_delete_history),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryEmptyState(
    isSearchFilterEmpty: Boolean,
    onStartScanning: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
            .testTag("history_empty_state"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.widthIn(max = 380.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(84.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isSearchFilterEmpty) {
                            Icons.Default.Search
                        } else {
                            Icons.Default.QrCodeScanner
                        },
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            Text(
                text = if (isSearchFilterEmpty) {
                    stringResource(R.string.history_empty_search_title)
                } else {
                    stringResource(R.string.history_empty_title)
                },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            Text(
                text = if (isSearchFilterEmpty) {
                    stringResource(R.string.history_empty_search_subtitle)
                } else {
                    stringResource(R.string.history_empty_subtitle)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            if (!isSearchFilterEmpty) {
                Spacer(modifier = Modifier.height(6.dp))
                Button(
                    onClick = onStartScanning,
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("history_empty_scan_cta")
                ) {
                    Icon(imageVector = Icons.Default.QrCodeScanner, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.history_start_scanning_cta))
                }
            }
        }
    }
}

private fun formatDateGroupHeader(
    timestamp: Long,
    todayLabel: String,
    yesterdayLabel: String
): String {
    val nowCal = Calendar.getInstance()
    val targetCal = Calendar.getInstance().apply { timeInMillis = timestamp }

    if (
        nowCal.get(Calendar.YEAR) == targetCal.get(Calendar.YEAR) &&
        nowCal.get(Calendar.DAY_OF_YEAR) == targetCal.get(Calendar.DAY_OF_YEAR)
    ) {
        return todayLabel
    }

    nowCal.add(Calendar.DAY_OF_YEAR, -1)
    if (
        nowCal.get(Calendar.YEAR) == targetCal.get(Calendar.YEAR) &&
        nowCal.get(Calendar.DAY_OF_YEAR) == targetCal.get(Calendar.DAY_OF_YEAR)
    ) {
        return yesterdayLabel
    }

    return DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault()).format(Date(timestamp))
}
