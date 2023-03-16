package example.imageviewer.model

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.Painter

interface Picture {
    suspend fun getImageBitmap(): ImageBitmap
    val name: String
    val description: String
    val geo: GeoPos
    @Composable
    fun thumbnail(): Painter
}

data class GeoPos(
    val latitude: Double,
    val longitude: Double,
)

class InMemoryPicture(
    val bitmap: ImageBitmap,
    val name: String,
    val description: String,
    val geo: GeoPos
)
