package example.imageviewer.model

import kotlinx.serialization.Serializable

@Serializable
class GpsPosition(
    val latitude: Double,
    val longitude: Double,
    val timeStamp: Long = 0,//todo
)

sealed interface PictureData {
    val name: String
    val description: String
    val gps: GpsPosition

    class Resource(
        val resource: String,
        val thumbnailResource: String,
        override val name: String,
        override val description: String,
        override val gps: GpsPosition
    ) : PictureData

    @Serializable
    class Camera(
        val id: String,
        override val name: String,
        override val description: String,
        override val gps: GpsPosition
    ) : PictureData {
        override fun equals(other: Any?): Boolean = (other as? Camera)?.id == id
        override fun hashCode(): Int = id.hashCode()
    }

}
