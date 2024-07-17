package org.jetbrains.compose.resources

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.unit.Density
import org.jetbrains.skia.Data
import org.jetbrains.skia.Image
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Rect
import org.jetbrains.skia.SamplingMode
import org.jetbrains.skia.Surface
import org.jetbrains.skia.svg.SVGDOM

internal actual fun ByteArray.toImageBitmap(resourceDensity: Int, targetDensity: Int): ImageBitmap {
    val image = Image.makeFromEncoded(this)

    val targetImage: Image
    //https://youtrack.jetbrains.com/issue/CMP-5657
    //android only downscales drawables. If there is only low dpi resource then use it as is (not upscale)
    //we need a consistent behavior on all platforms
    if (resourceDensity > targetDensity) {
        val scale = targetDensity.toFloat() / resourceDensity.toFloat()
        val targetH = image.height * scale
        val targetW = image.width * scale
        val srcRect = Rect.Companion.makeWH(image.width.toFloat(), image.height.toFloat())
        val dstRect = Rect.Companion.makeWH(targetW, targetH)

        targetImage = Surface.makeRasterN32Premul(targetW.toInt(), targetH.toInt()).run {
            val paint = Paint().apply { isAntiAlias = true }
            canvas.drawImageRect(image, srcRect, dstRect, SamplingMode.LINEAR, paint, true)
            makeImageSnapshot()
        }
    } else {
        targetImage = image
    }

    return targetImage.toComposeImageBitmap()
}

internal actual class SvgElement(val svgdom: SVGDOM)

internal actual fun ByteArray.toSvgElement(): SvgElement =
    SvgElement(SVGDOM(Data.makeFromBytes(this)))

internal actual fun SvgElement.toSvgPainter(density: Density): Painter =
    SvgPainter(svgdom, density)