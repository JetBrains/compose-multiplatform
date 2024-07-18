package example.imageviewer

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import example.imageviewer.filter.PlatformContext
import example.imageviewer.model.PictureData
import example.imageviewer.storage.IosImageStorage
import example.imageviewer.style.ImageViewerTheme
import example.imageviewer.view.Toast
import example.imageviewer.view.ToastState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIImage
import platform.UIKit.UIWindow

@Composable
internal fun ImageViewerIos() {
    val toastState = remember { mutableStateOf<ToastState>(ToastState.Hidden) }
    val ioScope: CoroutineScope = rememberCoroutineScope { ioDispatcher }
    val dependencies = remember(ioScope) {
        getDependencies(ioScope, toastState)
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

fun getDependencies(ioScope: CoroutineScope, toastState: MutableState<ToastState>) =
    object : Dependencies() {
        override val notification: Notification = object : PopupNotification() {
            override fun showPopUpMessage(text: String) {
                toastState.value = ToastState.Shown(text)
            }
        }

        override val imageStorage: IosImageStorage = IosImageStorage(pictures, ioScope)

        override val sharePicture: SharePicture = object : SharePicture {
            override fun share(context: PlatformContext, picture: PictureData) {
                ioScope.launch {
                    imageStorage.getNSURLToShare(picture).path?.let { imageUrl ->
                        withContext(Dispatchers.Main) {
                            val window = UIApplication.sharedApplication.windows.last() as? UIWindow
                            val currentViewController = window?.rootViewController
                            val activityViewController = UIActivityViewController(
                                activityItems = listOf(
                                    UIImage.imageWithContentsOfFile(imageUrl),
                                    picture.description
                                ),
                                applicationActivities = null
                            )
                            currentViewController?.presentViewController(
                                viewControllerToPresent = activityViewController,
                                animated = true,
                                completion = null,
                            )
                        }
                    }
                }
            }
        }
    }
