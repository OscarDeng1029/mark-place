package com.mylittleatlas.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mylittleatlas.app.presentation.AtlasController

@Composable
fun WelcomeV2(controller:AtlasController){
    var city by remember{mutableStateOf(controller.data.preferredCity.ifBlank{"上海"})}
    var choosing by remember{mutableStateOf(false)}
    AtlasFullPage("我的城市收藏手账",{}){
        Text("My Little\nAtlas ✧",style=MaterialTheme.typography.headlineLarge,modifier=Modifier.padding(top=26.dp))
        Text("把想去，慢慢变成去过。",style=MaterialTheme.typography.titleLarge)
        Box(Modifier.fillMaxWidth().height(235.dp)){
            PolaroidPhoto("city",Modifier.align(Alignment.CenterStart).padding(start=15.dp).size(180.dp,205.dp),rotation=-7f,caption="the little joys of life")
            PaperNote(Modifier.align(Alignment.BottomEnd).size(132.dp,100.dp).rotate(6f)){Handwritten("收藏心动\n记录抵达",size=18)}
        }
        Text("收下喜欢的地点，在地图上找到下一站，再把每一次到访写成自己的小小手账。",fontSize=13.sp,lineHeight=23.sp,color=AtlasColors.Muted)
        OutlinedButton(onClick={choosing=true},modifier=Modifier.fillMaxWidth()){AtlasIcon(Glyph.PIN,size=17.dp);Spacer(Modifier.width(8.dp));Text("从 "+city+" 开始探索")}
        PrimaryButton("翻开我的图鉴",{if(controller.chooseCity(city))controller.completeOnboarding()},Modifier.fillMaxWidth(),Glyph.BOOK)
        Text("不用登录，也可以开始。记录默认只留在本机。",fontSize=10.sp,color=AtlasColors.Muted,modifier=Modifier.align(Alignment.CenterHorizontally))
    }
    if(choosing)AlertDialog(onDismissRequest={choosing=false},title={Text("从哪座城开始？")},text={Column{
        AtlasField(city,{city=it},"城市名称")
        listOf(listOf("上海","北京","杭州"),listOf("成都","广州","深圳")).forEach{row->Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){row.forEach{c->Tag(c,city==c,{city=c})}}}
        Text("可以手动输入，之后随时切换。",fontSize=11.sp,color=AtlasColors.Muted)
    }},confirmButton={TextButton(onClick={choosing=false},enabled=city.isNotBlank()){Text("选好啦")}},containerColor=AtlasColors.Card)
}
