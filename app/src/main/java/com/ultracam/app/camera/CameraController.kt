package com.ultracam.app.camera

import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CaptureRequest
import android.net.Uri
import android.os.Build
import android.util.Range
import android.util.Size
import androidx.camera.camera2.interop.Camera2CameraControl
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.camera2.interop.CaptureRequestOptions
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.core.ZoomState
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Owns the entire CameraX stack and every hardware control ULTRACAM exposes.
 *
 *  - lens enumeration (wide / ultrawide / tele / front, whatever the device has)
 *  - full-resolution + aspect-ratio switching via ResolutionSelector
 *  - zoom, torch, flash, EV compensation, tap-to-focus
 *  - PRO manual controls via Camera2 interop: ISO, shutter, kelvin WB, focus
 *    distance, frame duration and AE/AWB/AF mode overrides
 *  - live frame stats through [HistogramAnalyzer]
 *
 * Manual settings are applied twice for reliability: live through
 * [Camera2CameraControl] (preview updates instantly, no rebind) and baked into
 * the still-capture use case at bind time (so JPEGs honor them too).
 */
@androidx.annotation.OptIn(androidx.camera.camera2.interop.ExperimentalCamera2Interop::class)
class CameraController(private val context: Context) {

    interface Listener {
        fun onLenses(lenses: List<LensInfo>, currentIndex: Int)
        fun onCameraReady(snapshot: CameraSnapshot)
        fun onZoomRatio(ratio: Float)
        fun onError(message: String)
    }

    var listener: Listener? = null
    var analyzer: HistogramAnalyzer? = null

    private val mainExecutor: Executor = ContextCompat.getMainExecutor(context)
    private val analysisExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    private var provider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var previewView: PreviewView? = null
    private var preview: Preview? = null
    private var imageCapture: ImageCapture? = null
    private var imageAnalysis: ImageAnalysis? = null

    private var lifecycleOwner: LifecycleOwner? = null
    private var started = false

    var lenses: List<LensInfo> = emptyList()
        private set
    private var lensIndex = 0

    private var resolution: Size? = null
    private var captureQualityMode = ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY
    private var fpsRanges: List<Range<Int>> = emptyList()
    private var fpsRange: Range<Int>? = null
    private var manual = ManualSettings()
    private var torchOn = false
    private var evIndex = 0
    private var flashMode = ImageCapture.FLASH_MODE_OFF
    private var pendingZoomRatio = 1f

    private var inFlightCapture = false
    private var needsCaptureRebind = false

    private var zoomObserver: Observer<ZoomState>? = null

    // ------------------------------------------------------------------ start

    fun start(owner: LifecycleOwner, preferredLensId: String?, preferredResolution: Size?) {
        if (started) return
        started = true
        lifecycleOwner = owner

        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            try {
                val p = future.get()
                provider = p
                lenses = enumerateLenses(p)
                if (lenses.isEmpty()) {
                    listener?.onError("No cameras were found on this device.")
                    return@addListener
                }
                lensIndex = lenses.indexOfFirst { it.cameraId == preferredLensId }
                    .takeIf { it >= 0 }
                    ?: lenses.indexOfFirst { !it.isFront }.takeIf { it >= 0 }
                    ?: 0
                val defaultRes = defaultResolution(lenses[lensIndex], p)
                resolution = preferredResolution?.takeIf { preferred ->
                    resolutionsFor(lenses[lensIndex]).any { it.size == preferred }
                } ?: defaultRes
                rebindAll()
                listener?.onLenses(lenses, lensIndex)
            } catch (t: Throwable) {
                listener?.onError(
                    "Camera is unavailable: ${t.message ?: t.javaClass.simpleName}"
                )
            }
        }, mainExecutor)
    }

    fun retry() {
        if (provider != null) rebindAll() else lifecycleOwner?.let { start(it, null, null) }
    }

    fun shutdown() {
        try {
            analysisExecutor.shutdown()
        } catch (_: Exception) {
        }
        try {
            detachZoomObserver()
            provider?.unbindAll()
        } catch (_: Exception) {
        }
    }

    // ------------------------------------------------------------- enumeration

    private fun enumerateLenses(p: ProcessCameraProvider): List<LensInfo> {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? android.hardware.camera2.CameraManager
            ?: return emptyList()
        val result = mutableListOf<LensInfo>()
        val seenKeys = mutableSetOf<String>()

        val availableIds = try {
            cameraManager.cameraIdList
        } catch (_: Throwable) {
            emptyArray()
        }

        for (id in availableIds) {
            try {
                val chars = cameraManager.getCameraCharacteristics(id)
                val facing = chars.get(CameraCharacteristics.LENS_FACING) ?: continue
                val focals = chars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
                val focal = focals?.firstOrNull() ?: 4.5f
                val sensor = chars.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)
                val pixels = chars.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE)
                val sensorWidth = sensor?.width?.takeIf { it > 0 } ?: 6.4f
                val equiv = 36f * focal / sensorWidth
                val mp = pixels?.let { it.width * it.height / 1_000_000f } ?: 0f

                val info = p.availableCameraInfos.firstOrNull {
                    Camera2CameraInfo.from(it).cameraId == id
                }
                val hasFlash = info?.hasFlashUnit() ?: (facing == CameraCharacteristics.LENS_FACING_BACK)

                val key = "logical_$id"
                if (seenKeys.add(key)) {
                    val label = if (facing == CameraCharacteristics.LENS_FACING_FRONT) {
                        "FRONT"
                    } else {
                        CameraMath.focalBucket(equiv)
                    }
                    result += LensInfo(
                        cameraId = id,
                        physicalCameraId = null,
                        facing = facing,
                        label = label,
                        focalEquiv35mm = equiv,
                        megapixels = mp,
                        hasFlash = hasFlash
                    )
                }

                // Enumerate physical cameras if this is a logical multi-camera
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val physicalIds = chars.physicalCameraIds
                    for (physId in physicalIds) {
                        val physKey = "phys_${id}_$physId"
                        if (seenKeys.contains(physKey)) continue
                        val physChars = try {
                            cameraManager.getCameraCharacteristics(physId)
                        } catch (_: Throwable) {
                            null
                        } ?: continue
                        val physFacing = physChars.get(CameraCharacteristics.LENS_FACING) ?: facing
                        val physFocals = physChars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
                        val physFocal = physFocals?.firstOrNull() ?: focal
                        val physSensor = physChars.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE) ?: sensor
                        val physPixels = physChars.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE) ?: pixels
                        val physSensorWidth = physSensor?.width?.takeIf { it > 0 } ?: sensorWidth
                        val physEquiv = 36f * physFocal / physSensorWidth
                        val physMp = physPixels?.let { it.width * it.height / 1_000_000f } ?: mp

                        val physLabel = if (physFacing == CameraCharacteristics.LENS_FACING_FRONT) {
                            "FRONT ($physId)"
                        } else {
                            CameraMath.focalBucket(physEquiv)
                        }
                        seenKeys.add(physKey)
                        result += LensInfo(
                            cameraId = id,
                            physicalCameraId = physId,
                            facing = physFacing,
                            label = physLabel,
                            focalEquiv35mm = physEquiv,
                            megapixels = physMp,
                            hasFlash = hasFlash
                        )
                    }
                }
            } catch (_: Throwable) {
            }
        }
        return result.sortedWith(compareBy({ it.isFront }, { it.focalEquiv35mm }))
    }

    fun resolutionsFor(lens: LensInfo): List<ResolutionOption> {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? android.hardware.camera2.CameraManager
            ?: return emptyList()
        val targetId = lens.physicalCameraId ?: lens.cameraId
        val chars = try {
            cameraManager.getCameraCharacteristics(targetId)
        } catch (_: Throwable) {
            try {
                cameraManager.getCameraCharacteristics(lens.cameraId)
            } catch (_: Throwable) {
                null
            }
        } ?: return emptyList()

        val map = chars.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            ?: return emptyList()

        val normalSizes = try {
            map.getOutputSizes(ImageFormat.JPEG)
        } catch (_: Throwable) {
            null
        } ?: emptyArray()

        val highResSizes = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                map.getHighResolutionOutputSizes(ImageFormat.JPEG)
            } catch (_: Throwable) {
                null
            } ?: emptyArray()
        } else emptyArray()

        val pixelArray = chars.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE)
        val activeArray = chars.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
        val maxSensorSize = pixelArray ?: activeArray?.let { Size(it.width(), it.height()) }

        val combinedList = (normalSizes + highResSizes).toMutableList()
        if (maxSensorSize != null && maxSensorSize.width >= 1920 && maxSensorSize.height >= 1080) {
            if (!combinedList.any { it.width == maxSensorSize.width && it.height == maxSensorSize.height }) {
                combinedList += maxSensorSize
            }
        }

        val allSizes = combinedList
            .filter { it.width >= 1280 && it.height >= 720 }
            .distinctBy { "${it.width}x${it.height}" }
            .sortedByDescending { it.width.toLong() * it.height }

        return allSizes.map { size ->
            val mp = size.width * size.height / 1_000_000f
            val mpLabel = if (mp >= 10f) "${mp.toInt()}MP" else String.format(java.util.Locale.US, "%.1fMP", mp)
            ResolutionOption(
                size = size,
                label = "${size.width}×${size.height} ($mpLabel)",
                aspectLabel = CameraMath.aspectLabel(size.width, size.height),
                megapixels = mp
            )
        }
    }

    private fun defaultResolution(lens: LensInfo, p: ProcessCameraProvider): Size? {
        val all = resolutionsFor(lens)
        return all.firstOrNull { it.aspectLabel == "4:3" }?.size
            ?: all.firstOrNull()?.size
    }

    private fun selectorForCurrentLens(): CameraSelector {
        val lens = lenses.getOrNull(lensIndex) ?: return CameraSelector.DEFAULT_BACK_CAMERA
        return CameraSelector.Builder()
            .addCameraFilter { cameraInfos ->
                val match = cameraInfos.filter { Camera2CameraInfo.from(it).cameraId == lens.cameraId }
                if (match.isNotEmpty()) {
                    match
                } else {
                    cameraInfos.filter {
                        Camera2CameraInfo.from(it).getCameraCharacteristic(CameraCharacteristics.LENS_FACING) == lens.facing
                    }
                }
            }
            .build()
    }

    private fun fallbackSelector(): CameraSelector {
        val lens = lenses.getOrNull(lensIndex)
        return if (lens?.isFront == true) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }
    }

    // ----------------------------------------------------------------- binding

    private fun buildPreview(): Preview {
        val builder = Preview.Builder()
        builder.setResolutionSelector(
            ResolutionSelector.Builder()
                .setAspectRatioStrategy(CameraMath.aspectStrategyFor(resolution))
                .build()
        )
        Camera2Interop.Extender(builder).apply { bakeManual(this) }
        return builder.build()
    }

    private fun buildImageCapture(): ImageCapture {
        val builder = ImageCapture.Builder()
            .setCaptureMode(captureQualityMode)
        val resolutionBuilder = ResolutionSelector.Builder()
        resolution?.let { size ->
            resolutionBuilder.setResolutionStrategy(
                ResolutionStrategy(
                    size,
                    ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                )
            )
        }
        builder.setResolutionSelector(resolutionBuilder.build())
        Camera2Interop.Extender(builder).apply { bakeManual(this) }
        return builder.build()
    }

    private fun buildAnalysis(): ImageAnalysis {
        val builder = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        builder.setResolutionSelector(
            ResolutionSelector.Builder()
                .setAspectRatioStrategy(CameraMath.aspectStrategyFor(resolution))
                .build()
        )
        Camera2Interop.Extender(builder).apply { bakeManual(this) }
        val built = builder.build()
        analyzer?.let { built.setAnalyzer(analysisExecutor, it) }
        return built
    }

    private fun bakeManual(ext: Camera2Interop.Extender<*>) {
        val lens = lenses.getOrNull(lensIndex)
        if (lens?.physicalCameraId != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ext.setPhysicalCameraId(lens.physicalCameraId)
        }
        if (manual.exposureOn) {
            ext.setCaptureRequestOption(
                CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF
            )
            ext.setCaptureRequestOption(CaptureRequest.SENSOR_SENSITIVITY, manual.iso)
            ext.setCaptureRequestOption(CaptureRequest.SENSOR_EXPOSURE_TIME, manual.shutterNs)
            ext.setCaptureRequestOption(
                CaptureRequest.SENSOR_FRAME_DURATION, frameDurationFor(manual)
            )
        }
        if (manual.wbOn) {
            ext.setCaptureRequestOption(
                CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_OFF
            )
            ext.setCaptureRequestOption(
                CaptureRequest.COLOR_CORRECTION_GAINS, CameraMath.kelvinToRggb(manual.kelvin)
            )
        }
        if (manual.focusOn) {
            ext.setCaptureRequestOption(
                CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF
            )
            ext.setCaptureRequestOption(CaptureRequest.LENS_FOCUS_DISTANCE, manual.diopters)
        }
    }

    private fun frameDurationFor(m: ManualSettings): Long {
        val fpsFloor = fpsRange?.lower?.takeIf { it > 0 }?.let { 1_000_000_000L / it } ?: 0L
        return maxOf(m.shutterNs, fpsFloor)
    }

    private fun rebindAll() {
        val p = provider ?: return
        val owner = lifecycleOwner ?: return
        try {
            detachZoomObserver()
            p.unbindAll()
            imageAnalysis?.clearAnalyzer()
            preview = buildPreview()
            imageCapture = buildImageCapture()
            imageAnalysis = buildAnalysis()
            camera = try {
                p.bindToLifecycle(
                    owner, selectorForCurrentLens(), preview, imageCapture, imageAnalysis
                )
            } catch (e1: Exception) {
                p.bindToLifecycle(
                    owner, fallbackSelector(), preview, imageCapture, imageAnalysis
                )
            }
            afterBind()
        } catch (e: Exception) {
            listener?.onError(
                "Failed to start the camera: ${e.message ?: e.javaClass.simpleName}"
            )
        }
    }

    /**
     * Rebuilds only the still-capture use case (keeping the preview stream
     * alive) so freshly-committed manual settings are baked into JPEGs.
     * Falls back to a full rebind if partial rebinding is rejected.
     */
    private fun rebindCaptureOnly() {
        if (inFlightCapture) {
            needsCaptureRebind = true
            return
        }
        val p = provider ?: return
        val owner = lifecycleOwner ?: return
        val oldCapture = imageCapture ?: return
        val livePreview = preview ?: return rebindAll()
        val liveAnalysis = imageAnalysis ?: return rebindAll()
        try {
            detachZoomObserver()
            p.unbind(oldCapture)
            imageCapture = buildImageCapture()
            camera = p.bindToLifecycle(
                owner, selectorForCurrentLens(), livePreview, imageCapture, liveAnalysis
            )
            afterBind()
        } catch (e: Exception) {
            rebindAll()
        }
    }

    private fun afterBind() {
        val cam = camera ?: return
        preview?.setSurfaceProvider(previewView?.surfaceProvider)
        imageCapture?.flashMode = flashMode
        cam.cameraControl.enableTorch(torchOn)
        if (!manual.exposureOn) {
            cam.cameraControl.setExposureCompensationIndex(evIndex)
        }
        cam.cameraControl.setZoomRatio(pendingZoomRatio)
        applyRuntimeManual()
        attachZoomObserver(cam)
        listener?.onCameraReady(currentSnapshot())
    }

    private fun applyRuntimeManual() {
        val cam = camera ?: return
        val b = CaptureRequestOptions.Builder()
        if (manual.exposureOn) {
            b.setCaptureRequestOption(
                CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF
            )
            b.setCaptureRequestOption(CaptureRequest.SENSOR_SENSITIVITY, manual.iso)
            b.setCaptureRequestOption(CaptureRequest.SENSOR_EXPOSURE_TIME, manual.shutterNs)
            b.setCaptureRequestOption(
                CaptureRequest.SENSOR_FRAME_DURATION, frameDurationFor(manual)
            )
        }
        if (manual.wbOn) {
            b.setCaptureRequestOption(
                CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_OFF
            )
            b.setCaptureRequestOption(
                CaptureRequest.COLOR_CORRECTION_GAINS, CameraMath.kelvinToRggb(manual.kelvin)
            )
        }
        if (manual.focusOn) {
            b.setCaptureRequestOption(
                CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF
            )
            b.setCaptureRequestOption(CaptureRequest.LENS_FOCUS_DISTANCE, manual.diopters)
        }
        fpsRange?.let {
            b.setCaptureRequestOption(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, it)
        }
        Camera2CameraControl.from(cam.cameraControl).captureRequestOptions = b.build()
    }

    private fun attachZoomObserver(cam: Camera) {
        val obs = Observer<ZoomState> { zs ->
            zs?.let { listener?.onZoomRatio(it.zoomRatio) }
        }
        zoomObserver = obs
        cam.cameraInfo.zoomState.observeForever(obs)
    }

    private fun detachZoomObserver() {
        zoomObserver?.let { obs ->
            camera?.cameraInfo?.zoomState?.removeObserver(obs)
        }
        zoomObserver = null
    }

    // ------------------------------------------------------------------ info

    private fun currentSnapshot(): CameraSnapshot {
        val cam = camera ?: throw IllegalStateException("no camera")
        val info = cam.cameraInfo
        val c2 = Camera2CameraInfo.from(info)
        val lens = lenses.getOrNull(lensIndex) ?: LensInfo(
            cameraId = c2.cameraId,
            facing = c2.getCameraCharacteristic(CameraCharacteristics.LENS_FACING)
                ?: CameraCharacteristics.LENS_FACING_BACK,
            label = "MAIN",
            focalEquiv35mm = 26f,
            megapixels = 0f,
            hasFlash = info.hasFlashUnit()
        )
        val zs = info.zoomState.value
        val ev = info.exposureState
        val isoRange = c2.getCameraCharacteristic(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)
        val shutterRange = c2.getCameraCharacteristic(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE)
        val minFocus = c2.getCameraCharacteristic(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE) ?: 0f
        val fps = c2.getCameraCharacteristic(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES)
            ?.filter { it.lower == it.upper && it.upper >= 20 }
            ?.map { it.upper }
            ?.distinct()
            ?.sorted()
            ?: emptyList()
        fpsRanges = c2.getCameraCharacteristic(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES)
            ?.toList() ?: emptyList()
        val resolutions = resolutionsFor(lens)

        return CameraSnapshot(
            lens = lens,
            zoomMin = zs?.minZoomRatio ?: 1f,
            zoomMax = zs?.maxZoomRatio ?: 1f,
            evRange = ev.exposureCompensationRange?.let { it.lower..it.upper } ?: (0..0),
            evStep = ev.exposureCompensationStep.toFloat(),
            evSupported = ev.isExposureCompensationSupported,
            isoRange = isoRange?.let { it.lower..it.upper } ?: 100..3200,
            shutterRange = shutterRange?.let { it.lower..it.upper }
                ?: 1_000_000L..33_000_000L,
            minFocusDiopters = minFocus,
            hasFlash = info.hasFlashUnit(),
            fpsOptions = fps,
            resolutions = resolutions,
            currentResolution = resolutions.firstOrNull { it.size == resolution },
            hardware = hardwareSpecs(c2, lens)
        )
    }

    private fun hardwareSpecs(c2: Camera2CameraInfo, lens: LensInfo): List<Pair<String, String>> {
        fun <T> char(key: CameraCharacteristics.Key<T>): T? =
            try {
                c2.getCameraCharacteristic(key)
            } catch (_: Throwable) {
                null
            }

        val sensor = char(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)
        val pixels = char(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE)
        val focalLengths = char(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
        val level = char(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)
        val hardwareLevel = when (level) {
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY -> "LEGACY"
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED -> "LIMITED"
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL -> "FULL"
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3 -> "LEVEL 3"
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_EXTERNAL -> "EXTERNAL"
            else -> "UNKNOWN"
        }
        val oisModes = c2.getCameraCharacteristic(CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION)
        val ois = if (oisModes != null && oisModes.isNotEmpty()) "OIS" else "NONE"
        val pixelPitch = if (sensor != null && pixels != null && pixels.width > 0) {
            sensor.width * 1000f / pixels.width
        } else 0f
        val mp = pixels?.let { it.width * it.height / 1_000_000f } ?: lens.megapixels

        val rows = mutableListOf<Pair<String, String>>()
        rows += "CAMERA" to "${lens.label} · ID ${lens.cameraId}"
        rows += "PIPELINE" to "Camera2 level $hardwareLevel"
        rows += "SENSOR" to if (mp > 0f) {
            String.format(java.util.Locale.US, "%.1f MP · %.0f×%.0f", mp, sensor?.width ?: 0f, sensor?.height ?: 0f)
        } else "unknown"
        if (pixelPitch > 0f) {
            rows += "PIXEL PITCH" to String.format(java.util.Locale.US, "%.2f µm", pixelPitch)
        }
        rows += "CROP FACTOR" to String.format(
            java.util.Locale.US, "%.1f×",
            if (sensor != null && sensor.width > 0) 36f / sensor.width else 0f
        )
        rows += "FOCAL (35MM EQ)" to "${lens.focalText}"
        focalLengths?.let {
            rows += "FOCAL LENGTHS" to it.joinToString("/") { f ->
                String.format(java.util.Locale.US, "%.1fmm", f)
            }
        }
        rows += "ISO RANGE" to "${_isoRangeText(c2)}"
        rows += "SHUTTER RANGE" to "${_shutterRangeText(c2)}"
        rows += "MIN FOCUS" to if (lens.isFront || _minFocus(c2) <= 0f) "FIXED" else {
            String.format(java.util.Locale.US, "%.2f m", 1f / _minFocus(c2))
        }
        rows += "STABILIZATION" to ois
        rows += "FLASH" to if (lens.hasFlash) "YES" else "NO"
        rows += "DEVICE" to "${Build.MANUFACTURER} ${Build.MODEL}"
        return rows
    }

    private fun _isoRangeText(c2: Camera2CameraInfo): String {
        val r = c2.getCameraCharacteristic(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)
        return r?.let { "${it.lower}-${it.upper}" } ?: "unknown"
    }

    private fun _shutterRangeText(c2: Camera2CameraInfo): String {
        val r = c2.getCameraCharacteristic(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE)
        return r?.let { "${CameraMath.formatShutter(it.lower)}-${CameraMath.formatShutter(it.upper)}" } ?: "unknown"
    }

    private fun _minFocus(c2: Camera2CameraInfo): Float =
        c2.getCameraCharacteristic(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE) ?: 0f

    // --------------------------------------------------------------- controls

    fun attachPreviewView(view: PreviewView?) {
        previewView = view
        preview?.setSurfaceProvider(view?.surfaceProvider)
    }

    fun selectLens(index: Int) {
        if (index !in lenses.indices || index == lensIndex) return
        lensIndex = index
        resolution = defaultResolution(lenses[index], provider ?: return)
        rebindAll()
    }

    fun setResolution(size: Size?) {
        resolution = size
        rebindAll()
    }

    fun setCaptureQuality(mode: Int) {
        if (captureQualityMode == mode) return
        captureQualityMode = mode
        rebindCaptureOnly()
    }

    fun setFps(fps: Int) {
        fpsRange = if (fps <= 0) {
            null
        } else {
            fpsRanges.firstOrNull { it.lower == fps && it.upper == fps }
        }
        applyRuntimeManual()
    }

    fun updateManual(settings: ManualSettings, commit: Boolean) {
        manual = settings
        applyRuntimeManual()
        if (commit) rebindCaptureOnly()
    }

    fun setTorch(on: Boolean) {
        torchOn = on
        camera?.cameraControl?.enableTorch(on)
    }

    fun setFlashMode(mode: Int) {
        flashMode = mode
        imageCapture?.flashMode = mode
    }

    fun setEvIndex(index: Int) {
        evIndex = index
        if (!manual.exposureOn) {
            camera?.cameraControl?.setExposureCompensationIndex(index)
        }
    }

    fun setZoomRatio(ratio: Float) {
        pendingZoomRatio = ratio
        val zs = camera?.cameraInfo?.zoomState?.value ?: return
        camera?.cameraControl?.setZoomRatio(
            ratio.coerceIn(zs.minZoomRatio, zs.maxZoomRatio)
        )
    }

    fun focusAt(x: Float, y: Float, onResult: (Boolean) -> Unit) {
        val view = previewView
        val cam = camera
        if (view == null || cam == null || manual.focusOn) {
            onResult(false)
            return
        }
        try {
            val point = view.meteringPointFactory.createPoint(x, y)
            val action = FocusMeteringAction.Builder(
                point,
                FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE or FocusMeteringAction.FLAG_AWB
            )
                .setAutoCancelDuration(4, TimeUnit.SECONDS)
                .build()
            val future = cam.cameraControl.startFocusAndMetering(action)
            future.addListener({
                try {
                    onResult(future.get().isFocusSuccessful)
                } catch (_: Throwable) {
                    onResult(false)
                }
            }, mainExecutor)
        } catch (_: Throwable) {
            onResult(false)
        }
    }

    // ---------------------------------------------------------------- capture

    fun takePhoto(onSaved: (uri: Uri, name: String) -> Unit, onError: (String) -> Unit) {
        val capture = imageCapture
        if (capture == null) {
            onError("The camera is still warming up.")
            return
        }
        if (inFlightCapture) {
            onError("A capture is already in flight.")
            return
        }
        val request = try {
            CaptureSaver.newRequest(context)
        } catch (t: Throwable) {
            onError("Could not create the destination: ${t.message}")
            return
        }
        inFlightCapture = true
        capture.flashMode = flashMode
        capture.takePicture(
            request.options,
            mainExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(results: ImageCapture.OutputFileResults) {
                    inFlightCapture = false
                    CaptureSaver.finalize(context, request)
                    val uri = results.savedUri ?: CaptureSaver.shareableUri(context, request)
                    onSaved(uri, request.name)
                    if (needsCaptureRebind) {
                        needsCaptureRebind = false
                        rebindCaptureOnly()
                    }
                }

                override fun onError(exception: androidx.camera.core.ImageCaptureException) {
                    inFlightCapture = false
                    CaptureSaver.cleanup(context, request)
                    onError(exception.message ?: "The capture failed.")
                    if (needsCaptureRebind) {
                        needsCaptureRebind = false
                        rebindCaptureOnly()
                    }
                }
            }
        )
    }
}
