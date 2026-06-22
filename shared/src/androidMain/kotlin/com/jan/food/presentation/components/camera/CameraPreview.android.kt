package com.jan.food.presentation.components.camera

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@Composable
actual fun CameraPreview(
    modifier: Modifier,
    onBarcodeScanned: (String?) -> Unit,
    focusRequest: FocusRequest?,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> hasPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    // Barcode scanning pipeline: raw detections feed the debouncer, which surfaces the callback.
    val currentOnScanned by rememberUpdatedState(onBarcodeScanned)
    val scope = rememberCoroutineScope()
    val debouncer = remember { BarcodeDebouncer(scope) { currentOnScanned(it) } }
    val scanner = remember {
        BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(
                    Barcode.FORMAT_EAN_13,
                    Barcode.FORMAT_EAN_8,
                    Barcode.FORMAT_UPC_A,
                    Barcode.FORMAT_UPC_E,
                )
                .build(),
        )
    }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose {
            analysisExecutor.shutdown()
            scanner.close()
        }
    }

    // Held so the focus effect can reach the PreviewView's metering factory and the bound camera,
    // both of which are otherwise scoped to the AndroidView factory / provider listener.
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }
    val cameraHolder = remember { CameraHolder() }

    if (!hasPermission) return

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder()
                    .setResolutionSelector(
                        ResolutionSelector.Builder()
                            .setResolutionStrategy(ResolutionStrategy.HIGHEST_AVAILABLE_STRATEGY)
                            .build(),
                    )
                    .build()
                    .also { it.surfaceProvider = previewView.surfaceProvider }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { analysis ->
                        analysis.setAnalyzer(
                            analysisExecutor,
                            BarcodeAnalyzer(scanner) { debouncer.onDetected(it) },
                        )
                    }

                cameraProvider.unbindAll()
                cameraHolder.camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageAnalysis,
                )
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        },
    )

    LaunchedEffect(focusRequest) {
        val request = focusRequest ?: return@LaunchedEffect
        val camera = cameraHolder.camera ?: return@LaunchedEffect
        if (previewView.width == 0 || previewView.height == 0) return@LaunchedEffect

        val point = previewView.meteringPointFactory.createPoint(
            request.x * previewView.width,
            request.y * previewView.height,
        )
        val action = FocusMeteringAction.Builder(
            point,
            FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE,
        ).setAutoCancelDuration(AUTO_CANCEL_SECONDS, TimeUnit.SECONDS).build()
        camera.cameraControl.startFocusAndMetering(action)
    }
}

/** How long CameraX keeps the manual focus before reverting to continuous auto-focus. */
private const val AUTO_CANCEL_SECONDS = 3L

/** Mutable handle for the [Camera] bound asynchronously inside the provider listener. */
private class CameraHolder {
    var camera: Camera? = null
}

/** Feeds CameraX frames to ML Kit and reports the first decoded barcode of each frame. */
private class BarcodeAnalyzer(
    private val scanner: BarcodeScanner,
    private val onBarcode: (String) -> Unit,
) : ImageAnalysis.Analyzer {

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }
        val input = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        scanner.process(input)
            .addOnSuccessListener { barcodes ->
                barcodes.firstNotNullOfOrNull { it.rawValue }?.let(onBarcode)
            }
            .addOnCompleteListener { imageProxy.close() }
    }
}
