package example.imageviewer

import androidx.compose.ui.Modifier
import example.imageviewer.model.PictureData
import kotlinx.coroutines.CoroutineDispatcher

expect fun Modifier.notchPadding(): Modifier

expect class PlatformStorableImage

expect fun createUUID(): String

expect val ioDispatcher: CoroutineDispatcher
