package com.mylittleatlas.app

import com.mylittleatlas.app.data.AtlasRepository
import com.mylittleatlas.app.data.AtlasStorage
import com.mylittleatlas.app.domain.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class AtlasDomainTest {
    @Test
    fun structuredImportProducesCandidatesWithoutCommittingThem() {
        val candidates = TextPlaceImporter.extract(
            "河畔咖啡｜滨江路 8 号｜上海\nhttps://www.xiaohongshu.com/explore/abc\n书页咖啡|大学路 18 号|上海",
        )
        assertEquals(2, candidates.size)
        assertEquals("河畔咖啡", candidates.first().name)
        assertEquals("上海", candidates.first().city)
    }

    @Test
    fun datesRejectImpossibleCalendarDaysAndAcceptLeapYears() {
        assertTrue(isValidAtlasDate("2024-02-29"))
        assertFalse(isValidAtlasDate("2025-02-29"))
        assertFalse(isValidAtlasDate("2026-04-31"))
    }

    @Test
    fun visitedAndWantRevisitCanBothBeTrue() {
        val place = Place("p1", "河畔咖啡", wantRevisit = true)
        val data = AtlasData(places = listOf(place), visits = listOf(Visit("v1", place.id, "2026-11-01", "")))
        assertTrue(data.matches(place, PlaceFilter.VISITED))
        assertTrue(data.matches(place, PlaceFilter.REVISIT))
        assertFalse(data.matches(place, PlaceFilter.WANT))
    }

    @Test
    fun backupRejectsVisitsWithoutPlaces() {
        val repository = AtlasRepository(MemoryStorage())
        val invalid = AtlasData(visits = listOf(Visit("v1", "missing", "2026-11-01", "")))
        assertFailsWith<IllegalArgumentException> { repository.restore(repository.export(invalid)) }
    }

    @Test
    fun repositoryRoundTripsPrivateCollectionsAndVisits() {
        val storage = MemoryStorage()
        val repository = AtlasRepository(storage)
        val place = Place("p1", "河畔咖啡", collectionIds = listOf("c1"), wantRevisit = true)
        val expected = AtlasData(
            places = listOf(place),
            collections = listOf(Collection("c1", "沿河散步")),
            visits = listOf(Visit("v1", "p1", "2026-11-01", "风很好")),
        )
        repository.save(expected)
        assertEquals(expected, repository.load())
        assertEquals(expected, repository.restore(repository.export(expected)))
    }

    private class MemoryStorage : AtlasStorage {
        private var value: String? = null
        override fun read() = value
        override fun write(content: String) { value = content }
    }

    @Test
    fun draftLayoutAndProfileRoundTripWithoutBecomingVisits() {
        val storage=MemoryStorage()
        val repository=AtlasRepository(storage)
        val expected=AtlasData(places=listOf(Place("p","小店")),visits=listOf(Visit("v","p","2026-11-01","那一天",listOf("photo-one","photo-two"),"film")),
            drafts=listOf(VisitDraft("d","p","2026-11-02","待续",listOf("draft-photo"),true)),signature="慢慢走",profileCover="local:cover",preferredCity="杭州")
        repository.save(expected)
        assertEquals(expected,repository.load())
        assertEquals(expected,repository.restore(repository.export(expected)))
        assertEquals(1,repository.load().visits.size)
        assertFalse(AtlasData(places=expected.places,drafts=expected.drafts).isVisited("p"))
    }

    @Test
    fun legacyDataKeepsExistingUsersPastWelcome() {
        val storage=MemoryStorage()
        storage.write("""{"schemaVersion":1,"nickname":"旧用户","places":[{"id":"p","name":"小店"}]}""")
        val data=AtlasRepository(storage).load()
        assertTrue(data.onboarded)
        assertEquals("旧用户",data.nickname)
        assertTrue(data.drafts.isEmpty())
        assertEquals("p",data.places.single().id)
    }

    @Test
    fun newLibraryStartsEmptyAndRequestsWelcome() {
        val data=AtlasRepository(MemoryStorage()).load()
        assertFalse(data.onboarded)
        assertFalse(data.demoContent)
        assertTrue(data.places.isEmpty())
    }

    @Test
    fun backupRejectsOrphanDraftsAndDuplicateVisitIds() {
        val repository=AtlasRepository(MemoryStorage())
        val orphan=AtlasData(drafts=listOf(VisitDraft("d","missing","2026-11-01")))
        assertFailsWith<IllegalArgumentException>{repository.restore(repository.export(orphan))}
        val visit=Visit("same","p","2026-11-01","")
        val duplicate=AtlasData(places=listOf(Place("p","小店")),visits=listOf(visit,visit))
        assertFailsWith<IllegalArgumentException>{repository.restore(repository.export(duplicate))}
    }
}
