package com.mylittleatlas.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mylittleatlas.app.domain.*
import com.mylittleatlas.app.presentation.AtlasController

@Composable
fun CollectionsV2(controller:AtlasController,collectionId:String?,onCollection:(String?)->Unit,onPlace:(Place)->Unit,
    search:String,onSearch:(String)->Unit,searching:Boolean,onSearching:(Boolean)->Unit,onAdd:()->Unit,wide:Boolean,
    onBack:()->Unit,onMap:(String?)->Unit,onShare:(String)->Unit) {
    val data=controller.data
    val folder=data.collections.find{it.id==collectionId}
    var filter by remember{mutableStateOf(PlaceFilter.ALL)}
    var editing by remember{mutableStateOf(false)}
    var options by remember{mutableStateOf(false)}
    var bulk by remember{mutableStateOf(false)}
    var selected by remember{mutableStateOf(emptySet<String>())}
    var picker by remember{mutableStateOf(false)}
    var deleteFolder by remember{mutableStateOf(false)}
    var deletePlaces by remember{mutableStateOf(false)}
    val places=data.filterPlaces(filter,search,collectionId)
    val toggle:(String)->Unit={id->selected=if(id in selected)selected-id else selected+id}
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().height(54.dp).padding(horizontal=8.dp),verticalAlignment=Alignment.CenterVertically){
            IconButton(Glyph.BACK,if(folder!=null)"返回收藏" else "返回手账",{if(folder!=null)onCollection(null) else {onSearching(false);onSearch("");onBack()}})
            Text(if(searching)"搜索我的收藏" else folder?.name ?: "我的收藏夹",fontSize=15.sp,modifier=Modifier.weight(1f))
            if(folder!=null)IconButton(Glyph.MORE,"收藏夹更多操作",{options=true}) else IconButton(Glyph.PLUS,"新建收藏夹",{editing=true})
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal=20.dp).padding(bottom=24.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){
            if(folder!=null&&!searching){
                Box(Modifier.fillMaxWidth().height(155.dp)){
                    Photo(folder.cover,Modifier.fillMaxSize().clip(RoundedCornerShape(9.dp)))
                    Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().background(AtlasColors.Ink.copy(alpha=.46f)).padding(15.dp)){
                        Text(folder.name,style=MaterialTheme.typography.headlineMedium,color=AtlasColors.Card)
                        Text(folder.subtitle,fontSize=11.sp,color=AtlasColors.Card,modifier=Modifier.padding(top=5.dp))
                    }
                }
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                    Tag("列表",true,{});Tag("地图",false,{onMap(folder.id)});Spacer(Modifier.weight(1f));Tag(if(bulk)"完成整理" else "整理",bulk,{bulk=!bulk;selected=emptySet()})
                }
            }
            OutlinedTextField(search,{onSearch(it);onSearching(true)},Modifier.fillMaxWidth(),placeholder={Text("地点、区域或想去的理由",fontSize=12.sp)},
                leadingIcon={AtlasIcon(Glyph.SEARCH,size=18.dp,color=AtlasColors.Muted)},singleLine=true,shape=RoundedCornerShape(11.dp),colors=OutlinedTextFieldDefaults.colors(unfocusedBorderColor=AtlasColors.Line))
            if(folder==null&&!searching&&search.isBlank()){
                Text("把心动，按喜欢的方式收好",style=MaterialTheme.typography.titleLarge)
                data.collections.chunked(if(wide)4 else 2).forEach{row->Row(horizontalArrangement=Arrangement.spacedBy(12.dp)){
                    row.forEach{CollectionTileV2(it,data,{onCollection(it.id)},Modifier.weight(1f))};repeat((if(wide)4 else 2)-row.size){Spacer(Modifier.weight(1f))}
                }}
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text("所有地点",style=MaterialTheme.typography.titleMedium,modifier=Modifier.weight(1f));TextButton(onClick={bulk=!bulk;selected=emptySet()}){Text(if(bulk)"完成整理" else "批量整理",fontSize=11.sp)}}
            }
            FilterRow(filter,{filter=it})
            Text(if(bulk)"已选 "+selected.size+" 处" else "共 "+places.size+" 个心动去处",fontSize=11.sp,color=AtlasColors.Muted)
            places.forEach{place->Row(verticalAlignment=Alignment.CenterVertically){
                if(bulk)Checkbox(place.id in selected,{toggle(place.id)})
                PlaceRow(place,data,{if(bulk)toggle(place.id) else onPlace(place)},Modifier.weight(1f))
            }}
            if(places.isEmpty())EmptyPage(if(search.isNotBlank())"还没找到「"+search+"」" else "这一页，等你慢慢填满",
                if(search.isNotBlank())"试试店名或所在区域，也可以直接添加这个地点。" else "收下一处喜欢的地方，留给下次出发。","添加一个地点",onAdd)
        }
        if(bulk)Column(Modifier.fillMaxWidth().background(AtlasColors.Card).padding(10.dp)){
            Row(verticalAlignment=Alignment.CenterVertically){TextButton(onClick={selected=if(selected.size==places.size)emptySet() else places.map{it.id}.toSet()}){Text(if(selected.size==places.size)"取消全选" else "全选")};Text("已选 "+selected.size+" 处",fontSize=12.sp)}
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceEvenly){
                TextButton(onClick={picker=true},enabled=selected.isNotEmpty()){Text("设定收藏夹")}
                if(folder!=null)TextButton(onClick={selected.forEach{id->data.places.find{it.id==id}?.let{controller.updatePlace(it.copy(collectionIds=it.collectionIds-folder.id))}};selected=emptySet()},enabled=selected.isNotEmpty()){Text("移出此夹")}
                TextButton(onClick={deletePlaces=true},enabled=selected.isNotEmpty()){Text("删除地点",color=AtlasColors.Peach)}
            }
        }
    }
    if(editing)FolderEditorV2(controller,folder,{editing=false})
    if(options&&folder!=null)AlertDialog(onDismissRequest={options=false},title={Text(folder.name)},text={Column{
        SettingRow(Glyph.EDIT,"编辑收藏夹","名称、简介与封面"){options=false;editing=true}
        SettingRow(Glyph.BOOKMARK,"批量整理地点","多选归类、移出或删除"){options=false;bulk=true}
        SettingRow(Glyph.SHARE,"分享这个收藏夹","只带入当前夹的地点"){options=false;onShare(folder.id)}
        SettingRow(Glyph.CLOSE,"删除收藏夹","保留所有地点与到访"){options=false;deleteFolder=true}
    }},confirmButton={TextButton(onClick={options=false}){Text("收好")}},containerColor=AtlasColors.Card)
    if(deleteFolder&&folder!=null)ConfirmDialog("删除这个收藏夹？","只移除收藏夹与归类关系，里面的地点和到访记录仍保留。","删除收藏夹",{if(controller.deleteCollection(folder.id)){deleteFolder=false;onCollection(null)}},{deleteFolder=false})
    if(deletePlaces)ConfirmDialog("删除 "+selected.size+" 个地点？","将同时删除关联的 "+data.visits.count{it.placeId in selected}+" 次到访和 "+data.drafts.count{it.placeId in selected}+" 篇草稿。原照片文件保留；仅取消归类请使用「移出此夹」。","删除地点",{if(controller.deletePlaces(selected)){selected=emptySet();deletePlaces=false}},{deletePlaces=false})
    if(picker)FolderPickerV2(controller,emptyList(),{if(controller.assignCollections(selected,it)){picker=false;selected=emptySet()}},{picker=false},"设定选中地点的收藏夹","本次选择将替换选中地点原有的归类。")
}

@Composable
fun FolderEditorV2(controller:AtlasController,folder:com.mylittleatlas.app.domain.Collection?,onClose:()->Unit){
    var name by remember{mutableStateOf(folder?.name ?: "")}
    var subtitle by remember{mutableStateOf(folder?.subtitle ?: "慢慢收集，慢慢出发。")}
    var cover by remember{mutableStateOf(folder?.cover ?: "cafe")}
    AtlasFullPage(if(folder==null)"新建收藏夹" else "编辑收藏夹",onClose){
        Box(Modifier.fillMaxWidth().height(185.dp)){
            Photo(cover,Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)))
        }
        OutlinedButton(onClick={controller.platform.pickPhoto{if(it!=null)cover="local:"+it}},Modifier.fillMaxWidth()){Text("从相册选择封面")}
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){listOf("cafe","dessert","city","bookshop").forEach{c->Photo(c,Modifier.weight(1f).height(60.dp).clip(RoundedCornerShape(5.dp)).clickable{cover=c})}}
        AtlasField(name,{name=it},"收藏夹名称")
        AtlasField(subtitle,{subtitle=it},"写一句小小的介绍",singleLine=false)
        Text("一个地点可以同时收进多个收藏夹。",fontSize=11.sp,color=AtlasColors.Muted)
        PrimaryButton("收好这个主题",{val ok=if(folder==null)controller.createCollection(name,subtitle,cover) else controller.updateCollection(folder.copy(name=name.trim(),subtitle=subtitle.trim(),cover=cover));if(ok)onClose()},Modifier.fillMaxWidth(),enabled=name.isNotBlank())
    }
}

@Composable
fun FolderPickerV2(controller:AtlasController,initial:List<String>,onSave:(List<String>)->Unit,onClose:()->Unit,title:String="收进收藏夹",subtitle:String="可以同时选择多个主题。"){
    var selected by remember{mutableStateOf(initial)}
    var create by remember{mutableStateOf(false)}
    AlertDialog(onDismissRequest=onClose,title={Text(title)},text={Column(Modifier.heightIn(max=380.dp).verticalScroll(rememberScrollState())){
        Text(subtitle,fontSize=11.sp,color=AtlasColors.Muted)
        controller.data.collections.forEach{c->Row(Modifier.fillMaxWidth().clickable{selected=if(c.id in selected)selected-c.id else selected+c.id},verticalAlignment=Alignment.CenterVertically){Checkbox(c.id in selected,{selected=if(it)selected+c.id else selected-c.id});Text(c.name,fontSize=13.sp)}}
        TextButton(onClick={create=true}){Text("＋ 新建收藏夹")}
    }},confirmButton={TextButton(onClick={onSave(selected)}){Text("确认归类")}},dismissButton={TextButton(onClick=onClose){Text("取消")}},containerColor=AtlasColors.Card)
    if(create)FolderEditorV2(controller,null,{create=false})
}
