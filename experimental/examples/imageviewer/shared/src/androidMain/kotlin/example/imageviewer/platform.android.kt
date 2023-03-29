package example.imageviewer

import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.Dispatchers
import java.util.*

actual fun Modifier.notchPadding(): Modifier = displayCutoutPadding().statusBarsPadding()

class AndroidStorableImage(
    val imageBitmap: ImageBitmap
)

actual typealias PlatformStorableImage = AndroidStorableImage

actual fun createUUID():String = UUID.randomUUID().toString()

actual val ioDispatcher = Dispatchers.IO
