package example.imageviewer.model

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.Painter

val globalPictures: SnapshotStateList<Picture> = mutableStateListOf<Picture>(*resourcePictures.toTypedArray())

sealed interface Picture {
    val name: String
    val description: String
    val geo: GeoPos
}

data class GeoPos(
    val latitude: Double,
    val longitude: Double,
)

data class InMemoryPicture(
    override val name: String,
    override val description: String,
    override val geo: GeoPos
) : Picture

data class DiskPicture(
    val fileName: String,
    val name: String,
    val description: String,
    val geo: GeoPos
)
