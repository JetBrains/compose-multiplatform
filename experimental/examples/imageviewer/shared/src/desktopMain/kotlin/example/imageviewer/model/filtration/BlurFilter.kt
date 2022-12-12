package example.imageviewer.model.filtration

import java.awt.image.BufferedImage
import example.imageviewer.core.BitmapFilter
import example.imageviewer.utils.applyBlurFilter

class BlurFilter : BitmapFilter {

    override fun apply(bitmap: BufferedImage): BufferedImage {
        return applyBlurFilter(bitmap)
    }
}