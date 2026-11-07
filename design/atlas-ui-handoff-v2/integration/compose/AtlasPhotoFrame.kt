package com.mylittleatlas.app.ui.handoff

// Integration example only; not compiled or copied into the app in this handoff.
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp

enum class AtlasFrameSpec(
    val width: Float, val height: Float,
    val photoX: Float, val photoY: Float, val photoWidth: Float, val photoHeight: Float,
) {
    Wide(344f, 233f, 6f, 6f, 332f, 203f),
    Thumb(140f, 150f, 7f, 7f, 126f, 118f),
    Small(135f, 128f, 6f, 6f, 123f, 104f),
    Portrait(180f, 210f, 8f, 8f, 164f, 174f),
}

/** Match the frame painter to spec. The caption slot remains real Compose text. */
@Composable
fun AtlasPhotoFrame(
    photo: Painter,
    frame: Painter,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    spec: AtlasFrameSpec = AtlasFrameSpec.Wide,
    scale: Float = 1f,
    rotationDegrees: Float = 0f,
    tape: Painter? = null,
    tapeRotationDegrees: Float = 4f,
    caption: @Composable () -> Unit = {},
) {
    // Parent layouts must leave space for the tape and rotated corners.
    Box(modifier.size((spec.width * scale).dp, (spec.height * scale).dp)
        .graphicsLayer { rotationZ = rotationDegrees }) {
        Image(photo, contentDescription,
            Modifier.offset((spec.photoX * scale).dp, (spec.photoY * scale).dp)
                .size((spec.photoWidth * scale).dp, (spec.photoHeight * scale).dp),
            contentScale = ContentScale.Crop, alignment = Alignment.Center)
        Image(frame, null, Modifier.matchParentSize(), contentScale = ContentScale.FillBounds)
        if (tape != null) {
            Image(tape, null,
                Modifier.align(Alignment.TopCenter).offset(y = (-8f * scale).dp)
                    .size((88f * scale).dp, (22f * scale).dp)
                    .graphicsLayer { rotationZ = tapeRotationDegrees },
                contentScale = ContentScale.FillBounds)
        }
        Box(Modifier.offset((6f * scale).dp, ((spec.photoY + spec.photoHeight + 3f) * scale).dp)
            .size(((spec.width - 12f) * scale).dp, ((spec.height - spec.photoY - spec.photoHeight - 4f) * scale).dp),
            contentAlignment = Alignment.Center) { caption() }
    }
}

@Composable
fun AtlasStickerBadge(
    painter: Painter,
    label: String?,
    locked: Boolean,
    modifier: Modifier = Modifier,
) {
    Image(painter, label, modifier, contentScale = ContentScale.Fit,
        alpha = if (locked) 0.34f else 1f,
        colorFilter = if (locked) ColorFilter.colorMatrix(
            ColorMatrix().apply { setToSaturation(0.08f) }) else null)
}
