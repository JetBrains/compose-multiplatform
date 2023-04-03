package example.imageviewer

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import example.imageviewer.model.PictureData
import kotlinx.coroutines.CoroutineDispatcher

expect fun Modifier.notchPadding(): Modifier

expect class PlatformStorableImage

expect fun createUUID(): String

expect val ioDispatcher: CoroutineDispatcher

expect val isShareFeatureSupported: Boolean

expect val shareIcon: ImageVector
