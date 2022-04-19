package org.jetbrains.compose.animatedimage

import androidx.compose.ui.res.useResource

class ResourceLoader(private val resourcePath: String) : AnimatedImageLoader {
    override suspend fun loadBytes(): ByteArray {
        return useResource(resourcePath) { it.readAllBytes() }
    }
}