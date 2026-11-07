package com.mylittleatlas.app.platform

/**
 * Implemented in Swift so system permission/picker lifetimes remain native.
 * All completions are delivered on the main queue, exactly once.
 */
interface IosPlatformBridge {
    var serviceUrl: String
    fun readData(): String?
    /** Return an error message on failure; the previous atomic file must remain intact. */
    fun writeData(content: String): String?
    fun today(): String
    fun uniqueId(): String
    fun nowMillis(): Long
    fun readImage(path: String): ByteArray?
    fun pickPhoto(onResult: (String?) -> Unit)
    fun exportBackup(json: String)
    fun importBackup(onResult: (String?) -> Unit)
    fun shareText(text: String)
    fun sharePng(bytes: ByteArray)
    fun savePng(bytes: ByteArray, onResult: (Boolean) -> Unit)
    fun locate(onResult: (String?, String?, String?) -> Unit)
    fun navigate(name: String, address: String, city: String, latitude: String?, longitude: String?)
    fun openSource(url: String)
    fun request(url: String, jsonBody: String?, tile: Boolean, onResult: (ByteArray?, String?) -> Unit): String
    fun cancelRequest(id: String)
    fun observeIncomingText(onText: (String) -> Unit)
}
