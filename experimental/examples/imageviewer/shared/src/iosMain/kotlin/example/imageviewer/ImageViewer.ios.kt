package example.imageviewer

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import example.imageviewer.storage.IosImageStorage
import example.imageviewer.style.ImageViewerTheme
import example.imageviewer.view.Toast
import example.imageviewer.view.ToastState
import kotlinx.coroutines.CoroutineScope

@Composable
internal fun ImageViewerIos(openShareController: (SharedPhoto) -> Unit) {
    val toastState = remember { mutableStateOf<ToastState>(ToastState.Hidden) }
    val ioScope: CoroutineScope = rememberCoroutineScope { ioDispatcher }
    val dependencies = remember(ioScope) {
        getDependencies(ioScope, toastState, openShareController)
    }

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

fun getDependencies(
    ioScope: CoroutineScope,
    toastState: MutableState<ToastState>,
    openShareController: (SharedPhoto) -> Unit
) =
    object : Dependencies() {
        override val notification: Notification = object : PopupNotification(localization) {
            override fun showPopUpMessage(text: String) {
                toastState.value = ToastState.Shown(text)
            }
        }
        override val imageStorage: ImageStorage = IosImageStorage(pictures, ioScope)

        override val openShareController: (SharedPhoto) -> Unit = { sharedPhoto ->
            openShareController(sharedPhoto)
        }
    }
