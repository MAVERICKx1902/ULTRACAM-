package com.ultracam.app.ui.screens

import android.os.Build
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.view.HapticFeedbackConstants
import com.ultracam.app.ui.CaptureMode
import com.ultracam.app.ui.CameraViewModel
import com.ultracam.app.ui.Haptic
import com.ultracam.app.ui.components.BottomBar
import com.ultracam.app.ui.components.CaptureFlashOverlay
import com.ultracam.app.ui.components.CountdownOverlay
import com.ultracam.app.ui.components.ErrorCard
import com.ultracam.app.ui.components.FocusReticleLayer
import com.ultracam.app.ui.components.GridOverlay
import com.ultracam.app.ui.components.HistogramPanel
import com.ultracam.app.ui.components.LevelOverlay
import com.ultracam.app.ui.components.PeakingLayer
import com.ultracam.app.ui.components.ProPanel
import com.ultracam.app.ui.components.SettingsSheet
import com.ultracam.app.ui.components.SideRail
import com.ultracam.app.ui.components.TopBar
import com.ultracam.app.ui.components.ZoomBadge
import com.ultracam.app.ui.glass.PrismEdgeOverlay
import com.ultracam.app.ui.theme.Void

/**
 * The instrument: full-bleed viewfinder wearing the liquid prism interface.
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

    // hardware haptics
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

    // liquid sheen follows the horizon
    val sheen = (attitude.rollDeg / 42f).coerceIn(-1f, 1f)

    Box(
        Modifier
            .fillMaxSize()
            .background(Void)
    ) {
        // ---- viewfinder ----
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    vm.controller.attachPreviewView(this)
                }
            }
        )

        // ---- gestures: tap = focus, double tap = zoom toggle, pinch = zoom ----
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

        // ---- viewfinder instrumentation layers ----
        PeakingLayer(
            bitmap = peakingBitmap,
            tick = peakingTick,
            modifier = Modifier.fillMaxSize()
        )
        GridOverlay(ui.grid, Modifier.fillMaxSize())
        PrismEdgeOverlay(Modifier.fillMaxSize())
        LevelOverlay(attitude, ui.levelOn, Modifier.fillMaxSize())

        // ---- top cluster ----
        Column(
            Modifier
                .fillMaxWidth()
                .padding(WindowInsets.statusBars.asPaddingValues())
                .padding(top = 8.dp)
        ) {
            TopBar(
                ui = ui,
                zoom = zoom,
                attitude = attitude,
                sheen = sheen,
                onLensClick = vm::cycleLens
            )
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
            ) {
                if (ui.histogramOn) {
                    HistogramPanel(
                        data = histogram,
                        modifier = Modifier
                            .width(136.dp)
                            .height(76.dp)
                    )
                }
                Spacer(Modifier.weight(1f))
            }
        }

        // ---- side rail ----
        SideRail(
            ui = ui,
            onFlash = vm::cycleFlash,
            onTorch = vm::toggleTorch,
            onGrid = vm::cycleGrid,
            onTimer = vm::cycleTimer,
            onHistogram = vm::toggleHistogram,
            onPeaking = vm::togglePeaking,
            onLevel = vm::toggleLevel,
            onSettings = { vm.setShowSettings(true) },
            modifier = Modifier.align(Alignment.CenterEnd)
        )

        ZoomBadge(
            zoom = zoom,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 168.dp)
        )

        FocusReticleLayer(
            events = vm.focusEvents,
            modifier = Modifier.fillMaxSize()
        )

        // ---- bottom cluster ----
        BottomBar(
            ui = ui,
            zoom = zoom,
            proPanel = {
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
                        .padding(end = 62.dp)
                )
            },
            onModeSelect = vm::setMode,
            onZoomPreset = vm::setZoomPreset,
            onShutterTap = vm::onShutterTap,
            onShutterHoldStart = vm::onShutterHold,
            onShutterHoldEnd = vm::stopBurst,
            onGalleryTap = vm::openLastPhoto,
            onGalleryLongPress = vm::shareLastPhoto,
            onFlip = vm::cycleLens,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(WindowInsets.navigationBars.asPaddingValues())
                .padding(bottom = 14.dp)
        )

        // ---- fx ----
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
