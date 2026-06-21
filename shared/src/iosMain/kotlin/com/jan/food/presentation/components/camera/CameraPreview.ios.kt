package com.jan.food.presentation.components.camera

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readValue
import kotlinx.cinterop.useContents
import platform.AVFoundation.AVCaptureConnection
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVCaptureDeviceFormat
import platform.AVFoundation.AVCaptureDeviceInput
import platform.AVFoundation.AVCaptureDevicePositionBack
import platform.AVFoundation.AVCaptureDeviceTypeBuiltInWideAngleCamera
import platform.AVFoundation.AVCaptureMetadataOutput
import platform.AVFoundation.AVCaptureMetadataOutputObjectsDelegateProtocol
import platform.AVFoundation.AVCaptureOutput
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
import platform.AVFoundation.authorizationStatusForMediaType
import platform.AVFoundation.AVAuthorizationStatusAuthorized
import platform.AVFoundation.AVAuthorizationStatusNotDetermined
import platform.AVFoundation.defaultDeviceWithDeviceType
import platform.AVFoundation.requestAccessForMediaType
import platform.CoreGraphics.CGRectZero
import platform.CoreMedia.CMTimeMake
import platform.CoreMedia.CMVideoFormatDescriptionGetDimensions
import platform.QuartzCore.CATransaction
import platform.QuartzCore.kCATransactionDisableActions
import platform.UIKit.UIView
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

        onDispose {
            dispatchBackground { session.stopRunning() }
        }
    }

    UIKitView(
        factory = { CameraContainerView(previewLayer) },
        modifier = modifier,
    )
}

/** [UIView] that keeps the capture preview layer sized to its bounds across layout passes. */
@OptIn(ExperimentalForeignApi::class)
private class CameraContainerView(
    private val previewLayer: AVCaptureVideoPreviewLayer,
) : UIView(frame = CGRectZero.readValue()) {

    init {
        layer.addSublayer(previewLayer)
    }

    override fun layoutSubviews() {
        super.layoutSubviews()
        // Resize without the implicit animation so the feed tracks rotations/resizes cleanly.
        CATransaction.begin()
        CATransaction.setValue(true, kCATransactionDisableActions)
        previewLayer.setFrame(bounds)
        CATransaction.commit()
    }
}

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

        val device = AVCaptureDevice.defaultDeviceWithDeviceType(
            deviceType = AVCaptureDeviceTypeBuiltInWideAngleCamera,
            mediaType = AVMediaTypeVideo,
            position = AVCaptureDevicePositionBack,
        )
        val input = device?.let { AVCaptureDeviceInput.deviceInputWithDevice(it, null) }
        if (input != null && session.canAddInput(input)) {
            session.addInput(input)
        }

        device?.let { configureTargetFrameRate(it) }

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

/** Target preview frame rate; applied only when the device has a format that supports it. */
private const val TARGET_FPS = 60.0

/**
 * Selects the highest-resolution [AVCaptureDeviceFormat] that supports [TARGET_FPS] and pins the
 * device to that frame rate. No-op (default ~30 fps) when no format advertises the target rate.
 */
@OptIn(ExperimentalForeignApi::class)
private fun configureTargetFrameRate(device: AVCaptureDevice) {
    val format = device.formats
        .filterIsInstance<AVCaptureDeviceFormat>()
        .filter { fmt ->
            fmt.videoSupportedFrameRateRanges
                .filterIsInstance<AVFrameRateRange>()
                .any { it.maxFrameRate >= TARGET_FPS }
        }
        .maxByOrNull { it.pixelCount() }
        ?: return

    if (!device.lockForConfiguration(null)) return
    device.activeFormat = format
    val duration = CMTimeMake(value = 1, timescale = TARGET_FPS.toInt())
    device.setActiveVideoMinFrameDuration(duration)
    device.setActiveVideoMaxFrameDuration(duration)
    device.unlockForConfiguration()
}

/** Total pixel count of the format's video dimensions, used to pick the sharpest 60-fps format. */
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
