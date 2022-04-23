package org.jetbrains.compose.animatedimage

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Codec
import org.jetbrains.skia.Data

actual abstract class AnimatedImageLoader {
    actual suspend fun loadAnimatedImage(): AnimatedImage = withContext(Dispatchers.IO) {
        val byteArray = generateByteArray()

        val data = Data.makeFromBytes(byteArray)
        val codec = Codec.makeFromData(data)

        return@withContext AnimatedImage(codec)
    }

    actual abstract suspend fun generateByteArray(): ByteArray
}