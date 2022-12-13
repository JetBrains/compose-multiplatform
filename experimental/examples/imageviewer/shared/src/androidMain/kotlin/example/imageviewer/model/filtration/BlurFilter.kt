package example.imageviewer.model.filtration

import android.content.Context
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import example.imageviewer.core.BitmapFilter
import example.imageviewer.utils.applyBlurFilter

class BlurFilter(private val context: Context) : BitmapFilter {

    override fun apply(bitmap: ImageBitmap): ImageBitmap =
        applyBlurFilter(bitmap.asAndroidBitmap(), context).asImageBitmap()
}
