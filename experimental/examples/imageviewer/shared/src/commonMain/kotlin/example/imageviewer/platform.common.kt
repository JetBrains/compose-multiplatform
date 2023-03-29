package example.imageviewer

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow

expect fun Modifier.notchPadding(): Modifier

expect class PlatformStorableImage

expect fun createUUID():String

expect val ioDispatcher: CoroutineDispatcher

@Composable
internal expect fun memoryWarningFlow(): Flow<Unit>
