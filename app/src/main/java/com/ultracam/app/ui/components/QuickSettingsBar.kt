package com.ultracam.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.FlashAuto
import androidx.compose.material.icons.rounded.FlashOff
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.GridOn
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.ultracam.app.ui.GridMode
import com.ultracam.app.ui.UiState
import com.ultracam.app.ui.theme.PixelPillBg
import com.ultracam.app.ui.theme.PixelYellow

/**
 * Pixel Camera Top Bar: Aspect Ratio chip (4:3, 16:9, 1:1, FULL),
 * Flash, Timer, Grid, and Settings.
 */
@Composable
fun QuickSettingsBar(
    ui: UiState,
    onAspectRatioClick: () -> Unit,
    onFlashClick: () -> Unit,
    onTimerClick: () -> Unit,
    onGridClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ---- Aspect Ratio Pill (4:3, 16:9, 1:1, FULL) ----
        PixelQuickPill(
            onClick = onAspectRatioClick
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.AspectRatio,
                    contentDescription = "Aspect Ratio",
                    tint = PixelYellow,
                    modifier = Modifier.height(16.dp)
                )
                Text(
                    text = ui.aspectRatio.label,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif,
                    color = Color.White
                )
            }
        }

        // ---- Flash Pill ----
        PixelQuickPill(
            onClick = onFlashClick
        ) {
            val icon = when (ui.flash) {
                FlashMode.OFF -> Icons.Rounded.FlashOff
                FlashMode.AUTO -> Icons.Rounded.FlashAuto
                FlashMode.ON -> Icons.Rounded.FlashOn
            }
            Icon(
                imageVector = icon,
                contentDescription = "Flash",
                tint = if (ui.flash != FlashMode.OFF) PixelYellow else Color.White.copy(alpha = 0.7f),
                modifier = Modifier.height(18.dp)
            )
        }

        // ---- Timer Pill ----
        PixelQuickPill(
            onClick = onTimerClick
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Timer,
                    contentDescription = "Timer",
                    tint = if (ui.timerSec > 0) PixelYellow else Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.height(18.dp)
                )
                if (ui.timerSec > 0) {
                    Text(
                        text = "${ui.timerSec}s",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = PixelYellow
                    )
                }
            }
        }

        // ---- Grid Pill ----
        PixelQuickPill(
            onClick = onGridClick
        ) {
            Icon(
                imageVector = Icons.Rounded.GridOn,
                contentDescription = "Grid",
                tint = if (ui.grid != GridMode.OFF) PixelYellow else Color.White.copy(alpha = 0.7f),
                modifier = Modifier.height(18.dp)
            )
        }

        // ---- Settings Pill ----
        PixelQuickPill(
            onClick = onSettingsClick
        ) {
            Icon(
                imageVector = Icons.Rounded.Settings,
                contentDescription = "Settings",
                tint = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.height(18.dp)
            )
        }
    }
}

@Composable
private fun PixelQuickPill(
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        Modifier
            .height(34.dp)
            .clip(RoundedCornerShape(50))
            .background(PixelPillBg)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
