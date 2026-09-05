package com.ultracam.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.MusicOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ultracam.app.ui.CaptureQuality
import com.ultracam.app.ui.UiState
import com.ultracam.app.ui.glass.GlassPanel
import com.ultracam.app.ui.glass.chromaticBorderBrush
import com.ultracam.app.ui.theme.PrismCyan
import com.ultracam.app.ui.theme.PrismViolet
import com.ultracam.app.ui.theme.TinyLabelStyle
import java.util.Locale

/**
 * Right slide-over glass sheet: capture pipeline (resolution, quality,
 * frame rate, sound), lens selection, and the full hardware dossier.
 */
@Composable
fun SettingsSheet(
    ui: UiState,
    sheen: Float,
    onClose: () -> Unit,
    onSelectResolution: (Int) -> Unit,
    onSelectLens: (Int) -> Unit,
    onQuality: (CaptureQuality) -> Unit,
    onFps: (Int) -> Unit,
    onSound: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = ui.showSettings,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Box(Modifier.fillMaxSize()) {
            // scrim
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClose
                    )
            )
            AnimatedVisibility(
                visible = ui.showSettings,
                enter = slideInHorizontally { it },
                exit = slideOutHorizontally { it },
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                GlassPanel(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(328.dp)
                        .padding(vertical = 10.dp),
                    shape = RoundedCornerShape(26.dp),
                    sheen = sheen,
                    fillAlpha = 0.16f
                ) {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        SheetHeader(onClose)

                        SectionLabel("CAPTURE — RESOLUTION")
                        ui.resolutions.forEachIndexed { index, option ->
                            SelectRow(
                                title = option.label,
                                subtitle = String.format(
                                    Locale.US, "%.1f MP · %s", option.megapixels, option.aspectLabel
                                ),
                                selected = ui.currentResolution?.size == option.size,
                                onClick = { onSelectResolution(index) }
                            )
                        }

                        SectionLabel("PIPELINE — QUALITY")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            QualityChip(
                                "MAX QUALITY",
                                ui.quality == CaptureQuality.QUALITY,
                                Modifier.weight(1f),
                                onClick = { onQuality(CaptureQuality.QUALITY) }
                            )
                            QualityChip(
                                "MIN LATENCY",
                                ui.quality == CaptureQuality.SPEED,
                                Modifier.weight(1f),
                                onClick = { onQuality(CaptureQuality.SPEED) }
                            )
                        }

                        SectionLabel("PIPELINE — TARGET FRAME RATE")
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            QualityChip(
                                "AUTO",
                                ui.fpsTarget == 0,
                                onClick = { onFps(0) }
                            )
                            ui.fpsOptions.take(4).forEach { fps ->
                                QualityChip(
                                    "$fps",
                                    ui.fpsTarget == fps,
                                    onClick = { onFps(fps) }
                                )
                            }
                        }

                        SectionLabel("SOUND")
                        SelectRow(
                            title = "Shutter click",
                            subtitle = "System camera sound",
                            selected = ui.soundOn,
                            onClick = onSound,
                            trailing = {
                                Icon(
                                    imageVector = if (ui.soundOn) {
                                        Icons.Rounded.MusicNote
                                    } else {
                                        Icons.Rounded.MusicOff
                                    },
                                    contentDescription = null,
                                    tint = if (ui.soundOn) PrismCyan else Color.White.copy(alpha = 0.4f)
                                )
                            }
                        )

                        SectionLabel("LENSES")
                        ui.lenses.forEachIndexed { index, lens ->
                            SelectRow(
                                title = lens.label,
                                subtitle = String.format(
                                    Locale.US,
                                    "%s · %.0fMP · ID %s",
                                    lens.focalText,
                                    lens.megapixels,
                                    lens.cameraId
                                ),
                                selected = ui.currentLens?.cameraId == lens.cameraId,
                                onClick = { onSelectLens(index) }
                            )
                        }

                        SectionLabel("HARDWARE")
                        ui.hardware.forEach { (key, value) ->
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = key,
                                    style = TinyLabelStyle.copy(
                                        color = PrismViolet.copy(alpha = 0.9f)
                                    ),
                                    modifier = Modifier.width(118.dp)
                                )
                                Text(
                                    text = value,
                                    style = TinyLabelStyle.copy(
                                        color = Color.White.copy(alpha = 0.82f),
                                        fontSize = 9.sp
                                    )
                                )
                            }
                        }

                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = "ULTRACAM 0.1.0 — liquid prism camera",
                            style = TinyLabelStyle.copy(color = Color.White.copy(alpha = 0.35f))
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SheetHeader(onClose: () -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "SETTINGS",
            style = TinyLabelStyle.copy(color = Color.White, fontSize = 12.sp)
        )
        Spacer(Modifier.weight(1f))
        Box(
            Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.10f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClose
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "Close settings",
                tint = Color.White
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = TinyLabelStyle.copy(color = Color.White.copy(alpha = 0.45f)),
        modifier = Modifier.padding(top = 6.dp)
    )
}

@Composable
private fun SelectRow(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (selected) PrismCyan.copy(alpha = 0.13f) else Color.White.copy(alpha = 0.05f)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = TinyLabelStyle.copy(
                    color = if (selected) PrismCyan else Color.White,
                    fontSize = 11.sp
                )
            )
            Text(
                text = subtitle,
                style = TinyLabelStyle.copy(color = Color.White.copy(alpha = 0.5f))
            )
        }
        if (trailing != null) {
            trailing()
        } else if (selected) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                tint = PrismCyan,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun QualityChip(
    label: String,
    active: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier
            .clip(RoundedCornerShape(50))
            .background(
                if (active) {
                    PrismCyan.copy(alpha = 0.25f)
                } else {
                    Color.White.copy(alpha = 0.07f)
                }
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 13.dp, vertical = 7.dp)
    ) {
        Text(
            text = label,
            style = TinyLabelStyle.copy(
                color = if (active) PrismCyan else Color.White.copy(alpha = 0.7f)
            )
        )
    }
}
