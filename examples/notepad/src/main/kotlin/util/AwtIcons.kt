package util

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.Point
import java.awt.image.BufferedImage
import java.awt.image.ColorModel
import java.awt.image.DataBuffer
import java.awt.image.DataBufferInt
import java.awt.image.Raster
import java.awt.image.SinglePixelPackedSampleModel
import kotlin.math.roundToInt

suspend fun ImageVector.toAwtImage(tintColor: Color): BufferedImage {
    return withContext(Dispatchers.Default) {
        compose {
            val density = Density(1f)
            val layoutDirection = LayoutDirection.Ltr

            lateinit var result: BufferedImage

            CompositionLocalProvider(
                LocalDensity provides density,
                LocalLayoutDirection provides layoutDirection,
            ) {
                result = rememberVectorPainter(this@toAwtImage)
                    .toAwtImage(density, layoutDirection, ColorFilter.tint(tintColor))
            }

            result
        }
    }
}

private fun Painter.toAwtImage(
    density: Density,
    layoutDirection: LayoutDirection,
    colorFilter: ColorFilter
): BufferedImage {
    require(intrinsicSize.isSpecified) {
        "Icon should support intrinsicSize"
    }

    val width = intrinsicSize.width.roundToInt()
    val height = intrinsicSize.height.roundToInt()
    val bitmap = ImageBitmap(width, height)
    val canvas = Canvas(bitmap)

    CanvasDrawScope().draw(
        density, layoutDirection, canvas, intrinsicSize
    ) {
        draw(intrinsicSize, colorFilter = colorFilter)
    }

    val pixels = IntArray(width * height)
    bitmap.readPixels(pixels)

    val bitMasks = intArrayOf(0xFF0000, 0xFF00, 0xFF, -0x1000000)
    val sm = SinglePixelPackedSampleModel(DataBuffer.TYPE_INT, width, height, bitMasks)
    val db = DataBufferInt(pixels, pixels.size)
    val wr = Raster.createWritableRaster(sm, db, Point())
    return BufferedImage(ColorModel.getRGBdefault(), wr, false, null)
}
