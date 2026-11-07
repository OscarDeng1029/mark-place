package com.mylittleatlas.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AtlasCalendar(initial:String,today:String,onSelect:(String)->Unit,onClose:()->Unit) {
    val initialParts=initial.split('-')
    var year by remember{mutableStateOf(initialParts.getOrNull(0)?.toIntOrNull() ?: today.take(4).toInt())}
    var month by remember{mutableStateOf((initialParts.getOrNull(1)?.toIntOrNull() ?: 1).coerceIn(1,12))}
    val leap=year%4==0&&(year%100!=0||year%400==0)
    val days=when(month){2->if(leap)29 else 28;4,6,9,11->30;else->31}
    val offsets=listOf(0,3,2,5,0,3,5,1,4,6,2,4)
    val y=year-if(month<3)1 else 0
    val first=((y+y/4-y/100+y/400+offsets[month-1]+1)%7+6)%7
    AlertDialog(onDismissRequest=onClose,title={Text("这次是哪一天？",style=MaterialTheme.typography.titleLarge)},text={
        Column {
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                IconButton(Glyph.BACK,"上个月",{if(month==1){year--;month=12}else month--})
                Text(year.toString()+" 年 "+month+" 月",modifier=Modifier.weight(1f),fontSize=15.sp)
                IconButton(Glyph.ARROW,"下个月",{if(month==12){year++;month=1}else month++})
            }
            Row{listOf("一","二","三","四","五","六","日").forEach{Text(it,Modifier.weight(1f),fontSize=11.sp,textAlign=androidx.compose.ui.text.style.TextAlign.Center)}}
            (0 until ((first+days+6)/7)).forEach{week->Row {
                (0..6).forEach{col->val day=week*7+col-first+1
                    val value=year.toString().padStart(4,'0')+"-"+month.toString().padStart(2,'0')+"-"+day.toString().padStart(2,'0')
                    val enabled=day in 1..days&&value<=today
                    Box(Modifier.weight(1f).height(38.dp).clip(CircleShape).background(if(value==initial)AtlasColors.SagePale else androidx.compose.ui.graphics.Color.Transparent)
                        .clickable(enabled=enabled){onSelect(value)},contentAlignment=Alignment.Center) {if(day in 1..days)Text(day.toString(),fontSize=12.sp,color=if(enabled)AtlasColors.Ink else AtlasColors.Line)}
                }
            }}
            TextButton(onClick={onSelect(today)}){Text("回到今天") }
        }
    },confirmButton={TextButton(onClick=onClose){Text("取消")}},containerColor=AtlasColors.Card)
}
