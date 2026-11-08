package com.mylittleatlas.app.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.mylittleatlas.app.data.AtlasRepository
import com.mylittleatlas.app.domain.*
import com.mylittleatlas.app.platform.PlatformServices

class AtlasController(val platform: PlatformServices) {
    private val repository = AtlasRepository(platform.storage)
    var loadError by mutableStateOf<String?>(null)
        private set
    var data by mutableStateOf(runCatching { repository.load() }.getOrElse {
        loadError = "无法读取本地记录。原数据已保留，请先导出原文件或恢复备份。"
        AtlasData(demoContent = false)
    })
        private set
    var message by mutableStateOf<String?>(null)

    private fun commit(next: AtlasData): Boolean {
        if (loadError != null) { message = loadError; return false }
        return runCatching { repository.save(next); data = next; true }.getOrElse {
            message = "保存失败，原记录已保留。请检查设备空间后重试。"; false
        }
    }

    fun addPlace(place: Place): Boolean {
        if (data.places.any { it.name == place.name && it.city == place.city && it.address == place.address }) {
            message = "这个地点已在你的收藏里。"; return false
        }
        return commit(data.copy(places = listOf(place) + data.places)).also { if (it) message = "已收进你的私人地图。" }
    }

    fun importSelected(candidates: List<ImportCandidate>, collectionIds: List<String>): Int {
        val next = data.places.toMutableList()
        for (candidate in candidates) {
            if (!TextPlaceImporter.isDuplicate(candidate, next)) {
                next.add(0, Place(platform.uniqueId(), candidate.name, candidate.city, address = candidate.address,
                    district = candidate.district, latitude = candidate.latitude, longitude = candidate.longitude,
                    gcjLatitude = candidate.gcjLatitude, gcjLongitude = candidate.gcjLongitude, poiId = candidate.poiId,
                    reason = candidate.reason, kind = candidate.kind,
                    cover = when(candidate.kind) { PlaceKind.FOOD -> "interior"; PlaceKind.SWEET -> "dessert"; PlaceKind.WALK -> "city"; else -> "cafe" },
                    collectionIds = collectionIds, source = candidate.source, addedOn = platform.today()))
            }
        }
        val count = next.size - data.places.size
        if (count == 0) { message = "所选地点已经在你的收藏里。"; return 0 }
        if (!commit(data.copy(places = next))) return 0
        val located = candidates.count { it.latitude != null && it.longitude != null }
        message = "已收藏 $count 个想去的地方。${if(located > 0) "$located 处已定位，可在地图查看。" else "可在地图中补全位置。"}"
        return count
    }

    fun updatePlace(place: Place): Boolean = commit(data.copy(places = data.places.map { if (it.id == place.id) place else it }))
    fun deletePlace(place: Place): Boolean = commit(data.copy(
        places = data.places.filterNot { it.id == place.id }, visits = data.visits.filterNot { it.placeId == place.id },
        drafts = data.drafts.filterNot { it.placeId == place.id },
    ))
    fun addCollection(name: String): Boolean {
        if (name.isBlank()) return false
        if (data.collections.any { it.name == name.trim() }) { message = "已经有同名收藏夹了。"; return false }
        return commit(data.copy(collections = data.collections + Collection(platform.uniqueId(), name.trim())))
    }

    fun createCollection(name: String, subtitle: String, cover: String): Boolean {
        if(name.isBlank())return false
        if(data.collections.any{it.name==name.trim()}){message="已经有同名收藏夹了。";return false}
        return commit(data.copy(collections=data.collections+Collection(platform.uniqueId(),name.trim(),subtitle.trim(),cover)))
    }

    fun addVisit(place: Place, date: String, note: String, photos: List<String>, revisit: Boolean): Boolean {
        return commit(data.copy(
            visits = listOf(Visit(platform.uniqueId(), place.id, date, note.trim(), photos)) + data.visits,
            places = data.places.map { if (it.id == place.id) it.copy(wantRevisit = revisit) else it },
        )).also { if (it) message = "又一段小小的回忆，被好好收下了。" }
    }

    fun updateVisit(visit: Visit): Boolean = commit(data.copy(visits=data.visits.map{if(it.id==visit.id)visit else it}))
    /** New places, the entry and removal of its draft are saved together. */
    fun saveJournalEntry(placeId: String?, name: String, city: String, date: String, note: String,
        photos: List<String>, revisit: Boolean, draftId: String? = null, visitId: String? = null,
        title: String = "", layout: String? = null): Visit? {
        if(name.isBlank() || city.isBlank() || !isValidAtlasDate(date) || date > platform.today()) {
            message = "请填写地点、城市和有效的到访日期。"; return null
        }
        val existingVisit = visitId?.let { id -> data.visits.find { it.id == id } }
        if(visitId != null && existingVisit == null) { message = "这篇记录已不存在。"; return null }
        val existingPlace = placeId?.let { id -> data.places.find { it.id == id } }
            ?.takeIf { it.name == name.trim() && it.city == city.trim() }
            ?: data.places.find { it.name == name.trim() && it.city == city.trim() }
        val place = existingPlace ?: Place(platform.uniqueId(), name.trim(), city.trim(), kind = PlaceKind.OTHER, cover = "city", addedOn = platform.today())
        val normalizedTitle = title.trim().ifBlank {
            existingVisit?.title?.trim()?.takeIf { it.isNotBlank() } ?: name.trim()
        }
        val normalizedLayout = layout?.trim()?.takeIf { it.isNotBlank() } ?: existingVisit?.layout ?: "warm"
        val visit = Visit(existingVisit?.id ?: platform.uniqueId(), place.id, date, note.trim(), photos,
            normalizedLayout, normalizedTitle)
        val nextPlaces = if(existingPlace == null) listOf(place.copy(wantRevisit = revisit)) + data.places
            else data.places.map { if(it.id == place.id) it.copy(wantRevisit = revisit) else it }
        val next = data.copy(places = nextPlaces, visits = listOf(visit) + data.visits.filterNot { it.id == visit.id },
            drafts = data.drafts.filterNot { it.id == draftId })
        return if(commit(next)) { message = "已保存，翻到新的一页。"; visit } else null
    }
    fun deleteVisit(id: String): Boolean = commit(data.copy(visits=data.visits.filterNot{it.id==id}))
    fun saveDraft(draft: VisitDraft): Boolean = commit(data.copy(drafts=listOf(draft)+data.drafts.filterNot{it.id==draft.id}))
    fun deleteDraft(id: String): Boolean = commit(data.copy(drafts=data.drafts.filterNot{it.id==id}))
    fun updateProfile(name: String, signature: String, cover: String): Boolean = commit(data.copy(nickname=name.trim().take(24),signature=signature.trim().take(120),profileCover=cover))
    fun completeOnboarding(): Boolean = commit(data.copy(onboarded=true))
    fun chooseCity(city:String): Boolean = commit(data.copy(preferredCity=city.trim()))
    fun updateCollection(collection: com.mylittleatlas.app.domain.Collection): Boolean = commit(data.copy(collections=data.collections.map{if(it.id==collection.id)collection else it}))
    fun deleteCollection(id: String): Boolean = commit(data.copy(collections=data.collections.filterNot{it.id==id},places=data.places.map{it.copy(collectionIds=it.collectionIds-id)}))
    fun assignCollections(ids: Set<String>, folders: List<String>): Boolean = commit(data.copy(places=data.places.map{if(it.id in ids)it.copy(collectionIds=folders) else it}))
    fun deletePlaces(ids: Set<String>): Boolean = commit(data.copy(places=data.places.filterNot{it.id in ids},visits=data.visits.filterNot{it.placeId in ids},drafts=data.drafts.filterNot{it.placeId in ids}))
    fun rename(name: String) { if (name.isNotBlank()) commit(data.copy(nickname = name.trim().take(24))) }
    fun navigate(place: Place) {
        platform.navigate(if (data.isDemoPlace(place)) place.copy(latitude = null, longitude = null) else place)
    }
    fun exportBackup() = platform.exportBackup(repository.export(data))
    fun restoreBackup() = platform.importBackup { raw ->
        if (raw != null) runCatching {
            val restored = repository.restore(raw)
            // Restore is explicitly initiated from a confirmation dialog in the UI.
            repository.save(restored); data = restored; loadError = null
            message = "备份已恢复。照片文件需保留在原设备。"
        }.onFailure { message = "未恢复：${it.message ?: "备份格式不正确"}" }
    }
    fun clearDemo(): Boolean = commit(AtlasData(nickname = data.nickname, demoContent = false,signature=data.signature,profileCover=data.profileCover))
}
