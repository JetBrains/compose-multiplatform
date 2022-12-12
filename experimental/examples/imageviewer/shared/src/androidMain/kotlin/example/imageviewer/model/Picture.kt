package example.imageviewer.model

import android.graphics.Bitmap

actual data class Picture(
    val source: String = "",
    val name: String = "",
    val image: Bitmap,
    val width: Int = 0,
    val height: Int = 0,
    val id: Int = 0
)