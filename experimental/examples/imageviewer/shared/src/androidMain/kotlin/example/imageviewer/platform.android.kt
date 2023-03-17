package example.imageviewer

import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap

actual fun Modifier.notchPadding(): Modifier = displayCutoutPadding().statusBarsPadding()

class AndroidStorableImage(
    val imageBitmap: ImageBitmap
)

actual typealias PlatformStorableImage = AndroidStorableImage
