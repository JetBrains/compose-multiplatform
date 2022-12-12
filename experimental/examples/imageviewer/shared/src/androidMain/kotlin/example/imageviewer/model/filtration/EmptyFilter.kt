package example.imageviewer.model.filtration


import android.graphics.Bitmap
import example.imageviewer.core.BitmapFilter

class EmptyFilter : BitmapFilter {

    override fun apply(bitmap: Bitmap): Bitmap {
        return bitmap
    }
}