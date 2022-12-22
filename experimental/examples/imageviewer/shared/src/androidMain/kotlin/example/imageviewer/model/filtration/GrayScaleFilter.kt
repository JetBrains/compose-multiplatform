package example.imageviewer.model.filtration

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import example.imageviewer.core.BitmapFilter
import example.imageviewer.utils.applyGrayScaleFilter

class GrayScaleFilter : BitmapFilter {

    override fun apply(bitmap: ImageBitmap): ImageBitmap =
        applyGrayScaleFilter(bitmap.asAndroidBitmap()).asImageBitmap()
}
