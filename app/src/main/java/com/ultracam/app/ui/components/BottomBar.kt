package com.ultracam.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cameraswitch
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.ultracam.app.ui.UiState
import com.ultracam.app.ui.glass.GlassCircleButton
import com.ultracam.app.ui.glass.chromaticBorderBrush
import com.ultracam.app.ui.theme.PrismCyan
import com.ultracam.app.ui.theme.TinyLabelStyle
import java.util.Locale

/** Bottom control stack: pro panel (in PRO) → mode pill → zoom chips → shutter row. */
@Composable
fun BottomBar(
    ui: UiState,
    zoom: Float,
    proPanel: @Composable () -> Unit,
    onModeSelect: (com.ultracam.app.ui.CaptureMode) -> Unit,
    onZoomPreset: (Float) -> Unit,
    onShutterTap: () -> Unit,
    onShutterHoldStart: () -> Unit,
    onShutterHoldEnd: () -> Unit,
    onGalleryTap: () -> Unit,
    onGalleryLongPress: () -> Unit,
    onFlip: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier.padding(horizontal = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        proPanel()

        if (ui.bursting) {
            BurstChip(ui.burstCount)
            Spacer(Modifier.height(8.dp))
        }

        ModeSelector(ui.mode, onModeSelect)

        Spacer(Modifier.height(10.dp))

        ZoomChips(zoom, ui.zoomMin, ui.zoomMax, onZoomPreset)

        Spacer(Modifier.height(14.dp))

        Row(
            Modifier
                .fillMaxWidth()
                .height(82.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GalleryChip(
                thumbnail = ui.lastPhoto,
                onTap = onGalleryTap,
                onLongPress = onGalleryLongPress
            )
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                ShutterButton(
                    onTap = onShutterTap,
                    onHoldStart = onShutterHoldStart,
                    onHoldEnd = onShutterHoldEnd,
                    busy = ui.capturing || ui.bursting
                )
            }
            GlassCircleButton(
                icon = Icons.Rounded.Cameraswitch,
                active = false,
                size = 48.dp,
                contentDescription = "Switch lens",
                onClick = onFlip
            )
        }
    }
}

@Composable
private fun BurstChip(count: Int) {
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(PrismCyan.copy(alpha = 0.18f))
            .border(1.dp, PrismCyan.copy(alpha = 0.7f), RoundedCornerShape(50))
            .padding(horizontal = 14.dp, vertical = 5.dp)
    ) {
        Text(
            text = "BURST ×$count",
            style = TinyLabelStyle.copy(color = PrismCyan)
        )
    }
}

@Composable
private fun ZoomChips(
    zoom: Float,
    min: Float,
    max: Float,
    onSelect: (Float) -> Unit
) {
    val presets = mutableListOf<Float>()
    listOf(0.5f, 1f, 2f, 5f).forEach { p ->
        if (p >= min - 0.05f && p <= max + 0.05f && !presets.any { kotlin.math.abs(it - p) < 0.01f }) {
            presets += p
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        presets.forEach { p ->
            ZoomChip(
                label = String.format(Locale.US, "%s×", trimZoom(p)),
                active = kotlin.math.abs(zoom - p) < 0.06f,
                onClick = { onSelect(p) }
            )
        }
        if (max > (presets.lastOrNull() ?: 0f) + 0.3f) {
            ZoomChip(
                label = String.format(Locale.US, "%s×", trimZoom(max)),
                active = zoom > max - 0.15f,
                onClick = { onSelect(max) }
            )
        }
    }
}

private fun trimZoom(v: Float): String =
    if (kotlin.math.abs(v - v.toInt()) < 0.05f) "${v.toInt()}" else String.format(Locale.US, "%.1f", v)

@Composable
private fun ZoomChip(label: String, active: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(
                if (active) {
                    Brush.linearGradient(
                        listOf(PrismCyan.copy(alpha = 0.35f), PrismCyan.copy(alpha = 0.12f))
                    )
                } else {
                    Brush.linearGradient(
                        listOf(Color.White.copy(alpha = 0.12f), Color.White.copy(alpha = 0.04f))
                    )
                }
            )
            .border(
                0.8.dp,
                if (active) PrismCyan.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.18f),
                RoundedCornerShape(50)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 12.dp, vertical = 5.dp)
    ) {
        Text(
            text = label,
            style = TinyLabelStyle.copy(
                color = if (active) PrismCyan else Color.White.copy(alpha = 0.75f)
            )
        )
    }
}

@Composable
private fun GalleryChip(
    thumbnail: androidx.compose.ui.graphics.ImageBitmap?,
    onTap: () -> Unit,
    onLongPress: () -> Unit
) {
    Box(
        Modifier
            .size(52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .border(1.dp, chromaticBorderBrush(), RoundedCornerShape(14.dp))
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onTap() },
                    onLongPress = { onLongPress() }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        if (thumbnail != null) {
            Image(
                bitmap = thumbnail,
                contentDescription = "Last capture — tap to open, hold to share",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(
                imageVector = Icons.Rounded.PhotoLibrary,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.5f)
            )
        }
    }
}
