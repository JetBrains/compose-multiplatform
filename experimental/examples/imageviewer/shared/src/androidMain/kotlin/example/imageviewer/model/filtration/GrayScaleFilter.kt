package example.imageviewer.model.filtration

import android.graphics.Bitmap
import example.imageviewer.core.BitmapFilter
import example.imageviewer.utils.applyGrayScaleFilter

class GrayScaleFilter : BitmapFilter {

    override fun apply(bitmap: Bitmap) : Bitmap {
        return  applyGrayScaleFilter(bitmap)
    }
}