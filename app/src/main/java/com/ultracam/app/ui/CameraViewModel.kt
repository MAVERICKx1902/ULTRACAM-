package com.ultracam.app.ui

import android.app.Application
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaActionSound
import android.net.Uri
import android.os.SystemClock
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ultracam.app.camera.CameraController
import com.ultracam.app.camera.CameraSnapshot
import com.ultracam.app.camera.HistogramAnalyzer
import com.ultracam.app.camera.HistogramData
import com.ultracam.app.camera.LensInfo
import com.ultracam.app.camera.ManualSettings
import com.ultracam.app.sensors.Attitude
import com.ultracam.app.sensors.AttitudeSensor
import com.ultracam.app.util.Prefs
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.resume

/**
 * Single source of truth for the camera UI. Bridges [CameraController]
 * (hardware) and Compose (liquid prism interface).
 */
class CameraViewModel(application: Application) :
    AndroidViewModel(application),
    CameraController.Listener {

    private val prefs = Prefs(application)

    val controller = CameraController(application)
    private val analyzer = HistogramAnalyzer()
    private val attitudeSensor = AttitudeSensor(application)
    private val sound = MediaActionSound()

    private val _ui = MutableStateFlow(initialState())
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    /** Live zoom ratio, separate so pinching doesn't recompose the world. */
    val zoom = MutableStateFlow(1f)

    /** Throttled luminance histogram, null until the first frame arrives. */
    val histogram = MutableStateFlow<HistogramData?>(null)

    /** Focus-peaking bitmap (reused instance) + invalidation counter. */
    val peakingBitmap = MutableStateFlow<android.graphics.Bitmap?>(null)
    val peakingTick = MutableStateFlow(0)

    /** Device attitude from the rotation-vector sensor (smoothed). */
    val attitude = MutableStateFlow(Attitude(0f, 0f, 0f))

    val focusEvents = MutableSharedFlow<FocusFx>(extraBufferCapacity = 16)
    val captureFx = MutableSharedFlow<Unit>(extraBufferCapacity = 4)
    val hapticFx = MutableSharedFlow<Haptic>(extraBufferCapacity = 32)

    private var lastHistogramAt = 0L
    private var focusCounter = 0L
    private var countdownJob: kotlinx.coroutines.Job? = null
    private var burstJob: kotlinx.coroutines.Job? = null
    private var bursting = false
    private var lastBurstEnd = 0L

    init {
        controller.analyzer = analyzer
        controller.listener = this

        analyzer.onHistogram = { data ->
            val now = SystemClock.uptimeMillis()
            if (now - lastHistogramAt >= 90) {
                lastHistogramAt = now
                histogram.value = data
            }
        }
        analyzer.onPeaking = { bmp ->
            peakingBitmap.value = bmp
            peakingTick.value = peakingTick.value + 1
        }
        analyzer.peakingEnabled = _ui.value.peakingOn

        try {
            sound.load(MediaActionSound.SHUTTER_CLICK)
        } catch (_: Exception) {
        }

        viewModelScope.launch {
            var sp = 0f
            var sr = 0f
            var sa = 0f
            attitudeSensor.flow().collect { a ->
                sp += (a.pitchDeg - sp) * 0.25f
                sr += (a.rollDeg - sr) * 0.25f
                sa += (a.azimuthDeg - sa) * 0.15f
                attitude.value = Attitude(sp, sr, sa)
            }
        }
    }

    private fun initialState(): UiState {
        val mode = if (prefs.mode == "PRO") CaptureMode.PRO else CaptureMode.AUTO
        val flash = when (prefs.flashMode) {
            "AUTO" -> FlashMode.AUTO
            "ON" -> FlashMode.ON
            else -> FlashMode.OFF
        }
        val grid = when (prefs.gridMode) {
            "THIRDS" -> GridMode.THIRDS
            "GOLDEN" -> GridMode.GOLDEN
            else -> GridMode.OFF
        }
        val quality = if (prefs.quality == "SPEED") CaptureQuality.SPEED else CaptureQuality.QUALITY
        return UiState(
            mode = mode,
            flash = flash,
            grid = grid,
            histogramOn = prefs.histogramOn,
            peakingOn = prefs.peakingOn,
            levelOn = prefs.levelOn,
            soundOn = prefs.soundOn,
            timerSec = prefs.timerSec,
            quality = quality,
            fpsTarget = prefs.fpsTarget
        )
    }

    // ------------------------------------------------------------- controller

    fun startCamera(lifecycleOwner: androidx.lifecycle.LifecycleOwner) {
        controller.start(lifecycleOwner, prefs.lensId, prefs.resolution)
    }

    override fun onLenses(lenses: List<LensInfo>, currentIndex: Int) {
        _ui.update { it.copy(lenses = lenses, currentLens = lenses.getOrNull(currentIndex)) }
    }

    override fun onCameraReady(snapshot: CameraSnapshot) {
        _ui.update { state ->
            state.copy(
                cameraReady = true,
                cameraError = null,
                currentLens = snapshot.lens,
                zoomMin = snapshot.zoomMin,
                zoomMax = snapshot.zoomMax,
                evRange = snapshot.evRange,
                evStep = snapshot.evStep,
                evSupported = snapshot.evSupported,
                isoRange = snapshot.isoRange,
                shutterRange = snapshot.shutterRange,
                minFocusDiopters = snapshot.minFocusDiopters,
                hasFlash = snapshot.hasFlash,
                fpsOptions = snapshot.fpsOptions,
                resolutions = snapshot.resolutions,
                currentResolution = snapshot.currentResolution,
                hardware = snapshot.hardware,
                // keep manual values inside the new lens' real ranges
                manualIso = snapshot.isoRange.let { state.manualIso.coerceIn(it.first, it.last) },
                manualShutterNs = snapshot.shutterRange
                    .let { state.manualShutterNs.coerceIn(it.first, it.last) }
            )
        }
        // Re-apply preferences that depend on camera capabilities.
        controller.setFlashMode(_ui.value.flash.captureMode)
        controller.setCaptureQuality(_ui.value.quality.mode)
        controller.setFps(_ui.value.fpsTarget)
        controller.setEvIndex(_ui.value.evIndex)
        if (_ui.value.torch) controller.setTorch(true)
        pushManual(false)
        prefs.lensId = snapshot.lens.cameraId
        snapshot.currentResolution?.let { prefs.resolution = it.size }
    }

    override fun onZoomRatio(ratio: Float) {
        zoom.value = ratio
    }

    override fun onError(message: String) {
        _ui.update { it.copy(cameraError = message) }
    }

    override fun onCleared() {
        try {
            sound.release()
        } catch (_: Exception) {
        }
        controller.shutdown()
        super.onCleared()
    }

    // ------------------------------------------------------------- selection

    fun selectLens(index: Int) {
        controller.selectLens(index)
    }

    fun cycleLens() {
        val s = _ui.value
        if (s.lenses.size < 2) return
        val next = (s.lenses.indexOf(s.currentLens) + 1) % s.lenses.size
        selectLens(if (next < 0) 0 else next)
    }

    fun setResolution(index: Int) {
        val s = _ui.value
        val option = s.resolutions.getOrNull(index) ?: return
        _ui.update { it.copy(currentResolution = option) }
        prefs.resolution = option.size
        controller.setResolution(option.size)
    }

    fun setQuality(quality: CaptureQuality) {
        _ui.update { it.copy(quality = quality) }
        prefs.quality = if (quality == CaptureQuality.SPEED) "SPEED" else "QUALITY"
        controller.setCaptureQuality(quality.mode)
    }

    fun setFps(fps: Int) {
        _ui.update { it.copy(fpsTarget = fps) }
        prefs.fpsTarget = fps
        controller.setFps(fps)
    }

    // ------------------------------------------------------------- toggles

    fun setMode(mode: CaptureMode) {
        _ui.update { it.copy(mode = mode) }
        prefs.mode = if (mode == CaptureMode.PRO) "PRO" else "AUTO"
        hapticFx.tryEmit(Haptic.TICK)
    }

    fun cycleFlash() {
        val next = _ui.value.flash.next()
        _ui.update { it.copy(flash = next) }
        prefs.flashMode = next.name
        controller.setFlashMode(next.captureMode)
        hapticFx.tryEmit(Haptic.TICK)
    }

    fun toggleTorch() {
        val next = !_ui.value.torch
        _ui.update { it.copy(torch = next) }
        controller.setTorch(next)
        hapticFx.tryEmit(Haptic.TICK)
    }

    fun cycleGrid() {
        val next = _ui.value.grid.next()
        _ui.update { it.copy(grid = next) }
        prefs.gridMode = next.name
        hapticFx.tryEmit(Haptic.TICK)
    }

    fun cycleTimer() {
        val next = when (_ui.value.timerSec) {
            0 -> 3
            3 -> 10
            else -> 0
        }
        _ui.update { it.copy(timerSec = next) }
        prefs.timerSec = next
        hapticFx.tryEmit(Haptic.TICK)
    }

    fun toggleHistogram() {
        val next = !_ui.value.histogramOn
        _ui.update { it.copy(histogramOn = next) }
        prefs.histogramOn = next
        if (!next) histogram.value = null
        hapticFx.tryEmit(Haptic.TICK)
    }

    fun togglePeaking() {
        val next = !_ui.value.peakingOn
        _ui.update { it.copy(peakingOn = next) }
        prefs.peakingOn = next
        analyzer.peakingEnabled = next
        if (!next) peakingBitmap.value = null
        hapticFx.tryEmit(Haptic.TICK)
    }

    fun toggleLevel() {
        val next = !_ui.value.levelOn
        _ui.update { it.copy(levelOn = next) }
        prefs.levelOn = next
        hapticFx.tryEmit(Haptic.TICK)
    }

    fun toggleSound() {
        val next = !_ui.value.soundOn
        _ui.update { it.copy(soundOn = next) }
        prefs.soundOn = next
    }

    fun setShowSettings(show: Boolean) {
        _ui.update { it.copy(showSettings = show) }
    }

    fun dismissError() {
        _ui.update { it.copy(cameraError = null) }
        controller.retry()
    }

    // ------------------------------------------------------------- manual

    fun setEvIndex(index: Int) {
        val clamped = index.coerceIn(_ui.value.evRange.first, _ui.value.evRange.last)
        _ui.update { it.copy(evIndex = clamped) }
        controller.setEvIndex(clamped)
    }

    fun setManualExposure(on: Boolean) {
        _ui.update { it.copy(manualExposure = on) }
        pushManual(true)
        hapticFx.tryEmit(Haptic.CONFIRM)
    }

    fun setIso(value: Int) {
        val snapped = com.ultracam.app.camera.CameraMath.snapIso(value, _ui.value.isoRange)
        _ui.update { it.copy(manualIso = snapped) }
        pushManual(false)
    }

    fun setShutterNs(value: Long) {
        val snapped = com.ultracam.app.camera.CameraMath.snapShutter(value, _ui.value.shutterRange)
        _ui.update { it.copy(manualShutterNs = snapped) }
        pushManual(false)
    }

    fun setManualWb(on: Boolean) {
        _ui.update { it.copy(manualWb = on) }
        pushManual(true)
        hapticFx.tryEmit(Haptic.CONFIRM)
    }

    fun setKelvin(value: Int) {
        _ui.update { it.copy(wbKelvin = value.coerceIn(2500, 9500)) }
        pushManual(false)
    }

    fun setManualFocus(on: Boolean) {
        _ui.update { it.copy(manualFocus = on) }
        pushManual(true)
        hapticFx.tryEmit(Haptic.CONFIRM)
    }

    fun setFocusDiopters(value: Float) {
        val max = _ui.value.minFocusDiopters
        _ui.update { it.copy(focusDiopters = value.coerceIn(0f, max.coerceAtLeast(0f))) }
        pushManual(false)
    }

    fun commitManual() = pushManual(true)

    private fun pushManual(commit: Boolean) {
        val s = _ui.value
        controller.updateManual(
            ManualSettings(
                exposureOn = s.manualExposure,
                iso = s.manualIso,
                shutterNs = s.manualShutterNs,
                wbOn = s.manualWb,
                kelvin = s.wbKelvin,
                focusOn = s.manualFocus,
                diopters = s.focusDiopters
            ),
            commit
        )
    }

    // ------------------------------------------------------------- zoom/focus

    fun pinchZoom(multiplier: Float) {
        val s = _ui.value
        if (multiplier == 1f || s.zoomMax <= s.zoomMin) return
        val target = (zoom.value * multiplier).coerceIn(s.zoomMin, s.zoomMax)
        controller.setZoomRatio(target)
    }

    fun setZoomPreset(ratio: Float) {
        controller.setZoomRatio(ratio)
        hapticFx.tryEmit(Haptic.TICK)
    }

    fun toggleZoomPreset() {
        val s = _ui.value
        if (zoom.value < 2f && s.zoomMax >= 1.9f) {
            controller.setZoomRatio(2f)
        } else {
            controller.setZoomRatio(1f)
        }
    }

    fun tapFocus(x: Float, y: Float) {
        val s = _ui.value
        if (!s.cameraReady) return
        if (s.mode == CaptureMode.PRO && s.manualFocus) return
        val id = ++focusCounter
        focusEvents.tryEmit(FocusFx(x, y, id, FocusPhase.START))
        controller.focusAt(x, y) { locked ->
            focusEvents.tryEmit(
                FocusFx(x, y, id, if (locked) FocusPhase.LOCKED else FocusPhase.FAILED)
            )
            if (locked) hapticFx.tryEmit(Haptic.TICK)
        }
    }

    // ------------------------------------------------------------- capture

    fun onShutterTap() {
        if (SystemClock.uptimeMillis() - lastBurstEnd < 600) return
        if (bursting) return
        capture()
    }

    fun onShutterHold() {
        startBurst()
    }

    fun capture() {
        val s = _ui.value
        if (!s.cameraReady || s.capturing || s.countdown > 0) return
        if (s.timerSec > 0) {
            startCountdown(s.timerSec)
            return
        }
        viewModelScope.launch {
            _ui.update { it.copy(capturing = true) }
            performCapture()
            _ui.update { it.copy(capturing = false) }
        }
    }

    private fun startCountdown(seconds: Int) {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            var remain = seconds
            while (remain > 0 && isActive) {
                _ui.update { it.copy(countdown = remain) }
                hapticFx.tryEmit(Haptic.TICK)
                delay(1000)
                remain--
            }
            _ui.update { it.copy(countdown = 0) }
            if (!isActive) return@launch
            _ui.update { it.copy(capturing = true) }
            performCapture()
            _ui.update { it.copy(capturing = false) }
        }
    }

    fun cancelCountdown() {
        if (_ui.value.countdown > 0) {
            countdownJob?.cancel()
            _ui.update { it.copy(countdown = 0) }
        }
    }

    private fun startBurst() {
        if (bursting) return
        val s = _ui.value
        if (!s.cameraReady) return
        bursting = true
        burstJob = viewModelScope.launch {
            _ui.update { it.copy(bursting = true, burstCount = 0) }
            while (isActive && bursting) {
                performCapture()
                delay(140)
            }
            _ui.update { it.copy(bursting = false) }
        }
    }

    fun stopBurst() {
        if (!bursting) return
        bursting = false
        lastBurstEnd = SystemClock.uptimeMillis()
        hapticFx.tryEmit(Haptic.CONFIRM)
    }

    private suspend fun performCapture() {
        try {
            val result = kotlinx.coroutines.suspendCancellableCoroutine<Pair<Uri, String>?> { cont ->
                controller.takePhoto(
                    onSaved = { uri, name ->
                        if (cont.isActive) cont.resume(uri to name)
                    },
                    onError = { message ->
                        _ui.update { it.copy(cameraError = message) }
                        if (cont.isActive) cont.resume(null)
                    }
                )
            }
            if (result == null) {
                hapticFx.tryEmit(Haptic.WARN)
                return
            }
            val (uri, _) = result
            if (_ui.value.soundOn) {
                try {
                    sound.play(MediaActionSound.SHUTTER_CLICK)
                } catch (_: Exception) {
                }
            }
            hapticFx.tryEmit(Haptic.CONFIRM)
            captureFx.tryEmit(Unit)
            val thumb = loadThumbnail(uri)
            _ui.update {
                it.copy(
                    lastPhotoUri = uri,
                    lastPhoto = thumb?.asImageBitmap() ?: it.lastPhoto,
                    burstCount = if (it.bursting) it.burstCount + 1 else 0
                )
            }
        } catch (t: Throwable) {
            _ui.update { it.copy(cameraError = t.message ?: "Capture failed") }
            hapticFx.tryEmit(Haptic.WARN)
        }
    }

    private fun loadThumbnail(uri: Uri): Bitmap? {
        return try {
            val resolver = getApplication<Application>().contentResolver
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, bounds)
            }
            var sample = 1
            while (bounds.outWidth / sample > 640 && bounds.outWidth > 0) sample *= 2
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            resolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, opts)
            }
        } catch (_: Exception) {
            null
        }
    }

    // ------------------------------------------------------------- gallery

    fun shareLastPhoto() {
        val uri = _ui.value.lastPhotoUri ?: return
        val context = getApplication<Application>()
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(
            Intent.createChooser(intent, "Share ULTRACAM shot").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }

    fun openLastPhoto() {
        val uri = _ui.value.lastPhotoUri ?: return
        val context = getApplication<Application>()
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "image/jpeg")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            context.startActivity(intent.apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
        } catch (_: Exception) {
            shareLastPhoto()
        }
    }
}
