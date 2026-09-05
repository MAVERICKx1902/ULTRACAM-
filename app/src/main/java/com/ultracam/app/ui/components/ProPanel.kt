package com.ultracam.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ultracam.app.camera.CameraMath
import com.ultracam.app.ui.CaptureMode
import com.ultracam.app.ui.UiState
import com.ultracam.app.ui.glass.GlassPanel
import com.ultracam.app.ui.theme.PrismCyan
import com.ultracam.app.ui.theme.TinyLabelStyle
import kotlin.math.roundToInt

/**
 * The PRO drawer: full manual hardware control.
 * Each axis has an AUTO chip that engages/disengages the override;
 * sliders scrub live (preview updates instantly) and commit on release
 * (still-capture pipeline gets rebaked).
 */
@Composable
fun ProPanel(
    ui: UiState,
    sheen: Float,
    onEvIndex: (Int) -> Unit,
    onIso: (Int) -> Unit,
    onIsoCommit: () -> Unit,
    onShutter: (Long) -> Unit,
    onShutterCommit: () -> Unit,
    onKelvin: (Int) -> Unit,
    onKelvinCommit: () -> Unit,
    onDiopters: (Float) -> Unit,
    onDioptersCommit: () -> Unit,
    onToggleExposure: () -> Unit,
    onToggleWb: () -> Unit,
    onToggleFocus: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = ui.mode == CaptureMode.PRO,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
        modifier = modifier
    ) {
        GlassPanel(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp),
            shape = RoundedCornerShape(20.dp),
            sheen = sheen
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("PRO CONTROL", style = TinyLabelStyle.copy(color = PrismCyan))
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = if (ui.manualExposure || ui.manualWb || ui.manualFocus) {
                            "MANUAL OVERRIDE"
                        } else {
                            "ALL AUTO — TAP CHIPS TO ENGAGE"
                        },
                        style = TinyLabelStyle.copy(color = Color.White.copy(alpha = 0.45f))
                    )
                }

                // ---- EV (available while AE drives exposure) ----
                if (!ui.manualExposure && ui.evSupported && ui.evRange.last > ui.evRange.first) {
                    val span = ui.evRange.last - ui.evRange.first
                    GlassSlider(
                        label = "EV",
                        valueText = CameraMath.formatEv(ui.evIndex, ui.evStep),
                        fraction = if (span > 0) {
                            (ui.evIndex - ui.evRange.first).toFloat() / span
                        } else 0.5f,
                        enabled = true,
                        onChange = { f ->
                            onEvIndex(
                                (f * span).roundToInt() + ui.evRange.first
                            )
                        },
                        onCommit = { }
                    )
                }

                // ---- ISO ----
                GlassSlider(
                    label = "ISO",
                    valueText = "${ui.manualIso}",
                    fraction = CameraMath.isoToNorm(ui.manualIso, ui.isoRange),
                    enabled = ui.manualExposure,
                    autoLabel = if (ui.manualExposure) "M" else "AUTO",
                    onAutoToggle = onToggleExposure,
                    onChange = { f -> onIso(CameraMath.normToIso(f, ui.isoRange)) },
                    onCommit = onIsoCommit
                )

                // ---- Shutter ----
                GlassSlider(
                    label = "TIME",
                    valueText = CameraMath.formatShutter(ui.manualShutterNs),
                    fraction = CameraMath.shutterToNorm(ui.manualShutterNs, ui.shutterRange),
                    enabled = ui.manualExposure,
                    autoLabel = if (ui.manualExposure) "M" else "AUTO",
                    onAutoToggle = onToggleExposure,
                    onChange = { f -> onShutter(CameraMath.normToShutter(f, ui.shutterRange)) },
                    onCommit = onShutterCommit
                )

                // ---- White balance ----
                GlassSlider(
                    label = "WB",
                    valueText = CameraMath.formatKelvin(ui.wbKelvin),
                    fraction = (ui.wbKelvin - 2500f) / (9500f - 2500f),
                    enabled = ui.manualWb,
                    autoLabel = if (ui.manualWb) "M" else "AUTO",
                    onAutoToggle = onToggleWb,
                    onChange = { f ->
                        onKelvin(((f * (9500 - 2500)).roundToInt() / 50 * 50) + 2500)
                    },
                    onCommit = onKelvinCommit
                )

                // ---- Focus ----
                if (ui.minFocusDiopters > 0f) {
                    GlassSlider(
                        label = "FOCUS",
                        valueText = CameraMath.formatDiopters(ui.focusDiopters),
                        fraction = if (ui.minFocusDiopters > 0f) {
                            (ui.focusDiopters / ui.minFocusDiopters).coerceIn(0f, 1f)
                        } else 0f,
                        enabled = ui.manualFocus,
                        autoLabel = if (ui.manualFocus) "M" else "AUTO",
                        onAutoToggle = onToggleFocus,
                        onChange = { f -> onDiopters(f * ui.minFocusDiopters) },
                        onCommit = onDioptersCommit
                    )
                }
            }
        }
    }
}
