package example.imageviewer.model

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList

val globalPictures: SnapshotStateList<PictureData> =//todo not global
    mutableStateListOf(*resourcePictures.toTypedArray())

sealed interface PictureData {
    val name: String
    val description: String
    val geo: GeoPos
}

class GeoPos(
    val latitude: Double,
    val longitude: Double,
)

class InMemoryPicture(
    override val name: String,
    override val description: String,
    override val geo: GeoPos
) : PictureData

class DiskPicture(
    val fileName: String,
    val name: String,
    val description: String,
    val geo: GeoPos
)
