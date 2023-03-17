package example.imageviewer.view

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import example.imageviewer.ImageStorage
import example.imageviewer.Localization
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalResourceApi::class)
@Composable
internal fun CameraScreen(localization: Localization, storage: ImageStorage, onBack: () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        CameraView(Modifier.fillMaxSize(), storage)
        TopLayout(
            alignLeftContent = {
                Tooltip(localization.back) {
                    CircularButton(
                        painterResource("arrowleft.png"),
                        onClick = { onBack() }
                    )
                }
            },
            alignRightContent = {},
        )
    }
}
