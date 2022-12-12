package example.imageviewer.model.filtration

import android.content.Context
import android.graphics.Bitmap
import example.imageviewer.core.BitmapFilter
import example.imageviewer.utils.applyBlurFilter

class BlurFilter(private val context: Context) : BitmapFilter {

    override fun apply(bitmap: Bitmap): Bitmap {
        return applyBlurFilter(bitmap, context)
    }
}