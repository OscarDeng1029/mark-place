package com.mylittleatlas.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
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
import kotlin.math.absoluteValue

/** The home is a book to browse. The separate Collections tab remains the library. */
@Composable
fun JournalHomeV3(data:AtlasData,today:String,onPlace:(Place)->Unit,onCollections:()->Unit,onMap:()->Unit,
    onTimeline:()->Unit,onVisit:(Visit)->Unit,onAdd:()->Unit,onRecord:(String)->Unit,wide:Boolean,initialPage:Int=0,onPageChange:(Int)->Unit={}) {
    val visits=data.visits.filter{v->data.places.any{it.id==v.placeId}}.sortedWith(compareByDescending<Visit>{it.date}.thenBy{it.id})
    val wishes=data.places.filter{!data.isVisited(it.id)}
    val pager=rememberPagerState(initialPage=initialPage.coerceIn(0,1+visits.size),pageCount={2+visits.size})
    LaunchedEffect(pager.currentPage){onPageChange(pager.currentPage)}
    val scope=rememberCoroutineScope()
    Column(Modifier.fillMaxSize().widthIn(max=if(wide)640.dp else 520.dp).padding(horizontal=12.dp),horizontalAlignment=Alignment.CenterHorizontally){
        Row(Modifier.fillMaxWidth().height(58.dp).padding(horizontal=7.dp),verticalAlignment=Alignment.CenterVertically){
            Column(Modifier.weight(1f)){
                Text("MY LITTLE ATLAS",fontSize=9.sp,letterSpacing=2.sp,color=AtlasColors.Muted)
                Text("日子，一页一页",style=MaterialTheme.typography.titleLarge,fontSize=21.sp)
            }
            TextButton(onClick=onTimeline){AtlasIcon(Glyph.LIST,size=16.dp);Spacer(Modifier.width(5.dp));Text("足迹索引",fontSize=11.sp)}
        }
        HorizontalPager(pager,Modifier.weight(1f).fillMaxWidth().semantics{contentDescription="生活手账，可左右翻页"},pageSpacing=14.dp,
            beyondViewportPageCount=1,key={if(it<2)"opening-"+it else visits[it-2].id}){index->
            val offset=((pager.currentPage-index)+pager.currentPageOffsetFraction).coerceIn(-1f,1f)
            BookLeafV3(Modifier.fillMaxSize().graphicsLayer{
                rotationY=offset*13f
                transformOrigin=TransformOrigin(if(offset>0)0f else 1f,.5f)
                cameraDistance=18f*density
                alpha=1f-offset.absoluteValue*.12f
            },index+1){
                when(index){
                    0->OpeningLeafV3(data,today,visits.firstOrNull(),wishes.firstOrNull(),onVisit,onPlace,onAdd,onCollections,onTimeline)
                    1->WishLeafV3(wishes,onPlace,onMap,onAdd)
                    else->{val visit=visits[index-2];val place=data.places.first{it.id==visit.placeId}
                        MemoryLeafV3(place,visit,index-2,{onVisit(visit)},{onRecord(place.id)})
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().height(48.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){
            TextButton(onClick={scope.launch{pager.animateScrollToPage((pager.currentPage-1).coerceAtLeast(0))}},enabled=pager.currentPage>0,contentPadding=PaddingValues(horizontal=8.dp)){Text("← 前一页",fontSize=11.sp)}
            Column(horizontalAlignment=Alignment.CenterHorizontally){
                Text(listOf("生活扉页","待出发清单").getOrNull(pager.currentPage) ?: "到访回忆",fontSize=10.sp,color=AtlasColors.Green)
                Text((pager.currentPage+1).toString().padStart(2,'0')+" / "+pager.pageCount.toString().padStart(2,'0')+"  ·  左右翻阅",fontSize=8.sp,color=AtlasColors.Muted)
            }
            TextButton(onClick={scope.launch{pager.animateScrollToPage((pager.currentPage+1).coerceAtMost(pager.pageCount-1))}},enabled=pager.currentPage<pager.pageCount-1,contentPadding=PaddingValues(horizontal=8.dp)){Text("后一页 →",fontSize=11.sp)}
        }
    }
}

@Composable
fun BookLeafV3(modifier:Modifier=Modifier,page:Int=1,content:@Composable BoxScope.()->Unit){
    Box(modifier.padding(start=6.dp,end=6.dp,top=6.dp,bottom=7.dp)){
        Box(Modifier.matchParentSize().offset(x=5.dp,y=5.dp).background(Color(0xFFE6DFCF),RoundedCornerShape(3.dp,10.dp,10.dp,3.dp)))
        Box(Modifier.matchParentSize().offset(x=2.dp,y=2.dp).background(AtlasColors.Card,RoundedCornerShape(3.dp,9.dp,9.dp,3.dp)).border(1.dp,AtlasColors.Line,RoundedCornerShape(3.dp,9.dp,9.dp,3.dp)))
        Box(Modifier.fillMaxSize().shadow(4.dp,RoundedCornerShape(3.dp,8.dp,8.dp,3.dp)).background(AtlasColors.Card,RoundedCornerShape(3.dp,8.dp,8.dp,3.dp))){
            AtlasPaperTexture(Modifier.matchParentSize())
            Box(Modifier.fillMaxHeight().width(19.dp).background(Brush.horizontalGradient(listOf(Color(0x1D756750),Color.Transparent))))
            Canvas(Modifier.matchParentSize()){
                val margin=32.dp.toPx()
                drawLine(Color(0x23C58360),Offset(margin,20.dp.toPx()),Offset(margin,size.height-23.dp.toPx()),1.dp.toPx())
                var y=40.dp.toPx()
                while(y<size.height-20.dp.toPx()){drawLine(Color(0x0B53634E),Offset(margin+6.dp.toPx(),y),Offset(size.width-14.dp.toPx(),y),.6.dp.toPx());y+=24.dp.toPx()}
            }
            content()
            Text("— "+page.toString().padStart(2,'0')+" —",fontSize=8.sp,color=AtlasColors.Muted,modifier=Modifier.align(Alignment.BottomCenter).padding(bottom=7.dp))
        }
        Column(Modifier.offset(x=(-5).dp).fillMaxHeight().padding(vertical=40.dp),verticalArrangement=Arrangement.SpaceEvenly){
            repeat(5){Box(Modifier.size(17.dp,7.dp).shadow(1.dp,RoundedCornerShape(50)).background(Color(0xFFAEA793),RoundedCornerShape(50)).border(1.dp,Color(0xFFDBD4C3),RoundedCornerShape(50)))}
        }
    }
}

@Composable
private fun OpeningLeafV3(data:AtlasData,today:String,latest:Visit?,wish:Place?,onVisit:(Visit)->Unit,onPlace:(Place)->Unit,onAdd:()->Unit,onCollections:()->Unit,onTimeline:()->Unit){
    BoxWithConstraints(Modifier.fillMaxSize().padding(start=37.dp,end=15.dp,top=20.dp,bottom=22.dp)){
        val h=maxHeight
        val w=maxWidth
        val recent=data.places.find{it.id==latest?.placeId}
        Column(Modifier.align(Alignment.TopStart)){
            Text(today.replace('-','.'),fontSize=10.sp,color=AtlasColors.Sage,letterSpacing=1.sp)
            Text("把日子，\n翻成喜欢的样子。",style=MaterialTheme.typography.titleLarge,fontSize=22.sp,lineHeight=29.sp,modifier=Modifier.padding(top=9.dp))
        }
        Text("VOL. 01",fontSize=9.sp,color=AtlasColors.Peach,modifier=Modifier.align(Alignment.TopEnd).rotate(90f).offset(x=17.dp,y=(-5).dp))
        val photoModifier=Modifier.offset(x=2.dp,y=h*.26f).width(w*.66f).height(h*.36f)
        if(latest!=null&&recent!=null)PolaroidPhoto(recent.cover,photoModifier.clickable{onVisit(latest)},caption=recent.name.take(20),rotation=-6f,localPath=latest.photoPaths.firstOrNull())
        else PolaroidPhoto("city",photoModifier.clickable(onClick=onAdd),caption="the first page is yours",rotation=-6f)
        PaperNote(Modifier.align(Alignment.TopEnd).offset(y=h*.38f).width(w*.46f).height(h*.29f).rotate(5f).clickable{if(latest!=null)onVisit(latest) else onAdd()}){
            Column{Text(if(latest==null)"写给今天" else "最近的一页",fontSize=9.sp,color=AtlasColors.Muted)
                Text(latest?.note?.ifBlank{"这次抵达，也值得记住。"} ?: "不用安排得很满，\n收下一点小美好就好。",fontSize=12.sp,lineHeight=21.sp,maxLines=4,overflow=TextOverflow.Ellipsis,modifier=Modifier.padding(top=7.dp))}
        }
        CoffeePostmark(Modifier.align(Alignment.CenterEnd).offset(y=(-h*.22f)).size(62.dp))
        Box(Modifier.align(Alignment.BottomStart).offset(y=(-h*.15f)).fillMaxWidth(.93f).rotate(-2f).background(AtlasColors.SagePale).clickable{if(wish!=null)onPlace(wish) else onAdd()}.padding(12.dp)){
            Column{Text("给下一次出发，留张便条",fontSize=9.sp,color=AtlasColors.Sage)
                Text(wish?.name ?: "你的第一处心动，会在哪里？",style=MaterialTheme.typography.titleMedium,fontSize=14.sp,maxLines=1,modifier=Modifier.padding(top=5.dp))
                Text(wish?.let{it.city+" · "+it.kind.title+"    去看看 ↗"} ?: "点这里，收藏一个喜欢的地方 ↗",fontSize=10.sp,color=AtlasColors.Muted,modifier=Modifier.padding(top=4.dp))}
        }
        Row(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(bottom=3.dp),horizontalArrangement=Arrangement.spacedBy(10.dp)){
            NotebookTicketV3(data.places.size.toString()+" 处心动","收在收藏里 ↗",onCollections,Modifier.weight(1f).rotate(2f))
            NotebookTicketV3(data.visits.size.toString()+" 次抵达","回看全部足迹 ↗",onTimeline,Modifier.weight(1f).rotate(-3f),AtlasColors.PeachPale)
        }
    }
}

@Composable
private fun WishLeafV3(wishes:List<Place>,onPlace:(Place)->Unit,onMap:()->Unit,onAdd:()->Unit){
    BoxWithConstraints(Modifier.fillMaxSize().padding(start=37.dp,end=16.dp,top=22.dp,bottom=27.dp)){
        val h=maxHeight;val w=maxWidth
        Column{Text("SOMEDAY, SOMEWHERE",fontSize=8.sp,letterSpacing=1.sp,color=AtlasColors.Sage);Text("还没去，\n也很期待。",style=MaterialTheme.typography.headlineMedium,fontSize=25.sp,lineHeight=31.sp,modifier=Modifier.padding(top=9.dp))}
        Handwritten(wishes.size.toString()+" 个小小愿望",Modifier.align(Alignment.TopEnd).offset(y=84.dp).rotate(6f),size=13)
        if(wishes.isEmpty())PaperNote(Modifier.align(Alignment.Center).fillMaxWidth().height(180.dp).rotate(-3f).clickable(onClick=onAdd)){Column{Handwritten("下一页，写下你的心动。",size=19);Text("点这里，添加想去的地方。",fontSize=12.sp,modifier=Modifier.padding(top=19.dp))}}
        else{
            val first=wishes.first()
            PolaroidPhoto(first.cover,Modifier.offset(x=3.dp,y=h*.30f).width(w*.61f).height(h*.34f).clickable{onPlace(first)},caption=first.name.take(19),rotation=-7f)
            wishes.getOrNull(1)?.let{second->PolaroidPhoto(second.cover,Modifier.align(Alignment.TopEnd).offset(y=h*.46f).width(w*.60f).height(h*.33f).clickable{onPlace(second)},caption=second.name.take(19),rotation=7f)}
            PaperNote(Modifier.offset(x=1.dp,y=h*.73f).width(w*.47f).height(h*.16f).rotate(-3f).clickable{onPlace(first)}){
                Text(first.reason.ifBlank{first.city+" · "+first.kind.title+"\n等一个刚好的周末。"},fontSize=11.sp,lineHeight=18.sp,maxLines=3,overflow=TextOverflow.Ellipsis)
            }
        }
        TextButton(onClick=onMap,modifier=Modifier.align(Alignment.BottomEnd)){AtlasIcon(Glyph.MAP,size=15.dp);Spacer(Modifier.width(6.dp));Text("在地图上挑下一站 →",fontSize=11.sp)}
    }
}

@Composable
private fun MemoryLeafV3(place:Place,visit:Visit,index:Int,onOpen:()->Unit,onRecord:()->Unit){
    BoxWithConstraints(Modifier.fillMaxSize().padding(start=37.dp,end=16.dp,top=22.dp,bottom=27.dp)){
        val h=maxHeight;val w=maxWidth;val variant=index%3;val alternate=variant==1
        Column(Modifier.align(if(alternate)Alignment.TopEnd else Alignment.TopStart)){
            Text(visit.date.replace('-','.'),fontSize=11.sp,color=AtlasColors.Sage,letterSpacing=1.sp)
            Text(place.name,style=MaterialTheme.typography.titleLarge,fontSize=22.sp,maxLines=2,overflow=TextOverflow.Ellipsis,modifier=Modifier.padding(top=7.dp))
            Text(place.city+" · "+place.kind.title,fontSize=10.sp,color=AtlasColors.Muted,modifier=Modifier.padding(top=5.dp))
        }
        PolaroidPhoto(place.cover,Modifier.align(if(alternate)Alignment.TopEnd else Alignment.TopStart).offset(x=if(variant==2)14.dp else 0.dp,y=h*(if(alternate).34f else .24f)).width(w*(if(variant==2).75f else .89f)).height(h*(if(alternate).41f else .43f)).clickable(onClick=onOpen),caption="a little moment to remember",rotation=when(variant){0->-5f;1->6f;else->-8f},localPath=visit.photoPaths.firstOrNull())
        if(alternate)NotebookTicketV3(place.kind.title+" · 给自己的一天","不赶时间，也是一种认真生活",onOpen,Modifier.offset(x=2.dp,y=h*.21f).fillMaxWidth(.81f).rotate(-3f),AtlasColors.PeachPale)
        PaperNote(Modifier.align(if(alternate)Alignment.BottomStart else Alignment.BottomEnd).offset(y=(-h*(if(alternate).08f else .10f))).width(w*(if(variant==2).95f else .87f)).height(h*(if(alternate).22f else .25f)).rotate(if(alternate)-3f else 3f).clickable(onClick=onOpen)){
            Text(visit.note.ifBlank{"那一天的风、光和心情，都被收在这一页。"},fontSize=13.sp,lineHeight=22.sp,maxLines=4,overflow=TextOverflow.Ellipsis)
        }
        CoffeePostmark(Modifier.align(if(alternate)Alignment.CenterStart else Alignment.CenterEnd).offset(y=h*.14f).size(65.dp))
        Row(Modifier.align(Alignment.BottomStart).fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){TextButton(onClick=onOpen){Text("展开这一页 ↗",fontSize=11.sp)};TextButton(onClick=onRecord){Text("再记一次到访",fontSize=11.sp)}}
    }
}

@Composable
fun NotebookTicketV3(title:String,subtitle:String,onClick:()->Unit,modifier:Modifier=Modifier,tint:Color=AtlasColors.SagePale){
    Column(modifier.background(tint,RoundedCornerShape(2.dp)).border(1.dp,AtlasColors.Line,RoundedCornerShape(2.dp)).clickable(onClick=onClick).padding(horizontal=10.dp,vertical=10.dp)){
        Text(title,fontSize=14.sp,color=AtlasColors.Ink)
        Text(subtitle,fontSize=9.sp,color=AtlasColors.Muted,modifier=Modifier.padding(top=4.dp))
    }
}
