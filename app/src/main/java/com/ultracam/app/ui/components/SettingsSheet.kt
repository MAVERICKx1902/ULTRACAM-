package com.ultracam.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ultracam.app.ui.CaptureQuality
import com.ultracam.app.ui.UiState
import com.ultracam.app.ui.theme.PixelPillBg
import com.ultracam.app.ui.theme.PixelPurpleAccent
import com.ultracam.app.ui.theme.PixelPurpleDark
import com.ultracam.app.ui.theme.PixelYellow
import java.util.Locale

/**
 * Pixel Camera Settings Overlay (Google Material 3 Expressive UI):
 * Matching exact specs from user reference screenshot:
 * - Translucent Material 3 sheet card (RoundedCornerShape 32.dp)
 * - Soft dark teal & purple toggle segments
 * - Crisp resolution cards with checkmark indicators
 * - Sound toggle & hardware dossier
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
    val tealAccent = Color(0xFF2E4D54)
    val tealHighlight = Color(0xFF80EEFF)
    val sheetBg = Color(0xF016161A)

    AnimatedVisibility(
        visible = ui.showSettings,
        enter = fadeIn(tween(250, easing = FastOutSlowInEasing)),
        exit = fadeOut(tween(200, easing = FastOutSlowInEasing)),
        modifier = modifier
    ) {
        Box(Modifier.fillMaxSize()) {
            // Scrim overlay
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClose
                    )
            )

            // Material 3 Slide-over Settings Card
            AnimatedVisibility(
                visible = ui.showSettings,
                enter = slideInHorizontally(
                    initialOffsetX = { it },
                    animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f)
                ),
                exit = slideOutHorizontally(
                    targetOffsetX = { it },
                    animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f)
                ),
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(340.dp)
                        .padding(vertical = 12.dp, horizontal = 8.dp)
                        .clip(RoundedCornerShape(32.dp))
                        .background(sheetBg)
                        .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(32.dp))
                ) {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .padding(WindowInsets.statusBars.asPaddingValues())
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // ---- Header ----
                        SheetHeader(onClose = onClose)

                        // ---- Section 1: Capture Resolution Cards ----
                        SectionLabel("CAPTURE — RESOLUTION")
                        ui.resolutions.forEachIndexed { index, option ->
                            val isSelected = ui.currentResolution?.size == option.size
                            ResolutionCard(
                                title = option.label,
                                subtitle = String.format(
                                    Locale.US, "%.1f MP  ·  %s", option.megapixels, option.aspectLabel
                                ),
                                selected = isSelected,
                                onClick = { onSelectResolution(index) }
                            )
                        }

                        // ---- Section 2: Pipeline Quality Segment ----
                        SectionLabel("PIPELINE — QUALITY")
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(50))
                                .background(Color(0xFF202226))
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            QualitySegmentPill(
                                label = "MAX QUALITY",
                                active = ui.quality == CaptureQuality.QUALITY,
                                activeBg = tealAccent,
                                activeText = tealHighlight,
                                modifier = Modifier.weight(1f),
                                onClick = { onQuality(CaptureQuality.QUALITY) }
                            )
                            QualitySegmentPill(
                                label = "MIN LATENCY",
                                active = ui.quality == CaptureQuality.SPEED,
                                activeBg = tealAccent,
                                activeText = tealHighlight,
                                modifier = Modifier.weight(1f),
                                onClick = { onQuality(CaptureQuality.SPEED) }
                            )
                        }

                        // ---- Section 3: Target Frame Rate Segment ----
                        SectionLabel("PIPELINE — TARGET FRAME RATE")
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(50))
                                .background(Color(0xFF202226))
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val fpsTargets = listOf(0, 20, 30, 60, 120)
                            fpsTargets.forEach { fps ->
                                val active = ui.fpsTarget == fps
                                val label = if (fps == 0) "AUTO" else "$fps"
                                QualitySegmentPill(
                                    label = label,
                                    active = active,
                                    activeBg = tealAccent,
                                    activeText = tealHighlight,
                                    modifier = Modifier.weight(1f),
                                    onClick = { onFps(fps) }
                                )
                            }
                        }

                        // ---- Section 4: Sound Toggle Card ----
                        SectionLabel("SOUND")
                        ResolutionCard(
                            title = "Shutter click",
                            subtitle = "System camera sound",
                            selected = ui.soundOn,
                            onClick = onSound,
                            trailing = {
                                Icon(
                                    imageVector = if (ui.soundOn) Icons.Rounded.MusicNote else Icons.Rounded.MusicOff,
                                    contentDescription = null,
                                    tint = if (ui.soundOn) tealHighlight else Color.White.copy(alpha = 0.4f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        )

                        // ---- Section 5: Lenses Cards ----
                        SectionLabel("LENSES")
                        ui.lenses.forEachIndexed { index, lens ->
                            ResolutionCard(
                                title = lens.label,
                                subtitle = String.format(
                                    Locale.US, "%s  ·  %.0fMP  ·  ID %s",
                                    lens.focalText, lens.megapixels, lens.cameraId
                                ),
                                selected = ui.currentLens == lens,
                                onClick = { onSelectLens(index) }
                            )
                        }

                        // ---- Section 6: Hardware Info Dossier ----
                        SectionLabel("HARDWARE DOSSIER")
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFF1B1C20))
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ui.hardware.forEach { (key, value) ->
                                Row(
                                    Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = key,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        fontFamily = FontFamily.Monospace,
                                        color = tealHighlight.copy(alpha = 0.85f),
                                        modifier = Modifier.width(118.dp)
                                    )
                                    Text(
                                        text = value,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color.White.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "ULTRACAM 0.1.0 — Material You Camera",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color.White.copy(alpha = 0.35f),
                            modifier = Modifier.align(Alignment.CenterHorizontally)
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
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.SansSerif,
            letterSpacing = 1.2.sp,
            color = Color.White
        )
        Spacer(Modifier.weight(1f))
        Box(
            Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.12f))
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
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold,
        fontFamily = FontFamily.Monospace,
        letterSpacing = 1.4.sp,
        color = Color.White.copy(alpha = 0.45f),
        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
    )
}

@Composable
private fun ResolutionCard(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    trailing: (@Composable () -> Unit)? = null
) {
    val cardBg = if (selected) Color(0xFF262930) else Color(0xFF1D1F24)
    val borderColor = if (selected) Color(0xFF4A505C) else Color.Transparent

    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(cardBg)
            .border(1.dp, borderColor, RoundedCornerShape(20.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                fontFamily = FontFamily.SansSerif,
                color = Color.White
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = Color.White.copy(alpha = 0.55f)
            )
        }
        if (trailing != null) {
            trailing()
        } else if (selected) {
            Box(
                Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF80EEFF).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = Color(0xFF80EEFF),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun QualitySegmentPill(
    label: String,
    active: Boolean,
    activeBg: Color,
    activeText: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier
            .clip(RoundedCornerShape(50))
            .background(if (active) activeBg else Color.Transparent)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 10.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
            fontFamily = FontFamily.SansSerif,
            color = if (active) activeText else Color.White.copy(alpha = 0.65f)
        )
    }
}
