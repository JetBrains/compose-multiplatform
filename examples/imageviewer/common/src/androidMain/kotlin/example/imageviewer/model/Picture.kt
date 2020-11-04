package example.imageviewer.model

import android.graphics.Bitmap

actual data class Picture(
    var source: String = "",
    var name: String = "",
    var image: Bitmap,
    var width: Int = 0,
    var height: Int = 0,
    var id: Int = 0
)