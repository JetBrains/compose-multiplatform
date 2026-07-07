package example.imageviewer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import imageviewer.shared.generated.resources.Res
import imageviewer.shared.generated.resources.kotlin_conf_description
import imageviewer.shared.generated.resources.kotlin_conf_name
import imageviewer.shared.generated.resources.new_photo_description
import imageviewer.shared.generated.resources.new_photo_name
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import org.jetbrains.compose.resources.stringResource

class NameAndDescription(
    val name: String,
    val description: String,
)

@Composable
fun createNewPhotoNameAndDescription(): NameAndDescription {
    val kotlinConfName = stringResource(Res.string.kotlin_conf_name)
    val kotlinConfDescription = stringResource(Res.string.kotlin_conf_description, getCurrentPlatform())
    val newPhotoName = stringResource(Res.string.new_photo_name)
    val newPhotoDescription = stringResource(Res.string.new_photo_description)
    return remember(kotlinConfName, kotlinConfDescription, newPhotoName, newPhotoDescription) {
        val kotlinConfEndTime =
            LocalDateTime(2023, Month.APRIL, 14, hour = 23, minute = 59).toInstant(TimeZone.UTC)

        if (Clock.System.now() < kotlinConfEndTime) {
            NameAndDescription(kotlinConfName, kotlinConfDescription)
        } else {
            NameAndDescription(newPhotoName, newPhotoDescription)
        }
    }
}
