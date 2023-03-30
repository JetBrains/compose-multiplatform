/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.resources

import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import org.jetbrains.skia.*
import kotlin.math.abs
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime
import kotlin.time.measureTimedValue

@Suppress(
    "INVISIBLE_MEMBER",
    "INVISIBLE_REFERENCE",
    "EXPOSED_PARAMETER_TYPE"
)
fun Image.myToComposeImageBitmap(): ImageBitmap = SkiaBackedImageBitmap(toBitmap())

private fun Image.toBitmap(): Bitmap {
    val bitmap = Bitmap()
    bitmap.allocPixels(ImageInfo.makeN32(width, height, ColorAlphaType.PREMUL))
    val canvas = org.jetbrains.skia.Canvas(bitmap)
    canvas.drawImage(this, 0f, 0f)
    bitmap.setImmutable()
    return bitmap
}

@OptIn(ExperimentalTime::class)
@Suppress(
    "INVISIBLE_MEMBER",
    "INVISIBLE_REFERENCE",
    "EXPOSED_PARAMETER_TYPE"
)
internal actual fun ByteArray.toImageBitmap(): ImageBitmap {
//    println("ASDASDASD34324234")

    val codec = Codec.makeFromData(Data.makeFromBytes(this))
    val btmp = codec.readPixels()
    return SkiaBackedImageBitmap(btmp)

    val img2 = Image.makeFromEncoded(this)
////    val data = Data.makeFromBytes(this)
////    val pixmap = Pixmap.make(img2.imageInfo, data, img2.bytesPerPixel)
//

    val c = img2.height * img2.width
    val pixels = makeByteArrayFromRGBArray(IntArray(c) { 0xffb63b49.toInt() })

    val b = Bitmap()
    b.allocPixels(img2.imageInfo)
    b.setImageInfo(img2.imageInfo)
    b.installPixels(pixels)
    return SkiaBackedImageBitmap(b)

    return Image.makeFromEncoded(this).myToComposeImageBitmap()
    val img = measureTimedValue {
        Image.makeFromEncoded(this)
    }.let {
        println("makeFromEncoded t = ${it.duration.inWholeMilliseconds}")
        it.value
    }
    val bitmap = Bitmap()
    measureTime {
        bitmap.allocPixels(img.imageInfo)
        img.readPixels(bitmap)
    }.also {
        println("alloc +  read Pixels t = ${it.inWholeMilliseconds}")
    }
    return measureTimedValue {
//        SkiaBackedImageBitmap(Bitmap.makeFromImage(img))
        SkiaBackedImageBitmap(bitmap)
    }.let {
        println("SkiaBackedImageBitmap t = ${it.duration.inWholeMilliseconds}")
        it.value
    }
}


fun makeByteArrayFromRGBArray(pixArray: IntArray): ByteArray {
    var result = ByteArray(pixArray.size * 4)
    var off = 0
    for (pix in pixArray) {
        result[off++] = org.jetbrains.skia.Color.getR(pix).toByte()
        result[off++] = org.jetbrains.skia.Color.getG(pix).toByte()
        result[off++] = org.jetbrains.skia.Color.getB(pix).toByte()
        result[off++] = org.jetbrains.skia.Color.getA(pix).toByte()
    }

    return result
}

fun imageFromIntArray(pixArray: IntArray, imageWidth: Int) = Image.makeRaster(
    imageInfo = ImageInfo(imageWidth, pixArray.size / imageWidth, ColorType.RGBA_8888, ColorAlphaType.UNPREMUL),
    rowBytes = imageWidth * 4 /* Four bytes per pixel */,
    bytes = makeByteArrayFromRGBArray(pixArray)
)

fun makeFromImage2(image: Image): Bitmap {
    val bitmap = Bitmap()
    bitmap.allocPixels(image.imageInfo)
    return if (image.readPixels(bitmap)) bitmap else {
        bitmap.close()
        throw RuntimeException("Failed to readPixels from $image")
    }
}

//    Image.makeFromEncoded(this).toComposeImageBitmap()
//    Image.makeFromEncoded(this).let {
//        SkiaBackedImageBitmap(Bitmap.makeFromImage(it))
//    }
