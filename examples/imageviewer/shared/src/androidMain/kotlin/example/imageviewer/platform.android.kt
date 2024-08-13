package example.imageviewer

import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.coroutines.Dispatchers
import java.util.UUID

class AndroidStorableImage(
    val imageBitmap: ImageBitmap
)

actual typealias PlatformStorableImage = AndroidStorableImage

actual fun createUUID(): String = UUID.randomUUID().toString()

actual val ioDispatcher = Dispatchers.IO

actual val isShareFeatureSupported: Boolean = true

actual val shareIcon: ImageVector = Icons.Filled.Share
