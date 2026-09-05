package com.ultracam.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CropFree
import androidx.compose.material.icons.rounded.LocalFlorist
import androidx.compose.material.icons.rounded.FlashAuto
import androidx.compose.material.icons.rounded.FlashOff
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Timer
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
import com.ultracam.app.camera.AspectRatioOption
import com.ultracam.app.ui.FlashMode
import com.ultracam.app.ui.UiState
import com.ultracam.app.ui.theme.PixelPillBg
import com.ultracam.app.ui.theme.PixelPurpleAccent
import com.ultracam.app.ui.theme.PixelPurpleDark
import com.ultracam.app.ui.theme.PixelYellow

/**
 * Pixel Camera Viewfinder Floating Overlays:
 * - Top Center Dropdown Arrow Pill ( ∨ )
 * - Top Left HDR Pill (HDR On)
 * - Left Floating Capsule Bar (Flash, Aspect Ratio, Timer, Settings)
 * - Right Macro Card Pill
 * - Bottom Left Gallery Button
 * - Bottom Right Sparkles Button
 */
@Composable
fun PixelViewfinderOverlays(
    ui: UiState,
    onDropdownClick: () -> Unit,
    onHdrToggle: () -> Unit,
    onVideoResClick: () -> Unit = {},
    onVideoFpsClick: () -> Unit = {},
    onFlashClick: () -> Unit,
    onAspectRatioClick: () -> Unit,
    onTimerClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onMacroClick: () -> Unit,
    onGalleryClick: () -> Unit,
    onFxClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier.fillMaxSize()) {
        // ---- 1. Top Center Dropdown Arrow Pill ( ∨ ) or Recording Indicator ----
        if (ui.recordingVideo) {
            val mins = ui.recordingDurationSec / 60
            val secs = ui.recordingDurationSec % 60
            val timeStr = String.format(java.util.Locale.US, "%02d:%02d", mins, secs)

            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 10.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFFCC1100))
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                    Text(
                        text = "REC $timeStr",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 10.dp)
                    .clip(RoundedCornerShape(50))
                    .background(PixelPillBg)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDropdownClick
                    )
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = "Quick Settings",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // ---- 2. Top Left Controls: Video Quality Pills or HDR Chip ----
        if (ui.mode == com.ultracam.app.ui.CaptureMode.VIDEO || ui.mode == com.ultracam.app.ui.CaptureMode.CINEMATIC) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 14.dp, top = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Resolution Pill (720p, 1080p, 4K)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(PixelPurpleAccent)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onVideoResClick
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = ui.videoRes.label,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif,
                        color = PixelPurpleDark
                    )
                }

                // FPS Pill (30, 60, 120 FPS)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(PixelPillBg)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onVideoFpsClick
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${ui.videoFps} FPS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif,
                        color = PixelYellow
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 14.dp, top = 14.dp)
                    .clip(RoundedCornerShape(50))
                    .background(PixelPurpleAccent)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onHdrToggle
                    )
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        text = "HDR",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif,
                        color = PixelPurpleDark
                    )
                    Text(
                        text = "On",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Normal,
                        color = PixelPurpleDark.copy(alpha = 0.8f)
                    )
                }
            }
        }

        // ---- 3. Left Vertical Capsule Floating Toolbar ----
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 14.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(PixelPillBg)
                .padding(vertical = 14.dp, horizontal = 10.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Flash Toggle Icon
                val flashIcon = when (ui.flash) {
                    FlashMode.OFF -> Icons.Rounded.FlashOff
                    FlashMode.AUTO -> Icons.Rounded.FlashAuto
                    FlashMode.ON -> Icons.Rounded.FlashOn
                }
                Icon(
                    imageVector = flashIcon,
                    contentDescription = "Flash",
                    tint = if (ui.flash != FlashMode.OFF) PixelYellow else Color.White,
                    modifier = Modifier
                        .size(22.dp)
                        .clickable { onFlashClick() }
                )

                // Aspect Ratio Chip Button (4:3, 16:9, 1:1, FULL)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onAspectRatioClick() }
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = ui.aspectRatio.label,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif,
                        color = Color.White
                    )
                }

                // Timer Icon
                Icon(
                    imageVector = Icons.Rounded.Timer,
                    contentDescription = "Timer",
                    tint = if (ui.timerSec > 0) PixelYellow else Color.White,
                    modifier = Modifier
                        .size(22.dp)
                        .clickable { onTimerClick() }
                )

                // Settings Gear Icon
                Icon(
                    imageVector = Icons.Rounded.Settings,
                    contentDescription = "Settings",
                    tint = Color.White,
                    modifier = Modifier
                        .size(22.dp)
                        .clickable { onSettingsClick() }
                )
            }
        }

        // ---- 4. Right Floating Macro Card ----
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 14.dp)
                .clip(RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp, topEnd = 6.dp, bottomEnd = 6.dp))
                .background(PixelPillBg)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onMacroClick
                )
                .padding(horizontal = 14.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Rounded.LocalFlorist,
                    contentDescription = "Macro",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Macro",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Text(
                    text = "Off",
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }

        // ---- 5. Bottom Left Gallery Shortcut Button ----
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = 16.dp)
                .size(48.dp)
                .clip(CircleShape)
                .background(PixelPillBg)
                .clickable { onGalleryClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.PhotoLibrary,
                contentDescription = "Gallery",
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }

        // ---- 6. Bottom Right Sparkles FX Button ----
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 16.dp)
                .size(48.dp)
                .clip(CircleShape)
                .background(PixelPillBg)
                .clickable { onFxClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.AutoAwesome,
                contentDescription = "Effects",
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
