package example.imageviewer.core

import java.awt.image.BufferedImage

interface BitmapFilter {
    fun apply(bitmap: BufferedImage) : BufferedImage
}