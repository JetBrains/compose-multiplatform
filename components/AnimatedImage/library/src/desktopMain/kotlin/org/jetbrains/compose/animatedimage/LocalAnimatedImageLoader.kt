package org.jetbrains.compose.animatedimage

import java.io.FileInputStream

internal class LocalAnimatedImageLoader(private val imageUrl: String) : AnimatedImageLoader() {
    var cachedBytes: ByteArray? = null

    override suspend fun generateByteArray(): ByteArray {
        var bytesArray: ByteArray? = cachedBytes

        if (bytesArray == null) {
            bytesArray = FileInputStream(imageUrl).use { fileInputStream ->
                fileInputStream.readBytes()
            }

            cachedBytes = bytesArray
        }

        return bytesArray
    }
}