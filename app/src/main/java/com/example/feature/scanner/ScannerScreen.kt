package com.example.feature.scanner

import android.Manifest
import android.content.pm.PackageManager
import android.view.HapticFeedbackConstants
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BurstMode
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.core.design.accentColor
import com.example.core.design.iconVector
import com.example.core.design.localizedLabel
import com.example.core.model.DetectedBarcodeCandidate
import com.example.core.scanner.MlKitBarcodeAnalyzer
import com.example.core.util.ExternalIntentHelper
import com.example.core.util.ScanFeedbackHelper
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.JetBrainsMonoFamily
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun ScannerScreen(
    viewModel: ScannerViewModel,
    onNavigateToResult: (Long) -> Unit,
    onNavigateToHistory: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val preferences by viewModel.preferences.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val view = LocalView.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    var permissionRequestedOnce by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        permissionRequestedOnce = true
        if (granted) {
            viewModel.resumeScanning()
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        viewModel.onGalleryImagePicked(context, uri)
    }

    val noCodeMsg = stringResource(R.string.scanner_gallery_no_code)
    val decodeErrMsg = stringResource(R.string.scanner_gallery_error)
    val savedToastMsg = stringResource(R.string.result_saved_toast)
    val torchUnavailableMsg = stringResource(R.string.scanner_torch_unavailable)

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        } else {
            viewModel.resumeScanning()
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    val granted = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.CAMERA
                    ) == PackageManager.PERMISSION_GRANTED
                    hasCameraPermission = granted
                    if (granted && uiState.detectedCandidates.isEmpty()) {
                        viewModel.resumeScanning()
                    }
                }
                Lifecycle.Event.ON_PAUSE -> {
                    viewModel.setTorchEnabled(false)
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(viewModel.events) {
        viewModel.events.collect { event ->
            when (event) {
                is ScannerEvent.NavigateToResult -> onNavigateToResult(event.scanId)
                is ScannerEvent.TriggerScanFeedback -> {
                    if (event.vibration) {
                        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                    }
                    ScanFeedbackHelper.triggerScanSuccessFeedback(
                        context = context,
                        soundEnabled = event.sound,
                        vibrationEnabled = event.vibration
                    )
                }
                ScannerEvent.ShowGalleryNoCodeFound -> {
                    snackbarHostState.showSnackbar(noCodeMsg)
                }
                ScannerEvent.ShowGalleryDecodeError -> {
                    snackbarHostState.showSnackbar(decodeErrMsg)
                }
                ScannerEvent.ShowSavedToast -> {
                    snackbarHostState.showSnackbar(savedToastMsg)
                }
            }
        }
    }

    if (uiState.showQrGeneratorSheet) {
        QrGeneratorSheet(
            onDismiss = { viewModel.setShowQrGeneratorSheet(false) },
            onSaveGeneratedPayload = { payload ->
                viewModel.saveGeneratedQrToHistory(payload)
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B1020))
            .testTag("scanner_screen_root")
    ) {
        if (hasCameraPermission && !uiState.cameraInitError) {
            CameraPreviewAndHud(
                uiState = uiState,
                batchMode = preferences.batchScanMode,
                onBarcodesDetected = viewModel::onCameraFramesDetected,
                onUpdateCapabilities = viewModel::updateCameraCapabilities,
                onCameraError = viewModel::onCameraInitError,
                onZoomChanged = viewModel::setZoomRatio,
                onSelectCandidate = viewModel::selectCandidate
            )
        } else {
            ScannerPermissionOrErrorFallback(
                hasCameraPermission = hasCameraPermission,
                permissionRequestedOnce = permissionRequestedOnce,
                cameraInitError = uiState.cameraInitError,
                onRequestPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                onOpenAppSettings = { ExternalIntentHelper.openAppDetailsSettings(context) },
                onRetryCamera = { viewModel.resumeScanning() },
                onPickFromGallery = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }
            )
        }

        // Top HUD Controls (Always accessible even if camera is denied)
        ScannerTopBar(
            torchEnabled = uiState.torchEnabled,
            hasFlashUnit = uiState.hasFlashUnit,
            batchMode = preferences.batchScanMode,
            isPaused = uiState.isScanningPaused,
            multiCodeCount = uiState.detectedCandidates.size,
            onToggleTorch = {
                if (uiState.hasFlashUnit) {
                    viewModel.setTorchEnabled(!uiState.torchEnabled)
                } else {
                    scope.launch { snackbarHostState.showSnackbar(torchUnavailableMsg) }
                }
            },
            onToggleBatchMode = viewModel::toggleBatchMode,
            onOpenQrGenerator = { viewModel.setShowQrGeneratorSheet(true) },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        )

        // Bottom Controls / Multi-code Selection / Batch Counter
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .widthIn(max = 560.dp)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Multi-code selection card when multiple codes are locked
            val selectedCandidate = uiState.selectedCandidate
            if (uiState.detectedCandidates.size > 1 && selectedCandidate != null) {
                MultiCodeSelectionCard(
                    candidates = uiState.detectedCandidates,
                    selectedCandidate = selectedCandidate,
                    onSelectCandidate = viewModel::selectCandidate,
                    onOpenSelected = viewModel::openSelectedCandidateDetails,
                    onResumeScanning = viewModel::resumeScanning
                )
            } else if (preferences.batchScanMode && uiState.batchSessionScans.isNotEmpty()) {
                val latestScan = uiState.batchSessionScans.first()
                BatchSessionFloatingCard(
                    totalCount = uiState.batchSessionScans.size,
                    latestTitle = latestScan.displayTitle,
                    latestFormat = latestScan.format.displayName,
                    onOpenLatest = { onNavigateToResult(latestScan.id) },
                    onViewHistory = onNavigateToHistory,
                    onClearSession = viewModel::clearBatchSession
                )
            }

            // Zoom presets + Gallery button bar
            ScannerBottomControlsBar(
                hasCameraPermission = hasCameraPermission && !uiState.cameraInitError,
                currentZoom = uiState.currentZoomRatio,
                maxZoom = uiState.maxZoomRatio,
                onSetZoom = viewModel::setZoomRatio,
                onPickFromGallery = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onOpenQrGenerator = { viewModel.setShowQrGeneratorSheet(true) }
            )
        }

        // Gallery Decoding Loading Overlay
        AnimatedVisibility(
            visible = uiState.isGalleryLoading,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color(0xE6151B2E),
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    CircularProgressIndicator(color = IndigoPrimary)
                    Text(
                        text = stringResource(R.string.scanner_gallery_loading),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 96.dp)
        )
    }
}

@Composable
private fun CameraPreviewAndHud(
    uiState: ScannerUiState,
    batchMode: Boolean,
    onBarcodesDetected: (List<DetectedBarcodeCandidate>) -> Unit,
    onUpdateCapabilities: (Boolean, Float, Float, Float) -> Unit,
    onCameraError: () -> Unit,
    onZoomChanged: (Float) -> Unit,
    onSelectCandidate: (DetectedBarcodeCandidate) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    var cameraInstance by remember { mutableStateOf<Camera?>(null) }
    var focusTapOffset by remember { mutableStateOf<Offset?>(null) }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }

    val analyzer = remember {
        MlKitBarcodeAnalyzer(
            isPausedProvider = { uiState.isScanningPaused },
            onBarcodesDetected = onBarcodesDetected
        )
    }

    DisposableEffect(lifecycleOwner) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val listener = Runnable {
            try {
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }
                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        it.setAnalyzer(analysisExecutor, analyzer)
                    }

                val selector = if (cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) {
                    CameraSelector.DEFAULT_BACK_CAMERA
                } else {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                }

                cameraProvider.unbindAll()
                val camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    selector,
                    preview,
                    imageAnalysis
                )
                cameraInstance = camera

                val zoomState = camera.cameraInfo.zoomState.value
                onUpdateCapabilities(
                    camera.cameraInfo.hasFlashUnit(),
                    zoomState?.minZoomRatio ?: 1f,
                    zoomState?.maxZoomRatio ?: 4f,
                    zoomState?.zoomRatio ?: 1f
                )
            } catch (_: Exception) {
                onCameraError()
            }
        }
        cameraProviderFuture.addListener(listener, ContextCompat.getMainExecutor(context))

        onDispose {
            try {
                cameraProviderFuture.get().unbindAll()
            } catch (_: Exception) {
                // Ignore unbind errors during disposal
            }
            analyzer.close()
            analysisExecutor.shutdown()
        }
    }

    LaunchedEffect(uiState.torchEnabled, cameraInstance) {
        val cam = cameraInstance ?: return@LaunchedEffect
        if (cam.cameraInfo.hasFlashUnit()) {
            cam.cameraControl.enableTorch(uiState.torchEnabled)
        }
    }

    LaunchedEffect(uiState.currentZoomRatio, cameraInstance) {
        val cam = cameraInstance ?: return@LaunchedEffect
        cam.cameraControl.setZoomRatio(uiState.currentZoomRatio)
    }

    LaunchedEffect(focusTapOffset) {
        if (focusTapOffset != null) {
            delay(900)
            focusTapOffset = null
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(uiState.minZoomRatio, uiState.maxZoomRatio) {
                detectTransformGestures { _, _, zoomChange, _ ->
                    if (zoomChange != 1f) {
                        val updated = (uiState.currentZoomRatio * zoomChange)
                            .coerceIn(uiState.minZoomRatio, uiState.maxZoomRatio)
                        onZoomChanged(updated)
                    }
                }
            }
            .pointerInput(uiState.detectedCandidates) {
                detectTapGestures { tapOffset ->
                    // Check if user tapped one of the multi-code bounding boxes first
                    if (uiState.detectedCandidates.size > 1) {
                        val normX = tapOffset.x / size.width.coerceAtLeast(1)
                        val normY = tapOffset.y / size.height.coerceAtLeast(1)
                        val hit = uiState.detectedCandidates.firstOrNull { candidate ->
                            val box = candidate.boundingBox ?: return@firstOrNull false
                            normX in (box.left - 0.04f)..(box.right + 0.04f) &&
                                normY in (box.top - 0.04f)..(box.bottom + 0.04f)
                        }
                        if (hit != null) {
                            onSelectCandidate(hit)
                            return@detectTapGestures
                        }
                    }

                    // Otherwise perform CameraX Tap-to-Focus
                    val cam = cameraInstance ?: return@detectTapGestures
                    focusTapOffset = tapOffset
                    try {
                        val point = previewView.meteringPointFactory.createPoint(tapOffset.x, tapOffset.y)
                        val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF)
                            .build()
                        cam.cameraControl.startFocusAndMetering(action)
                    } catch (_: Exception) {
                        // Ignore focus errors on fixed-focus lenses
                    }
                }
            }
    ) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier
                .fillMaxSize()
                .testTag("camera_preview_view")
        )

        val infiniteTransition = rememberInfiniteTransition(label = "scanner_pulse")
        val pulseScale by infiniteTransition.animateFloat(
            initialValue = 0.98f,
            targetValue = 1.03f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1200),
                repeatMode = RepeatMode.Reverse
            ),
            label = "reticle_scale"
        )
        val pulseAlpha by infiniteTransition.animateFloat(
            initialValue = 0.65f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1200),
                repeatMode = RepeatMode.Reverse
            ),
            label = "reticle_alpha"
        )

        val activeAccentColor = when {
            uiState.detectedCandidates.isNotEmpty() -> EmeraldSuccess
            batchMode -> EmeraldSuccess
            else -> IndigoPrimary
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasW = size.width
            val canvasH = size.height
            val baseFrameSize = min(canvasW, canvasH) * 0.68f
            val frameSize = baseFrameSize * pulseScale
            val left = (canvasW - frameSize) / 2f
            val top = (canvasH - frameSize) / 2f - (canvasH * 0.04f)
            val right = left + frameSize
            val bottom = top + frameSize
            val cornerRadiusPx = 28.dp.toPx()

            // Semi-transparent vignette outside the scanning frame
            val outerPath = Path().apply {
                addRect(Rect(0f, 0f, canvasW, canvasH))
            }
            val cutoutPath = Path().apply {
                addRoundRect(
                    RoundRect(
                        rect = Rect(left, top, right, bottom),
                        cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx)
                    )
                )
            }
            val scrimPath = Path.combine(PathOperation.Difference, outerPath, cutoutPath)
            drawPath(
                path = scrimPath,
                color = Color(0x880B1020)
            )

            // Subtle frame outline
            drawRoundRect(
                color = Color.White.copy(alpha = 0.22f),
                topLeft = Offset(left, top),
                size = Size(frameSize, frameSize),
                cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
                style = Stroke(width = 1.5.dp.toPx())
            )

            // Four highlighted corner brackets
            val bracketLen = 36.dp.toPx()
            val strokeW = 4.5.dp.toPx()
            val bracketColor = activeAccentColor.copy(alpha = pulseAlpha)

            // Top-Left Bracket
            val tlPath = Path().apply {
                moveTo(left, top + bracketLen)
                lineTo(left, top + cornerRadiusPx * 0.6f)
                quadraticTo(left, top, left + cornerRadiusPx * 0.6f, top)
                lineTo(left + bracketLen, top)
            }
            drawPath(tlPath, color = bracketColor, style = Stroke(width = strokeW, cap = StrokeCap.Round))

            // Top-Right Bracket
            val trPath = Path().apply {
                moveTo(right - bracketLen, top)
                lineTo(right - cornerRadiusPx * 0.6f, top)
                quadraticTo(right, top, right, top + cornerRadiusPx * 0.6f)
                lineTo(right, top + bracketLen)
            }
            drawPath(trPath, color = bracketColor, style = Stroke(width = strokeW, cap = StrokeCap.Round))

            // Bottom-Left Bracket
            val blPath = Path().apply {
                moveTo(left, bottom - bracketLen)
                lineTo(left, bottom - cornerRadiusPx * 0.6f)
                quadraticTo(left, bottom, left + cornerRadiusPx * 0.6f, bottom)
                lineTo(left + bracketLen, bottom)
            }
            drawPath(blPath, color = bracketColor, style = Stroke(width = strokeW, cap = StrokeCap.Round))

            // Bottom-Right Bracket
            val brPath = Path().apply {
                moveTo(right - bracketLen, bottom)
                lineTo(right - cornerRadiusPx * 0.6f, bottom)
                quadraticTo(right, bottom, right, bottom - cornerRadiusPx * 0.6f)
                lineTo(right, bottom - bracketLen)
            }
            drawPath(brPath, color = bracketColor, style = Stroke(width = strokeW, cap = StrokeCap.Round))

            // Subtle center optical target crosshair (not a fake laser line)
            val centerX = (left + right) / 2f
            val centerY = (top + bottom) / 2f
            val crossHalf = 8.dp.toPx()
            drawLine(
                color = Color.White.copy(alpha = 0.35f),
                start = Offset(centerX - crossHalf, centerY),
                end = Offset(centerX + crossHalf, centerY),
                strokeWidth = 1.5.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawLine(
                color = Color.White.copy(alpha = 0.35f),
                start = Offset(centerX, centerY - crossHalf),
                end = Offset(centerX, centerY + crossHalf),
                strokeWidth = 1.5.dp.toPx(),
                cap = StrokeCap.Round
            )

            // Draw interactive bounding boxes when multiple codes are detected
            if (uiState.detectedCandidates.size > 1) {
                uiState.detectedCandidates.forEach { candidate ->
                    val box = candidate.boundingBox ?: return@forEach
                    val isSelected = candidate.id == uiState.selectedCandidate?.id
                    val boxColor = if (isSelected) IndigoPrimary else AmberWarning
                    val bLeft = box.left * canvasW
                    val bTop = box.top * canvasH
                    val bWidth = ((box.right - box.left) * canvasW).coerceAtLeast(48.dp.toPx())
                    val bHeight = ((box.bottom - box.top) * canvasH).coerceAtLeast(48.dp.toPx())

                    drawRoundRect(
                        color = boxColor.copy(alpha = if (isSelected) 0.28f else 0.16f),
                        topLeft = Offset(bLeft, bTop),
                        size = Size(bWidth, bHeight),
                        cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx())
                    )
                    drawRoundRect(
                        color = boxColor,
                        topLeft = Offset(bLeft, bTop),
                        size = Size(bWidth, bHeight),
                        cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx()),
                        style = Stroke(width = if (isSelected) 3.5.dp.toPx() else 2.dp.toPx())
                    )
                }
            }

            // Draw Tap-to-Focus ring if active
            focusTapOffset?.let { tap ->
                drawCircle(
                    color = IndigoPrimary.copy(alpha = 0.85f),
                    radius = 28.dp.toPx(),
                    center = tap,
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }

        // Instruction Pill below the reticle
        val instructionOffsetY = (maxHeight.value * 0.72f).dp
        Surface(
            shape = RoundedCornerShape(50),
            color = Color(0xCC0B1020),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset { IntOffset(0, instructionOffsetY.roundToPx()) }
                .padding(horizontal = 24.dp)
        ) {
            Text(
                text = if (uiState.detectedCandidates.size > 1) {
                    stringResource(R.string.scanner_status_multi_code, uiState.detectedCandidates.size)
                } else {
                    stringResource(R.string.scanner_instruction)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(horizontal = 18.dp, vertical = 10.dp)
                    .testTag("scanner_instruction_text")
            )
        }
    }
}

@Composable
private fun ScannerTopBar(
    torchEnabled: Boolean,
    hasFlashUnit: Boolean,
    batchMode: Boolean,
    isPaused: Boolean,
    multiCodeCount: Int,
    onToggleTorch: () -> Unit,
    onToggleBatchMode: () -> Unit,
    onOpenQrGenerator: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 600.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Status Indicator Pill
        Surface(
            shape = RoundedCornerShape(50),
            color = Color(0xCC151B2E),
            modifier = Modifier.border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.12f),
                shape = RoundedCornerShape(50)
            )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                multiCodeCount > 1 -> AmberWarning
                                batchMode -> EmeraldSuccess
                                isPaused -> AmberWarning
                                else -> EmeraldSuccess
                            }
                        )
                )
                Text(
                    text = when {
                        multiCodeCount > 1 -> stringResource(R.string.scanner_status_paused)
                        batchMode -> stringResource(R.string.scanner_status_batch)
                        isPaused -> stringResource(R.string.scanner_status_paused)
                        else -> stringResource(R.string.scanner_status_ready)
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White
                )
            }
        }

        // Action Controls: Batch Mode, Create QR, Torch
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = batchMode,
                onClick = onToggleBatchMode,
                label = { Text(stringResource(R.string.scanner_batch_toggle)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.BurstMode,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = Color(0xCC151B2E),
                    labelColor = Color.White,
                    iconColor = Color.White,
                    selectedContainerColor = IndigoPrimary,
                    selectedLabelColor = Color.White,
                    selectedLeadingIconColor = Color.White
                ),
                border = null,
                modifier = Modifier.testTag("scanner_batch_mode_chip")
            )

            IconButton(
                onClick = onOpenQrGenerator,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = Color(0xCC151B2E),
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .size(48.dp)
                    .testTag("scanner_open_qr_gen_button")
            ) {
                Icon(
                    imageVector = Icons.Default.QrCode2,
                    contentDescription = stringResource(R.string.scanner_create_qr_button)
                )
            }

            IconButton(
                onClick = onToggleTorch,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = if (torchEnabled) AmberWarning else Color(0xCC151B2E),
                    contentColor = if (torchEnabled) Color(0xFF0B1020) else Color.White.copy(
                        alpha = if (hasFlashUnit) 1f else 0.45f
                    )
                ),
                modifier = Modifier
                    .size(48.dp)
                    .testTag("scanner_torch_button")
            ) {
                Icon(
                    imageVector = if (torchEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                    contentDescription = if (torchEnabled) {
                        stringResource(R.string.scanner_torch_on)
                    } else {
                        stringResource(R.string.scanner_torch_off)
                    }
                )
            }
        }
    }
}

@Composable
private fun ScannerBottomControlsBar(
    hasCameraPermission: Boolean,
    currentZoom: Float,
    maxZoom: Float,
    onSetZoom: (Float) -> Unit,
    onPickFromGallery: () -> Unit,
    onOpenQrGenerator: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color(0xD9151B2E),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.1f),
                shape = RoundedCornerShape(24.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledTonalButton(
                onClick = onPickFromGallery,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = IndigoPrimary,
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                modifier = Modifier
                    .height(48.dp)
                    .testTag("scanner_gallery_button")
            ) {
                Icon(
                    imageVector = Icons.Default.PhotoLibrary,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.scanner_gallery_button),
                    style = MaterialTheme.typography.labelLarge
                )
            }

            if (hasCameraPermission && maxZoom > 1.05f) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val zoomSteps = remember(maxZoom) {
                        buildList {
                            add(1.0f)
                            if (maxZoom >= 2.0f) add(2.0f)
                            if (maxZoom >= 4.0f) add(min(4.0f, maxZoom))
                        }
                    }
                    zoomSteps.forEach { step ->
                        val isSelected = kotlin.math.abs(currentZoom - step) < 0.35f
                        Surface(
                            shape = CircleShape,
                            color = if (isSelected) IndigoPrimary else Color(0xFF1E2640),
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .clickable { onSetZoom(step) }
                                .testTag("scanner_zoom_${step.toInt()}x")
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${step.toInt()}x",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            } else {
                OutlinedButton(
                    onClick = onOpenQrGenerator,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    modifier = Modifier.height(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCode2,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.scanner_create_qr_button))
                }
            }
        }
    }
}

@Composable
private fun MultiCodeSelectionCard(
    candidates: List<DetectedBarcodeCandidate>,
    selectedCandidate: DetectedBarcodeCandidate,
    onSelectCandidate: (DetectedBarcodeCandidate) -> Unit,
    onOpenSelected: () -> Unit,
    onResumeScanning: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xF2151B2E)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("multi_code_selection_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.scanner_status_multi_code, candidates.size),
                    style = MaterialTheme.typography.titleSmall,
                    color = AmberWarning
                )
                TextButton(onClick = onResumeScanning) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.scanner_resume_scanning))
                }
            }

            // Quick horizontal chips for each detected code
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                candidates.take(4).forEachIndexed { index, candidate ->
                    val isSelected = candidate.id == selectedCandidate.id
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelectCandidate(candidate) },
                        label = {
                            Text(
                                text = "#${index + 1} ${candidate.format.displayName}",
                                maxLines = 1
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = IndigoPrimary,
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFF1E2640),
                            labelColor = Color.White
                        ),
                        border = null
                    )
                }
            }

            // Selected candidate preview row
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF0B1020),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = selectedCandidate.parsedContent.category.accentColor().copy(alpha = 0.2f),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = selectedCandidate.parsedContent.category.iconVector(),
                                contentDescription = null,
                                tint = selectedCandidate.parsedContent.category.accentColor()
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = selectedCandidate.parsedContent.category.localizedLabel(),
                            style = MaterialTheme.typography.labelMedium,
                            color = IndigoPrimary
                        )
                        Text(
                            text = selectedCandidate.rawValue,
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = JetBrainsMonoFamily,
                            color = Color.White,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Button(
                onClick = onOpenSelected,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("multi_code_open_button")
            ) {
                Text(stringResource(R.string.scanner_open_selected))
            }
        }
    }
}

@Composable
private fun BatchSessionFloatingCard(
    totalCount: Int,
    latestTitle: String,
    latestFormat: String,
    onOpenLatest: () -> Unit,
    onViewHistory: () -> Unit,
    onClearSession: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xF2151B2E)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("batch_session_card")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = EmeraldSuccess,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = stringResource(R.string.scanner_batch_count_title, totalCount),
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White
                    )
                }
                TextButton(onClick = onClearSession) {
                    Text(stringResource(R.string.scanner_batch_clear))
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF0B1020))
                    .clickable(onClick = onOpenLatest)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = latestTitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = latestFormat,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF94A3B8)
                    )
                }
                TextButton(onClick = onViewHistory) {
                    Text(stringResource(R.string.scanner_batch_view_history))
                }
            }
        }
    }
}

@Composable
private fun ScannerPermissionOrErrorFallback(
    hasCameraPermission: Boolean,
    permissionRequestedOnce: Boolean,
    cameraInitError: Boolean,
    onRequestPermission: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onRetryCamera: () -> Unit,
    onPickFromGallery: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF151B2E)),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 440.dp)
                .testTag("scanner_permission_card")
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = IndigoPrimary.copy(alpha = 0.18f),
                    modifier = Modifier.size(68.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = IndigoPrimary,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }

                Text(
                    text = if (cameraInitError) {
                        stringResource(R.string.scanner_camera_error)
                    } else {
                        stringResource(R.string.permission_camera_title)
                    },
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = when {
                        cameraInitError -> stringResource(R.string.scanner_camera_error)
                        permissionRequestedOnce -> stringResource(R.string.permission_camera_permanently_denied)
                        else -> stringResource(R.string.permission_camera_rationale)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF94A3B8),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                if (cameraInitError) {
                    Button(
                        onClick = onRetryCamera,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("scanner_retry_camera_button")
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.scanner_camera_retry))
                    }
                } else if (!hasCameraPermission) {
                    Button(
                        onClick = onRequestPermission,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("permission_grant_button")
                    ) {
                        Text(stringResource(R.string.permission_grant_button))
                    }

                    if (permissionRequestedOnce) {
                        OutlinedButton(
                            onClick = onOpenAppSettings,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("permission_settings_button")
                        ) {
                            Icon(imageVector = Icons.Default.Settings, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.permission_open_settings))
                        }
                    }
                }

                FilledTonalButton(
                    onClick = onPickFromGallery,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("permission_gallery_fallback_button")
                ) {
                    Icon(imageVector = Icons.Default.PhotoLibrary, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.permission_use_gallery_instead))
                }
            }
        }
    }
}
