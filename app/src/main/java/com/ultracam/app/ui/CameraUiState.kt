package com.ultracam.app.ui

import androidx.camera.core.ImageCapture
import androidx.compose.ui.graphics.ImageBitmap
import android.net.Uri
import com.ultracam.app.camera.LensInfo
import com.ultracam.app.camera.ResolutionOption

enum class CaptureMode(val label: String) {
    CINEMATIC("CINEMATIC"),
    VIDEO("VIDEO"),
    PHOTO("PHOTO"),
    PORTRAIT("PORTRAIT"),
    PRO("PRO")
}

enum class FlashMode(val label: String, val captureMode: Int) {
    OFF("OFF", ImageCapture.FLASH_MODE_OFF),
    AUTO("AUTO", ImageCapture.FLASH_MODE_AUTO),
    ON("ON", ImageCapture.FLASH_MODE_ON);

    fun next(): FlashMode = when (this) {
        OFF -> AUTO
        AUTO -> ON
        ON -> OFF
    }
}

enum class GridMode(val label: String) {
    OFF("OFF"),
    THIRDS("3×3"),
    GOLDEN("PHI");

    fun next(): GridMode = when (this) {
        OFF -> THIRDS
        THIRDS -> GOLDEN
        GOLDEN -> OFF
    }
}

enum class CaptureQuality(val label: String, val mode: Int) {
    QUALITY("MAX QUALITY", ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY),
    SPEED("MIN LATENCY", ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
}

enum class FocusPhase { START, LOCKED, FAILED }

data class FocusFx(
    val x: Float,
    val y: Float,
    val id: Long,
    val phase: FocusPhase
)

enum class Haptic { TICK, CONFIRM, WARN }

enum class VideoResOption(val label: String, val width: Int, val height: Int) {
    RES_720P("720p", 1280, 720),
    RES_1080P("1080p", 1920, 1080),
    RES_4K("4K", 3840, 2160);

    fun next(): VideoResOption = when (this) {
        RES_720P -> RES_1080P
        RES_1080P -> RES_4K
        RES_4K -> RES_720P
    }
}

data class UiState(
    // camera lifecycle
    val cameraReady: Boolean = false,
    val cameraError: String? = null,

    // mode + manual block
    val mode: CaptureMode = CaptureMode.PHOTO,
    val aspectRatio: com.ultracam.app.camera.AspectRatioOption = com.ultracam.app.camera.AspectRatioOption.RATIO_4_3,
    val videoRes: VideoResOption = VideoResOption.RES_1080P,
    val videoFps: Int = 60,
    val showZoomWheel: Boolean = false,
    val manualExposure: Boolean = false,
    val manualIso: Int = 400,
    val manualShutterNs: Long = 16_666_667L,
    val manualWb: Boolean = false,
    val wbKelvin: Int = 5600,
    val manualFocus: Boolean = false,
    val focusDiopters: Float = 0f,
    val evIndex: Int = 0,

    // device capabilities
    val evRange: IntRange = 0..0,
    val evStep: Float = 0f,
    val evSupported: Boolean = false,
    val isoRange: IntRange = 100..3200,
    val shutterRange: LongRange = 1_000_000L..33_000_000L,
    val minFocusDiopters: Float = 0f,
    val zoomMin: Float = 1f,
    val zoomMax: Float = 1f,
    val hasFlash: Boolean = false,
    val fpsOptions: List<Int> = emptyList(),
    val lenses: List<LensInfo> = emptyList(),
    val currentLens: LensInfo? = null,
    val resolutions: List<ResolutionOption> = emptyList(),
    val currentResolution: ResolutionOption? = null,
    val hardware: List<Pair<String, String>> = emptyList(),

    // quick toggles
    val flash: FlashMode = FlashMode.OFF,
    val torch: Boolean = false,
    val grid: GridMode = GridMode.OFF,
    val histogramOn: Boolean = false,
    val peakingOn: Boolean = false,
    val levelOn: Boolean = false,
    val soundOn: Boolean = true,
    val timerSec: Int = 0,
    val quality: CaptureQuality = CaptureQuality.QUALITY,
    val fpsTarget: Int = 60,
    val showSettings: Boolean = false,

    // capture fx
    val capturing: Boolean = false,
    val bursting: Boolean = false,
    val burstCount: Int = 0,
    val countdown: Int = 0,
    val recordingVideo: Boolean = false,
    val recordingDurationSec: Int = 0,
    val lastPhoto: ImageBitmap? = null,
    val lastPhotoUri: Uri? = null
)
