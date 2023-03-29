package example.imageviewer

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import java.util.*

actual fun Modifier.notchPadding(): Modifier = Modifier.padding(top = 12.dp)

class DesktopStorableImage(
    val imageBitmap: ImageBitmap
)

actual typealias PlatformStorableImage = DesktopStorableImage

actual fun createUUID(): String = UUID.randomUUID().toString()

actual val ioDispatcher = Dispatchers.IO

@Composable
internal actual fun memoryWarningFlow(): Flow<Unit> = emptyFlow()
