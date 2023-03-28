package example.imageviewer

import androidx.compose.ui.Modifier

expect fun Modifier.notchPadding(): Modifier

expect class PlatformStorableImage

expect fun createUUID():String
