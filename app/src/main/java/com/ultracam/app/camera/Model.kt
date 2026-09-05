package com.ultracam.app.camera

import android.hardware.camera2.CameraCharacteristics
import android.util.Size
import kotlin.math.roundToInt

/** A physical/logical camera exposed by the device. */
data class LensInfo(
    val cameraId: String,
    val facing: Int,
    val label: String,
    val focalEquiv35mm: Float,
    val megapixels: Float,
    val hasFlash: Boolean
) {
    val isFront: Boolean
        get() = facing == CameraCharacteristics.LENS_FACING_FRONT

    val focalText: String
        get() = "${if (focalEquiv35mm >= 10f) focalEquiv35mm.roundToInt() else focalEquiv35mm}MM"
}

/** A selectable still-capture resolution for a given lens. */
data class ResolutionOption(
    val size: Size,
    val label: String,
    val aspectLabel: String,
    val megapixels: Float
)

/** Full manual control block. Everything off = CameraX defaults. */
data class ManualSettings(
    val exposureOn: Boolean = false,
    val iso: Int = 400,
    val shutterNs: Long = 16_666_667L,
    val wbOn: Boolean = false,
    val kelvin: Int = 5600,
    val focusOn: Boolean = false,
    val diopters: Float = 0f
)

/** Snapshot of everything the UI needs to know about the active camera. */
data class CameraSnapshot(
    val lens: LensInfo,
    val zoomMin: Float,
    val zoomMax: Float,
    val evRange: IntRange,
    val evStep: Float,
    val evSupported: Boolean,
    val isoRange: IntRange,
    val shutterRange: LongRange,
    val minFocusDiopters: Float,
    val hasFlash: Boolean,
    val fpsOptions: List<Int>,
    val resolutions: List<ResolutionOption>,
    val currentResolution: ResolutionOption?,
    val hardware: List<Pair<String, String>>
)
