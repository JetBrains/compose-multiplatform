package example.imageviewer.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import example.imageviewer.LocalImageStorage
import kotlinx.coroutines.delay

@Composable
internal fun CameraScreen(onBack: (resetSelectedPicture: Boolean) -> Unit) {
    val storage = LocalImageStorage.current
    var showCamera by remember { mutableStateOf(false) }
    LaunchedEffect(onBack) {
        if (!showCamera) {
            delay(300) // for animation
            showCamera = true
        }
    }
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (showCamera) {
            CameraView(Modifier.fillMaxSize(), onCapture = { picture, image ->
                storage.saveImage(picture, image)
                onBack(true)
            })
        }
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
