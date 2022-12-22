package example.imageviewer.view

import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.window.*
import example.imageviewer.*
import example.imageviewer.core.BitmapFilter
import example.imageviewer.core.FilterType
import example.imageviewer.model.*
import example.imageviewer.model.Notification
import example.imageviewer.model.State
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

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ApplicationScope.ImageViewerDesktop() {
    val toastState = remember { mutableStateOf<ToastState>(ToastState.Hidden) }
    val state = remember { mutableStateOf(State()) }
    val ioScope: CoroutineScope = rememberCoroutineScope { Dispatchers.IO }
    val dependencies = remember(ioScope) { getDependencies(ioScope, toastState) }

    Window(
        onCloseRequest = ::exitApplication,
        title = "Image Viewer",
        state = WindowState(
            position = WindowPosition.Aligned(Alignment.Center),
            size = getPreferredWindowSize(800, 1000)
        ),
        icon = painterResource("ic_imageviewer_round.png"),
        onKeyEvent = {
            if (it.type == KeyEventType.KeyUp) {
                when (it.key) {
                    Key.DirectionLeft -> state.previousImage()
                    Key.DirectionRight -> state.nextImage()
                }
            }
            false
        }
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Gray
        ) {
            ImageViewerCommon(
                state = state,
                dependencies = dependencies
            )
            Toast(toastState)
        }
    }

}

private fun getDependencies(ioScope: CoroutineScope, toastState: MutableState<ToastState>) = object : Dependencies {
    override val ioScope: CoroutineScope = ioScope
    override fun getFilter(type: FilterType): BitmapFilter = when (type) {
        FilterType.GrayScale -> GrayScaleFilter()
        FilterType.Pixel -> PixelFilter()
        FilterType.Blur -> BlurFilter()
    }

    override val localization: Localization = object : Localization {
        override val back: String get() = ResString.back
        override val appName: String get() = ResString.appName
        override val loading: String get() = ResString.loading
        override val repoInvalid: String get() = ResString.repoInvalid
        override val repoEmpty: String get() = ResString.repoEmpty
        override val noInternet: String get() = ResString.noInternet
        override val loadImageUnavailable: String get() = ResString.loadImageUnavailable
        override val lastImage: String get() = ResString.lastImage
        override val firstImage: String get() = ResString.firstImage
        override val picture: String get() = ResString.picture
        override val size: String get() = ResString.size
        override val pixels: String get() = ResString.pixels
        override val refreshUnavailable: String get() = ResString.refreshUnavailable
    }

    override val httpClient: HttpClient = HttpClient(CIO)

    override val imageRepository: ContentRepository<ImageBitmap> =
        createRealRepository(httpClient)
            .decorateWithDiskCache(
                ioScope,
                File(System.getProperty("user.home")!!).resolve("Pictures").resolve("imageviewer")
            )
            .adapter { it.toImageBitmap() }

    override val notification: Notification = object : PopupNotification(localization) {
        override fun showPopUpMessage(text: String) {
            toastState.value = ToastState.Shown(text)
        }
    }
}

sealed interface ToastState {
    object Hidden : ToastState
    class Shown(val message: String):ToastState
}
