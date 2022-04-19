package org.jetbrains.compose.animatedimage

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileInputStream

class LocalLoader(private val imageUrl: String) : AnimatedImageLoader {
    var cachedBytes: ByteArray? = null

    override suspend fun loadBytes(): ByteArray = withContext(Dispatchers.IO) {
        var bytesArray: ByteArray? = cachedBytes

        if (bytesArray == null) {
            bytesArray = FileInputStream(imageUrl).readAllBytes() ?: byteArrayOf()

            cachedBytes = bytesArray
        }

        return@withContext bytesArray
    }
}