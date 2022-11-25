package example.imageviewer.model.filtration

import android.graphics.Bitmap
import example.imageviewer.core.BitmapFilter
import example.imageviewer.utils.applyPixelFilter

class PixelFilter : BitmapFilter {

    override fun apply(bitmap: Bitmap): Bitmap {
        return applyPixelFilter(bitmap)
    }
}