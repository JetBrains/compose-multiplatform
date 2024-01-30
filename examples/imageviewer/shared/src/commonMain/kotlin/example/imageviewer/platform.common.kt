package example.imageviewer

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.coroutines.CoroutineDispatcher

expect class PlatformStorableImage

expect fun createUUID(): String

expect val ioDispatcher: CoroutineDispatcher

expect val isShareFeatureSupported: Boolean

expect val shareIcon: ImageVector
