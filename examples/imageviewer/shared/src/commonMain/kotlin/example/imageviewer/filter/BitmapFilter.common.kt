package example.imageviewer.filter

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap

fun getFilter(type: FilterType): (ImageBitmap, PlatformContext) -> ImageBitmap =
    when (type) {
        FilterType.GrayScale -> ::grayScaleFilter
        FilterType.Pixel -> ::pixelFilter
        FilterType.Blur -> ::blurFilter
    }

expect fun grayScaleFilter(bitmap: ImageBitmap, context: PlatformContext): ImageBitmap
expect fun pixelFilter(bitmap: ImageBitmap, context: PlatformContext): ImageBitmap
expect fun blurFilter(bitmap: ImageBitmap, context: PlatformContext): ImageBitmap

expect class PlatformContext

@Composable
expect fun getPlatformContext(): PlatformContext
