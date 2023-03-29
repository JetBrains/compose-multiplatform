package example.imageviewer

import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import java.util.*

actual fun Modifier.notchPadding(): Modifier = displayCutoutPadding().statusBarsPadding()

class AndroidStorableImage(
    val imageBitmap: ImageBitmap
)

actual typealias PlatformStorableImage = AndroidStorableImage

actual fun createUUID():String = UUID.randomUUID().toString()

actual val ioDispatcher = Dispatchers.IO

@Composable
internal actual fun memoryWarningFlow(): Flow<Unit> = emptyFlow()
