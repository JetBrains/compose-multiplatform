package example.imageviewer.view

import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.LayoutDirection
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
import example.imageviewer.utils.decorateWithDiskCache
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

val LocalWindowSize =  staticCompositionLocalOf {
    DpSize.Unspecified
}

@Composable
fun ApplicationScope.ImageViewerDesktop() {
    val state = rememberWindowState()
    CompositionLocalProvider(LocalWindowSize provides state.size) {
        //    if (content.isContentReady()) {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Image Viewer",
            state = WindowState(
                position = WindowPosition.Aligned(Alignment.Center),
                size = getPreferredWindowSize(800, 1000)
            ),
            icon = painterResource("ic_imageviewer_round.png")
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Gray
            ) {
                ImageViewerCommon(dependencies = object : Dependencies {
                    override fun getFilter(type: FilterType): BitmapFilter = when (type) {
                        FilterType.GrayScale -> GrayScaleFilter()
                        FilterType.Pixel -> PixelFilter()
                        FilterType.Blur -> BlurFilter()
                    }

                    override val localization: Localization = object : Localization {
                        override val back: String
                            get() = ResString.back
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
                            showPopUpMessage(ResString.repoInvalid)
                        }

                        override fun notifyRepoIsEmpty() {
                            showPopUpMessage(ResString.repoEmpty)
                        }

                        override fun notifyNoInternet() {
                            showPopUpMessage(ResString.noInternet)
                        }

                        override fun notifyLoadImageUnavailable() {
                            showPopUpMessage("${ResString.noInternet}\n${ResString.loadImageUnavailable}")
                        }

                        override fun notifyLastImage() {
                            showPopUpMessage(ResString.lastImage)
                        }

                        override fun notifyFirstImage() {
                            showPopUpMessage(ResString.firstImage)
                        }

                        override fun notifyRefreshUnavailable() {
                            showPopUpMessage("${ResString.noInternet}\n${ResString.refreshUnavailable}")
                        }

                        override fun notifyImageData(picture: Picture) {
                            showPopUpMessage(
                                """
                                ${ResString.picture} ${picture.name}
                                ${ResString.size} ${picture.width}x${picture.height} ${ResString.pixels}
                            """.trimIndent()
                            )
                        }
                    }
                })
            }
        }
//        } else {
//            Window(
//                onCloseRequest = ::exitApplication,
//                title = "Image Viewer",
//                state = WindowState(
//                    position = WindowPosition.Aligned(Alignment.Center),
//                    size = getPreferredWindowSize(800, 300)
//                ),
//                undecorated = true,
//                icon = icon,
//            ) {
//                SplashUI(content)
//            }
//        }
    }

    Toast(message.value, toastState)
}

fun showPopUpMessage(text: String) {
    message.value = text
    toastState.value = true
}
