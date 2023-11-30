/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.animatedimage

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Codec
import org.jetbrains.skia.Data

internal abstract class AnimatedImageLoader {
    suspend fun loadAnimatedImage(): AnimatedImage = withContext(Dispatchers.IO) {
        val byteArray = generateByteArray()

        val data = Data.makeFromBytes(byteArray)
        val codec = Codec.makeFromData(data)

        return@withContext AnimatedImage(codec)
    }

    abstract suspend fun generateByteArray(): ByteArray
}