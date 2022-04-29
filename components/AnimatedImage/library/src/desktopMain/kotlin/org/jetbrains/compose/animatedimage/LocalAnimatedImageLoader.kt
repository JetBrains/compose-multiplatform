package org.jetbrains.compose.animatedimage

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileInputStream

internal class LocalAnimatedImageLoader(private val imageUrl: String) : AnimatedImageLoader() {
    var cachedBytes: ByteArray? = null

    override suspend fun generateByteArray(): ByteArray = withContext(Dispatchers.IO) {
        var bytesArray: ByteArray? = cachedBytes

        if (bytesArray == null) {
            bytesArray = FileInputStream(imageUrl).use { fileInputStream ->
                fileInputStream.readBytes()
            }

            cachedBytes = bytesArray
        }

        return@withContext bytesArray
    }
}