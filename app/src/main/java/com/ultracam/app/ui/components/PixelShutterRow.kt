package com.ultracam.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.ultracam.app.ui.theme.PixelPillBg

/**
 * Pixel Camera Shutter Row:
 * Left: Gallery thumbnail chip
 * Center: Material Pixel shutter button (white outer ring + white inner disc)
 * Right: Camera switch button
 */
@Composable
fun PixelShutterRow(
    lastPhoto: ImageBitmap?,
    capturing: Boolean,
    isVideoMode: Boolean = false,
    isRecordingVideo: Boolean = false,
    onShutterTap: () -> Unit,
    onGalleryTap: () -> Unit,
    onGalleryLongPress: () -> Unit,
    onFlipCamera: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier
            .fillMaxWidth()
            .height(96.dp)
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ---- Left: Gallery Thumbnail Chip ----
        Box(
            Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(PixelPillBg)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { onGalleryTap() },
                        onLongPress = { onGalleryLongPress() }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            if (lastPhoto != null) {
                Image(
                    bitmap = lastPhoto,
                    contentDescription = "Last Capture",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.PhotoLibrary,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // ---- Center: Material Pixel Shutter Button ----
        Box(
            Modifier.weight(1f),
            contentAlignment = Alignment.Center
        ) {
            PixelShutterButton(
                onClick = onShutterTap,
                capturing = capturing,
                isVideoMode = isVideoMode,
                isRecordingVideo = isRecordingVideo
            )
        }

        // ---- Right: Camera Switch Button ----
        Box(
            Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(PixelPillBg)
                .clickable { onFlipCamera() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Cameraswitch,
                contentDescription = "Switch Camera",
                tint = Color.White,
                modifier = Modifier.size(26.dp)
            )
        }
    }
}

@Composable
private fun PixelShutterButton(
    onClick: () -> Unit,
    capturing: Boolean,
    isVideoMode: Boolean,
    isRecordingVideo: Boolean
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed || capturing) 0.88f else 1.0f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 500f),
        label = "shutterScale"
    )

    val innerColor = when {
        isRecordingVideo -> Color(0xFFFF3B30)
        isVideoMode -> Color(0xFFFF5252)
        else -> com.ultracam.app.ui.theme.PixelPurpleAccent
    }

    val innerShape = if (isRecordingVideo) RoundedCornerShape(12.dp) else CircleShape

    Box(
        Modifier
            .size(80.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .border(3.5.dp, Color.White, CircleShape)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(if (isRecordingVideo) 18.dp else 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .clip(innerShape)
                .background(innerColor)
        )
    }
}
