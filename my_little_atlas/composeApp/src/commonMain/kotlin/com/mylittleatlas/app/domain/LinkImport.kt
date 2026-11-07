package com.mylittleatlas.app.domain

import kotlinx.serialization.json.*

/** Public sharing links are kept intact during reading; only the note URL is persisted. */
object SourceLinks {
    private val url = Regex("https?://[^\\s<>\"'，。；！）】]+", RegexOption.IGNORE_CASE)
    private val hosts = setOf("xiaohongshu.com", "www.xiaohongshu.com", "xhslink.com", "www.xhslink.com", "xhslink.cn", "www.xhslink.cn")
    fun extract(text: String): String? = url.findAll(text).map { it.value.trimEnd(')', ',', '.', ';', '!', '?') }.firstOrNull { value ->
        val authority = value.substringAfter("://").substringBefore('/').substringBefore('?').lowercase()
        authority in hosts || authority.removeSuffix(":443") in hosts
    }?.replaceFirst(Regex("^http:", RegexOption.IGNORE_CASE), "https:")
    fun noteId(text: String): String? = Regex("/(?:explore|discovery/item)/(\\w+)").find(text)?.groupValues?.get(1)
    fun persistent(url: String): String = url.substringBefore('?').substringBefore('#')
}

data class ResolvedPoi(
    val id: String, val name: String, val address: String, val city: String, val district: String,
    val latitude: Double, val longitude: Double, val gcjLatitude: Double? = null, val gcjLongitude: Double? = null,
    val level: String = "",
)

data class NoteShop(
    val id: String, val name: String, val hint: String, val evidence: String, val category: String,
    val place: ResolvedPoi? = null, val candidates: List<ResolvedPoi> = emptyList(), val matchReason: String = "",
) {
    fun candidate(city: String, source: String) = ImportCandidate(
        name, place?.address ?: hint, place?.city?.ifBlank { city } ?: city, SourceLinks.persistent(source),
        district = place?.district.orEmpty(), latitude = place?.latitude, longitude = place?.longitude,
        gcjLatitude = place?.gcjLatitude, gcjLongitude = place?.gcjLongitude, poiId = place?.id.orEmpty(),
        reason = evidence, kind = when (category) { "food" -> PlaceKind.FOOD; "dessert" -> PlaceKind.SWEET; "coffee" -> PlaceKind.COFFEE; else -> PlaceKind.WALK },
    )
}

data class NoteImport(
    val title: String, val text: String, val source: String, val shops: List<NoteShop>,
    val warnings: List<String>, val method: String,
)

internal fun JsonObject.string(key: String) = (get(key) as? JsonPrimitive)?.contentOrNull.orEmpty()
internal fun JsonObject.number(key: String) = (get(key) as? JsonPrimitive)?.doubleOrNull
internal fun JsonObject.objects(key: String) = (get(key) as? JsonArray)?.mapNotNull { it as? JsonObject }.orEmpty()

object ImportResponse {
    fun poi(value: JsonObject): ResolvedPoi? {
        val lat = value.number("lat") ?: return null
        val lng = value.number("lng") ?: return null
        if (!lat.isFinite() || !lng.isFinite() || lat !in -85.051129..85.051129 || lng !in -180.0..180.0) return null
        val gcjLat = value.number("gcjLat")?.takeIf { it.isFinite() && it in -85.051129..85.051129 }
        val gcjLng = value.number("gcjLng")?.takeIf { it.isFinite() && it in -180.0..180.0 }
        return ResolvedPoi(value.string("id"), value.string("name"), value.string("address"), value.string("city"),
            value.string("district"), lat, lng, gcjLat, gcjLng, value.string("level"))
    }
    fun shops(root: JsonObject): List<NoteShop> = root.objects("shops").mapNotNull { s ->
        val name = s.string("name").trim()
        if (name.isBlank()) return@mapNotNull null
        NoteShop(s.string("id"), name, s.string("addressHint"), s.string("evidence"), s.string("category"),
            (s["place"] as? JsonObject)?.let(::poi), s.objects("candidates").mapNotNull(::poi),
            (s["matching"] as? JsonObject)?.string("reason").orEmpty())
    }.distinctBy { it.place?.id?.takeIf(String::isNotBlank) ?: "${it.name.lowercase()}|${it.hint}" }
    fun warnings(root: JsonObject) = (root["warnings"] as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }.orEmpty()
    fun note(root: JsonObject, source: String): NoteImport {
        val note = root["note"] as? JsonObject ?: JsonObject(emptyMap())
        return NoteImport(note.string("title"), note.string("text"), SourceLinks.persistent(note.string("sourceUrl").ifBlank { source }),
            shops(root), warnings(root), root.string("method"))
    }
}
