package example.imageviewer.model.filtration


import java.awt.image.BufferedImage
import example.imageviewer.core.BitmapFilter

class EmptyFilter : BitmapFilter {

    override fun apply(bitmap: BufferedImage): BufferedImage {
        return bitmap
    }
}