package com.jan.food.presentation.components.camera

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readValue
import kotlinx.cinterop.useContents
import platform.AVFoundation.AVAuthorizationStatusAuthorized
import platform.AVFoundation.AVAuthorizationStatusNotDetermined
import platform.AVFoundation.AVCaptureConnection
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVCaptureDeviceFormat
import platform.AVFoundation.AVCaptureDeviceInput
import platform.AVFoundation.AVCaptureDevicePositionBack
import platform.AVFoundation.AVCaptureDeviceType
import platform.AVFoundation.AVCaptureDeviceTypeBuiltInDualWideCamera
import platform.AVFoundation.AVCaptureDeviceTypeBuiltInTripleCamera
import platform.AVFoundation.AVCaptureDeviceTypeBuiltInWideAngleCamera
import platform.AVFoundation.AVCaptureMetadataOutput
import platform.AVFoundation.AVCaptureMetadataOutputObjectsDelegateProtocol
import platform.AVFoundation.AVCaptureOutput
import platform.AVFoundation.AVCapturePrimaryConstituentDeviceRestrictedSwitchingBehaviorConditionNone
import platform.AVFoundation.AVCapturePrimaryConstituentDeviceSwitchingBehaviorAuto
import platform.AVFoundation.AVCapturePrimaryConstituentDeviceSwitchingBehaviorUnsupported
import platform.AVFoundation.AVCaptureSession
import platform.AVFoundation.AVCaptureSessionPresetInputPriority
import platform.AVFoundation.AVCaptureVideoPreviewLayer
import platform.AVFoundation.AVFrameRateRange
import platform.AVFoundation.AVLayerVideoGravityResizeAspectFill
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.AVMetadataMachineReadableCodeObject
import platform.AVFoundation.AVMetadataObjectTypeEAN13Code
import platform.AVFoundation.AVMetadataObjectTypeEAN8Code
import platform.AVFoundation.AVMetadataObjectTypeUPCECode
import platform.AVFoundation.activePrimaryConstituentDeviceSwitchingBehavior
import platform.AVFoundation.authorizationStatusForMediaType
import platform.AVFoundation.defaultDeviceWithDeviceType
import platform.AVFoundation.isVirtualDevice
import platform.AVFoundation.requestAccessForMediaType
import platform.AVFoundation.setPrimaryConstituentDeviceSwitchingBehavior
import platform.CoreGraphics.CGRectZero
import platform.CoreMedia.CMTimeMake
import platform.CoreMedia.CMVideoFormatDescriptionGetDimensions
import platform.QuartzCore.CATransaction
import platform.QuartzCore.kCATransactionDisableActions
import platform.UIKit.UIBlurEffect
import platform.UIKit.UIBlurEffectStyle
import platform.UIKit.UIView
import platform.UIKit.UIVisualEffectView
import platform.darwin.DISPATCH_QUEUE_PRIORITY_DEFAULT
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_global_queue
import platform.darwin.dispatch_get_main_queue

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun CameraPreview(
    modifier: Modifier,
    onBarcodeScanned: (String?) -> Unit,
    blurred: Boolean,
) {
    val session = remember { AVCaptureSession() }
    val previewLayer = remember { AVCaptureVideoPreviewLayer(session = session) }

    // Barcode scanning pipeline: the metadata delegate feeds the debouncer, which surfaces the
    // callback. The delegate is remembered so Kotlin/Native ARC doesn't collect it while
    // AVFoundation holds it weakly.
    val currentOnScanned by rememberUpdatedState(onBarcodeScanned)
    val scope = rememberCoroutineScope()
    val debouncer = remember { BarcodeDebouncer(scope) { currentOnScanned(it) } }
    val delegate = remember { BarcodeScanDelegate { debouncer.onDetected(it) } }

    DisposableEffect(Unit) {
        previewLayer.videoGravity = AVLayerVideoGravityResizeAspectFill

        when (AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo)) {
            AVAuthorizationStatusAuthorized -> startSession(session, delegate)
            AVAuthorizationStatusNotDetermined -> AVCaptureDevice.requestAccessForMediaType(
                AVMediaTypeVideo,
            ) { granted -> if (granted) startSession(session, delegate) }
            else -> Unit
        }

        onDispose { dispatchBackground { session.stopRunning() } }
    }

    UIKitView(
        factory = { CameraContainerView(previewLayer) },
        // Compose's blur can't reach the native preview layer, so blur natively over it instead.
        update = { view -> view.setBlurred(blurred) },
        modifier = modifier,
        // A passive display surface with no touch handling of its own.
        properties = UIKitInteropProperties(interactionMode = null),
    )
}

/**
 * [UIView] that keeps the capture preview layer sized to its bounds across layout passes and hosts
 * a native blur overlay ([UIVisualEffectView]) that fades in over the feed when used as a backdrop.
 */
@OptIn(ExperimentalForeignApi::class)
private class CameraContainerView(
    private val previewLayer: AVCaptureVideoPreviewLayer,
) : UIView(frame = CGRectZero.readValue()) {

    private val blurView = UIVisualEffectView(
        effect = UIBlurEffect.effectWithStyle(UIBlurEffectStyle.UIBlurEffectStyleRegular),
    ).apply { alpha = 0.0 }

    init {
        layer.addSublayer(previewLayer)
        // Added as a subview so it composites above the preview layer.
        addSubview(blurView)
    }

    override fun layoutSubviews() {
        super.layoutSubviews()
        // Resize without the implicit animation so the feed tracks rotations/resizes cleanly.
        CATransaction.begin()
        CATransaction.setValue(true, kCATransactionDisableActions)
        previewLayer.setFrame(bounds)
        blurView.setFrame(bounds)
        CATransaction.commit()
    }

    /** Fades the blur overlay in (true) or out (false) to match the requested backdrop state. */
    fun setBlurred(blurred: Boolean) {
        val target = if (blurred) 1.0 else 0.0
        if (blurView.alpha == target) return
        UIView.animateWithDuration(BLUR_FADE_SECONDS) { blurView.alpha = target }
    }
}

/** Duration of the blur fade-in/out, matching the screen's fade transition. */
private const val BLUR_FADE_SECONDS = 0.3

/** Receives decoded machine-readable codes from the capture session on the main queue. */
@OptIn(ExperimentalForeignApi::class)
private class BarcodeScanDelegate(
    private val onBarcode: (String) -> Unit,
) : NSObject(), AVCaptureMetadataOutputObjectsDelegateProtocol {

    override fun captureOutput(
        output: AVCaptureOutput,
        didOutputMetadataObjects: List<*>,
        fromConnection: AVCaptureConnection,
    ) {
        didOutputMetadataObjects
            .filterIsInstance<AVMetadataMachineReadableCodeObject>()
            .firstNotNullOfOrNull { it.stringValue }
            ?.let(onBarcode)
    }
}

/** Builds the capture session on a background queue: camera input + barcode metadata output. */
@OptIn(ExperimentalForeignApi::class)
private fun startSession(
    session: AVCaptureSession,
    delegate: AVCaptureMetadataOutputObjectsDelegateProtocol,
) {
    dispatchBackground {
        session.beginConfiguration()
        // InputPriority lets the manually selected device format/frame rate drive the session
        // instead of a preset overriding them.
        if (session.canSetSessionPreset(AVCaptureSessionPresetInputPriority)) {
            session.sessionPreset = AVCaptureSessionPresetInputPriority
        }

        val device = selectBackCamera()
        val input = device?.let { AVCaptureDeviceInput.deviceInputWithDevice(it, null) }
        if (input != null && session.canAddInput(input)) {
            session.addInput(input)
        }
        device?.let { configureBackCamera(it) }

        val metadataOutput = AVCaptureMetadataOutput()
        if (session.canAddOutput(metadataOutput)) {
            session.addOutput(metadataOutput)
            metadataOutput.setMetadataObjectsDelegate(delegate, dispatch_get_main_queue())
            // Supported types are only populated once the output is attached to the session.
            metadataOutput.metadataObjectTypes = listOf(
                AVMetadataObjectTypeEAN13Code,
                AVMetadataObjectTypeEAN8Code,
                AVMetadataObjectTypeUPCECode,
            )
        }

        session.commitConfiguration()
        session.startRunning()
    }
}

/**
 * Picks the back camera, preferring a multi-lens virtual device that includes the ultra-wide lens
 * (triple, then dual-wide) so the system can automatically switch to it for macro (close-up) focus,
 * exactly like the stock Camera app. Falls back to the plain wide-angle camera on phones without a
 * macro-capable lens.
 */
@OptIn(ExperimentalForeignApi::class)
private fun selectBackCamera(): AVCaptureDevice? {
    fun deviceOfType(type: AVCaptureDeviceType) = AVCaptureDevice.defaultDeviceWithDeviceType(
        deviceType = type,
        mediaType = AVMediaTypeVideo,
        position = AVCaptureDevicePositionBack,
    )
    return deviceOfType(AVCaptureDeviceTypeBuiltInTripleCamera)
        ?: deviceOfType(AVCaptureDeviceTypeBuiltInDualWideCamera)
        ?: deviceOfType(AVCaptureDeviceTypeBuiltInWideAngleCamera)
}

/** Target preview frame rate; applied only when a format supports it without disabling macro. */
private const val TARGET_FPS = 60.0

/**
 * Configures the chosen camera: enables automatic macro switching and pins the sharpest 60 fps
 * format — but never at the cost of macro. On a multi-lens device, if the high-frame-rate format
 * turns out to disable lens switching, the default format (~30 fps) is kept instead, since macro is
 * the more useful capability for scanning items up close.
 */
@OptIn(ExperimentalForeignApi::class)
private fun configureBackCamera(device: AVCaptureDevice) {
    if (!device.lockForConfiguration(null)) return

    if (device.isVirtualDevice()) {
        device.setPrimaryConstituentDeviceSwitchingBehavior(
            AVCapturePrimaryConstituentDeviceSwitchingBehaviorAuto,
            AVCapturePrimaryConstituentDeviceRestrictedSwitchingBehaviorConditionNone,
        )
    }

    val fastestFormat = device.formats
        .filterIsInstance<AVCaptureDeviceFormat>()
        .filter { format -> format.supportsFrameRate(TARGET_FPS) }
        .maxByOrNull { it.pixelCount() }

    if (fastestFormat != null) {
        val defaultFormat = device.activeFormat
        device.activeFormat = fastestFormat
        if (device.disabledMacroSwitching()) {
            device.activeFormat = defaultFormat
        } else {
            val duration = CMTimeMake(value = 1, timescale = TARGET_FPS.toInt())
            device.setActiveVideoMinFrameDuration(duration)
            device.setActiveVideoMaxFrameDuration(duration)
        }
    }

    device.unlockForConfiguration()
}

/** True once the active format has left a virtual device unable to auto-switch lenses (macro). */
@OptIn(ExperimentalForeignApi::class)
private fun AVCaptureDevice.disabledMacroSwitching(): Boolean =
    isVirtualDevice() &&
        activePrimaryConstituentDeviceSwitchingBehavior ==
        AVCapturePrimaryConstituentDeviceSwitchingBehaviorUnsupported

/** Whether any of the format's frame-rate ranges reaches [fps]. */
@OptIn(ExperimentalForeignApi::class)
private fun AVCaptureDeviceFormat.supportsFrameRate(fps: Double): Boolean =
    videoSupportedFrameRateRanges
        .filterIsInstance<AVFrameRateRange>()
        .any { it.maxFrameRate >= fps }

/** Total pixel count of the format's video dimensions, used to pick the sharpest format. */
@OptIn(ExperimentalForeignApi::class)
private fun AVCaptureDeviceFormat.pixelCount(): Long =
    CMVideoFormatDescriptionGetDimensions(formatDescription).useContents {
        width.toLong() * height.toLong()
    }

private fun dispatchBackground(block: () -> Unit) {
    dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT.toLong(), 0u)) {
        block()
    }
}
