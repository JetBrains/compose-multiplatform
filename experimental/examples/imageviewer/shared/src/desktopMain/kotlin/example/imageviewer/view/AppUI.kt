package example.imageviewer.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Surface
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.window.*
import example.imageviewer.*
import example.imageviewer.core.BitmapFilter
import example.imageviewer.core.FilterType
import example.imageviewer.model.*
import example.imageviewer.model.Notification
import example.imageviewer.model.filtration.BlurFilter
import example.imageviewer.model.filtration.GrayScaleFilter
import example.imageviewer.model.filtration.PixelFilter
import example.imageviewer.style.Gray
import example.imageviewer.style.icAppRounded
import example.imageviewer.utils.getPreferredWindowSize
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.io.File

private val message: MutableState<String> = mutableStateOf("")
private val toastState: MutableState<Boolean> = mutableStateOf(false)
val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)//todo

@Composable
fun ApplicationScope.ImageViewerDesktop() {
    val state = rememberWindowState()
    val icon = icAppRounded()
//    if (content.isContentReady()) {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Image Viewer",
            state = WindowState(
                position = WindowPosition.Aligned(Alignment.Center),
                size = getPreferredWindowSize(800, 1000)
            ),
            icon = icon
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Gray
            ) {
                ImageViewerCommon(dependencies = object : Dependencies {
                    override fun getFilter(type: FilterType): BitmapFilter = when(type) {
                        FilterType.GrayScale -> GrayScaleFilter()
                        FilterType.Pixel -> PixelFilter()
                        FilterType.Blur -> BlurFilter()
                    }
                    override val localization: Localization = object : Localization {
                        override val appName: String
                            get() = ResString.appName
                        override val loading: String
                            get() = ResString.loading
                    }
                    override val imageRepository: ContentRepository<NetworkRequest, ImageBitmap> =
                        createRealRepository(HttpClient(CIO))
                            .decorateWithDiskCache(
                                ioScope,
                                File(System.getProperty("user.home")!!).resolve("Pictures").resolve("imageviewer")
                            )
                            .adapter { it.toImageBitmap() }

                    override val notification: Notification = object : Notification {
                        override fun notifyInvalidRepo() {
                            showPopUpMessage("notifyInvalidRepo")
                        }

                        override fun notifyRepoIsEmpty() {
                            showPopUpMessage("notifyRepoIsEmpty")
                        }

                        override fun notifyNoInternet() {
                            showPopUpMessage("notifyNoInternet")
                        }

                        override fun notifyLoadImageUnavailable() {
                            showPopUpMessage("notifyLoadImageUnavailable")
                        }

                        override fun notifyLastImage() {
                            showPopUpMessage("notifyLastImage")
                        }

                        override fun notifyFirstImage() {
                            showPopUpMessage("notifyFirstImage")
                        }

                        override fun notifyRefreshUnavailable() {
                            showPopUpMessage("notifyRefreshUnavailable")
                        }

                        override fun notifyImageData(picture: Picture) {
                            showPopUpMessage("notifyImageData(picture: Pictur")
                        }
                    }
                })
            }
        }
//    } else {
//        Window(
//            onCloseRequest = ::exitApplication,
//            title = "Image Viewer",
//            state = WindowState(
//                position = WindowPosition.Aligned(Alignment.Center),
//                size = getPreferredWindowSize(800, 300)
//            ),
//            undecorated = true,
//            icon = icon,
//        ) {
//            SplashUI(content)
//        }
//    }

    Toast(message.value, toastState)
}

fun showPopUpMessage(text: String) {
    message.value = text
    toastState.value = true
}
