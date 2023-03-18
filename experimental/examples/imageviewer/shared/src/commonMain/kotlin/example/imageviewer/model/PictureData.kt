package example.imageviewer.model

import kotlinx.serialization.Serializable

@Serializable
data class GpsPosition(
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
        override val name: String,
        override val description: String,
        override val gps: GpsPosition
    ) : PictureData

    @Serializable
    data class Camera(
        val fileName: String,
        override val name: String,
        override val description: String,
        override val gps: GpsPosition
    ) : PictureData

}
