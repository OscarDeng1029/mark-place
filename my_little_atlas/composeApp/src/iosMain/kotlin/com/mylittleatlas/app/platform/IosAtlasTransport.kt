package com.mylittleatlas.app.platform

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlin.coroutines.coroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal class IosAtlasTransport(
    private val bridge: IosPlatformBridge,
    private val endpoint: () -> String,
) : AtlasTransport {
    private val slots = Semaphore(6)
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun request(path: String, body: JsonObject?): JsonObject {
        val base = endpoint().trim().trimEnd('/')
        require(base.startsWith("https://") || base.startsWith("http://127.0.0.1:") || base.startsWith("http://localhost:")) {
            "服务地址需为 HTTPS，或本机调试地址。"
        }
        val bytes = fetch(base + path, body?.toString(), false)
        return runCatching { json.parseToJsonElement(bytes.decodeToString()) as? JsonObject }.getOrNull()
            ?: error("服务未返回有效结果，请检查解析服务地址。")
    }

    override suspend fun tile(z: Int, x: Int, y: Int): ByteArray {
        require(z in 0..18 && x in 0 until (1 shl z) && y in 0 until (1 shl z))
        return slots.withPermit {
            val urls = listOf(1, 2).map {
                "https://webrd0$it.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=1&style=7&x=$x&y=$y&z=$z"
            } + (endpoint().trimEnd('/') + "/tiles/road/$z/$x/$y.png")
            for (url in urls) {
                coroutineContext.ensureActive()
                try {
                    val bytes = fetch(url, null, true)
                    // NSURLSession owns the bounded HTTP cache and respects Cache-Control.
                    return@withPermit bytes
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) { /* Same direct-host then service fallback as Android. */ }
            }
            error("地图暂时无法连接，点击重试。")
        }
    }

    private suspend fun fetch(url: String, body: String?, tile: Boolean): ByteArray =
        suspendCancellableCoroutine { continuation ->
            val id = bridge.request(url, body, tile) { bytes, error ->
                if (continuation.isActive) {
                    if (bytes != null) continuation.resume(bytes)
                    else continuation.resumeWithException(IllegalStateException(error ?: "网络请求失败，请重试。"))
                }
            }
            continuation.invokeOnCancellation { bridge.cancelRequest(id) }
        }
}
