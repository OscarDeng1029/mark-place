package com.mylittleatlas.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.mylittleatlas.resources.*
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.imageResource
import org.jetbrains.compose.resources.painterResource

/** Central mapping: generated replacements can replace resources without changing pages. */
object ScrapbookArt {
    val wideFrame = Res.drawable.atlas_frame_polaroid_wide
    val smallFrame = Res.drawable.atlas_frame_polaroid_small
    val note = Res.drawable.atlas_paper_torn_note
    val cat = Res.drawable.atlas_doodle_cat
    val badges = listOf(Res.drawable.atlas_sticker_mountain, Res.drawable.atlas_sticker_coffee,
        Res.drawable.atlas_sticker_camera, Res.drawable.atlas_sticker_star)
}

@Composable
fun AtlasPaperTexture(modifier: Modifier = Modifier) {
    val tile = imageResource(Res.drawable.atlas_paper_cream_tile)
    Canvas(modifier) {
        val w = 192.dp.roundToPx().coerceAtLeast(1)
        val h = 48.dp.roundToPx().coerceAtLeast(1)
        for (y in 0..size.height.toInt() step h) {
            for (x in 0..size.width.toInt() step w) {
                drawImage(tile, dstOffset = IntOffset(x,y), dstSize = IntSize(w,h))
            }
        }
    }
}

@Composable
fun PaperNote(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(modifier) {
        Image(painterResource(ScrapbookArt.note), null, Modifier.matchParentSize(), contentScale = ContentScale.FillBounds)
        Box(Modifier.padding(16.dp), content = content)
    }
}

@Composable
fun PolaroidPhoto(cover: String, modifier: Modifier = Modifier, caption: String = "",
    rotation: Float = -2f, localPath: String? = null, showTape: Boolean = true) {
    BoxWithConstraints(modifier.rotate(rotation)) {
        val photoHeight = (maxHeight - 30.dp).coerceAtLeast(1.dp)
        if (localPath != null) com.mylittleatlas.app.platform.LocalPhoto(localPath,
            Modifier.padding(start=6.dp,top=6.dp).width((maxWidth-12.dp).coerceAtLeast(1.dp)).height(photoHeight))
        else Photo(cover, Modifier.padding(start=6.dp,top=6.dp).width((maxWidth-12.dp).coerceAtLeast(1.dp)).height(photoHeight))
        Image(painterResource(ScrapbookArt.wideFrame), null, Modifier.matchParentSize(), contentScale=ContentScale.FillBounds)
        if (caption.isNotBlank()) Handwritten(caption, Modifier.align(Alignment.BottomStart).padding(start=14.dp,bottom=6.dp), size=11)
        if (showTape) Tape(Modifier.align(Alignment.TopCenter).offset(y=(-8).dp).rotate(4f))
    }
}

@Composable
fun ScrapbookBadge(index: Int, description: String?, modifier: Modifier = Modifier, unlocked: Boolean = true) {
    Image(painterResource(ScrapbookArt.badges[index.coerceIn(0,3)]), description, modifier,
        contentScale=ContentScale.Fit, alpha=if(unlocked)1f else .34f,
        colorFilter=if(unlocked)null else ColorFilter.colorMatrix(ColorMatrix().apply{setToSaturation(.08f)}))
}

@Composable
fun CoffeePostmark(modifier: Modifier = Modifier) {
    Box(modifier.rotate(-12f),contentAlignment=Alignment.Center) {
        Image(painterResource(Res.drawable.atlas_stamp_ring), null, Modifier.matchParentSize())
        Image(painterResource(Res.drawable.atlas_stamp_coffee_ink), null, Modifier.fillMaxSize(.36f))
    }
}
