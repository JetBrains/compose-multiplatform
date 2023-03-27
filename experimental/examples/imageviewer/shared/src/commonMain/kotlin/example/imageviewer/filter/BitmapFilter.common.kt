package example.imageviewer.filter

import androidx.compose.ui.graphics.ImageBitmap
import example.imageviewer.core.FilterType

fun getFilter(type: FilterType): (ImageBitmap) -> ImageBitmap =
    when (type) {
        FilterType.GrayScale -> ::grayScaleFilter
        FilterType.Pixel -> ::pixelFilter
        FilterType.Blur -> ::blurFilter
    }

expect fun grayScaleFilter(bitmap: ImageBitmap): ImageBitmap
expect fun pixelFilter(bitmap: ImageBitmap): ImageBitmap
expect fun blurFilter(bitmap: ImageBitmap): ImageBitmap
