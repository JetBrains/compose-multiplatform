package org.jetbrains.compose.animatedimage

import androidx.compose.ui.res.useResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ResourceLoader(private val resourcePath: String) : AnimatedImageLoader {
    override suspend fun loadBytes(): ByteArray = withContext(Dispatchers.IO) {
        return@withContext useResource(resourcePath) { it.readAllBytes() }
    }
}