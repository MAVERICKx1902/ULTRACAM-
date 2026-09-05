package com.ultracam.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ultracam.app.ui.glass.GlassPanel
import com.ultracam.app.ui.glass.prismGradientBrush
import com.ultracam.app.ui.theme.PrismAmber
import com.ultracam.app.ui.theme.TinyLabelStyle
import kotlinx.coroutines.flow.SharedFlow

/** Full-screen capture flash: white blink that fades like a Polaroid pop. */
@Composable
fun CaptureFlashOverlay(events: SharedFlow<Unit>, modifier: Modifier = Modifier) {
    val alpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        events.collect {
            alpha.snapTo(0.65f)
            alpha.animateTo(0f, tween(340))
        }
    }
    Box(
        modifier
            .fillMaxSize()
            .graphicsLayer { this.alpha = alpha.value }
            .background(Color.White)
    )
}

/** Big countdown with a pulsing prism digit. Tapping cancels. */
@Composable
fun CountdownOverlay(
    countdown: Int,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (countdown <= 0) return
    val transition = rememberInfiniteTransition(label = "countdownPulse")
    val pulse by transition.animateFloat(
        initialValue = 0.86f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
        label = "countdownPulseScale"
    )
    Box(
        modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.30f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onCancel
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$countdown",
                color = Color.White,
                fontSize = 92.sp,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                modifier = Modifier.graphicsLayer {
                    scaleX = pulse
                    scaleY = pulse
                }
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "TAP TO CANCEL",
                style = TinyLabelStyle.copy(color = PrismAmber)
            )
        }
    }
}

/** Glass error card with retry. */
@Composable
fun ErrorCard(
    error: String?,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = error != null,
        enter = fadeIn() + scaleIn(initialScale = 0.92f),
        exit = fadeOut() + scaleOut(targetScale = 0.92f),
        modifier = modifier
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            GlassPanel(
                shape = RoundedCornerShape(22.dp),
                fillAlpha = 0.2f
            ) {
                Column(
                    Modifier.padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Warning,
                        contentDescription = null,
                        tint = PrismAmber
                    )
                    Text(
                        text = error ?: "",
                        style = TinyLabelStyle.copy(color = Color.White, fontSize = 11.sp),
                        textAlign = TextAlign.Center
                    )
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(50))
                            .background(prismGradientBrush())
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onRetry
                            )
                            .padding(horizontal = 18.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = null,
                                tint = Color(0xFF05070D),
                                modifier = Modifier.padding(end = 6.dp).height(14.dp)
                            )
                            Text(
                                text = "RETRY",
                                style = TinyLabelStyle.copy(color = Color(0xFF05070D))
                            )
                        }
                    }
                }
            }
        }
    }
}
