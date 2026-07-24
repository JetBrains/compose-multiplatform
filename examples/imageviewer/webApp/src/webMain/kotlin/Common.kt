import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import example.imageviewer.*
import example.imageviewer.style.ImageViewerTheme
import example.imageviewer.ioDispatcher
import example.imageviewer.view.Toast
import example.imageviewer.view.ToastState
import kotlinx.coroutines.CoroutineScope

@Composable
internal fun ImageViewerWeb() {
    val toastState = remember { mutableStateOf<ToastState>(ToastState.Hidden) }
    val ioScope: CoroutineScope = rememberCoroutineScope { ioDispatcher }
    val dependencies = remember(ioScope) { getDependencies(toastState) }

    ImageViewerTheme {
        Surface(
            modifier = Modifier.fillMaxSize()
        ) {
            ImageViewerCommon(
                dependencies = dependencies
            )
            Toast(toastState)
        }
    }
}

fun getDependencies(toastState: MutableState<ToastState>) = object : Dependencies() {
    override val imageStorage: ImageStorage = WebImageStorage()
    override val sharePicture = WebSharePicture()
    override val notification = WebPopupNotification(toastState, localization)
}
