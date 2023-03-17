package example.imageviewer.model

class GeoPos(
    val latitude: Double,
    val longitude: Double,
    val timeStamp: Long = 0,//todo
)

sealed interface PictureData {
    val name: String
    val description: String
    val geo: GeoPos

    class Resource(
        val resource: String,
        override val name: String,
        override val description: String,
        override val geo: GeoPos
    ) : PictureData

    class Camera(
        val fileName: String,
        override val name: String,
        override val description: String,
        override val geo: GeoPos
    ) : PictureData

}
