package org.jetbrains.compose.resources

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

internal expect val cacheDispatcher: CoroutineDispatcher

//images and fonts
private val loadedBytesCache = mutableMapOf<String, ByteArray>()

//@TestOnly
internal fun dropBytesCache() {
    loadedBytesCache.clear()
}

internal suspend fun loadBytes(path: String, resourceReader: ResourceReader): ByteArray {
    return withContext(cacheDispatcher) {
        loadedBytesCache.getOrPut(path) { resourceReader.read(path) }
    }
}