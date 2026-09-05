package com.ultracam.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ultracam.app.ui.CaptureMode
import com.ultracam.app.ui.glass.chromaticBorderBrush
import com.ultracam.app.ui.theme.ChipTextStyle
import com.ultracam.app.ui.theme.PrismCyan

/**
 * AUTO / PRO pill. The indicator slides with a springy "liquid" motion and
 * leaves a soft trail behind it.
 */
@Composable
fun ModeSelector(
    mode: CaptureMode,
    onSelect: (CaptureMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val itemWidth = 84.dp
    val indicatorOffset by animateDpAsState(
        targetValue = if (mode == CaptureMode.PRO) itemWidth else 0.dp,
        animationSpec = spring(dampingRatio = 0.62f, stiffness = 460f),
        label = "modeIndicator"
    )

    Box(
        modifier
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.07f))
            .border(1.dp, chromaticBorderBrush(), RoundedCornerShape(50))
    ) {
        Box(
            Modifier
                .offset(x = indicatorOffset)
                .width(itemWidth)
                .height(34.dp)
                .padding(3.dp)
                .clip(RoundedCornerShape(50))
                .background(
                    androidx.compose.ui.graphics.Brush.linearGradient(
                        listOf(PrismCyan.copy(alpha = 0.28f), PrismCyan.copy(alpha = 0.12f))
                    )
                )
        )
        Row {
            ModeLabel(
                "AUTO",
                mode == CaptureMode.AUTO,
                Modifier
                    .width(itemWidth)
                    .height(34.dp),
                onClick = { onSelect(CaptureMode.AUTO) }
            )
            ModeLabel(
                "PRO",
                mode == CaptureMode.PRO,
                Modifier
                    .width(itemWidth)
                    .height(34.dp),
                onClick = { onSelect(CaptureMode.PRO) }
            )
        }
    }
}

@Composable
private fun ModeLabel(
    text: String,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val color by animateColorAsState(
        targetValue = if (selected) Color.White else Color.White.copy(alpha = 0.55f),
        label = "modeLabelColor"
    )
    Box(
        modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick
        ),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, style = ChipTextStyle.copy(color = color))
    }
}
