package example.imageviewer

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import example.imageviewer.utils.UUID
import kotlinx.coroutines.Dispatchers

class WebStorableImage(
    val imageBitmap: ImageBitmap
)

actual typealias PlatformStorableImage = WebStorableImage

actual val ioDispatcher = Dispatchers.Default

actual val isShareFeatureSupported: Boolean = false

actual val shareIcon: ImageVector = Icons.Filled.Share

actual fun createUUID(): String = UUID.v4()