package com.mylittleatlas.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ExperimentalGraphicsApi
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mylittleatlas.app.domain.*
import com.mylittleatlas.app.platform.PlatformServices
import com.mylittleatlas.app.presentation.AtlasController
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ImportSheet(controller: AtlasController, initialText: String, onClose: () -> Unit) {
    var text by remember { mutableStateOf(initialText) }
    var tab by remember { mutableStateOf(if (initialText.startsWith("http")) 1 else 0) }
    var name by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("上海") }
    var source by remember { mutableStateOf(if (initialText.startsWith("http")) initialText else "") }
    var selected by remember { mutableStateOf(setOf<String>()) }
    var folders by remember { mutableStateOf(emptySet<String>()) }
    val candidates = remember(text) { TextPlaceImporter.extract(text) }
    val key: (ImportCandidate) -> String = { "${it.city}|${it.name}|${it.address}" }
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxWidth().fillMaxHeight(.94f).widthIn(max = 620.dp).padding(12.dp),
            shape = RoundedCornerShape(22.dp), color = AtlasColors.Paper) {
            Column(Modifier.fillMaxSize()) {
                Row(Modifier.fillMaxWidth().padding(start = 22.dp, end = 10.dp, top = 13.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("收藏一处新去处", style = MaterialTheme.typography.titleLarge)
                        Text("慢慢收集，按自己的节奏出发", fontSize = 11.sp, color = AtlasColors.Muted)
                    }
                    IconButton(Glyph.CLOSE, "关闭", onClose)
                }
                Row(Modifier.padding(horizontal = 22.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Tag("粘贴笔记", tab == 0, { tab = 0 })
                    Tag("手动添加 / 链接", tab == 1, { tab = 1 })
                }
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    if (tab == 0) {
                        Text("把整理好的店名和地址粘贴在这里。每行一处，格式：店名｜地址｜城市。", fontSize = 12.sp, color = AtlasColors.Muted)
                        BasicTextField(text, { text = it; selected = emptySet() }, Modifier.fillMaxWidth().heightIn(min = 118.dp)
                            .clip(RoundedCornerShape(13.dp)).background(AtlasColors.Card).padding(14.dp),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = AtlasColors.Ink),
                            decorationBox = { field -> if (text.isBlank()) Text("例如：河畔咖啡｜上海市徐汇区滨江路｜上海", fontSize = 12.sp, color = AtlasColors.Muted); field() })
                        if (candidates.isNotEmpty()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("识别到 ${candidates.size} 处", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                                TextButton(onClick = {
                                    selected = if (selected.size == candidates.count { !TextPlaceImporter.isDuplicate(it, controller.data.places) }) emptySet()
                                    else candidates.filterNot { TextPlaceImporter.isDuplicate(it, controller.data.places) }.map { key(it) }.toSet()
                                }) { Text(if (selected.isEmpty()) "全选未收藏" else "清除选择") }
                            }
                            candidates.forEach { candidate ->
                                val duplicate = TextPlaceImporter.isDuplicate(candidate, controller.data.places)
                                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(AtlasColors.Card)
                                    .clickable(enabled = !duplicate) { selected = if (key(candidate) in selected) selected - key(candidate) else selected + key(candidate) }
                                    .padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(key(candidate) in selected, { checked -> selected = if (checked) selected + key(candidate) else selected - key(candidate) }, enabled = !duplicate)
                                    Column(Modifier.weight(1f).padding(start = 5.dp)) {
                                        Text(candidate.name, style = MaterialTheme.typography.titleMedium)
                                        Text("${candidate.address} · ${candidate.city}", fontSize = 11.sp, color = AtlasColors.Muted)
                                    }
                                    if (duplicate) Text("已收藏", fontSize = 10.sp, color = AtlasColors.Muted)
                                }
                            }
                        } else if (text.isNotBlank()) Text("没有识别到结构化地址。可以手动添加；分享链接不会自动抓取页面内容。", fontSize = 12.sp, color = AtlasColors.Muted)
                    } else {
                        Text("记录你自己挑中的地点。外部链接会作为来源保存，不会读取网页内容。", fontSize = 12.sp, color = AtlasColors.Muted)
                        AtlasField(name, { name = it }, "地点名称")
                        AtlasField(address, { address = it }, "地址 / 街道")
                        AtlasField(city, { city = it }, "城市")
                        AtlasField(source, { source = it }, "灵感来源链接（选填）")
                    }
                    Text("放进收藏夹", fontSize = 12.sp, color = AtlasColors.Muted)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        controller.data.collections.forEach { folder ->
                            Tag(folder.name, folder.id in folders, { folders = if (folder.id in folders) folders - folder.id else folders + folder.id })
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
                Row(Modifier.fillMaxWidth().background(AtlasColors.Card).padding(16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onClose, Modifier.height(50.dp).weight(1f), shape = RoundedCornerShape(14.dp)) { Text("先不收藏") }
                    PrimaryButton(if (tab == 0) "收藏已选 ${selected.size} 处" else "收藏这个地点", {
                        if (tab == 0) {
                            val chosen = candidates.filter { key(it) in selected }
                            controller.importSelected(chosen, folders.toList())
                            if (chosen.isNotEmpty()) onClose()
                        } else if (name.isNotBlank() && address.isNotBlank() && city.isNotBlank()) {
                            val place = Place(controller.platform.uniqueId(), name.trim(), city.trim(), address = address.trim(),
                                collectionIds = folders.toList(), source = source.trim(), addedOn = controller.platform.today())
                            if (controller.addPlace(place)) onClose()
                        }
                    }, Modifier.weight(1.4f), Glyph.PLUS, enabled = if (tab == 0) selected.isNotEmpty() else name.isNotBlank() && address.isNotBlank() && city.isNotBlank())
                }
            }
        }
    }
}

@OptIn(ExperimentalGraphicsApi::class)
@Composable
fun ShareSheet(data: AtlasData, place: Place?, platform: PlatformServices, onClose: () -> Unit) {
    val layer = rememberGraphicsLayer()
    val scope = rememberCoroutineScope()
    val places = if (place != null) listOf(place) else data.places
    val visits = if (place != null) data.visits.count { it.placeId == place.id } else data.visits.size
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxWidth().fillMaxHeight(.92f).widthIn(max = 560.dp).padding(12.dp),
            shape = RoundedCornerShape(22.dp), color = AtlasColors.Paper) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("我的城市收藏图鉴", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    IconButton(Glyph.CLOSE, "关闭", onClose)
                }
                Text("分享卡片默认隐藏详细地址", fontSize = 11.sp, color = AtlasColors.Muted, modifier = Modifier.align(Alignment.Start).padding(top = 5.dp, bottom = 14.dp))
                Column(Modifier.fillMaxWidth().widthIn(max = 360.dp).clip(RoundedCornerShape(4.dp)).background(AtlasColors.Card)
                    .drawWithContent { layer.record { this@drawWithContent.drawContent() }; drawLayer(layer) }.padding(18.dp)) {
                    Text("MY LITTLE ATLAS", fontSize = 9.sp, color = AtlasColors.Muted)
                    Text(place?.name ?: "把想去的地方", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(top = 9.dp))
                    Text(if (place != null) "${place.city} · ${place.kind.title}" else "慢慢变成去过的地方", fontSize = 12.sp, color = AtlasColors.Muted, modifier = Modifier.padding(top = 4.dp))
                    Spacer(Modifier.height(15.dp))
                    if (place != null) {
                        Photo(place.cover, Modifier.fillMaxWidth().height(190.dp).clip(RoundedCornerShape(3.dp)), place.name)
                        Text(place.reason.ifBlank { "留一处心动，等下次出发。" }, fontSize = 12.sp, color = AtlasColors.Ink,
                            modifier = Modifier.padding(top = 12.dp))
                    } else {
                        Text("${places.size} 处收藏   ·   ${data.places.map { it.city }.distinct().size} 座城市   ·   $visits 段回忆",
                            fontSize = 12.sp, color = AtlasColors.Ink, modifier = Modifier.padding(vertical = 7.dp))
                        Text("每一处心动，都值得被好好收下。", fontSize = 11.sp, color = AtlasColors.Muted)
                        places.take(4).forEach { item ->
                            Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Photo(item.cover, Modifier.size(43.dp).clip(RoundedCornerShape(3.dp)))
                                Column(Modifier.padding(start = 10.dp)) {
                                    Text(item.name, fontSize = 12.sp)
                                    Text(item.city, fontSize = 10.sp, color = AtlasColors.Muted)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(17.dp))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("My Little Atlas", fontFamily = FontFamily.Serif, fontSize = 12.sp, color = AtlasColors.Green, modifier = Modifier.weight(1f))
                        Text("仅分享主动选择的内容", fontSize = 9.sp, color = AtlasColors.Muted)
                    }
                }
                Spacer(Modifier.height(18.dp))
                PrimaryButton("生成分享图片", {
                    scope.launch { runCatching { platform.shareImage(layer.toImageBitmap()) }.onFailure { platform.shareText("我的城市收藏图鉴：收藏 ${places.size} 处，去过 $visits 次。") } }
                }, Modifier.fillMaxWidth().widthIn(max = 360.dp), Glyph.SHARE)
                TextButton(onClick = { platform.shareText("我的城市收藏图鉴：收藏 ${places.size} 处，去过 $visits 次。") }) { Text("分享文字摘要") }
            }
        }
    }
}
