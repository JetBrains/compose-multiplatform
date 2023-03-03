package example.imageviewer

import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.*

actual fun Modifier.notchPadding():Modifier = displayCutoutPadding().statusBarsPadding()
