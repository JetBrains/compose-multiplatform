package example.imageviewer.model.filtration

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.graphics.toComposeImageBitmap
import example.imageviewer.core.BitmapFilter
import example.imageviewer.utils.applyBlurFilter

class BlurFilter : BitmapFilter {

    override fun apply(bitmap: ImageBitmap): ImageBitmap {
        return applyBlurFilter(bitmap.toAwtImage()).toComposeImageBitmap()
    }
}
