package example.imageviewer

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.coroutines.Dispatchers
import java.util.UUID

class DesktopStorableImage(
    val imageBitmap: ImageBitmap
)

actual typealias PlatformStorableImage = DesktopStorableImage

actual fun createUUID(): String = UUID.randomUUID().toString()

actual val ioDispatcher = Dispatchers.IO

actual val isShareFeatureSupported: Boolean = false

actual val shareIcon: ImageVector = Icons.Filled.Share
