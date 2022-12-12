package example.imageviewer.core

import java.awt.image.BufferedImage

actual typealias CommonBitmap = BufferedImage

actual fun createEmptyBitmap(): CommonBitmap = BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
