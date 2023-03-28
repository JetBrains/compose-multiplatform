package example.imageviewer

import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp
import java.util.*

actual fun Modifier.notchPadding(): Modifier = Modifier.padding(top = 12.dp)

class DesktopStorableImage(
    val imageBitmap: ImageBitmap
)

actual typealias PlatformStorableImage = DesktopStorableImage

actual fun createUUID():String = UUID.randomUUID().toString()
