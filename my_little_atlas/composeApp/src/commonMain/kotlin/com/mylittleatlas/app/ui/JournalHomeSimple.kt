package com.mylittleatlas.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mylittleatlas.app.domain.*
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import kotlin.math.absoluteValue

@Composable
fun CityFilterRow(cities: List<String>, selected: String?, onCity: (String?)->Unit, modifier: Modifier = Modifier,
    count: (String?)->Int = { 0 }) {
    if(cities.size < 2) return
    Row(modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        (listOf<String?>(null) + cities).forEach { city ->
            FilterChip(selected = city == selected, onClick = { onCity(city) }, label = {
                Text((city ?: "全部城市") + "  " + count(city), fontSize = 11.sp)
            }, shape = RoundedCornerShape(8.dp), colors = FilterChipDefaults.filterChipColors(
                containerColor=Color(0xFFF1F2EA),labelColor=AtlasColors.Muted,
                selectedContainerColor=AtlasColors.SagePale,selectedLabelColor=AtlasColors.Green))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalHomeSimple(data: AtlasData, onOpen: (Visit)->Unit, onEdit: (Visit)->Unit, onWrite: ()->Unit,
    onCollect: ()->Unit, wide: Boolean, initialPage: Int, onPageChange: (Int)->Unit,
    requestedVisit: String?, onRequestConsumed: ()->Unit, directoryCity: String?, onDirectoryCity: (String?)->Unit) {
    val visits = data.journalVisits()
    val pager = rememberPagerState(initialPage = initialPage.coerceIn(0, (visits.size-1).coerceAtLeast(0)), pageCount = { visits.size.coerceAtLeast(1) })
    val scope = rememberCoroutineScope()
    var directory by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    LaunchedEffect(pager.currentPage) { onPageChange(pager.currentPage) }
    LaunchedEffect(requestedVisit, visits.map { it.id }) {
        val target = visits.indexOfFirst { it.id == requestedVisit }
        if(target >= 0) { pager.scrollToPage(target); onRequestConsumed() }
        else if(pager.currentPage >= visits.size && visits.isNotEmpty()) pager.scrollToPage(visits.lastIndex)
    }
    Column(Modifier.fillMaxSize().widthIn(max = if(wide)640.dp else 520.dp).padding(horizontal = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth().height(76.dp).padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("我的手账", style = MaterialTheme.typography.headlineMedium, fontSize = 25.sp)
                Text(if(visits.isEmpty()) "把生活，慢慢记下来" else "那些值得记住的日常", fontSize = 10.sp, color = AtlasColors.Muted, modifier = Modifier.padding(top=3.dp))
            }
            if(visits.isNotEmpty()) TextButton(onClick = { directory = true }) {
                AtlasIcon(Glyph.LIST, size = 17.dp); Spacer(Modifier.width(5.dp)); Text("目录", fontSize = 12.sp)
            }
        }
        HorizontalPager(pager, Modifier.weight(1f).fillMaxWidth().semantics { contentDescription = "我的手账，可左右翻页" },
            pageSpacing = 14.dp, beyondViewportPageCount = 1, key = { visits.getOrNull(it)?.id ?: "empty-notebook" }) { index ->
            val offset = ((pager.currentPage-index)+pager.currentPageOffsetFraction).coerceIn(-1f,1f)
            BookLeafV3(Modifier.fillMaxSize().graphicsLayer {
                rotationY = offset * 13f; transformOrigin = TransformOrigin(if(offset>0)0f else 1f,.5f)
                cameraDistance = 18f * density; alpha = 1f-offset.absoluteValue*.12f
            }, index+1) {
                val visit = visits.getOrNull(index)
                val place = data.places.find { it.id == visit?.placeId }
                if(visit != null && place != null) JournalMemoryLeaf(place, visit, index, data.isDemoPlace(place), { onOpen(visit) }, { onEdit(visit) })
                else EmptyJournalLeaf(onWrite, onCollect)
            }
        }
        if(visits.isNotEmpty()) Row(Modifier.fillMaxWidth().height(53.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            IconButton(Glyph.BACK,"前一页 →",{ scope.launch { pager.animateScrollToPage((pager.currentPage-1).coerceAtLeast(0)) } },Modifier.graphicsLayer { alpha=if(pager.currentPage>0) 1f else .25f })
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("第 ${pager.currentPage+1} / ${visits.size} 页", fontSize = 10.sp, color = AtlasColors.Green)
                Text("左右翻阅 · 最近的记录在前", fontSize = 8.sp, color = AtlasColors.Muted)
            }
            IconButton(Glyph.ARROW,"后一页 →",{ scope.launch { pager.animateScrollToPage((pager.currentPage+1).coerceAtMost(visits.lastIndex)) } },Modifier.graphicsLayer { alpha=if(pager.currentPage<visits.lastIndex) 1f else .25f })
        } else Text("每一次记录，都会长成新的一页。", fontSize = 10.sp, color = AtlasColors.Muted, modifier = Modifier.padding(vertical = 18.dp))
    }
    if(directory) {
        val cities = visits.mapNotNull { v -> data.places.find { it.id == v.placeId }?.city }.distinct()
        val activeCity = directoryCity?.takeIf { it in cities }
        val matches = visits.filter { v ->
            val place = data.places.first { it.id == v.placeId }
            (activeCity == null || place.city == activeCity) &&
            (query.isBlank() || listOf(place.name, place.city, v.date, v.title, v.note).any { it.contains(query.trim(), true) })
        }
        ModalBottomSheet(onDismissRequest = { directory = false }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Color(0xFFFDFCF8), modifier = Modifier.widthIn(max=640.dp)) {
            Column(Modifier.fillMaxWidth().fillMaxHeight(.78f).padding(horizontal = 22.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("手账目录", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    IconButton(Glyph.CLOSE,"关闭目录",{directory=false})
                }
                Text("点一下记录，翻到那一天", fontSize=11.sp, color=AtlasColors.Muted, modifier=Modifier.padding(bottom=10.dp))
                CityFilterRow(cities,activeCity,onDirectoryCity) { city -> visits.count { v -> city == null || data.places.any { it.id==v.placeId && it.city==city } } }
                OutlinedTextField(query,{query=it},Modifier.fillMaxWidth().padding(top=7.dp,bottom=14.dp),singleLine=true,
                    placeholder={Text("找地点、日期或记录的文字",fontSize=11.sp)}, leadingIcon={AtlasIcon(Glyph.SEARCH,size=16.dp)},shape=RoundedCornerShape(9.dp))
                LazyColumn(Modifier.weight(1f), contentPadding=PaddingValues(bottom=24.dp)) {
                    matches.groupBy { it.date.take(7) }.forEach { (month, entries) ->
                        item("month-$month") { Text(month.replace("-"," 年 ")+" 月",fontSize=11.sp,color=AtlasColors.Sage,modifier=Modifier.padding(top=8.dp,bottom=5.dp)) }
                        items(entries,key={it.id}) { visit ->
                            val place=data.places.first { it.id==visit.placeId }
                            Row(Modifier.fillMaxWidth().clickable {
                                directory=false
                                scope.launch { pager.scrollToPage(visits.indexOfFirst { it.id==visit.id }) }
                            }.padding(vertical=14.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                                Photo(visit.photoPaths.firstOrNull()?.let { "local:$it" } ?: place.cover,Modifier.size(54.dp,64.dp).clip(RoundedCornerShape(4.dp)))
                                Column(Modifier.weight(1f)) {
                                    Text(visit.date.drop(5).replace("-"," / ")+" · "+place.city,fontSize=10.sp,color=AtlasColors.Muted)
                                    Text(place.name,fontSize=13.sp,modifier=Modifier.padding(top=4.dp),maxLines=1,overflow=TextOverflow.Ellipsis)
                                    Text(visit.note.ifBlank { "在这里，留下一点生活。" },fontSize=10.sp,color=AtlasColors.Muted,maxLines=1,overflow=TextOverflow.Ellipsis,modifier=Modifier.padding(top=4.dp))
                                }
                                if(visits.getOrNull(pager.currentPage)?.id==visit.id) Text("正在\n翻阅",fontSize=9.sp,color=AtlasColors.Green)
                                else AtlasIcon(Glyph.ARROW,size=16.dp)
                            }
                            HorizontalDivider(color=AtlasColors.Line)
                        }
                    }
                    if(matches.isEmpty()) item { Text("没有找到这篇记录，换个城市或词语试试。",fontSize=12.sp,color=AtlasColors.Muted,modifier=Modifier.padding(vertical=24.dp)) }
                }
            }
        }
    }
}

@Composable
private fun EmptyJournalLeaf(onWrite:()->Unit,onCollect:()->Unit) {
    BoxWithConstraints(Modifier.fillMaxSize().padding(start=38.dp,end=23.dp,top=28.dp,bottom=30.dp)) {
        Text("MY LITTLE ATLAS",fontSize=9.sp,letterSpacing=2.sp,color=AtlasColors.Sage)
        Text("第一篇，\n从你开始。",style=MaterialTheme.typography.headlineMedium,fontSize=27.sp,lineHeight=42.sp,modifier=Modifier.padding(top=47.dp))
        Image(painterResource(ScrapbookArt.cat),null,Modifier.align(Alignment.Center).fillMaxWidth(.75f).height(103.dp))
        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
            Text("记下地点、日期和一点心情，\n就能自动变成一页手账。",fontSize=11.sp,lineHeight=22.sp,color=AtlasColors.Muted)
            Spacer(Modifier.height(22.dp))
            PrimaryButton("写下第一篇小记",onWrite,Modifier.fillMaxWidth(),Glyph.EDIT)
            TextButton(onClick=onCollect,modifier=Modifier.align(Alignment.CenterHorizontally)) { Text("先收藏一个想去的地方",fontSize=10.sp) }
        }
    }
}

@Composable
private fun JournalMemoryLeaf(place:Place,visit:Visit,index:Int,demo:Boolean,onOpen:()->Unit,onEdit:()->Unit) {
    BoxWithConstraints(Modifier.fillMaxSize().padding(start=37.dp,end=16.dp,top=23.dp,bottom=26.dp)) {
        val h=maxHeight;val w=maxWidth;val alternate=index%3==1
        val title=visit.displayTitle(place.name)
        val body=visit.displayBody()
        Column(Modifier.align(Alignment.TopStart)) {
            Text(visit.date.replace('-','.')+" · "+place.city,fontSize=10.sp,color=AtlasColors.Sage,letterSpacing=1.sp)
            Text(title,style=MaterialTheme.typography.titleLarge,fontSize=22.sp,lineHeight=32.sp,maxLines=2,overflow=TextOverflow.Ellipsis,modifier=Modifier.padding(top=12.dp))
            Row(Modifier.padding(top=9.dp).clickable(onClick=onOpen),verticalAlignment=Alignment.CenterVertically) {
                AtlasIcon(Glyph.PIN,size=12.dp,color=AtlasColors.Muted)
                Text(place.name+" · "+place.kind.title,fontSize=10.sp,color=AtlasColors.Muted,maxLines=1,overflow=TextOverflow.Ellipsis,modifier=Modifier.padding(start=4.dp))
            }
        }
        if(visit.photoPaths.isNotEmpty() || demo) JournalPhotoPiece(place,visit,
            Modifier.align(if(alternate)Alignment.TopEnd else Alignment.TopStart).offset(y=h*.32f).width(w*.88f).height(h*.42f).clickable(onClick=onOpen),
            caption=place.name,rotation=if(alternate)5f else -5f)
        else Box(Modifier.offset(x=5.dp,y=h*.36f).fillMaxWidth(.92f).height(h*.25f).rotate(-3f).background(AtlasColors.SagePale).clickable(onClick=onOpen).padding(20.dp)) {
            Column { Handwritten(place.name,size=21);Text("这一天，也值得记住。",fontSize=11.sp,color=AtlasColors.Sage,modifier=Modifier.padding(top=14.dp)) }
        }
        CoffeePostmark(Modifier.align(if(alternate)Alignment.TopStart else Alignment.TopEnd).offset(y=h*.25f).size(59.dp))
        PaperNote(Modifier.align(if(alternate)Alignment.BottomStart else Alignment.BottomEnd).offset(y=(-h*.075f)).width(w*.91f).height(h*.22f).rotate(if(alternate)-3f else 3f).clickable(onClick=onOpen)) {
            Text(body.ifBlank { "来过这里，就已经很值得记住。" },fontSize=12.sp,lineHeight=21.sp,maxLines=4,overflow=TextOverflow.Ellipsis)
        }
        Row(Modifier.align(Alignment.BottomStart).fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween) {
            TextButton(onClick=onOpen,contentPadding=PaddingValues(0.dp)) { Text("展开这一页 ↗",fontSize=10.sp,color=AtlasColors.Muted) }
            TextButton(onClick=onEdit,contentPadding=PaddingValues(0.dp)) { AtlasIcon(Glyph.EDIT,size=12.dp);Spacer(Modifier.width(4.dp));Text("编辑这一页",fontSize=10.sp) }
        }
    }
}

@Composable
private fun JournalPhotoPiece(place: Place, visit: Visit, modifier: Modifier, caption: String, rotation: Float) {
    val source=visit.photoPaths.firstOrNull()?.let { "local:$it" } ?: place.cover
    when(visit.layout) {
        "film" -> Column(modifier.rotate(rotation).background(AtlasColors.Ink).padding(8.dp)) {
            Photo(source,Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(2.dp)))
            Text("A MOMENT TO KEEP  /  ${visit.date}",fontSize=8.sp,color=AtlasColors.Card,modifier=Modifier.padding(top=7.dp))
        }
        "note" -> PaperNote(modifier.rotate(rotation)) {
            Column(Modifier.fillMaxSize()) {
                Photo(source,Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(2.dp)))
                Handwritten(caption,Modifier.padding(top=6.dp),size=11)
            }
        }
        else -> PolaroidPhoto(place.cover,modifier,caption=caption,rotation=rotation,localPath=visit.photoPaths.firstOrNull())
    }
}
