package com.mylittleatlas.app.data

import com.mylittleatlas.app.domain.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

interface AtlasStorage {
    fun read(): String?
    /** Implementations atomically replace the file; a failed write keeps prior content. */
    fun write(content: String)
}

class AtlasRepository(private val storage: AtlasStorage) {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    fun load(): AtlasData {
        val raw = storage.read() ?: return AtlasData(demoContent=false,onboarded=false)
        return json.decodeFromString<AtlasData>(raw).also {
            require(it.schemaVersion == 1) { "此备份来自较新版本，请先更新 App。" }
        }
    }

    fun save(data: AtlasData) = storage.write(export(data))
    fun export(data: AtlasData) = json.encodeToString(data)

    fun restore(raw: String): AtlasData {
        val data = json.decodeFromString<AtlasData>(raw)
        require(data.schemaVersion == 1) { "不支持此备份版本。" }
        require(data.places.map { it.id }.distinct().size == data.places.size) { "备份中有重复地点编号。" }
        require(data.collections.map { it.id }.distinct().size == data.collections.size) { "备份中有重复收藏夹编号。" }
        require(data.visits.all { v -> data.places.any { it.id == v.placeId } }) { "备份包含没有对应地点的到访记录。" }
        require(data.drafts.all { v -> data.places.any { it.id == v.placeId } }) { "备份包含没有对应地点的草稿。" }
        require(data.visits.map{it.id}.distinct().size==data.visits.size) { "备份中有重复到访编号。" }
        require(data.drafts.map{it.id}.distinct().size==data.drafts.size) { "备份中有重复草稿编号。" }
        return data
    }
}

object DemoAtlas {
    fun create() = AtlasData(
        collections = listOf(
            Collection("coffee", "咖啡慢一点", "一杯咖啡的时间，留给自己。", "cafe", "sage"),
            Collection("sweet", "甜品小宇宙", "日子总要有一点甜。", "dessert", "peach"),
            Collection("weekend", "周末出走计划", "不用太远，刚好心动。", "city", "blue"),
            Collection("books", "在书页里散步", "把生活翻到喜欢的那一页。", "bookshop", "sage"),
        ),
        places = listOf(
            Place("hidden", "Hidden Coffee", district = "徐汇区", address = "武康路 · 街角", cover = "interior",
                reason = "想坐在窗边，喝一杯拿铁，看街上的人慢慢经过。", collectionIds = listOf("coffee", "weekend"),
                latitude = 31.2074, longitude = 121.4365, wantRevisit = true, addedOn = "2026-10-12"),
            Place("slow", "慢半拍咖啡", district = "静安区", address = "愚园路 · 树荫下", cover = "cafe",
                reason = "朋友说这里的阳光和手冲都很好。", collectionIds = listOf("coffee", "weekend"),
                latitude = 31.2231, longitude = 121.4392, addedOn = "2026-11-03"),
            Place("butter", "Butter & Crumb", district = "黄浦区", address = "南昌路 · 小巷", kind = PlaceKind.SWEET, cover = "dessert",
                reason = "留一个空着的下午，去吃刚烤好的可颂。", collectionIds = listOf("sweet", "weekend"),
                latitude = 31.2154, longitude = 121.4581, addedOn = "2026-11-02"),
            Place("paper", "纸间书店", district = "徐汇区", address = "衡山路 · 老洋房", kind = PlaceKind.BOOK, cover = "bookshop",
                reason = "安静的小书店，想带一本新书回家。", collectionIds = listOf("books", "weekend"),
                latitude = 31.2042, longitude = 121.4495, addedOn = "2026-10-29"),
            Place("daily", "日常之间", district = "长宁区", address = "新华路 · 街角", cover = "coffee",
                reason = "收藏每一个被咖啡香叫醒的周末。", collectionIds = listOf("coffee"),
                latitude = 31.2076, longitude = 121.4182, addedOn = "2026-10-18"),
            Place("green", "街角花园", district = "静安区", address = "巨鹿路 · 花园里", kind = PlaceKind.FOOD, cover = "interior",
                reason = "和喜欢的人，慢慢吃一顿午饭。", collectionIds = listOf("weekend"),
                latitude = 31.2205, longitude = 121.4503, wantRevisit = true, addedOn = "2026-10-16"),
            Place("riverside", "江边散步", district = "黄浦区", address = "滨江步道", kind = PlaceKind.WALK, cover = "city",
                reason = "傍晚沿着江走走，把风装进口袋。", collectionIds = listOf("weekend"),
                latitude = 31.2243, longitude = 121.4880, addedOn = "2026-10-11"),
            Place("tart", "一颗柠檬", district = "徐汇区", address = "永康路 · 甜品店", kind = PlaceKind.SWEET, cover = "dessert",
                reason = "柠檬塔和周五，都是快乐的形状。", collectionIds = listOf("sweet"),
                latitude = 31.2108, longitude = 121.4510, addedOn = "2026-11-04"),
        ),
        visits = listOf(
            Visit("visit-hidden", "hidden", "2026-11-01", "阳光正好，很喜欢这家店。\n拿铁的奶泡很绵，窗外的梧桐也刚好。"),
            Visit("visit-green", "green", "2026-10-25", "和朋友从中午坐到傍晚。\n没有计划的一天，也可以很美好。"),
            Visit("visit-paper", "paper", "2026-10-18", "找到一本喜欢的散文，\n也找到一个可以放空的角落。"),
        ),
    )
}
