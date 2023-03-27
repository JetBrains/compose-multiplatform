package example.imageviewer.view

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import example.imageviewer.ImageStorageLocal

@Composable
internal fun CameraScreen(onBack: () -> Unit) {
    val storage = ImageStorageLocal.current
    Box(Modifier.fillMaxSize()) {
        CameraView(Modifier.fillMaxSize(), onCapture = { picture, image ->
            storage.saveImage(picture, image)
            onBack()
        })
        TopLayout(
            alignLeftContent = {
                BackButton(onBack)
            },
            alignRightContent = {},
        )
    }
}
