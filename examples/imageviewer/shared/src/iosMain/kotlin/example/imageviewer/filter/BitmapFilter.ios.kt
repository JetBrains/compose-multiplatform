package example.imageviewer.filter

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorInfo
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.FilterTileMode
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageFilter
import org.jetbrains.skia.ImageInfo
import org.jetbrains.skia.Paint

actual fun grayScaleFilter(bitmap: ImageBitmap, context: PlatformContext): ImageBitmap {
    return applyGrayScaleFilter(bitmap.asSkiaBitmap()).asComposeImageBitmap()
}

actual fun pixelFilter(bitmap: ImageBitmap, context: PlatformContext): ImageBitmap {
    return applyPixelFilter(bitmap.asSkiaBitmap()).asComposeImageBitmap()
}

actual fun blurFilter(bitmap: ImageBitmap, context: PlatformContext): ImageBitmap {
    return applyBlurFilter(bitmap.asSkiaBitmap()).asComposeImageBitmap()
}

actual class PlatformContext

@Composable
actual fun getPlatformContext(): PlatformContext = PlatformContext()

private fun scaleBitmapAspectRatio(
    bitmap: Bitmap,
    width: Int,
    height: Int
): Bitmap {
    val boundWidth = width.toFloat()
    val boundHeight = height.toFloat()

    val ratioX = boundWidth / bitmap.width
    val ratioY = boundHeight / bitmap.height
    val ratio = if (ratioX < ratioY) ratioX else ratioY

    val resultWidth = (bitmap.width * ratio).toInt()
    val resultHeight = (bitmap.height * ratio).toInt()

    val result = Bitmap().apply {
        allocN32Pixels(resultWidth, resultHeight)
    }
    val canvas = Canvas(result)
    canvas.drawImageRect(Image.makeFromBitmap(bitmap), result.bounds.toRect())
    canvas.readPixels(result, 0, 0)
    canvas.close()

    return result
}

private fun applyGrayScaleFilter(bitmap: Bitmap): Bitmap {
    val imageInfo = ImageInfo(
        width = bitmap.width,
        height = bitmap.height,
        colorInfo = ColorInfo(ColorType.GRAY_8, ColorAlphaType.PREMUL, null)
    )
    val result = Bitmap().apply {
        allocPixels(imageInfo)
    }

    val canvas = Canvas(result)
    canvas.drawImageRect(Image.makeFromBitmap(bitmap), bitmap.bounds.toRect())
    canvas.readPixels(result, 0, 0)
    canvas.close()

    return result
}

private fun applyPixelFilter(bitmap: Bitmap): Bitmap {
    val width = bitmap.width
    val height = bitmap.height

    var result = scaleBitmapAspectRatio(bitmap, width / 12, height / 12)
    result = scaleBitmapAspectRatio(result, width, height)

    return result
}

private fun applyBlurFilter(bitmap: Bitmap): Bitmap {
    val result = Bitmap().apply {
        allocN32Pixels(bitmap.width, bitmap.height)
    }
    val blur = Paint().apply {
        imageFilter = ImageFilter.makeBlur(12f, 12f, FilterTileMode.CLAMP)
    }

    val canvas = Canvas(result)
    canvas.saveLayer(null, blur)
    canvas.drawImageRect(Image.makeFromBitmap(bitmap), bitmap.bounds.toRect())
    canvas.restore()
    canvas.readPixels(result, 0, 0)
    canvas.close()

    return result
}
