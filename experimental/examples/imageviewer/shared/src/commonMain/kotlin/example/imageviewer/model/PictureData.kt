package example.imageviewer.model

class GpsPosition(
    val latitude: Double,
    val longitude: Double
)

sealed interface PictureData {
    val name: String
    val description: String
    val gps: GpsPosition
    val dateString: String

    data class Resource(
        val resource: String,
        val thumbnailResource: String,
        override val name: String,
        override val description: String,
        override val gps: GpsPosition,
        override val dateString: String,
    ) : PictureData
}
