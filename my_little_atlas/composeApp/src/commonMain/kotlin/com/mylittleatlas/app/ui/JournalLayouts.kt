package com.mylittleatlas.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mylittleatlas.app.domain.Place
import com.mylittleatlas.app.domain.Visit
import com.mylittleatlas.app.domain.JournalLayoutCatalog
import org.jetbrains.compose.resources.painterResource

enum class JournalPageStyle(val id: String, val title: String, val description: String) {
    WARM("warm", "暖色手账", "单图 · 胶带相纸"),
    FILM("film", "胶片日记", "单图 · 深色胶片"),
    NOTE("note", "纸上小记", "单图 · 撕纸便笺"),
    POSTCARD("postcard", "旅途明信片", "单图 · 邮戳留白"),
    DUO("duo", "双影拼贴", "双图 · 左右错落"),
    STORY("story", "上下叙事", "双图 · 上下交叠"),
    SQUARE("square", "方形留影", "方形满版 · 角落纸签");

    companion object {
        fun from(id: String) = entries.firstOrNull { it.id == JournalLayoutCatalog.baseId(id) } ?: WARM
    }
}

data class JournalStyleChoice(val style: JournalPageStyle, val mirrored: Boolean) {
    val id: String get() = JournalLayoutCatalog.mirrorId(style.id, mirrored)
    val title: String get() = if (mirrored) "${style.title} · 镜像" else style.title
    val description: String get() = if (mirrored) "左右镜像 · 保持照片顺序" else style.description
}

val allJournalStyleChoices: List<JournalStyleChoice> = JournalPageStyle.entries.flatMap { style ->
    listOf(JournalStyleChoice(style, false), JournalStyleChoice(style, true))
}

fun isMirroredJournalLayout(id: String): Boolean = JournalLayoutCatalog.isMirror(id)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun JournalStylePicker(selected: String, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        // Leave room for per-item pixel rounding so three cards fit at every density.
        val tileWidth = (maxWidth - 20.dp) / 3
        FlowRow(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            allJournalStyleChoices.forEach { choice ->
                val active = selected == choice.id
                Column(Modifier.width(tileWidth).clip(RoundedCornerShape(9.dp))
                    .background(if(active) AtlasColors.SagePale else AtlasColors.Card)
                    .border(if(active) 1.5.dp else .5.dp, if(active) AtlasColors.Green else AtlasColors.Line, RoundedCornerShape(9.dp))
                    .selectable(active, role = Role.RadioButton, onClick = { onSelect(choice.id) })
                    .padding(7.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.fillMaxWidth().height(48.dp)) {
                        JournalLayoutThumbnail(choice.style, choice.mirrored, Modifier.fillMaxSize())
                        if(active) AtlasIcon(Glyph.CHECK, Modifier.align(Alignment.TopEnd), AtlasColors.Green, 12.dp)
                    }
                    Text(choice.title, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                    Text(choice.description, fontSize = 8.sp, color = AtlasColors.Muted, modifier = Modifier.padding(top = 3.dp))
                }
            }
        }
    }
}

@Composable
private fun JournalLayoutThumbnail(style: JournalPageStyle, mirrored: Boolean, modifier: Modifier) {
    Canvas(modifier) {
        val w = size.width; val h = size.height
        fun frame(x: Float, y: Float, width: Float, height: Float, film: Boolean = false) {
            drawRect(if(film) AtlasColors.Ink else Color.White, Offset(w*x,h*y), Size(w*width,h*height))
            drawRect(AtlasColors.Sage.copy(alpha = .35f), Offset(w*x+3.dp.toPx(),h*y+3.dp.toPx()),
                Size((w*width-6.dp.toPx()).coerceAtLeast(1f),(h*height-10.dp.toPx()).coerceAtLeast(1f)))
        }
        fun x(value: Float, width: Float = 0f) = if (mirrored) 1f - value - width else value
        when(style) {
            JournalPageStyle.DUO -> { frame(x(.05f,.46f),.08f,.46f,.76f); frame(x(.49f,.46f),.20f,.46f,.76f) }
            JournalPageStyle.STORY -> { frame(x(.05f,.66f),.02f,.66f,.52f); frame(x(.29f,.66f),.46f,.66f,.52f) }
            JournalPageStyle.POSTCARD -> {
                drawRect(Color.White, Offset(w*.02f,h*.12f), Size(w*.96f,h*.76f))
                frame(x(.04f,.61f),.16f,.61f,.68f)
                drawRect(AtlasColors.Peach.copy(alpha=.5f),Offset(w*x(.76f,.12f),h*.20f),Size(w*.12f,h*.19f),style=Stroke(1.dp.toPx()))
                repeat(3) { drawLine(AtlasColors.Sage,Offset(w*x(.70f,.21f),h*(.49f+it*.12f)),Offset(w*x(.70f,.21f)+w*.21f,h*(.49f+it*.12f)),1.dp.toPx()) }
            }
            JournalPageStyle.NOTE -> {
                drawRoundRect(AtlasColors.PeachPale,Offset(w*.08f,h*.04f),Size(w*.84f,h*.92f),CornerRadius(3.dp.toPx()))
                frame(x(.14f,.72f),.12f,.72f,.70f)
            }
            JournalPageStyle.SQUARE -> { drawRect(AtlasColors.PeachPale, Offset(w*.12f,h*.06f), Size(w*.76f,h*.78f)); frame(x(.18f,.64f),.10f,.64f,.64f) }
            else -> frame(x(.12f,.76f),.05f,.76f,.90f,style==JournalPageStyle.FILM)
        }
        if(style==JournalPageStyle.WARM) drawRect(Color(0xFFDCCB9E),Offset(w*.36f,0f),Size(w*.28f,h*.14f))
    }
}

/** A fixed composition shared by the notebook, detail view, editor preview and exported poster. */
@Composable
fun JournalPhotoComposition(place: Place, visit: Visit, modifier: Modifier = Modifier,
    showPlaceCover: Boolean = false, onOpen: (() -> Unit)? = null) {
    val sources = visit.photoPaths.map { "local:$it" }.ifEmpty { if(showPlaceCover) listOf(place.cover) else emptyList() }
    val style = JournalPageStyle.from(visit.layout)
    val mirrored = isMirroredJournalLayout(visit.layout)
    BoxWithConstraints(modifier) {
        val w = maxWidth; val h = maxHeight
        if(sources.isEmpty()) {
            PaperNote(Modifier.fillMaxSize().rotate(-2f)) {
                Column(Modifier.align(Alignment.Center)) {
                    Handwritten(place.name, size = 22)
                    Text("这一天，也值得记住。", fontSize = 11.sp, color = AtlasColors.Sage, modifier = Modifier.padding(top=14.dp))
                }
            }
        } else when {
            style==JournalPageStyle.DUO && sources.size>1 -> {
                JournalPhotoCard(sources,visit,0,JournalPageStyle.WARM,Modifier.align(if(mirrored) Alignment.TopEnd else Alignment.TopStart).offset(y=h*.03f).width(w*.64f).height(h*.82f).rotate(if(mirrored) 5f else -5f),onOpen)
                JournalPhotoCard(sources,visit,1,JournalPageStyle.WARM,Modifier.align(if(mirrored) Alignment.TopStart else Alignment.TopEnd).offset(y=h*.16f).width(w*.64f).height(h*.78f).rotate(if(mirrored) -4f else 4f),onOpen)
            }
            style==JournalPageStyle.STORY && sources.size>1 -> {
                JournalPhotoCard(sources,visit,0,JournalPageStyle.FILM,Modifier.align(if(mirrored) Alignment.TopEnd else Alignment.TopStart).width(w*.82f).height(h*.50f).rotate(if(mirrored) 3f else -3f),onOpen)
                JournalPhotoCard(sources,visit,1,JournalPageStyle.WARM,Modifier.align(if(mirrored) Alignment.BottomStart else Alignment.BottomEnd).width(w*.82f).height(h*.50f).rotate(if(mirrored) -4f else 4f),onOpen)
            }
            style==JournalPageStyle.FILM && sources.size>1 -> {
                JournalPhotoCard(sources,visit,0,JournalPageStyle.FILM,Modifier.align(if(mirrored) Alignment.TopEnd else Alignment.TopStart).width(w*.76f).height(h*.56f).rotate(if(mirrored) 3f else -3f),onOpen)
                JournalPhotoCard(sources,visit,1,JournalPageStyle.FILM,Modifier.align(if(mirrored) Alignment.BottomStart else Alignment.BottomEnd).width(w*.76f).height(h*.54f).rotate(if(mirrored) -3f else 3f),onOpen)
            }
            style==JournalPageStyle.SQUARE -> {
                val side = minOf(w*.82f, h*.92f)
                JournalSquareCard(sources, visit, 0,
                    Modifier.align(Alignment.Center).width(side).aspectRatio(1f).rotate(if(mirrored) 2f else -2f), onOpen)
            }
            style==JournalPageStyle.POSTCARD -> {
                Row(Modifier.fillMaxSize().rotate(if(mirrored) 2f else -2f).background(AtlasColors.Card).padding(9.dp)) {
                    if(mirrored) PostcardInfo(place,visit,w,onOpen) else JournalPhotoCarousel(sources,visit.id,0,Modifier.weight(1f).fillMaxHeight(),onOpen)
                    if(!mirrored) PostcardInfo(place,visit,w,onOpen) else JournalPhotoCarousel(sources,visit.id,0,Modifier.weight(1f).fillMaxHeight(),onOpen)
                }
            }
            else -> JournalPhotoCard(sources,visit,0,style,Modifier.fillMaxSize().rotate(if(style==JournalPageStyle.FILM) (if(mirrored) -2f else 2f) else if(mirrored) 4f else -4f),onOpen)
        }
    }
}

@Composable
private fun PostcardInfo(place: Place, visit: Visit, width: androidx.compose.ui.unit.Dp, onOpen: (() -> Unit)?) {
    Column(Modifier.width(width*.25f).fillMaxHeight().padding(start=8.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.SpaceBetween) {
                        CoffeePostmark(Modifier.size(42.dp))
                        Column(verticalArrangement=Arrangement.spacedBy(7.dp)) {
                            Text(place.city,fontSize=10.sp,color=AtlasColors.Sage)
                            repeat(3) { Box(Modifier.fillMaxWidth().height(.5.dp).background(AtlasColors.Line)) }
                            Text(visit.date.replace('-','.'),fontSize=8.sp,color=AtlasColors.Muted)
                        }
                        Text("POSTCARD",fontSize=7.sp,letterSpacing=1.sp,color=AtlasColors.Sage)
    }
}

@Composable
private fun JournalSquareCard(sources: List<String>, visit: Visit, slot: Int, modifier: Modifier, onOpen: (() -> Unit)?) {
    Box(modifier.background(AtlasColors.Card).padding(8.dp)) {
        JournalPhotoCarousel(sources,visit.id,slot,Modifier.fillMaxSize(),onOpen)
        Tape(Modifier.align(Alignment.TopCenter).offset(y=(-7).dp).width(50.dp))
        Text("SQUARE MOMENT",fontSize=7.sp,letterSpacing=1.5.sp,color=AtlasColors.Sage,
            modifier=Modifier.align(Alignment.BottomStart).padding(start=9.dp,bottom=5.dp))
    }
}

@Composable
private fun JournalPhotoCard(sources: List<String>, visit: Visit, slot: Int, style: JournalPageStyle,
    modifier: Modifier, onOpen: (() -> Unit)?) {
    when(style) {
        JournalPageStyle.FILM -> Column(modifier.background(AtlasColors.Ink).padding(7.dp)) {
            Row(Modifier.fillMaxWidth().padding(bottom=5.dp),horizontalArrangement=Arrangement.SpaceBetween) {
                repeat(8) { Box(Modifier.size(5.dp,3.dp).background(AtlasColors.Paper)) }
            }
            JournalPhotoCarousel(sources,visit.id,slot,Modifier.weight(1f).fillMaxWidth(),onOpen)
            Text("A MOMENT TO KEEP",fontSize=7.sp,color=AtlasColors.Card,modifier=Modifier.padding(top=6.dp))
        }
        JournalPageStyle.NOTE -> PaperNote(modifier) {
            Column(Modifier.fillMaxSize()) {
                JournalPhotoCarousel(sources,visit.id,slot,Modifier.weight(1f).fillMaxWidth(),onOpen)
                Handwritten("today is a little gift",Modifier.padding(top=7.dp),size=11)
            }
        }
        else -> Box(modifier) {
            Column(Modifier.fillMaxSize().padding(6.dp)) {
                JournalPhotoCarousel(sources,visit.id,slot,Modifier.weight(1f).fillMaxWidth(),onOpen)
                Spacer(Modifier.height(20.dp))
            }
            Image(painterResource(ScrapbookArt.wideFrame),null,Modifier.matchParentSize(),contentScale=ContentScale.FillBounds)
            Text(visit.date.replace('-','.'),fontSize=8.sp,color=AtlasColors.Sage,modifier=Modifier.align(Alignment.BottomStart).padding(start=12.dp,bottom=6.dp))
            Tape(Modifier.align(Alignment.TopCenter).offset(y=(-6).dp).width(45.dp))
        }
    }
}

@Composable
private fun JournalPhotoCarousel(sources: List<String>, visitId: String, slot: Int, modifier: Modifier,
    onOpen: (() -> Unit)?) {
    key(visitId,slot,sources) {
        val pager = rememberPagerState(initialPage=slot.coerceAtMost(sources.lastIndex),pageCount={sources.size})
        // Consume the remainder at either photo edge before it can reach the notebook's pager.
        val photoScroll = remember { object : NestedScrollConnection {
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource) = Offset(available.x,0f)
            override suspend fun onPostFling(consumed: Velocity, available: Velocity) = Velocity(available.x,0f)
        } }
        Box(modifier.clip(RoundedCornerShape(2.dp)).nestedScroll(photoScroll).semantics {
            contentDescription="手账照片 ${slot+1}"
            stateDescription="第 ${pager.currentPage+1} 张，共 ${sources.size} 张"
        }) {
            HorizontalPager(pager,Modifier.fillMaxSize(),beyondViewportPageCount=1) { index ->
                Photo(sources[index],Modifier.fillMaxSize().then(if(onOpen!=null)Modifier.clickable(onClick=onOpen) else Modifier),"第 ${index+1} 张照片")
            }
            if(sources.size>1) Text("‹ ${pager.currentPage+1} / ${sources.size} ›",fontSize=9.sp,color=Color.White,
                modifier=Modifier.align(Alignment.BottomEnd).padding(5.dp).clip(RoundedCornerShape(10.dp)).background(AtlasColors.Ink.copy(alpha=.65f)).padding(horizontal=7.dp,vertical=3.dp))
        }
    }
}
