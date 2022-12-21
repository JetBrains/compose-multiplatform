package example.imageviewer.utils

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale

fun adjustImageScale(bitmap: ImageBitmap): ContentScale {
    val bitmapRatio = (10 * bitmap.width.toFloat() / bitmap.height).toInt()
    val displayRatio = (10 * displayWidth().toFloat() / displayHeight()).toInt()

    if (displayRatio > bitmapRatio) {
        return ContentScale.FillHeight
    }
    return ContentScale.FillWidth
}

 fun displayWidth(): Int = TODO()
 fun displayHeight(): Int = TODO()
