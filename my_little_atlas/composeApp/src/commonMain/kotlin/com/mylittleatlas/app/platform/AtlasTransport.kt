package com.mylittleatlas.app.platform

import androidx.compose.ui.graphics.ImageBitmap
import com.mylittleatlas.app.domain.*
import kotlinx.serialization.json.*

interface AtlasTransport {
    suspend fun request(path: String, body: JsonObject? = null): JsonObject
    suspend fun tile(z: Int, x: Int, y: Int): ByteArray
}

expect fun decodeMapTile(bytes: ByteArray): ImageBitmap

class AtlasService(private val transport: AtlasTransport) {
    suspend fun parse(text: String, city: String): NoteImport {
        val link = SourceLinks.extract(text)
        val root = transport.request("/api/parse", buildJsonObject {
            put(if (link != null) "url" else "text", link ?: text)
            put("city", city); put("recognitionMode", "auto"); put("deferMatching", true)
        })
        return ImportResponse.note(root, link.orEmpty())
    }
    suspend fun match(note: NoteImport, city: String): NoteImport {
        val root = transport.request("/api/match", buildJsonObject {
            put("city", city); put("title", note.title); put("text", note.text)
            put("shops", buildJsonArray { note.shops.forEach { s -> add(buildJsonObject {
                put("id", s.id); put("name", s.name); put("addressHint", s.hint); put("evidence", s.evidence); put("category", s.category)
            }) } })
        })
        return note.copy(shops = ImportResponse.shops(root), warnings = note.warnings + ImportResponse.warnings(root))
    }
    suspend fun search(keyword: String, city: String, address: Boolean = false): List<ResolvedPoi> {
        val root = transport.request(if (address) "/api/address/geocode" else "/api/search", buildJsonObject {
            put(if (address) "address" else "keyword", keyword); put("city", city)
        })
        return root.objects("candidates").mapNotNull(ImportResponse::poi)
    }
}
