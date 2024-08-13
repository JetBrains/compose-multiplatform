package example.imageviewer.filter

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.graphics.toComposeImageBitmap
import java.awt.image.BufferedImage
import java.awt.image.ConvolveOp
import java.awt.image.Kernel


actual fun grayScaleFilter(bitmap: ImageBitmap, context: PlatformContext): ImageBitmap {
    return applyGrayScaleFilter(bitmap.toAwtImage()).toComposeImageBitmap()
}

actual fun pixelFilter(bitmap: ImageBitmap, context: PlatformContext): ImageBitmap {
    return applyPixelFilter(bitmap.toAwtImage()).toComposeImageBitmap()
}

actual fun blurFilter(bitmap: ImageBitmap, context: PlatformContext): ImageBitmap {
    return applyBlurFilter(bitmap.toAwtImage()).toComposeImageBitmap()
}

actual class PlatformContext

@Composable
actual fun getPlatformContext(): PlatformContext = PlatformContext()

fun scaleBitmapAspectRatio(
    bitmap: BufferedImage,
    width: Int,
    height: Int
): BufferedImage {
    val boundW: Float = width.toFloat()
    val boundH: Float = height.toFloat()

    val ratioX: Float = boundW / bitmap.width
    val ratioY: Float = boundH / bitmap.height
    val ratio: Float = if (ratioX < ratioY) ratioX else ratioY

    val resultH = (bitmap.height * ratio).toInt()
    val resultW = (bitmap.width * ratio).toInt()

    val result = BufferedImage(resultW, resultH, BufferedImage.TYPE_INT_ARGB)
    val graphics = result.createGraphics()
    graphics.drawImage(bitmap, 0, 0, resultW, resultH, null)
    graphics.dispose()

    return result
}

private fun applyGrayScaleFilter(bitmap: BufferedImage): BufferedImage {
    val result = BufferedImage(
        bitmap.width,
        bitmap.height,
        BufferedImage.TYPE_BYTE_GRAY
    )

    val graphics = result.graphics
    graphics.drawImage(bitmap, 0, 0, null)
    graphics.dispose()

    return result
}

private fun applyPixelFilter(bitmap: BufferedImage): BufferedImage {
    val w: Int = bitmap.width
    val h: Int = bitmap.height
    var result = scaleBitmapAspectRatio(bitmap, w / 12, h / 12)
    result = scaleBitmapAspectRatio(result, w, h)
    return result
}

private fun applyBlurFilter(bitmap: BufferedImage): BufferedImage {
    var result = BufferedImage(bitmap.width, bitmap.height, bitmap.type)
    val graphics = result.graphics

    graphics.drawImage(bitmap, 0, 0, null)
    graphics.dispose()

    val radius = 9
    val size = 9
    val weight: Float = 1.0f / (size * size)
    val matrix = FloatArray(size * size)

    for (i in matrix.indices) {
        matrix[i] = weight
    }

    val kernel = Kernel(radius, size, matrix)
    val op = ConvolveOp(kernel, ConvolveOp.EDGE_NO_OP, null)
    result = op.filter(result, null)

    return result.getSubimage(
        radius,
        radius,
        result.width - radius * 2,
        result.height - radius * 2
    )
}
