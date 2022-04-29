package org.jetbrains.compose.animatedimage

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

internal class NetworkAnimatedImageLoader(private val imageUrl: String) : AnimatedImageLoader() {
    var cachedBytes: ByteArray? = null

    override suspend fun generateByteArray(): ByteArray = withContext(Dispatchers.IO) {
        var bytesArray: ByteArray? = cachedBytes

        if (bytesArray == null) {
            bytesArray = URL(imageUrl).readBytes()

            cachedBytes = bytesArray
        }

        return@withContext bytesArray
    }
}