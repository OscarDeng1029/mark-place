package com.mylittleatlas.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mylittleatlas.app.domain.*
import com.mylittleatlas.app.presentation.AtlasController

@Composable
fun AtlasFullPage(title:String,onClose:()->Unit,actions:@Composable RowScope.()->Unit={},content:@Composable ColumnScope.()->Unit) {
    Dialog(onDismissRequest=onClose,properties=DialogProperties(usePlatformDefaultWidth=false)) {
        Paper(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).imePadding(), color = PersonalBackground) {
            Column(Modifier.fillMaxSize()) {
                Row(Modifier.fillMaxWidth().background(Color.White).height(54.dp).padding(horizontal=8.dp),verticalAlignment=Alignment.CenterVertically) {
                    IconButton(Glyph.BACK,"返回",onClose);Text(title,fontSize=16.sp,fontWeight=FontWeight.SemiBold,modifier=Modifier.weight(1f));actions()
                }
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(17.dp),content=content)
            }
        }
    }
}

@Composable
fun ProfileV2(controller:AtlasController,onShare:()->Unit,onJournal:()->Unit,onCity:()->Unit,onDraft:(VisitDraft)->Unit,onCollections:()->Unit={}) {
    val data=controller.data
    var page by remember{mutableStateOf("")}
    var deletingDraft by remember{mutableStateOf<VisitDraft?>(null)}
    PersonalCenterV4(data,{page="edit"},{page="settings"},{page="badges"},{page="drafts"},onShare,onJournal,onCity,onCollections)
    deletingDraft?.let{draft->ConfirmDialog("删除这篇草稿？","未完成的文字将被删除，原照片文件继续保留。","删除草稿",{if(controller.deleteDraft(draft.id))deletingDraft=null},{deletingDraft=null})}
    when(page){
        "settings"->SettingsV2(controller,{page=""})
        "edit"->EditProfileV2(controller,{page=""})
        "badges"->BadgesV2(data,{page=""})
        "drafts"->AtlasFullPage("未完成的草稿",{page=""}) {
            if(data.drafts.isEmpty())EmptyPage("暂无草稿","未完成的到访记录会保存在这里。")
            data.drafts.forEach{draft->val place=data.places.find{it.id==draft.placeId}
                if(place!=null)Column(Modifier.fillMaxWidth().background(AtlasColors.Card,RoundedCornerShape(12.dp)).padding(15.dp)){
                    Text(place.name,style=MaterialTheme.typography.titleMedium);Text(draft.date,fontSize=10.sp,color=AtlasColors.Muted)
                    Text(draft.note.ifBlank{"还没写下文字"},maxLines=2,fontSize=12.sp,modifier=Modifier.padding(top=9.dp))
                    Row{TextButton(onClick={page="";onDraft(draft)}){Text("继续写")};TextButton(onClick={deletingDraft=draft}){Text("删除草稿")}}
                }
            }
        }
    }
}

@Composable
fun EditProfileV2(controller:AtlasController,onClose:()->Unit) {
    var name by remember{mutableStateOf(controller.data.nickname)}
    var signature by remember{mutableStateOf(controller.data.signature)}
    var cover by remember{mutableStateOf(controller.data.profileCover)}
    AtlasFullPage("编辑个人资料",onClose) {
        Photo(cover,Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(11.dp)))
        Row(horizontalArrangement=Arrangement.spacedBy(9.dp)){
            OutlinedButton(onClick={controller.platform.pickPhoto{if(it!=null)cover="local:"+it}}){Text("从相册选封面") }
            TextButton(onClick={cover="city"}){Text("使用默认封面") }
        }
        AtlasField(name,{name=it.take(24)},"昵称")
        AtlasField(signature,{signature=it.take(120)},"一句话介绍",singleLine=false)
        PrimaryButton("保存资料",{if(controller.updateProfile(name,signature,cover))onClose()},Modifier.fillMaxWidth(),Glyph.CHECK,enabled=name.isNotBlank())
    }
}

private fun badgeProgress(data:AtlasData,index:Int)=when(index){
    0->data.places.filter{data.isVisited(it.id)}.map{it.city}.distinct().size
    1->data.places.count{it.kind==PlaceKind.COFFEE&&data.isVisited(it.id)}
    2->data.visits.count{it.photoPaths.isNotEmpty()}
    else->data.places.count{data.isVisited(it.id)}
}

@Composable
fun BadgesV2(data:AtlasData,onClose:()->Unit) {
    var selected by remember{mutableStateOf<Int?>(null)}
    val labels=listOf("城市探索家","咖啡爱好者","留住光影","百店俱乐部")
    val goals=listOf(1,1,1,100)
    AtlasFullPage("我的勋章墙",onClose){
        Text("慢慢走，也有小小成就。",style=MaterialTheme.typography.headlineMedium)
        Text("每一枚图案，都来自你真实留下的足迹。",fontSize=12.sp,color=AtlasColors.Muted)
        (0..1).forEach{row->Row(horizontalArrangement=Arrangement.spacedBy(14.dp)){(0..1).forEach{col->val i=row*2+col
            Column(Modifier.weight(1f).background(AtlasColors.Card,RoundedCornerShape(13.dp)).clickable{selected=i}.padding(18.dp),horizontalAlignment=Alignment.CenterHorizontally){
                ScrapbookBadge(i,null,Modifier.size(92.dp),badgeProgress(data,i)>=goals[i])
                Text(labels[i],fontSize=12.sp,modifier=Modifier.padding(top=13.dp))
                Text(badgeProgress(data,i).toString()+" / "+goals[i],fontSize=10.sp,color=AtlasColors.Muted,modifier=Modifier.padding(top=8.dp))
            }
        }}}
    }
    selected?.let{i->val count=badgeProgress(data,i);val goal=goals[i]
        AlertDialog(onDismissRequest={selected=null},title={Text(labels[i])},text={Column(Modifier.fillMaxWidth(),horizontalAlignment=Alignment.CenterHorizontally){
            ScrapbookBadge(i,null,Modifier.size(148.dp),count>=goal)
            Text(if(count>=goal)"这份小成就，已经属于你。" else "再慢慢走走，下一枚就在路上。",fontSize=13.sp,modifier=Modifier.padding(top=20.dp))
            Text(listOf("在一座城市留下到访","到访一家咖啡馆","为一段到访添加照片","到访一百个不同地点")[i],fontSize=12.sp,color=AtlasColors.Muted,modifier=Modifier.padding(top=10.dp))
            LinearProgressIndicator(progress={(count.toFloat()/goal).coerceIn(0f,1f)},modifier=Modifier.fillMaxWidth().padding(top=18.dp))
            Text(count.toString()+" / "+goal,fontSize=11.sp,modifier=Modifier.padding(top=8.dp))
        }},confirmButton={TextButton(onClick={selected=null}){Text("收好了")}},containerColor=AtlasColors.Card)
    }
}

@Composable
fun SettingsV2(controller:AtlasController,onClose:()->Unit) {
    var restore by remember{mutableStateOf(false)}
    var clear by remember{mutableStateOf(false)}
    var info by remember{mutableStateOf("")}
    AtlasFullPage("设置",onClose) {
        Text("数据与应用设置",fontSize=20.sp,fontWeight=FontWeight.SemiBold)
        SettingRow(Glyph.DOWNLOAD,"导出备份","保存地点、收藏夹、文字、草稿与资料"){controller.exportBackup()}
        SettingRow(Glyph.FOLDER,"恢复备份","恢复前会再次确认"){restore=true}
        Box(Modifier.fillMaxWidth().background(Color.White,RoundedCornerShape(12.dp)).padding(16.dp)){Text("JSON 备份不含照片文件。请同时保留原设备的应用照片，跨设备恢复时图片可能不可用。",fontSize=12.sp,color=AtlasColors.Muted)}
        SettingRow(Glyph.LOCK,"账号与设备同步","当前使用本机私密存储"){info="account"}
        SettingRow(Glyph.LAYERS,"关于这本图鉴","版本 0.1.0 · 原生多平台"){info="about"}
        if(controller.data.demoContent)SettingRow(Glyph.CLOSE,"开始我的空白图鉴","会清除当前图鉴中的地点、到访与草稿"){clear=true}
    }
    if(restore)ConfirmDialog("恢复备份","将替换本机的地点、收藏夹、到访与草稿。建议先导出备份。","选择备份",{restore=false;controller.restoreBackup()},{restore=false})
    if(clear)ConfirmDialog("翻开属于你的第一页","这会清空当前地点、到访与草稿，包括你在示例中新增的内容。需要保留时先导出备份。","清空并开始",{if(controller.clearDemo())clear=false},{clear=false})
    if(info.isNotBlank())AlertDialog(onDismissRequest={info=""},title={Text(if(info=="account")"账号与设备同步" else "关于 My Little Atlas")},
        text={Text(if(info=="account")"当前版本无需登录，数据保存在本机。云端账号与跨设备同步尚未接入，可通过备份文件迁移文字记录。" else "Kotlin Compose Multiplatform 原生 App。地图使用真实高德瓦片，导入支持公开笔记识别与门店核对。内置示例地点为虚构，照片为氛围素材。图片与文字仅在主动分享时导出。",fontSize=13.sp)},
        confirmButton={TextButton(onClick={info=""}){Text("知道了")}},containerColor=AtlasColors.Card)
}
