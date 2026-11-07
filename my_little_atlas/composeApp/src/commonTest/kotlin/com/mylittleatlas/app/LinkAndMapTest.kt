package com.mylittleatlas.app

import com.mylittleatlas.app.data.AtlasRepository
import com.mylittleatlas.app.data.AtlasStorage
import com.mylittleatlas.app.domain.*
import kotlinx.serialization.json.*
import kotlin.test.*

class LinkAndMapTest {
    @Test fun sharingTextPreservesSignedUrlWhileRejectingSpoofedHosts() {
        val signed = "http://xhslink.cn/o/Ab12?xsec_token=a%2Bb&xsec_source=share"
        assertEquals(signed.replace("http:","https:"),SourceLinks.extract("好店推荐 $signed，复制打开小红书"))
        assertNull(SourceLinks.extract("https://xiaohongshu.com.attacker.example/explore/123"))
        assertNull(SourceLinks.extract("https://user@xiaohongshu.com/explore/123"))
        assertNull(SourceLinks.extract("https://xiaohongshu.com:8080/explore/123"))
        assertEquals("abc123",SourceLinks.noteId("https://www.xiaohongshu.com/discovery/item/abc123?xsec_token=secret"))
        assertEquals("https://xhslink.cn/o/Ab12",SourceLinks.persistent(signed.replace("http:","https:")))
    }

    @Test fun responseUsesRealPoiCoordinatesDeduplicatesAndKeepsUnresolvedShops() {
        val root = Json.parseToJsonElement("""{
          "note":{"title":"咖啡笔记","text":"推荐两家店","sourceUrl":"https://www.xiaohongshu.com/explore/123?xsec_token=secret"},
          "method":"ai","shops":[
            {"id":"a","name":"咖啡A","addressHint":"福田","evidence":"p1 咖啡A","category":"coffee","place":{"id":"poi-a","name":"咖啡A","address":"深圳市福田区某路8号","city":"深圳","lat":22.55,"lng":114.05,"gcjLat":22.547,"gcjLng":114.055}},
            {"id":"duplicate","name":"咖啡A 别名","place":{"id":"poi-a","lat":22.55,"lng":114.05}},
            {"id":"b","name":"咖啡B","addressHint":"圆岭","place":null},
            {"id":"c","name":"咖啡C","place":{"id":"invalid","lat":200,"lng":114}}
          ]} """).jsonObject
        val note = ImportResponse.note(root,"")
        assertEquals(3,note.shops.size)
        val candidate = note.shops.first().candidate("深圳",note.source)
        assertEquals(22.55,candidate.latitude)
        assertEquals(22.547,candidate.gcjLatitude)
        assertEquals("poi-a",candidate.poiId)
        assertFalse(candidate.source.contains("secret"))
        val pending = note.shops[1].candidate("深圳",note.source)
        assertNull(pending.latitude)
        assertEquals("圆岭",pending.address)
        assertEquals(candidate.source,pending.source)
        assertNull(note.shops[2].place)
        assertTrue(TextPlaceImporter.isDuplicate(candidate,listOf(Place("saved","咖啡A 新名字",poiId="poi-a"))))
    }

    @Test fun mapCoordinatesMatchReferenceAndRoundTripProjection() {
        val gcj = MapGeometry.gcj(GeoPoint(31.213,121.447))
        assertTrue(kotlin.math.abs(gcj.latitude-31.21107)<.0001)
        assertTrue(kotlin.math.abs(gcj.longitude-121.45153)<.0002)
        val projected = MapGeometry.project(gcj,15)
        val back = MapGeometry.unproject(projected,15)
        assertTrue(kotlin.math.abs(gcj.latitude-back.latitude)<1e-8)
        assertTrue(kotlin.math.abs(gcj.longitude-back.longitude)<1e-8)
        val p=Place("real","门店",latitude=31.213,longitude=121.447,gcjLatitude=31.211,gcjLongitude=121.451)
        assertEquals(GeoPoint(31.211,121.451),MapGeometry.position(p))
        assertEquals(GeoPoint(40.0,-74.0),MapGeometry.gcj(GeoPoint(40.0,-74.0)))
    }

    @Test fun oldBackupsStillLoadAndNewCoordinateFieldsSurviveRestore() {
        val storage = object: AtlasStorage { override fun read() = """{"places":[{"id":"old","name":"旧地点"}],"demoContent":false}"""; override fun write(content:String) {} }
        val repository = AtlasRepository(storage)
        assertEquals("",repository.load().places.first().poiId)
        val data=AtlasData(places=listOf(Place("new","新地点",latitude=22.55,longitude=114.05,gcjLatitude=22.547,gcjLongitude=114.055,poiId="amap-1")))
        assertEquals(data,repository.restore(repository.export(data)))
        assertFalse(data.isDemoPlace(data.places.first()))
        assertTrue(AtlasData().isDemoPlace(Place("hidden","Hidden Coffee")))
    }

    @Test fun fittedMapKeepsDistantPointsInsideUsableViewport() {
        val points=listOf(GeoPoint(22.54,114.03),GeoPoint(22.59,114.16))
        val (center,zoom)=MapGeometry.fit(points,330.0,360.0)
        val origin=MapGeometry.project(center,zoom)
        points.forEach {
            val projected=MapGeometry.project(it,zoom)
            assertTrue(kotlin.math.abs(projected.x-origin.x)*256<=165)
            assertTrue(kotlin.math.abs(projected.y-origin.y)*256<=180)
        }
    }
}
