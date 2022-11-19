import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.rememberImageBitmap
import org.jetbrains.compose.resources.resource

@OptIn(ExperimentalResourceApi::class)
@Composable
internal fun UseResources() {
    Column {
        Image(
            bitmap = resource("img.png").rememberImageBitmap(),
            contentDescription = null,
        )
    }
}
