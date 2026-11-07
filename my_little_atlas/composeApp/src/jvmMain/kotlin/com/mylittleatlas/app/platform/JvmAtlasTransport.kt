package com.mylittleatlas.app.platform

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.json.*
import java.net.HttpURLConnection
import java.net.URI
import java.io.IOException

/** Shared by Android and Desktop; the service holds the model and map credentials. */
class JvmAtlasTransport(private val endpoint: () -> String) : AtlasTransport {
    private val json = Json { ignoreUnknownKeys = true }
    private data class CachedTile(val bytes: ByteArray, val until: Long)
    private val cache = LinkedHashMap<String, CachedTile>(192, .75f, true)
    private val slots = Semaphore(6)

    override suspend fun request(path: String, body: JsonObject?): JsonObject = withContext(Dispatchers.IO) {
        val base = endpoint().trim().trimEnd('/')
        require(base.startsWith("https://") || base.startsWith("http://127.0.0.1:") || base.startsWith("http://localhost:") || base.startsWith("http://10.0.2.2:")) {
            "服务地址需为 HTTPS，或本机调试地址。"
        }
        val connection = URI(base + path).toURL().openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 10000; connection.readTimeout = 240000
            connection.setRequestProperty("Accept", "application/json")
            connection.instanceFollowRedirects = false
            if (body != null) {
                connection.requestMethod = "POST"; connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val raw = stream?.use { readLimited(it, 2_500_000).toString(Charsets.UTF_8) }.orEmpty()
            val response = runCatching { json.parseToJsonElement(raw) as? JsonObject }.getOrNull()
            if (status !in 200..299) throw ServiceException((response?.get("error") as? JsonPrimitive)?.contentOrNull ?: "解析服务返回错误 ($status)，请重试。")
            response ?: throw ServiceException("服务未返回有效结果，请检查解析服务地址。")
        } catch (e: CancellationException) { throw e
        } catch (e: ServiceException) { throw e
        } catch (e: java.net.SocketTimeoutException) { throw ServiceException("解析服务连接超时，请稍后重试。")
        } catch (e: IOException) { throw ServiceException("无法连接解析服务，请检查服务地址及电脑上的服务是否运行。")
        } finally { connection.disconnect() }
    }

    override suspend fun tile(z: Int, x: Int, y: Int): ByteArray = withContext(Dispatchers.IO) {
        require(z in 0..18 && x in 0 until (1 shl z) && y in 0 until (1 shl z))
        val key = "$z/$x/$y"
        synchronized(cache) { cache[key]?.takeIf { it.until > System.currentTimeMillis() }?.let { return@withContext it.bytes } }
        slots.withPermit {
            synchronized(cache) { cache[key]?.takeIf { it.until > System.currentTimeMillis() }?.let { return@withContext it.bytes } }
            val urls = listOf(1, 2).map { "https://webrd0$it.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=1&style=7&x=$x&y=$y&z=$z" } +
                "${endpoint().trimEnd('/')}/tiles/road/$z/$x/$y.png"
            for (url in urls) {
                ensureActive()
                val conn = URI(url).toURL().openConnection() as HttpURLConnection
                try {
                    conn.connectTimeout = 5000; conn.readTimeout = 9000
                    conn.setRequestProperty("User-Agent", "MyLittleAtlas/0.1 (native app)")
                    if (conn.responseCode != 200 || !conn.contentType.orEmpty().startsWith("image/")) continue
                    val bytes = conn.inputStream.use { readLimited(it, 1_000_000) }
                    val control = conn.getHeaderField("Cache-Control").orEmpty()
                    if (!control.contains("no-store") && !control.contains("no-cache")) {
                        val ttl = Regex("max-age=(\\d+)").find(control)?.groupValues?.get(1)?.toLongOrNull()?.coerceAtMost(86400) ?: 3600
                        synchronized(cache) {
                            cache[key] = CachedTile(bytes, System.currentTimeMillis() + ttl * 1000)
                            while (cache.size > 192) cache.remove(cache.keys.first())
                        }
                    }
                    return@withContext bytes
                } catch (e: CancellationException) { throw e
                } catch (_: IOException) { /* Try the reference project's next tile host / proxy. */
                } finally { conn.disconnect() }
            }
            throw ServiceException("地图暂时无法连接，点击重试。")
        }
    }

    private fun readLimited(input: java.io.InputStream, limit: Int): ByteArray {
        val result = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            if (result.size() + count > limit) throw IOException("Response too large")
            result.write(buffer, 0, count)
        }
        return result.toByteArray()
    }
}

class ServiceException(message: String) : Exception(message)
