package example.imageviewer.model

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList

class GeoPos(
    val latitude: Double,
    val longitude: Double,
)

sealed interface PictureData {
    val name: String
    val description: String
    val geo: GeoPos
}

class DiskPicture(
    val fileName: String,
    override val name: String,
    override val description: String,
    override val geo: GeoPos
) : PictureData

class ResourcePicture(
    val resource: String,
    override val name: String,
    override val description: String,
    override val geo: GeoPos
) : PictureData
