/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.ui.graphics

import androidx.compose.ui.graphics.colorspace.ColorSpace
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import org.jetbrains.skija.Image

/**
 * Create an [ImageAsset] from the given [Image]. Note this does
 * not create a copy of the original [Image] and changes to it
 * will modify the returned [ImageAsset]
 */
fun Image.asImageAsset(): ImageAsset = DesktopImageAsset(this)

@Suppress("UNUSED_PARAMETER")
internal actual fun ActualImageAsset(
    width: Int,
    height: Int,
    config: ImageAssetConfig,
    hasAlpha: Boolean,
    colorSpace: ColorSpace
): ImageAsset {
    TODO()
}

/**
 * Create an [ImageAsset] from an image file stored in resources for the application
 *
 * @param path path to the image file
 *
 * @return Loaded image file represented as an [ImageAsset]
 */
fun imageFromResource(path: String): ImageAsset =
    Image.makeFromEncoded(loadResource(path)).asImageAsset()

private fun loadResource(path: String): ByteArray {
    val resource = Thread.currentThread().contextClassLoader.getResource(path)
    requireNotNull(resource) { "Resource $path not found" }
    return resource.readBytes()
}

/**
 * @Throws UnsupportedOperationException if this [ImageAsset] is not backed by an
 * org.jetbrains.skija.Image
 */
fun ImageAsset.asDesktopImage(): Image =
    when (this) {
        is DesktopImageAsset -> image
        else -> throw UnsupportedOperationException("Unable to obtain org.jetbrains.skija.Image")
    }

private class DesktopImageAsset(val image: Image) : ImageAsset {
    override val colorSpace = ColorSpaces.Srgb
    override val config = ImageAssetConfig.Argb8888
    override val hasAlpha = true
    override val height get() = image.height
    override val width get() = image.width
    override fun prepareToDraw() = Unit

    override fun readPixels(
        buffer: IntArray,
        startX: Int,
        startY: Int,
        width: Int,
        height: Int,
        bufferOffset: Int,
        stride: Int
    ) {
        TODO("ImageAsset.readPixels not implemented yet")
    }
}