package example.imageviewer.core

import androidx.compose.ui.graphics.ImageBitmap

fun createEmptyBitmap(): ImageBitmap = ImageBitmap(1,1)

interface BitmapFilter {
    fun apply(bitmap: ImageBitmap): ImageBitmap
}
