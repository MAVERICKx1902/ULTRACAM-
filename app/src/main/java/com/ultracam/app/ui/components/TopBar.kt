package com.ultracam.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ultracam.app.camera.CameraMath
import com.ultracam.app.sensors.Attitude
import com.ultracam.app.ui.UiState
import com.ultracam.app.ui.glass.GlassPanel
import com.ultracam.app.ui.theme.PrismGreen
import com.ultracam.app.ui.theme.ReadoutTextStyle
import com.ultracam.app.ui.theme.TinyLabelStyle
import java.util.Locale

/**
 * Glass instrument cluster: lens chip + compass on the first row,
 * a monospace telemetry strip underneath.
 */
@Composable
fun TopBar(
    ui: UiState,
    zoom: Float,
    attitude: Attitude,
    sheen: Float,
    onLensClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LensChip(ui, sheen, onLensClick)
            Spacer(Modifier.weight(1f))
            CompassChip(attitude, sheen, ui.levelOn)
        }
        Spacer(Modifier.height(8.dp))
        GlassPanel(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            sheen = sheen
        ) {
            Text(
                text = readoutLine(ui, zoom),
                style = ReadoutTextStyle.copy(color = Color.White.copy(alpha = 0.86f)),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 7.dp)
            )
        }
    }
}

@Composable
private fun LensChip(ui: UiState, sheen: Float, onClick: () -> Unit) {
    GlassPanel(
        shape = RoundedCornerShape(16.dp),
        sheen = sheen
    ) {
        Column(
            Modifier
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                )
                .padding(horizontal = 12.dp, vertical = 7.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = (ui.currentLens?.label ?: "—") +
                    if (ui.lenses.size > 1) " ▾" else "",
                style = TinyLabelStyle.copy(
                    color = Color.White,
                    fontSize = TinyLabelStyle.fontSize
                )
            )
            val mp = ui.currentResolution?.megapixels ?: ui.currentLens?.megapixels ?: 0f
            val focal = ui.currentLens?.focalText ?: ""
            Text(
                text = String.format(Locale.US, "%.0fMP · %s", mp, focal),
                style = TinyLabelStyle.copy(color = Color.White.copy(alpha = 0.55f))
            )
        }
    }
}

@Composable
private fun CompassChip(attitude: Attitude, sheen: Float, showLevel: Boolean) {
    GlassPanel(shape = RoundedCornerShape(16.dp), sheen = sheen) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "${CameraMath.compassLabel(attitude.azimuthDeg)} " +
                    String.format(Locale.US, "%.0f°", ((attitude.azimuthDeg % 360f) + 360f) % 360f),
                style = TinyLabelStyle
            )
            if (showLevel) {
                Text(
                    text = attitude.pitchText,
                    style = TinyLabelStyle.copy(
                        color = if (attitude.isLevel) PrismGreen else Color.White.copy(alpha = 0.7f)
                    )
                )
            }
        }
    }
}

private fun readoutLine(ui: UiState, zoom: Float): String {
    val parts = mutableListOf<String>()
    parts += if (ui.manualExposure) {
        "ISO${ui.manualIso} ${CameraMath.formatShutter(ui.manualShutterNs)}"
    } else if (ui.evSupported && ui.evIndex != 0) {
        "AE ${CameraMath.formatEv(ui.evIndex, ui.evStep)}"
    } else {
        "AE AUTO"
    }
    parts += if (ui.manualWb) "${ui.wbKelvin}K" else "AWB"
    parts += if (ui.manualFocus) "MF" else "AF"
    parts += if (ui.fpsTarget > 0) "${ui.fpsTarget}FPS" else "FPS AUTO"
    parts += String.format(Locale.US, "%.1f×", zoom)
    return parts.joinToString("  ·  ")
}
