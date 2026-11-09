package com.mylittleatlas.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.ExperimentalGraphicsApi
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mylittleatlas.app.domain.*
import com.mylittleatlas.app.platform.PlatformServices
import kotlinx.coroutines.launch

@OptIn(ExperimentalGraphicsApi::class)
@Composable
fun ShareSheet(data:AtlasData,place:Place?,platform:PlatformServices,onClose:()->Unit,visit:Visit?=null,title:String?=null) {
    val layer=rememberGraphicsLayer()
    val scope=rememberCoroutineScope()
    var showDate by remember{mutableStateOf(false)}
    var showAddress by remember{mutableStateOf(false)}
    var includeNote by remember{mutableStateOf(true)}
    val currentVisit=visit ?: place?.let{p->data.visits.filter{it.placeId==p.id}.maxByOrNull{it.date}}
    var template by remember{mutableStateOf(currentVisit?.layout ?: "warm")}
    var busy by remember{mutableStateOf(false)}
    var feedback by remember{mutableStateOf<String?>(null)}
    var saved by remember{mutableStateOf(false)}
    val places=place?.let{listOf(it)} ?: data.places
    val visits=if(visit!=null)1 else data.visits.count{v->places.any{it.id==v.placeId}}
    val heading=title ?: place?.name ?: data.nickname+"的城市图鉴"
    val summary=heading+"："+places.size+" 处收藏，"+visits+" 段回忆。"
    AtlasFullPage(if(saved)"这份美好，已收好" else "分享这份美好",onClose) {
        if(saved)PaperNote(Modifier.fillMaxWidth()){Column{
            Text("图片已保存",style=MaterialTheme.typography.titleLarge)
            Text("可以在系统相册或所选文件位置找到它。",fontSize=12.sp,color=AtlasColors.Muted)
        }}
        Text("选一个喜欢的样子",fontSize=12.sp,color=AtlasColors.Muted)
        JournalStylePicker(template,{template=it;saved=false})
        // Record the exact on-screen poster including the user's privacy choices.
        Column(Modifier.fillMaxWidth().drawWithContent{layer.record{this@drawWithContent.drawContent()};drawLayer(layer)}
            .background(if(template=="film")AtlasColors.Ink else AtlasColors.Card).padding(18.dp)){
            val ink=if(template=="film")AtlasColors.Card else AtlasColors.Ink
            Text("MY LITTLE ATLAS",fontSize=9.sp,color=if(template=="film")AtlasColors.Blue else AtlasColors.Muted)
            Text(heading,style=MaterialTheme.typography.headlineSmall,color=ink,modifier=Modifier.padding(top=12.dp))
            Text(place?.let{it.city+" · "+it.kind.title} ?: "慢慢收藏，慢慢出发。",fontSize=11.sp,color=ink.copy(alpha=.7f),modifier=Modifier.padding(top=5.dp,bottom=15.dp))
            if(place!=null){
                JournalCoverV2(place,(currentVisit ?: Visit("preview",place.id,"","",emptyList())).copy(layout=template),Modifier.fillMaxWidth(),data.isDemoPlace(place))
                if(includeNote)Text(currentVisit?.note?.ifBlank{place.reason} ?: place.reason,fontSize=12.sp,lineHeight=21.sp,color=ink,modifier=Modifier.padding(top=16.dp))
                if(showDate&&currentVisit!=null)Text(currentVisit.date,fontSize=10.sp,color=ink.copy(alpha=.7f),modifier=Modifier.padding(top=10.dp))
                if(showAddress&&place.address.isNotBlank())Text(place.address,fontSize=10.sp,color=ink.copy(alpha=.7f),modifier=Modifier.padding(top=6.dp))
            }else{
                PolaroidPhoto(places.firstOrNull()?.cover ?: "city",Modifier.fillMaxWidth().height(160.dp),caption="my little collection")
                Text(places.size.toString()+" 处收藏  ·  "+places.map{it.city}.distinct().size+" 座城市  ·  "+visits+" 段回忆",fontSize=11.sp,color=ink,modifier=Modifier.padding(vertical=14.dp))
                places.take(6).forEach{p->Row(Modifier.fillMaxWidth().padding(top=8.dp),verticalAlignment=Alignment.CenterVertically){
                    Photo(p.cover,Modifier.size(40.dp));Column(Modifier.padding(start=10.dp)){Text(p.name,fontSize=12.sp,color=ink);Text(p.city,fontSize=9.sp,color=ink.copy(alpha=.65f))}
                }}
                if(places.size>6)Text("以及其他 "+(places.size-6)+" 个心动去处",fontSize=10.sp,color=ink.copy(alpha=.7f),modifier=Modifier.padding(top=10.dp))
            }
            Spacer(Modifier.height(20.dp));ThinDivider();Text("My Little Atlas  ·  collect a better life",fontSize=9.sp,color=ink.copy(alpha=.7f),modifier=Modifier.padding(top=10.dp))
        }
        if(place!=null){
            if(currentVisit!=null)SharePrivacyRow("显示本次到访日期",showDate,{showDate=it;saved=false})
            SharePrivacyRow("显示详细地址",showAddress,{showAddress=it;saved=false})
            SharePrivacyRow("包含文字小记",includeNote,{includeNote=it;saved=false})
        }
        feedback?.let{Text(it,fontSize=12.sp,color=AtlasColors.Peach)}
        PrimaryButton(if(busy)"正在生成…" else "保存到相册 / 文件",{
            busy=true;feedback=null
            scope.launch{runCatching{platform.saveImage(layer.toImageBitmap())}.onSuccess{if(it)saved=true else feedback="没有保存图片。可重试，或用下方系统分享发送图片。"}.onFailure{feedback="生成失败，请重试。"};busy=false}
        },Modifier.fillMaxWidth(),Glyph.DOWNLOAD,enabled=!busy)
        OutlinedButton(onClick={busy=true;feedback=null;scope.launch{runCatching{platform.shareImage(layer.toImageBitmap())}.onFailure{feedback="无法打开系统分享，请重试。"};busy=false}},modifier=Modifier.fillMaxWidth(),enabled=!busy){Text("打开系统分享")}
        TextButton(onClick={platform.shareText(summary)},modifier=Modifier.align(Alignment.CenterHorizontally)){Text("分享文字摘要",fontSize=12.sp)}
    }
}

@Composable
private fun SharePrivacyRow(label:String,value:Boolean,onChange:(Boolean)->Unit){Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text(label,fontSize=12.sp,modifier=Modifier.weight(1f));Switch(value,onChange)}}
