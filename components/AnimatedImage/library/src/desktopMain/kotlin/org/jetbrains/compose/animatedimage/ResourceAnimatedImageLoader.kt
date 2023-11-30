package org.jetbrains.compose.animatedimage

import androidx.compose.ui.res.useResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class ResourceAnimatedImageLoader(private val resourcePath: String) : AnimatedImageLoader() {
    override suspend fun generateByteArray(): ByteArray = withContext(Dispatchers.IO) {
        return@withContext useResource(resourcePath) { it.readAllBytes() }
    }
}