package example.imageviewer.view

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import example.imageviewer.LocalImageStorage

@Composable
internal fun CameraScreen(onBack: (resetSelectedPhoto: Boolean) -> Unit) {
    val storage = LocalImageStorage.current
    Box(Modifier.fillMaxSize()) {
        CameraView(Modifier.fillMaxSize(), onCapture = { picture, image ->
            storage.saveImage(picture, image)
            onBack(true)
        })
        TopLayout(
            alignLeftContent = {
                BackButton {
                    onBack(false)
                }
            },
            alignRightContent = {},
        )
    }
}
