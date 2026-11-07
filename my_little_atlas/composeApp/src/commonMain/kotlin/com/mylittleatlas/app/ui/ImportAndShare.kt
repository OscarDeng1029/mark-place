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
