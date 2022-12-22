package example.imageviewer.model.filtration

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import example.imageviewer.core.BitmapFilter
import example.imageviewer.utils.applyPixelFilter

class PixelFilter : BitmapFilter {
    override fun apply(bitmap: ImageBitmap): ImageBitmap =
        applyPixelFilter(bitmap.asAndroidBitmap()).asImageBitmap()
}
