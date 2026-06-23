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
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
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
    blurred: Boolean,
    tapToFocusEnabled: Boolean,
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

    // The PreviewView and the bound Camera are held outside the AndroidView factory so the tap
    // handler can reach the metering-point factory and CameraControl.
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }
    val cameraHolder = remember { CameraHolder() }
    var focusReticle by remember { mutableStateOf<FocusReticleState?>(null) }

    if (!hasPermission) return

    // Compose's blur works over the TextureView-backed PreviewView, so blur here natively.
    val blurRadius by animateDpAsState(
        targetValue = if (blurred) BlurRadius else 0.dp,
        label = "cameraBlur",
    )

    // Re-keyed on tapToFocusEnabled so the tap handler is torn down when focusing is disabled.
    val tapModifier = if (tapToFocusEnabled) {
        Modifier.pointerInput(Unit) {
            detectTapGestures { tap ->
                focusAt(previewView, cameraHolder.camera, tap)
                // tapCount keeps the reticle animation distinct per tap, even on the same spot.
                focusReticle = FocusReticleState(tap, (focusReticle?.tapCount ?: 0L) + 1L)
            }
        }
    } else {
        Modifier
    }

    Box(modifier = modifier.then(tapModifier)) {
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .blur(blurRadius, BlurredEdgeTreatment.Rectangle),
            factory = {
                val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
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
                }, ContextCompat.getMainExecutor(context))

                previewView
            },
        )

        focusReticle?.let { FocusReticle(it) }
    }
}

/** Focuses (and meters exposure) on the tapped point of the [previewView]. */
private fun focusAt(previewView: PreviewView, camera: Camera?, tap: Offset) {
    if (camera == null || previewView.width == 0 || previewView.height == 0) return
    // The tap is in the same pixel space as the PreviewView, which fills the gesture Box.
    val point = previewView.meteringPointFactory.createPoint(tap.x, tap.y)
    val action = FocusMeteringAction.Builder(
        point,
        FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE,
    ).setAutoCancelDuration(AUTO_CANCEL_SECONDS, TimeUnit.SECONDS).build()
    camera.cameraControl.startFocusAndMetering(action)
}

/** The tapped point plus a per-tap counter that restarts the reticle animation. */
private data class FocusReticleState(val center: Offset, val tapCount: Long)

/** White ring at the tapped point that fades in (150 ms) then out (150 ms) on each tap. */
@Composable
private fun FocusReticle(state: FocusReticleState) {
    val alpha = remember { Animatable(0f) }
    LaunchedEffect(state.tapCount) {
        alpha.snapTo(0f)
        alpha.animateTo(1f, tween(FADE_HALF_MILLIS))
        alpha.animateTo(0f, tween(FADE_HALF_MILLIS))
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        drawCircle(
            color = Color.White,
            radius = RETICLE_DIAMETER.toPx() / 2f,
            center = state.center,
            alpha = alpha.value,
            style = Stroke(width = RETICLE_STROKE.toPx()),
        )
    }
}

/** Blur radius applied to the feed while it is used as a backdrop. */
private val BlurRadius = 40.dp

private val RETICLE_DIAMETER = 64.dp
private val RETICLE_STROKE = 2.dp
private const val FADE_HALF_MILLIS = 150

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
