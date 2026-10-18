package com.example.core.scanner

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.Rect
import android.net.Uri
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.exifinterface.media.ExifInterface
import com.example.core.model.BarcodeFormatType
import com.example.core.model.DetectedBarcodeCandidate
import com.example.core.model.NormalizedBoundingBox
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.Closeable
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.hypot
import kotlin.math.max

class MlKitBarcodeAnalyzer(
    private val isPausedProvider: () -> Boolean,
    private val onBarcodesDetected: (List<DetectedBarcodeCandidate>) -> Unit
) : ImageAnalysis.Analyzer, Closeable {

    private val scanner: BarcodeScanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
            .build()
    )

    private val isProcessing = AtomicBoolean(false)

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        if (isPausedProvider()) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        if (!isProcessing.compareAndSet(false, true)) {
            imageProxy.close()
            return
        }

        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        val frameWidth = if (rotationDegrees == 90 || rotationDegrees == 270) {
            imageProxy.height
        } else {
            imageProxy.width
        }
        val frameHeight = if (rotationDegrees == 90 || rotationDegrees == 270) {
            imageProxy.width
        } else {
            imageProxy.height
        }

        val inputImage = InputImage.fromMediaImage(mediaImage, rotationDegrees)

        scanner.process(inputImage)
            .addOnSuccessListener { barcodes ->
                if (!isPausedProvider() && barcodes.isNotEmpty()) {
                    val candidates = mapBarcodesToCandidates(
                        barcodes = barcodes,
                        width = frameWidth,
                        height = frameHeight
                    )
                    if (candidates.isNotEmpty()) {
                        onBarcodesDetected(candidates)
                    }
                }
            }
            .addOnCompleteListener {
                isProcessing.set(false)
                imageProxy.close()
            }
    }

    override fun close() {
        scanner.close()
    }

    companion object {
        suspend fun scanFromUri(
            context: Context,
            uri: Uri
        ): Result<List<DetectedBarcodeCandidate>> = withContext(Dispatchers.IO) {
            var scannerInstance: BarcodeScanner? = null
            try {
                val bitmap = decodeSampledBitmapWithOrientation(context, uri)
                    ?: return@withContext Result.failure(IllegalArgumentException("Cannot decode bitmap"))

                val options = BarcodeScannerOptions.Builder()
                    .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                    .build()
                scannerInstance = BarcodeScanning.getClient(options)

                val inputImage = InputImage.fromBitmap(bitmap, 0)
                val barcodes = scannerInstance.process(inputImage).await()
                val candidates = mapBarcodesToCandidates(
                    barcodes = barcodes,
                    width = bitmap.width,
                    height = bitmap.height
                )
                Result.success(candidates)
            } catch (e: Exception) {
                Result.failure(e)
            } finally {
                scannerInstance?.close()
            }
        }

        fun mapBarcodesToCandidates(
            barcodes: List<Barcode>,
            width: Int,
            height: Int
        ): List<DetectedBarcodeCandidate> {
            val safeW = max(width, 1).toFloat()
            val safeH = max(height, 1).toFloat()

            val mapped = barcodes.mapNotNull { barcode ->
                val raw = barcode.rawValue ?: barcode.displayValue ?: return@mapNotNull null
                if (raw.isBlank()) return@mapNotNull null

                val format = mapMlKitFormat(barcode.format)
                val parsed = SmartContentParser.parse(raw, format)
                val normalizedBox = barcode.boundingBox?.toNormalizedBox(safeW, safeH)
                val id = "${format.name}_${raw.hashCode()}_${normalizedBox?.centerX ?: 0f}"

                DetectedBarcodeCandidate(
                    id = id,
                    rawValue = raw,
                    format = format,
                    parsedContent = parsed,
                    boundingBox = normalizedBox
                )
            }

            // Sort so the code closest to the center of the frame is first (Primary code)
            return mapped.sortedBy { candidate ->
                val box = candidate.boundingBox
                if (box != null) {
                    hypot((box.centerX - 0.5f).toDouble(), (box.centerY - 0.5f).toDouble())
                } else {
                    1.0
                }
            }.distinctBy { "${it.format.name}:${it.rawValue}" }
        }

        private fun Rect.toNormalizedBox(width: Float, height: Float): NormalizedBoundingBox {
            return NormalizedBoundingBox(
                left = (left / width).coerceIn(0f, 1f),
                top = (top / height).coerceIn(0f, 1f),
                right = (right / width).coerceIn(0f, 1f),
                bottom = (bottom / height).coerceIn(0f, 1f)
            )
        }

        fun mapMlKitFormat(mlKitFormat: Int): BarcodeFormatType {
            return when (mlKitFormat) {
                Barcode.FORMAT_QR_CODE -> BarcodeFormatType.QR_CODE
                Barcode.FORMAT_DATA_MATRIX -> BarcodeFormatType.DATA_MATRIX
                Barcode.FORMAT_PDF417 -> BarcodeFormatType.PDF417
                Barcode.FORMAT_AZTEC -> BarcodeFormatType.AZTEC
                Barcode.FORMAT_EAN_13 -> BarcodeFormatType.EAN_13
                Barcode.FORMAT_EAN_8 -> BarcodeFormatType.EAN_8
                Barcode.FORMAT_UPC_A -> BarcodeFormatType.UPC_A
                Barcode.FORMAT_UPC_E -> BarcodeFormatType.UPC_E
                Barcode.FORMAT_CODE_128 -> BarcodeFormatType.CODE_128
                Barcode.FORMAT_CODE_39 -> BarcodeFormatType.CODE_39
                Barcode.FORMAT_CODE_93 -> BarcodeFormatType.CODE_93
                Barcode.FORMAT_CODABAR -> BarcodeFormatType.CODABAR
                Barcode.FORMAT_ITF -> BarcodeFormatType.ITF
                else -> BarcodeFormatType.UNKNOWN
            }
        }

        private fun decodeSampledBitmapWithOrientation(
            context: Context,
            uri: Uri,
            maxDimension: Int = 2048
        ): Bitmap? {
            val resolver = context.contentResolver
            val boundsOptions = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            resolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOptions)
            }
            if (boundsOptions.outWidth <= 0 || boundsOptions.outHeight <= 0) return null

            var sampleSize = 1
            while (
                boundsOptions.outWidth / sampleSize > maxDimension ||
                boundsOptions.outHeight / sampleSize > maxDimension
            ) {
                sampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            val rawBitmap = resolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            } ?: return null

            val rotation = resolver.openInputStream(uri)?.use { stream ->
                val exif = ExifInterface(stream)
                when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
            } ?: 0f

            if (rotation == 0f) return rawBitmap

            val matrix = Matrix().apply { postRotate(rotation) }
            return Bitmap.createBitmap(
                rawBitmap,
                0,
                0,
                rawBitmap.width,
                rawBitmap.height,
                matrix,
                true
            )
        }
    }
}
