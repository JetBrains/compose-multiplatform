package example.imageviewer

import androidx.compose.runtime.Composable
import imageviewer.shared.generated.resources.Res
import imageviewer.shared.generated.resources.new_photo_description
import imageviewer.shared.generated.resources.new_photo_name
import org.jetbrains.compose.resources.stringResource

class NameAndDescription(
    val name: String,
    val description: String,
)

@Composable
fun createNewPhotoNameAndDescription(): NameAndDescription {
    val newPhotoName = stringResource(Res.string.new_photo_name)
    val newPhotoDescription = stringResource(Res.string.new_photo_description)
    return NameAndDescription(newPhotoName, newPhotoDescription)
}
