package example.imageviewer.filter

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.*
import example.imageviewer.utils.applyBlurFilter
import example.imageviewer.utils.applyGrayScaleFilter
import example.imageviewer.utils.applyPixelFilter

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
