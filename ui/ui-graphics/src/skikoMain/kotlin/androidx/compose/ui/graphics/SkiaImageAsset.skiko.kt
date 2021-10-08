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
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorInfo
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo
import kotlin.math.abs

/**
 * Create an [ImageBitmap] from the given [Bitmap]. Note this does
 * not create a copy of the original [Bitmap] and changes to it
 * will modify the returned [ImageBitmap]
 */
fun Bitmap.asComposeImageBitmap(): ImageBitmap = SkiaBackedImageBitmap(this)

/**
 * Create an [ImageBitmap] from the given [Image].
 */
fun Image.toComposeImageBitmap(): ImageBitmap = SkiaBackedImageBitmap(toBitmap())

private fun Image.toBitmap(): Bitmap {
    val bitmap = Bitmap()
    bitmap.allocPixels(ImageInfo.makeN32(width, height, ColorAlphaType.PREMUL))
    val canvas = org.jetbrains.skia.Canvas(bitmap)
    canvas.drawImage(this, 0f, 0f)
    bitmap.setImmutable()
    return bitmap
}

internal actual fun ActualImageBitmap(
    width: Int,
    height: Int,
    config: ImageBitmapConfig,
    hasAlpha: Boolean,
    colorSpace: ColorSpace
): ImageBitmap {
    val colorType = config.toSkiaColorType()
    val alphaType = if (hasAlpha) ColorAlphaType.PREMUL else ColorAlphaType.OPAQUE
    val skiaColorSpace = colorSpace.toSkiaColorSpace()
    val colorInfo = ColorInfo(colorType, alphaType, skiaColorSpace)
    val imageInfo = ImageInfo(colorInfo, width, height)
    val bitmap = Bitmap()
    bitmap.allocPixels(imageInfo)
    return SkiaBackedImageBitmap(bitmap)
}

/**
 * Obtain a reference to the [org.jetbrains.skia.Bitmap]
 *
 * @Throws UnsupportedOperationException if this [ImageBitmap] is not backed by an
 * org.jetbrains.skia.Image
 */
fun ImageBitmap.asSkiaBitmap(): Bitmap =
    when (this) {
        is SkiaBackedImageBitmap -> bitmap
        else -> throw UnsupportedOperationException("Unable to obtain org.jetbrains.skia.Image")
    }

private class SkiaBackedImageBitmap(val bitmap: Bitmap) : ImageBitmap {
    override val colorSpace = bitmap.colorSpace.toComposeColorSpace()
    override val config = bitmap.colorType.toComposeConfig()
    override val hasAlpha = !bitmap.isOpaque
    override val height get() = bitmap.height
    override val width get() = bitmap.width
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
        // similar to https://cs.android.com/android/platform/superproject/+/42c50042d1f05d92ecc57baebe3326a57aeecf77:frameworks/base/graphics/java/android/graphics/Bitmap.java;l=2007
        val lastScanline: Int = bufferOffset + (height - 1) * stride
        require(startX >= 0 && startY >= 0)
        require(width > 0 && startX + width <= this.width)
        require(height > 0 && startY + height <= this.height)
        require(abs(stride) >= width)
        require(bufferOffset >= 0 && bufferOffset + width <= buffer.size)
        require(lastScanline >= 0 && lastScanline + width <= buffer.size)

        // similar to https://cs.android.com/android/platform/superproject/+/9054ca2b342b2ea902839f629e820546d8a2458b:frameworks/base/libs/hwui/jni/Bitmap.cpp;l=898;bpv=1
        val colorInfo = ColorInfo(
            ColorType.BGRA_8888,
            ColorAlphaType.UNPREMUL,
            org.jetbrains.skia.ColorSpace.sRGB
        )
        val imageInfo = ImageInfo(colorInfo, width, height)
        val bytesPerPixel = 4
        val bytes = bitmap.readPixels(imageInfo, stride * bytesPerPixel.toLong(), startX, startY)!!
        bytes.putBytesInto(buffer, bufferOffset, bytes.size / bytesPerPixel)
    }
}

internal expect fun ByteArray.putBytesInto(array: IntArray, offset: Int, length: Int)

// TODO(demin): [API] maybe we should use:
//  `else -> throw UnsupportedOperationException()`
//  in toSkiaColorType/toComposeConfig/toComposeColorSpace/toSkiaColorSpace
//  see [https://android-review.googlesource.com/c/platform/frameworks/support/+/1429835/comment/c219501b_63c3d1fe/]

private fun ImageBitmapConfig.toSkiaColorType() = when (this) {
    ImageBitmapConfig.Argb8888 -> ColorType.N32
    ImageBitmapConfig.Alpha8 -> ColorType.ALPHA_8
    ImageBitmapConfig.Rgb565 -> ColorType.RGB_565
    ImageBitmapConfig.F16 -> ColorType.RGBA_F16
    else -> ColorType.N32
}

private fun ColorType.toComposeConfig() = when (this) {
    ColorType.N32 -> ImageBitmapConfig.Argb8888
    ColorType.ALPHA_8 -> ImageBitmapConfig.Alpha8
    ColorType.RGB_565 -> ImageBitmapConfig.Rgb565
    ColorType.RGBA_F16 -> ImageBitmapConfig.F16
    else -> ImageBitmapConfig.Argb8888
}

private fun org.jetbrains.skia.ColorSpace?.toComposeColorSpace(): ColorSpace {
    return when (this) {
        org.jetbrains.skia.ColorSpace.sRGB -> ColorSpaces.Srgb
        org.jetbrains.skia.ColorSpace.sRGBLinear -> ColorSpaces.LinearSrgb
        org.jetbrains.skia.ColorSpace.displayP3 -> ColorSpaces.DisplayP3
        else -> ColorSpaces.Srgb
    }
}

// TODO(demin): support all color spaces.
//  to do this we need to implement SkColorSpace::MakeRGB in skia
private fun ColorSpace.toSkiaColorSpace(): org.jetbrains.skia.ColorSpace {
    return when (this) {
        ColorSpaces.Srgb -> org.jetbrains.skia.ColorSpace.sRGB
        ColorSpaces.LinearSrgb -> org.jetbrains.skia.ColorSpace.sRGBLinear
        ColorSpaces.DisplayP3 -> org.jetbrains.skia.ColorSpace.displayP3
        else -> org.jetbrains.skia.ColorSpace.sRGB
    }
}