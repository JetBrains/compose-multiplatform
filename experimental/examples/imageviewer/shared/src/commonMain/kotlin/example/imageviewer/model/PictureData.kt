package example.imageviewer.model

import example.imageviewer.createUUID
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable

@Serializable
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

    @Serializable
    data class Camera(
        val id: String,
        val timeStampSeconds: Long,
        override val name: String,
        override val description: String,
        override val gps: GpsPosition,
    ) : PictureData {
        override val dateString: String
            get(): String {
                val instantTime = Instant.fromEpochSeconds(timeStampSeconds, 0)
                val utcTime = instantTime.toLocalDateTime(TimeZone.UTC)
                val date = utcTime.date
                val monthStr = date.month.name.lowercase()
                    .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                    .take(3)
                val dayStr = date.dayOfMonth
                return "$dayStr $monthStr."
            }
    }
}

fun createCameraPictureData(name: String, description: String, gps: GpsPosition) =
    PictureData.Camera(
        id = createUUID(),
        timeStampSeconds = Clock.System.now().epochSeconds,
        name = name,
        description = description,
        gps = gps,
    )
