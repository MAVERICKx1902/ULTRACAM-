package com.ultracam.app.ui.screens

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ultracam.app.camera.AspectRatioOption
import com.ultracam.app.ui.CameraViewModel
import com.ultracam.app.ui.CaptureMode
import com.ultracam.app.ui.Haptic
import com.ultracam.app.ui.components.CaptureFlashOverlay
import com.ultracam.app.ui.components.CountdownOverlay
import com.ultracam.app.ui.components.ErrorCard
import com.ultracam.app.ui.components.FocusReticleLayer
import com.ultracam.app.ui.components.GridOverlay
import com.ultracam.app.ui.components.HistogramPanel
import com.ultracam.app.ui.components.LevelOverlay
import com.ultracam.app.ui.components.PeakingLayer
import com.ultracam.app.ui.components.PixelModeCarousel
import com.ultracam.app.ui.components.PixelShutterRow
import com.ultracam.app.ui.components.PixelViewfinderOverlays
import com.ultracam.app.ui.components.PixelZoomControls
import com.ultracam.app.ui.components.ProPanel
import com.ultracam.app.ui.components.SettingsSheet
import com.ultracam.app.ui.theme.Void

/**
 * Pixel Camera UI (High Performance & Material 3 Expressive Motion):
 * - Isolated composition tree to prevent unnecessary recompositions during gesture scrubbing
 * - Google Material 3 spring physics for mode bar, shutter button, and Arc Zoom Wheel
 * - Edge-to-edge rounded viewfinder frame
 */
@Composable
fun CameraScreen(vm: CameraViewModel) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val zoom by vm.zoom.collectAsStateWithLifecycle()
    val histogram by vm.histogram.collectAsStateWithLifecycle()
    val peakingTick by vm.peakingTick.collectAsStateWithLifecycle()
    val peakingBitmap by vm.peakingBitmap.collectAsStateWithLifecycle()
    val attitude by vm.attitude.collectAsStateWithLifecycle()

    val view = LocalView.current
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(lifecycleOwner) {
        vm.startCamera(lifecycleOwner)
    }

    // Hardware haptics feedback
    LaunchedEffect(view) {
        vm.hapticFx.collect { kind ->
            val constant = when (kind) {
                Haptic.TICK -> HapticFeedbackConstants.CLOCK_TICK
                Haptic.CONFIRM ->
                    if (Build.VERSION.SDK_INT >= 30) {
                        HapticFeedbackConstants.CONFIRM
                    } else {
                        HapticFeedbackConstants.KEYBOARD_TAP
                    }
                Haptic.WARN -> HapticFeedbackConstants.LONG_PRESS
            }
            view.performHapticFeedback(constant)
        }
    }

    val sheen = (attitude.rollDeg / 42f).coerceIn(-1f, 1f)

    Box(
        Modifier
            .fillMaxSize()
            .background(Void)
    ) {
        // ---- Main Viewfinder & Controls Column ----
        Column(
            Modifier
                .fillMaxSize()
                .padding(WindowInsets.statusBars.asPaddingValues()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Viewfinder frame with rounded corners matching Pixel Camera app design
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                val viewportModifier = when (ui.aspectRatio) {
                    AspectRatioOption.RATIO_4_3 -> Modifier.aspectRatio(3f / 4f)
                    AspectRatioOption.RATIO_16_9 -> Modifier.aspectRatio(9f / 16f)
                    AspectRatioOption.RATIO_1_1 -> Modifier.aspectRatio(1f)
                    AspectRatioOption.RATIO_FULL -> Modifier.fillMaxSize()
                }

                val clipShape = if (ui.aspectRatio == AspectRatioOption.RATIO_FULL) {
                    RoundedCornerShape(0.dp)
                } else {
                    RoundedCornerShape(24.dp)
                }

                Box(
                    modifier = viewportModifier
                        .clip(clipShape)
                        .background(Color.Black)
                ) {
                    // Isolated Native Camera2 PreviewView
                    CameraPreviewSurface(vm = vm)

                    // Touch Gestures (Tap to Focus & Pinch Zoom)
                    Box(
                        Modifier
                            .fillMaxSize()
                            .pointerInput(ui.mode, ui.manualFocus) {
                                detectTapGestures(
                                    onTap = { offset -> vm.tapFocus(offset.x, offset.y) },
                                    onDoubleTap = { vm.toggleZoomPreset() }
                                )
                            }
                            .pointerInput(Unit) {
                                detectTransformGestures { _, _, zoomChange, _ ->
                                    if (zoomChange != 1f) vm.pinchZoom(zoomChange)
                                }
                            }
                    )

                    // Live Camera Layers & Overlays
                    PeakingLayer(
                        bitmap = peakingBitmap,
                        tick = peakingTick,
                        modifier = Modifier.fillMaxSize()
                    )
                    GridOverlay(ui.grid, Modifier.fillMaxSize())
                    LevelOverlay(attitude, ui.levelOn, Modifier.fillMaxSize())

                    FocusReticleLayer(
                        events = vm.focusEvents,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Pixel Floating Overlays (HDR, Flash, Aspect Ratio, Timer, Settings, Macro, Gallery, Sparkles)
                    PixelViewfinderOverlays(
                        ui = ui,
                        onDropdownClick = { vm.setShowSettings(true) },
                        onHdrToggle = { /* toggle HDR */ },
                        onVideoResClick = vm::cycleVideoRes,
                        onVideoFpsClick = vm::cycleVideoFps,
                        onFlashClick = vm::cycleFlash,
                        onAspectRatioClick = vm::cycleAspectRatio,
                        onTimerClick = vm::cycleTimer,
                        onSettingsClick = { vm.setShowSettings(true) },
                        onMacroClick = { /* toggle macro */ },
                        onGalleryClick = vm::openLastPhoto,
                        onFxClick = { /* open FX panel */ },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Floating Zoom Controls & Expanding Arc Zoom Wheel Dial
                    PixelZoomControls(
                        zoom = zoom,
                        minZoom = ui.zoomMin,
                        maxZoom = ui.zoomMax,
                        showWheel = ui.showZoomWheel,
                        onZoomChange = vm::setZoomRatio,
                        onToggleWheel = vm::setShowZoomWheel,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 12.dp)
                    )
                }
            }

            // Histogram Panel
            if (ui.histogramOn) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 4.dp),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Start
                ) {
                    HistogramPanel(
                        data = histogram,
                        modifier = Modifier
                            .width(136.dp)
                            .height(76.dp)
                    )
                }
            }

            // ---- Bottom Control Area (PRO Panel, Mode Carousel, Shutter Row) ----
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(WindowInsets.navigationBars.asPaddingValues())
                    .padding(bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // PRO Drawer Controls
                if (ui.mode == CaptureMode.PRO) {
                    ProPanel(
                        ui = ui,
                        sheen = sheen,
                        onEvIndex = vm::setEvIndex,
                        onIso = vm::setIso,
                        onIsoCommit = vm::commitManual,
                        onShutter = vm::setShutterNs,
                        onShutterCommit = vm::commitManual,
                        onKelvin = vm::setKelvin,
                        onKelvinCommit = vm::commitManual,
                        onDiopters = vm::setFocusDiopters,
                        onDioptersCommit = vm::commitManual,
                        onToggleExposure = { vm.setManualExposure(!ui.manualExposure) },
                        onToggleWb = { vm.setManualWb(!ui.manualWb) },
                        onToggleFocus = { vm.setManualFocus(!ui.manualFocus) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 4.dp)
                    )
                }

                Spacer(Modifier.height(4.dp))

                // Pixel Mode Selector Bar (CINEMATIC, VIDEO, PHOTO, PORTRAIT, PRO)
                PixelModeCarousel(
                    currentMode = ui.mode,
                    onSelectMode = vm::setMode
                )

                Spacer(Modifier.height(8.dp))

                // Pixel Shutter Control Row
                PixelShutterRow(
                    lastPhoto = ui.lastPhoto,
                    capturing = ui.capturing || ui.bursting,
                    isVideoMode = ui.mode == CaptureMode.VIDEO || ui.mode == CaptureMode.CINEMATIC,
                    isRecordingVideo = ui.recordingVideo,
                    onShutterTap = vm::onShutterTap,
                    onGalleryTap = vm::openLastPhoto,
                    onGalleryLongPress = vm::shareLastPhoto,
                    onFlipCamera = vm::cycleLens
                )
            }
        }

        // ---- Settings Sheet & System Overlays ----
        SettingsSheet(
            ui = ui,
            sheen = sheen,
            onClose = { vm.setShowSettings(false) },
            onSelectResolution = vm::setResolution,
            onSelectLens = vm::selectLens,
            onQuality = vm::setQuality,
            onFps = vm::setFps,
            onSound = vm::toggleSound,
            modifier = Modifier.fillMaxSize()
        )

        CountdownOverlay(
            countdown = ui.countdown,
            onCancel = vm::cancelCountdown,
            modifier = Modifier.fillMaxSize()
        )

        CaptureFlashOverlay(
            events = vm.captureFx,
            modifier = Modifier.fillMaxSize()
        )

        ErrorCard(
            error = ui.cameraError,
            onRetry = vm::dismissError,
            modifier = Modifier.fillMaxSize()
        )
    }
}

/**
 * Isolated Camera Preview surface to prevent AndroidView recomposition overhead
 * during high-frequency gesture scrubbing or sensor updates.
 */
@Composable
private fun CameraPreviewSurface(
    vm: CameraViewModel,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { ctx ->
            PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
                vm.controller.attachPreviewView(this)
            }
        }
    )
}
