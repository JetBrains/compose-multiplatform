import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.rememberImageBitmap
import org.jetbrains.compose.resources.rememberImageBitmapAsync
import org.jetbrains.compose.resources.resource

@OptIn(ExperimentalResourceApi::class)
@Composable
internal fun UseResources() {
    Column {
        Text("Hello, resources")
        val img = resource("img.png").rememberImageBitmapAsync()
        if (img != null) {
            Image(
                bitmap = img,
                contentDescription = null,
            )
        } else {
            Image(
                imageVector = Icons.Filled.Lock,
                contentDescription = null
            )
        }
    }
}
