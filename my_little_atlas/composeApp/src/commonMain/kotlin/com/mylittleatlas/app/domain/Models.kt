package com.mylittleatlas.app.domain

import kotlinx.serialization.Serializable

@Serializable
enum class PlaceKind(val title: String, val symbol: String) {
    COFFEE("咖啡", "☕"), FOOD("餐厅", "◡"), SWEET("甜品", "◇"), BOOK("书店", "▥"), WALK("漫游", "⌁")
}

enum class PlaceFilter(val title: String) { ALL("全部"), WANT("想去"), VISITED("去过"), REVISIT("再去") }

@Serializable
data class Place(
    val id: String,
    val name: String,
    val city: String = "上海",
    val district: String = "",
    val address: String = "",
    val kind: PlaceKind = PlaceKind.COFFEE,
    val cover: String = "cafe",
    val reason: String = "",
    val collectionIds: List<String> = emptyList(),
    val source: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val gcjLatitude: Double? = null,
    val gcjLongitude: Double? = null,
    val poiId: String = "",
    val wantRevisit: Boolean = false,
    val addedOn: String = "",
)

@Serializable
data class Collection(
    val id: String,
    val name: String,
    val subtitle: String = "慢慢收集，慢慢出发。",
    val cover: String = "cafe",
    val tint: String = "sage",
)

@Serializable
data class Visit(
    val id: String,
    val placeId: String,
    val date: String,
    val note: String,
    val photoPaths: List<String> = emptyList(),
    val layout: String = "warm",
)

@Serializable
data class VisitDraft(
    val id: String, val placeId: String, val date: String, val note: String = "",
    val photoPaths: List<String> = emptyList(), val revisit: Boolean = false,
)

@Serializable
data class AtlasData(
    val schemaVersion: Int = 1,
    val places: List<Place> = emptyList(),
    val collections: List<Collection> = emptyList(),
    val visits: List<Visit> = emptyList(),
    val nickname: String = "小满",
    val demoContent: Boolean = true,
    val signature: String = "收集生活，也收集自己。",
    val profileCover: String = "city",
    val drafts: List<VisitDraft> = emptyList(),
    val onboarded: Boolean = true,
    val preferredCity: String = "",
)

fun AtlasData.isVisited(id: String) = visits.any { it.placeId == id }
fun AtlasData.isDemoPlace(place: Place) = demoContent && place.poiId.isBlank() &&
    place.id in setOf("hidden", "slow", "butter", "paper", "daily", "green", "riverside", "tart")
fun AtlasData.matches(place: Place, filter: PlaceFilter): Boolean = when (filter) {
    PlaceFilter.ALL -> true
    PlaceFilter.WANT -> !isVisited(place.id)
    PlaceFilter.VISITED -> isVisited(place.id)
    PlaceFilter.REVISIT -> isVisited(place.id) && place.wantRevisit
}

fun AtlasData.filterPlaces(filter: PlaceFilter, query: String = "", collectionId: String? = null): List<Place> =
    places.filter { p ->
        matches(p, filter) && (collectionId == null || collectionId in p.collectionIds) &&
            (query.isBlank() || listOf(p.name, p.city, p.district, p.address, p.reason, p.kind.title)
                .any { it.contains(query.trim(), ignoreCase = true) })
    }

fun AtlasData.countIn(collection: Collection) = places.count { collection.id in it.collectionIds }

/** Selection is explicit. Matching candidates never mutates the library. */
data class ImportCandidate(
    val name: String, val address: String, val city: String, val source: String,
    val district: String = "", val latitude: Double? = null, val longitude: Double? = null,
    val gcjLatitude: Double? = null, val gcjLongitude: Double? = null, val poiId: String = "",
    val reason: String = "", val kind: PlaceKind = PlaceKind.COFFEE,
)

object TextPlaceImporter {
    fun extract(text: String): List<ImportCandidate> = text.lineSequence()
        .map(String::trim)
        .map { it.replace('\uFF5C', '|') }
        .filter { it.isNotBlank() && !it.startsWith("https://") && !it.startsWith("http://") }
        .map { it.replace(Regex("^[\\s📍·•*#\\d.、]+"), "") }
        .mapNotNull { line ->
            val parts = line.split(Regex("[|｜]"), limit = 3).map(String::trim)
            if (parts.size < 2 || parts[0].isBlank() || parts[1].isBlank()) null
            else ImportCandidate(parts[0].take(80), parts[1].take(200), parts.getOrElse(2) { "上海" }.take(40), "文本导入")
        }.distinctBy { "${it.city}|${it.name}|${it.address}".lowercase() }.toList()

    fun isDuplicate(candidate: ImportCandidate, places: List<Place>): Boolean = places.any {
        (candidate.poiId.isNotBlank() && candidate.poiId == it.poiId) ||
            (it.name.equals(candidate.name, true) && it.city == candidate.city && it.address == candidate.address)
    }
}

fun isValidAtlasDate(value: String): Boolean {
    if (!Regex("\\d{4}-\\d{2}-\\d{2}").matches(value)) return false
    val year = value.substring(0, 4).toIntOrNull() ?: return false
    val month = value.substring(5, 7).toIntOrNull() ?: return false
    val day = value.substring(8, 10).toIntOrNull() ?: return false
    if (month !in 1..12 || day < 1) return false
    val leap = year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)
    val days = when (month) {
        2 -> if (leap) 29 else 28
        4, 6, 9, 11 -> 30
        else -> 31
    }
    return day <= days
}
