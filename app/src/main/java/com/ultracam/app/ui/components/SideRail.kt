package com.ultracam.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.FlashAuto
import androidx.compose.material.icons.rounded.FlashOff
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.Grain
import androidx.compose.material.icons.rounded.GridOn
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Straighten
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ultracam.app.ui.FlashMode
import com.ultracam.app.ui.UiState
import com.ultracam.app.ui.glass.GlassCircleButton

/** Vertical quick-toggle rail floating on the right edge of the viewfinder. */
@Composable
fun SideRail(
    ui: UiState,
    onFlash: () -> Unit,
    onTorch: () -> Unit,
    onGrid: () -> Unit,
    onTimer: () -> Unit,
    onHistogram: () -> Unit,
    onPeaking: () -> Unit,
    onLevel: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier.padding(end = 10.dp, bottom = 150.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        val flashIcon = when (ui.flash) {
            FlashMode.OFF -> Icons.Rounded.FlashOff
            FlashMode.AUTO -> Icons.Rounded.FlashAuto
            FlashMode.ON -> Icons.Rounded.FlashOn
        }
        GlassCircleButton(
            icon = flashIcon,
            active = ui.flash != FlashMode.OFF,
            contentDescription = "Flash mode: ${ui.flash.label}",
            onClick = onFlash
        )
        if (ui.hasFlash) {
            GlassCircleButton(
                icon = Icons.Rounded.Bolt,
                active = ui.torch,
                contentDescription = "Torch",
                onClick = onTorch
            )
        }
        GlassCircleButton(
            icon = Icons.Rounded.GridOn,
            active = ui.grid != com.ultracam.app.ui.GridMode.OFF,
            contentDescription = "Grid: ${ui.grid.label}",
            onClick = onGrid
        )
        GlassCircleButton(
            icon = Icons.Rounded.Timer,
            active = ui.timerSec > 0,
            contentDescription = "Self timer: ${ui.timerSec}s",
            onClick = onTimer
        )
        GlassCircleButton(
            icon = Icons.Rounded.BarChart,
            active = ui.histogramOn,
            contentDescription = "Histogram",
            onClick = onHistogram
        )
        GlassCircleButton(
            icon = Icons.Rounded.Grain,
            active = ui.peakingOn,
            contentDescription = "Focus peaking",
            onClick = onPeaking
        )
        GlassCircleButton(
            icon = Icons.Rounded.Straighten,
            active = ui.levelOn,
            contentDescription = "Level",
            onClick = onLevel
        )
        GlassCircleButton(
            icon = Icons.Rounded.Settings,
            active = false,
            contentDescription = "Settings",
            onClick = onSettings
        )
    }
}
