package com.mylittleatlas.app

import androidx.compose.ui.graphics.ImageBitmap
import com.mylittleatlas.app.data.AtlasRepository
import com.mylittleatlas.app.data.AtlasStorage
import com.mylittleatlas.app.domain.*
import com.mylittleatlas.app.platform.AtlasTransport
import com.mylittleatlas.app.platform.PlatformServices
import com.mylittleatlas.app.presentation.AtlasController
import kotlin.test.*

class JournalFlowTest {
    @Test fun newEntrySavesPlaceVisitAndDraftRemovalTogether() {
        val platform=TestPlatform()
        val controller=AtlasController(platform)
        controller.saveDraft(VisitDraft("draft","","2026-11-08",placeName="湖边",city="杭州"))
        val visit=controller.saveJournalEntry(null,"湖边","杭州","2026-11-08","坐一会儿",emptyList(),false,"draft",title="风吹过湖面",layout="film")
        assertNotNull(visit)
        assertEquals("杭州",controller.data.places.single().city)
        assertEquals(PlaceKind.OTHER,controller.data.places.single().kind)
        assertEquals(controller.data.places.single().id,visit.placeId)
        assertEquals("风吹过湖面",visit.title)
        assertEquals("film",visit.layout)
        assertEquals(1,controller.data.visits.size)
        assertTrue(controller.data.drafts.isEmpty())
        assertEquals(controller.data,AtlasRepository(platform.storage).load())
    }

    @Test fun explicitTitleAndBodyStaySeparateAndLegacyNotesRemainReadable() {
        val place=Place("p","纸间书店")
        val modern=Visit("modern",place.id,"2026-11-08","正文第二行",title="这一页的标题")
        assertEquals("这一页的标题",modern.displayTitle(place.name))
        assertEquals("正文第二行",modern.displayBody())
        val legacy=Visit("legacy",place.id,"2026-11-07","旧标题\n旧正文")
        assertEquals("旧标题",legacy.displayTitle(place.name))
        assertEquals("旧正文",legacy.displayBody())
    }

    @Test fun failedSaveKeepsDraftAndDoesNotLeaveAnExtraPlace() {
        val platform=TestPlatform()
        val controller=AtlasController(platform)
        controller.saveDraft(VisitDraft("draft","","2026-11-08",placeName="待续"))
        val before=controller.data
        platform.memory.fail=true
        assertNull(controller.saveJournalEntry(null,"小店","上海","2026-11-08","",emptyList(),false,"draft"))
        assertEquals(before,controller.data)
        assertEquals(before,AtlasRepository(platform.storage).load())
    }

    @Test fun sameNamedPlacesInDifferentCitiesStaySeparateAndEditingKeepsIdAndLayout() {
        val platform=TestPlatform()
        val controller=AtlasController(platform)
        val first=assertNotNull(controller.saveJournalEntry(null,"街角","上海","2026-11-07","",emptyList(),false))
        controller.updateVisit(first.copy(layout="film"))
        controller.saveJournalEntry(null,"街角","杭州","2026-11-08","",emptyList(),false)
        val edited=assertNotNull(controller.saveJournalEntry(first.placeId,"街角","上海","2026-11-06","改过了",emptyList(),true,visitId=first.id))
        assertEquals(2,controller.data.places.size)
        assertEquals(2,controller.data.visits.size)
        assertEquals(first.id,edited.id)
        assertEquals("film",edited.layout)
        assertEquals(setOf("上海","杭州"),controller.data.visitedCities().toSet())
        assertEquals("2026-11-08",controller.data.journalVisits().first().date)
    }

    @Test fun unfinishedPlaceDraftRoundTripsWithoutCreatingPlaceOrVisit() {
        val platform=TestPlatform()
        val controller=AtlasController(platform)
        val draft=VisitDraft("new","","2026-11-08","未完成",placeName="新店",city="南京")
        assertTrue(controller.saveDraft(draft))
        val repository=AtlasRepository(platform.storage)
        assertEquals(draft,repository.restore(repository.export(controller.data)).drafts.single())
        assertTrue(controller.data.places.isEmpty())
        assertTrue(controller.data.visits.isEmpty())
    }

    @Test fun invalidEntryDoesNotCreateRecordsAndCitiesOnlyCountVisits() {
        val platform=TestPlatform()
        val controller=AtlasController(platform)
        assertNull(controller.saveJournalEntry(null,"店名","上海","2026-11-09","",emptyList(),false))
        assertNull(controller.saveJournalEntry(null,"店名","上海","2026-02-30","",emptyList(),false))
        assertTrue(controller.data.places.isEmpty())
        controller.addPlace(Place("wish","想去的小店",city="南京"))
        assertTrue(controller.data.visitedCities().isEmpty())
        controller.saveJournalEntry(null,"去过的小店","杭州","2026-11-08","",emptyList(),false)
        assertEquals(listOf("杭州"),controller.data.visitedCities())
    }

    private class Memory:AtlasStorage {
        var value:String?=null
        var fail=false
        override fun read()=value
        override fun write(content:String) { check(!fail);value=content }
    }
    private class TestPlatform:PlatformServices {
        val memory=Memory()
        override val storage:AtlasStorage=memory
        override var serviceUrl=""
        override val transport:AtlasTransport get()=error("No network during journal tests")
        var serial=0
        override fun today()="2026-11-08"
        override fun uniqueId()="id-${serial++}"
        override fun pickPhoto(onResult:(String?)->Unit)=Unit
        override fun exportBackup(json:String)=Unit
        override fun importBackup(onResult:(String?)->Unit)=Unit
        override fun shareText(text:String)=Unit
        override fun shareImage(bitmap:ImageBitmap)=Unit
        override suspend fun saveImage(bitmap:ImageBitmap)=false
        override fun locate(onResult:(GeoPoint?,String?)->Unit)=Unit
        override fun navigate(place:Place)=Unit
        override fun openSource(url:String)=Unit
    }
}
