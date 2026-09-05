package com.ultracam.app.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ultracam.app.ui.glass.AuroraBackground
import com.ultracam.app.ui.glass.GlassPanel
import com.ultracam.app.ui.glass.PrismLogo
import com.ultracam.app.ui.glass.prismGradientBrush
import com.ultracam.app.ui.theme.TinyLabelStyle
import com.ultracam.app.ui.theme.Void

/**
 * First-run screen: the prism sigil, the promise, and the camera permission
 * gate. If the permission was permanently denied, deep-links to app settings.
 */
@Composable
fun OnboardingScreen(
    deniedForever: Boolean,
    onGrant: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Box(
        modifier
            .fillMaxSize()
            .background(Void)
    ) {
        AuroraBackground(Modifier.fillMaxSize())

        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            PrismLogo(
                modifier = Modifier
                    .size(190.dp)
                    .padding(bottom = 8.dp)
            )

            Spacer(Modifier.height(10.dp))

            Text(
                text = "ULTRACAM",
                color = Color.White,
                fontSize = 34.sp,
                letterSpacing = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Liquid optics. Raw hardware.",
                style = TinyLabelStyle.copy(
                    color = Color.White.copy(alpha = 0.65f),
                    fontSize = 11.sp
                )
            )

            Spacer(Modifier.height(28.dp))

            GlassPanel(shape = RoundedCornerShape(20.dp), fillAlpha = 0.13f) {
                Text(
                    text = "ULTRACAM talks straight to your camera hardware: " +
                        "every lens, the full sensor readout, manual ISO, shutter, " +
                        "white balance and focus — plus live histograms, focus " +
                        "peaking and a horizon level from your motion sensors.\n\n" +
                        "It needs camera access to do any of that.",
                    style = TinyLabelStyle.copy(
                        color = Color.White.copy(alpha = 0.78f),
                        fontSize = 10.sp,
                        lineHeight = 16.sp
                    ),
                    textAlign = TextAlign.Start,
                    modifier = Modifier.padding(18.dp)
                )
            }

            Spacer(Modifier.height(26.dp))

            Box(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(prismGradientBrush())
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = if (deniedForever) {
                            { openAppSettings(context) }
                        } else {
                            onGrant
                        }
                    )
                    .padding(horizontal = 30.dp, vertical = 13.dp)
            ) {
                Text(
                    text = if (deniedForever) "OPEN SETTINGS" else "GRANT CAMERA ACCESS",
                    color = Void,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 2.sp
                )
            }

            if (!deniedForever) {
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "Nothing is uploaded. Shots stay in Pictures/ULTRACAM on your device.",
                    style = TinyLabelStyle.copy(
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 9.sp
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(260.dp)
                )
            }
        }
    }
}

private fun openAppSettings(context: android.content.Context) {
    try {
        val intent = Intent(
            android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null)
        )
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        // Some restricted contexts can't launch settings; nothing more to do.
    }
}