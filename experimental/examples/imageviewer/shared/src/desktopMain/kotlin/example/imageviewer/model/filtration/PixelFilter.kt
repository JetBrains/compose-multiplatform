package example.imageviewer.model.filtration

import java.awt.image.BufferedImage
import example.imageviewer.core.BitmapFilter
import example.imageviewer.utils.applyPixelFilter

class PixelFilter : BitmapFilter {

    override fun apply(bitmap: BufferedImage): BufferedImage {
        return applyPixelFilter(bitmap)
    }
}