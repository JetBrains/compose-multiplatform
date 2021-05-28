package common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import util.toAwtImage
import java.awt.image.BufferedImage

val LocalAppResources = staticCompositionLocalOf<AppResources> {
    error("LocalNotepadResources isn't provided")
}

@Composable
fun rememberAppResources(): AppResources {
    val resources = remember { AppResources() }

    LaunchedEffect(Unit) {
        resources.init()
    }

    return resources
}

class AppResources {
    var icon: BufferedImage? by mutableStateOf(null)
        private set

    suspend fun init() {
        icon = Icons.Default.Description.toAwtImage(Color(0xFF2CA4E1))
    }
}