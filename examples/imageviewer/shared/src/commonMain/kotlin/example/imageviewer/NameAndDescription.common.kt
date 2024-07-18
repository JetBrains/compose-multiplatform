package example.imageviewer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import imageviewer.shared.generated.resources.Res
import imageviewer.shared.generated.resources.newPhotoDescription
import imageviewer.shared.generated.resources.newPhotoName
import org.jetbrains.compose.resources.stringResource

class NameAndDescription(
    val name: String,
    val description: String,
)

@Composable
fun createNewPhotoNameAndDescription(): NameAndDescription {
    val name = stringResource(Res.string.newPhotoName)
    val description = stringResource(Res.string.newPhotoDescription)
    return remember {
        NameAndDescription(
            name,
            description
        )
    }
}
