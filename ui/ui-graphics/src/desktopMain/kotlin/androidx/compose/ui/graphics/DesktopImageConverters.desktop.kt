/*
 * Copyright 2021 The Android Open Source Project
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

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ImageInfo
import java.awt.Graphics
import java.awt.Image
import java.awt.Point
import java.awt.image.AbstractMultiResolutionImage
import java.awt.image.BufferedImage
import java.awt.image.ColorModel
import java.awt.image.DataBuffer
import java.awt.image.DataBufferInt
import java.awt.image.ImageObserver
import java.awt.image.ImageProducer
import java.awt.image.MultiResolutionImage
import java.awt.image.Raster
import java.awt.image.SinglePixelPackedSampleModel
import kotlin.math.roundToInt

/**
 * Convert AWT [BufferedImage] to Compose [Painter], so it would be possible to pass it to Compose
 * functions. Don't mutate [BufferedImage] after converting it to [Painter], it will lead to
 * undefined behavior.
 */
@Deprecated("Use toPainter", replaceWith = ReplaceWith("toPainter()"))
fun BufferedImage.asPainter(): Painter = BufferedImagePainter(this)

/**
 * Convert AWT [BufferedImage] to Compose [Painter], so it would be possible to pass it to Compose
 * functions. Don't mutate [BufferedImage] after converting it to [Painter], because
 * [BufferedImage] can be reused to avoid unnecessary conversions.
 */
fun BufferedImage.toPainter(): Painter = BufferedImagePainter(this)

private class BufferedImagePainter(val image: BufferedImage) : Painter() {
    private val bitmap by lazy { image.toComposeImageBitmap() }

    override val intrinsicSize = Size(image.width.toFloat(), image.height.toFloat())

    override fun DrawScope.onDraw() {
        val intSize = IntSize(size.width.toInt(), size.height.toInt())
        drawImage(bitmap, dstSize = intSize)
    }
}

/**
 * Convert Compose [Painter] to AWT [Image]. The result will not be rasterized right now, it
 * will be rasterized when AWT will request the image with needed width and height, by calling
 * [AbstractMultiResolutionImage.getResolutionVariant] on Windows/Linux, or
 * [AbstractMultiResolutionImage.getResolutionVariants] on macOs.
 *
 * At the rasterization moment, [density] and [layoutDirection] will be passed to the painter.
 * Usually most painters don't use them. Like the painters for svg/xml/raster resources:
 * they don't use absolute '.dp' values to draw, they use values which are relative
 * to their viewport.
 *
 * [density] also will be used to rasterize the default image, which can be used by some implementations
 * (Tray icon on macOs, disabled icon for menu items)
 *
 * @param size the size of the [Image]
 */
@Deprecated(
    "Use toAwtImage",
    replaceWith = ReplaceWith("toAwtImage(density, layoutDirection, size)")
)
fun Painter.asAwtImage(
    density: Density,
    layoutDirection: LayoutDirection,
    size: Size = intrinsicSize
): Image = toAwtImage(density, layoutDirection, size)

/**
 * Convert Compose [Painter] to AWT [Image]. The result will not be rasterized right now, it
 * will be rasterized when AWT will request the image with needed width and height, by calling
 * [AbstractMultiResolutionImage.getResolutionVariant] on Windows/Linux, or
 * [AbstractMultiResolutionImage.getResolutionVariants] on macOs.
 *
 * At the rasterization moment, [density] and [layoutDirection] will be passed to the painter.
 * Usually most painters don't use them. Like the painters for svg/xml/raster resources:
 * they don't use absolute '.dp' values to draw, they use values which are relative
 * to their viewport.
 *
 * @param size the size of the [Image]
 */
fun Painter.toAwtImage(
    density: Density,
    layoutDirection: LayoutDirection,
    size: Size = intrinsicSize
): Image {
    require(size.isSpecified) {
        "Cannot convert Painter with unspecified size. Please set size explicitly."
    }
    return PainterImage(
        painter = this,
        density = density,
        layoutDirection = layoutDirection,
        size = size
    )
}

private class PainterImage(
    private val painter: Painter,
    private val density: Density,
    private val layoutDirection: LayoutDirection,
    size: Size,
) : Image(), MultiResolutionImage {
    private val width = size.width.toInt()
    private val height = size.height.toInt()
    override fun getWidth(observer: ImageObserver?) = width
    override fun getHeight(observer: ImageObserver?) = height

    override fun getResolutionVariant(
        destImageWidth: Double,
        destImageHeight: Double
    ): Image {
        val width = destImageWidth.toInt()
        val height = destImageHeight.toInt()
        return if (
            painter is BufferedImagePainter &&
            painter.image.width == width &&
            painter.image.height == height
        ) {
            painter.image
        } else {
            asBitmap(width, height).toAwtImage()
        }
    }

    private fun asBitmap(width: Int, height: Int): ImageBitmap {
        val bitmap = ImageBitmap(width, height)
        val canvas = Canvas(bitmap)
        val floatSize = Size(width.toFloat(), height.toFloat())

        CanvasDrawScope().draw(
            density, layoutDirection, canvas, floatSize
        ) {
            with(painter) {
                draw(floatSize)
            }
        }

        return bitmap
    }

    override fun getProperty(name: String, observer: ImageObserver?): Any = UndefinedProperty
    override fun getSource(): ImageProducer = defaultImage.source
    override fun getGraphics(): Graphics = defaultImage.graphics

    private val defaultImage by lazy {
        // optimizations to avoid unnecessary rasterizations
        when (painter) {
            is BufferedImagePainter -> painter.image
            is BitmapPainter -> asBitmap(width, height).toAwtImage()
            else -> asBitmap(
                (width * density.density).roundToInt(),
                (height * density.density).roundToInt()
            ).toAwtImage()
        }
    }

    override fun getResolutionVariants() = listOf(defaultImage)
}

// TODO(demin): should we optimize toAwtImage/toBitmap? Currently we convert colors according to the
//  current colorModel. But we can get raw BufferedImage.getRaster() and set a different colorModel.

/**
 * Convert Compose [ImageBitmap] to AWT [BufferedImage]
 */

@Deprecated("use toAwtImage", replaceWith = ReplaceWith("toAwtImage"))
fun ImageBitmap.asAwtImage(): BufferedImage = toAwtImage()

/**
 * Convert Compose [ImageBitmap] to AWT [BufferedImage]
 */
fun ImageBitmap.toAwtImage(): BufferedImage {
    // TODO(demin): use asDesktopBitmap().toBufferedImage() from skiko, when we fix it. Currently
    //  some images convert with graphical artifacts

    val pixels = IntArray(width * height)
    readPixels(pixels)

    val a = 0xff shl 24
    val r = 0xff shl 16
    val g = 0xff shl 8
    val b = 0xff shl 0
    val bitMasks = intArrayOf(r, g, b, a)
    val sm = SinglePixelPackedSampleModel(DataBuffer.TYPE_INT, width, height, bitMasks)
    val db = DataBufferInt(pixels, pixels.size)
    val wr = Raster.createWritableRaster(sm, db, Point())
    return BufferedImage(ColorModel.getRGBdefault(), wr, false, null)
}

/**
 * Convert AWT [BufferedImage] to Compose [ImageBitmap]
 */
@Deprecated("use toComposeImageBitmap()", replaceWith = ReplaceWith("toComposeImageBitmap()"))
fun BufferedImage.toComposeBitmap(): ImageBitmap = toComposeImageBitmap()

/**
 * Convert AWT [BufferedImage] to Compose [ImageBitmap]
 */
fun BufferedImage.toComposeImageBitmap(): ImageBitmap {
    // TODO(demin): use toBitmap().asImageBitmap() from skiko, when we fix its performance
    //  (it is 40x slower)

    val bytesPerPixel = 4
    val pixels = ByteArray(width * height * bytesPerPixel)

    var k = 0
    for (y in 0 until height) {
        for (x in 0 until width) {
            val argb = getRGB(x, y)
            val a = (argb shr 24) and 0xff
            val r = (argb shr 16) and 0xff
            val g = (argb shr 8) and 0xff
            val b = (argb shr 0) and 0xff
            pixels[k++] = b.toByte()
            pixels[k++] = g.toByte()
            pixels[k++] = r.toByte()
            pixels[k++] = a.toByte()
        }
    }

    val bitmap = Bitmap()
    bitmap.allocPixels(ImageInfo.makeS32(width, height, ColorAlphaType.UNPREMUL))
    bitmap.installPixels(pixels)
    return bitmap.asComposeImageBitmap()
}