package com.jan.food.presentation.components.camera

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Turns the raw per-frame barcode detections coming from a platform camera pipeline into the
 * "emit on change / null after a quiet period" signal that [CameraPreview] exposes.
 *
 * Feed every decoded frame into [onDetected]; the latest distinct barcode is reported once, and a
 * `null` is reported once no barcode has been seen for [timeoutMillis]. Detections are expected on a
 * single (main) thread, so no synchronization is performed.
 *
 * @param scope the [CoroutineScope] used to run the quiet-period timer.
 * @param timeoutMillis how long without a detection before `null` is emitted.
 * @param onBarcode invoked with the decoded barcode, or `null` when nothing has been visible.
 */
internal class BarcodeDebouncer(
    private val scope: CoroutineScope,
    private val timeoutMillis: Long = 3_000L,
    private val onBarcode: (String?) -> Unit,
) {
    private var lastEmitted: String? = null
    private var timeoutJob: Job? = null

    /** Called for every frame in which a barcode was decoded. */
    fun onDetected(barcode: String) {
        timeoutJob?.cancel()
        if (barcode != lastEmitted) {
            lastEmitted = barcode
            onBarcode(barcode)
        }
        timeoutJob = scope.launch {
            delay(timeoutMillis)
            if (lastEmitted != null) {
                lastEmitted = null
                onBarcode(null)
            }
        }
    }
}
