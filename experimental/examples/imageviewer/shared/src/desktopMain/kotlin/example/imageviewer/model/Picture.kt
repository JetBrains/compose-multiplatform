package example.imageviewer.model

import java.awt.image.BufferedImage

actual data class Picture(
    var source: String = "",
    var name: String = "",
    var image: BufferedImage,
    var width: Int = 0,
    var height: Int = 0,
    var id: Int = 0
)